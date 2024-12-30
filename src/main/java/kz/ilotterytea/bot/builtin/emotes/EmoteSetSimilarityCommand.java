package kz.ilotterytea.bot.builtin.emotes;

import com.github.ilotterytea.emotes4j.seventv.api.SevenTVAPIClient;
import com.github.ilotterytea.emotes4j.seventv.api.emotes.Emote;
import com.github.ilotterytea.emotes4j.seventv.api.emotes.EmoteSet;
import com.github.ilotterytea.emotes4j.seventv.api.users.User;
import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.api.commands.*;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.i18n.LineIds;
import kz.ilotterytea.bot.utils.ParsedMessage;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Emote set similarity command.
 *
 * @author ilotterytea
 * @since 1.4
 */
public class EmoteSetSimilarityCommand implements Command {
    @Override
    public String getNameId() {
        return "esimilarity";
    }

    @Override
    public List<String> getAliases() {
        return Collections.singletonList("esim");
    }

    @Override
    public Response run(Request request) throws Exception {
        ParsedMessage message = request.getMessage();
        Channel channel = request.getChannel();

        if (message.getMessage().isEmpty()) {
            throw CommandException.notEnoughArguments(request, CommandArgument.VALUE);
        }

        // Setting the origin and target channels:
        String[] s = message.getMessage().get().split(" ");
        String originChannel;
        String targetChannel;

        if (s.length == 1) {
            originChannel = channel.getAliasName();
            targetChannel = s[0];
        } else {
            originChannel = s[0];
            targetChannel = s[1];
        }

        if (originChannel.equals(targetChannel)) {
            throw CommandException.incorrectArgument(request, CommandArgument.VALUE, targetChannel);
        }

        // Getting Twitch users:
        List<com.github.twitch4j.helix.domain.User> userList = Huinyabot.getInstance().getClient()
                .getHelix()
                .getUsers(
                        Huinyabot.getInstance().getCredential().getAccessToken(),
                        null,
                        List.of(originChannel, targetChannel)
                )
                .execute()
                .getUsers();

        if (userList.size() <= 1) {
            throw CommandException.notFound(request, originChannel + " | " + targetChannel);
        }

        com.github.twitch4j.helix.domain.User originUser = userList.stream().filter(p -> p.getLogin().equals(originChannel)).findFirst().get();
        com.github.twitch4j.helix.domain.User targetUser = userList.stream().filter(p -> p.getLogin().equals(targetChannel)).findFirst().get();

        // Getting emote sets:
        Optional<User> originSTVUser = SevenTVAPIClient.getUser(originUser.getId());
        Optional<User> targetSTVUser = SevenTVAPIClient.getUser(targetUser.getId());

        if (originSTVUser.isEmpty() || targetSTVUser.isEmpty()) {
            throw CommandException.notFound(request, originChannel + " | " + targetChannel);
        }

        EmoteSet originEmoteSet = originSTVUser.get().getEmoteSet();
        EmoteSet targetEmoteSet = targetSTVUser.get().getEmoteSet();
        int similarity = 0;

        // Comparing emote set:
        for (Emote emote : originEmoteSet.getEmotes()) {
            if (targetEmoteSet.getEmotes().stream().anyMatch(e -> e.getId().equals(emote.getId()))) {
                similarity += 1;
            }
        }

        if (similarity == 0) {
            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_ESIMILARITY_NOSIMILARITY,
                    Huinyabot.getInstance().getLocale().literalText(
                            channel.getPreferences().getLanguage(),
                            LineIds.STV
                    ),
                    originChannel,
                    targetChannel
            ));
        }

        double percentage = ((float) similarity / (float) targetEmoteSet.getEmotes().size()) * 100.0f;

        return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                channel.getPreferences().getLanguage(),
                LineIds.C_ESIMILARITY_SUCCESS,
                Huinyabot.getInstance().getLocale().literalText(
                        channel.getPreferences().getLanguage(),
                        LineIds.STV
                ),
                originChannel,
                String.valueOf(Math.round(percentage)),
                targetChannel,
                String.valueOf(similarity),
                String.valueOf(targetEmoteSet.getEmotes().size())
        ));
    }
}
