package kz.ilotterytea.bot.clients;

import com.github.twitch4j.helix.domain.Chatter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.SharedConstants;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.entities.channels.ChannelFeature;
import kz.ilotterytea.bot.entities.events.Event;
import kz.ilotterytea.bot.entities.events.EventFlag;
import kz.ilotterytea.bot.i18n.LineIds;
import kz.ilotterytea.bot.utils.HibernateUtil;
import kz.ilotterytea.bot.utils.StringUtils;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class GithubListenerClient {
    private record Commit(String author, String message, String sha) {
    }

    private final Logger log = LoggerFactory.getLogger(GithubListenerClient.class);
    private final String token;

    private final HashMap<String, ArrayList<String>> commits;

    public GithubListenerClient() {
        this.token = SharedConstants.PROPERTIES.getProperty("github.token");

        this.commits = new HashMap<>();
    }

    public void run() {
        if (this.token == null || this.token.isEmpty()) {
            log.info("Unable to run Github client because the token is empty or null!");
            return;
        }

        new Timer().schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    checkNewRepositories();
                    HashMap<String, ArrayList<Commit>> commits = checkNewCommits();
                    notifyCommits(commits);
                    for (Map.Entry<String, ArrayList<Commit>> commit : commits.entrySet()) {
                        GithubListenerClient.this.commits.get(commit.getKey())
                                .addAll(commit.getValue()
                                        .stream()
                                        .map(Commit::sha)
                                        .toList()
                                );
                    }
                } catch (Exception e) {
                    log.error("An exception was thrown while processing GitHub commits", e);
                }
            }
        }, 10000, 10000);
    }

    private void checkNewRepositories() {
        Session session = HibernateUtil.getSessionFactory().openSession();
        List<Event> events = session.createQuery("from Event where eventType = GITHUB", Event.class).getResultList();
        session.close();

        // Adding new repos
        for (Event event : events) {
            if (commits.containsKey(event.getCustomAliasId())) continue;
            commits.put(event.getCustomAliasId(), new ArrayList<>());
            log.info("Started listening for {}'s commits...", event.getCustomAliasId());
        }

        // Removing old repos
        for (String repo : commits.keySet()) {
            if (events.stream().anyMatch((x) -> x.getCustomAliasId().equals(repo))) continue;
            commits.remove(repo);
            log.info("Stopped listening for {}'s commits", repo);
        }
    }

    private HashMap<String, ArrayList<Commit>> checkNewCommits() {
        HashMap<String, ArrayList<Commit>> newCommits = new HashMap<>();

        for (Map.Entry<String, ArrayList<String>> entry : this.commits.entrySet()) {
            Request httpRequest = new Request.Builder()
                    .get()
                    .url(String.format("https://api.github.com/repos/%s/commits", entry.getKey()))
                    .addHeader("Accept", "application/vnd.github+json")
                    .addHeader("X-GitHub-Api-Version", "2022-11-28")
                    .addHeader("Authorization", "Bearer " + token)
                    .build();

            OkHttpClient client = new OkHttpClient();

            try (Response response = client.newCall(httpRequest).execute()) {
                if (response.code() != 200 || response.body() == null) continue;

                ArrayList<Commit> newCommitList = new ArrayList<>();
                JsonArray commits = JsonParser.parseString(response.body().string()).getAsJsonArray();

                for (JsonElement element : commits) {
                    JsonObject commit = element.getAsJsonObject();

                    String sha = commit.get("sha").getAsString();
                    if (entry.getValue().contains(sha) || commit.get("author").isJsonNull() || commit.get("commit").isJsonNull())
                        continue;

                    String authorName = commit.get("author").getAsJsonObject().get("login").getAsString();
                    String message = commit.get("commit").getAsJsonObject().get("message").getAsString();

                    Commit newCommit = new Commit(authorName, message, sha);
                    newCommitList.add(newCommit);
                }

                if (newCommitList.isEmpty()) continue;
                Collections.reverse(newCommitList);
                newCommits.put(entry.getKey(), newCommitList);
            } catch (Exception e) {
                log.error("An exception was thrown while getting commits", e);
            }
        }

        return newCommits;
    }

    private void notifyCommits(HashMap<String, ArrayList<Commit>> commits) {
        Session session = HibernateUtil.getSessionFactory().openSession();

        // three nested for-loop algorithm pffffttt
        for (Map.Entry<String, ArrayList<Commit>> commit : commits.entrySet()) {
            if (this.commits.get(commit.getKey()).isEmpty()) continue;

            List<Event> events = session.createQuery("from Event where customAliasId = :customAliasId and eventType = GITHUB", Event.class)
                    .setParameter("customAliasId", commit.getKey())
                    .getResultList();

            for (Event event : events) {
                Channel channel = event.getChannel();
                if (channel.getPreferences().getFeatures().contains(ChannelFeature.SILENT_MODE)) {
                    continue;
                }

                Hibernate.initialize(event.getSubscriptions());

                final Set<String> names = event.getSubscriptions()
                        .stream()
                        .map(it -> it.getUser().getAliasName())
                        .collect(Collectors.toSet());

                if (event.getFlags().contains(EventFlag.MASSPING)) {
                    try {
                        List<Chatter> chatters = Huinyabot.getInstance().getClient().getHelix()
                                .getChatters(
                                        SharedConstants.TWITCH_TOKEN,
                                        channel.getAliasId().toString(),
                                        Huinyabot.getInstance().getCredential().getUserId(),
                                        1000,
                                        null
                                )
                                .execute()
                                .getChatters();

                        names.addAll(chatters
                                .stream()
                                .map(Chatter::getUserLogin)
                                .collect(Collectors.toSet()));
                    } catch (Exception e) {
                        log.error("Failed to get a list of chatters", e);
                    }
                }

                for (Commit c : commit.getValue()) {
                    String line = Huinyabot.getInstance().getLocale().formattedText(
                            channel.getPreferences().getLanguage(),
                            LineIds.EVENTS_MESSAGE,
                            event.getMessage()
                                    .replace("%{sha}", c.sha().substring(0, 7))
                                    .replace("%{author}", c.author())
                                    .replace("%{message}", c.message())
                    );

                    if (names.isEmpty()) {
                        Huinyabot.getInstance().getClient().getChat().sendMessage(
                                channel.getAliasName(),
                                line
                        );
                        continue;
                    }

                    final List<String> formattedNameList = StringUtils.joinStringsWithFixedLength(
                            ", ",
                            names.stream().map(it -> "@" + it).collect(Collectors.toList()),
                            500 - line.length()
                    );

                    for (String formattedName : formattedNameList) {
                        Huinyabot.getInstance().getClient().getChat().sendMessage(
                                channel.getAliasName(),
                                String.format("%s%s%s",
                                        line,
                                        Huinyabot.getInstance().getLocale().literalText(
                                                channel.getPreferences().getLanguage(),
                                                LineIds.EVENTS_MESSAGE_SUFFIX
                                        ),
                                        formattedName
                                )
                        );
                    }
                }
            }
        }

        session.close();
    }
}
