package kz.ilotterytea.bot.builtin.emotes;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.SharedConstants;
import kz.ilotterytea.bot.api.commands.Command;
import kz.ilotterytea.bot.api.commands.Request;
import kz.ilotterytea.bot.api.commands.Response;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.i18n.LineIds;
import kz.ilotterytea.bot.models.serverresponse.Emote;
import kz.ilotterytea.bot.models.serverresponse.ServerPayload;
import kz.ilotterytea.bot.utils.ParsedMessage;
import okhttp3.OkHttpClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Emote top command.
 *
 * @author ilotterytea
 * @since 1.1
 */
public class EmoteTopCommand implements Command {
    @Override
    public String getNameId() {
        return "etop";
    }

    @Override
    public int getDelay() {
        return 10000;
    }

    @Override
    public List<String> getAliases() {
        return List.of("emotetop", "топэмоутов");
    }

    @Override
    public Response run(Request request) {
        ParsedMessage message = request.getMessage();
        Channel channel = request.getChannel();

        final int MAX_COUNT = 10;
        int count;

        if (message.getMessage().isEmpty()) {
            count = MAX_COUNT;
        } else {
            String[] s = message.getMessage().get().split(" ");

            try {
                count = Integer.parseInt(s[1]);
            } catch (ArrayIndexOutOfBoundsException | NumberFormatException e) {
                count = MAX_COUNT;
            }
        }

        if (count > MAX_COUNT) {
            count = MAX_COUNT;
        }

        OkHttpClient client = new OkHttpClient.Builder().build();
        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .get()
                .url(SharedConstants.STATS_URL + "/api/v1/channel/" + channel.getAliasId() + "/emotes")
                .build();

        ArrayList<Emote> emotes;

        try (okhttp3.Response response = client.newCall(httpRequest).execute()) {
            if (response.code() != 200) {
                return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                        channel.getPreferences().getLanguage(),
                        LineIds.HTTP_ERROR,
                        String.valueOf(response.code()),
                        "Stats API"
                ));
            }

            if (response.body() == null) {
                return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                        channel.getPreferences().getLanguage(),
                        LineIds.SOMETHING_WENT_WRONG
                ));
            }

            String body = response.body().string();

            ServerPayload<ArrayList<Emote>> payload = new Gson().fromJson(body, new TypeToken<ServerPayload<ArrayList<Emote>>>() {
            }.getType());

            if (payload.getData() != null) {
                emotes = payload.getData();
            } else {
                return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_ETOP_NOCHANNELEMOTES,
                        Huinyabot.getInstance().getLocale().literalText(
                                channel.getPreferences().getLanguage(),
                                LineIds.STV
                        ),
                        Huinyabot.getInstance().getLocale().literalText(
                                channel.getPreferences().getLanguage(),
                                LineIds.STV
                        )
                ));
            }
        } catch (IOException e) {
            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.SOMETHING_WENT_WRONG
            ));
        }

        if (emotes.isEmpty()) {
            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_ETOP_NOCHANNELEMOTES,
                    Huinyabot.getInstance().getLocale().literalText(
                            channel.getPreferences().getLanguage(),
                            LineIds.STV
                    ),
                    Huinyabot.getInstance().getLocale().literalText(
                            channel.getPreferences().getLanguage(),
                            LineIds.STV
                    )
            ));
        }

        // Remove the deleted emotes:
        emotes.removeIf(e -> e.getDeletionTimestamp() != null);

        // Sort the emotes by used count:
        emotes.sort(Comparator.comparingInt(Emote::getUsedTimes));
        Collections.reverse(emotes);

        if (emotes.size() < count) {
            count = emotes.size();
        }

        ArrayList<String> msgs = new ArrayList<>();

        msgs.add("");
        int index = 0;

        for (int i = 0; i < count; i++) {
            Emote em = emotes.get(i);

            StringBuilder sb = new StringBuilder();

            if (
                    Huinyabot.getInstance().getLocale().formattedText(
                            channel.getPreferences().getLanguage(),
                            LineIds.C_ETOP_SUCCESS,
                            Huinyabot.getInstance().getLocale().literalText(
                                    channel.getPreferences().getLanguage(),
                                    LineIds.STV
                            ),
                            msgs.get(index) + (i + 1) + ". " + em.getName()
                                    + (em.getGlobal() ? " *" : "")
                                    + " (" + em.getUsedTimes() + "); "
                    ).length() < 500
            ) {
                sb.append(msgs.get(index))
                        .append(i + 1)
                        .append(". ")
                        .append(em.getName())
                        .append(em.getGlobal() ? " ^" : "")
                        .append(" (")
                        .append(em.getUsedTimes())
                        .append("); ");
            } else {
                msgs.add("");
                index++;
            }

            msgs.remove(index);
            msgs.add(index, sb.toString());
        }

        return Response.ofMultiple(msgs.stream().map((msg) -> Huinyabot.getInstance().getLocale().formattedText(
                channel.getPreferences().getLanguage(),
                LineIds.C_ETOP_SUCCESS,
                Huinyabot.getInstance().getLocale().literalText(
                        channel.getPreferences().getLanguage(),
                        LineIds.STV
                ),
                msg
        )).toList());
    }
}
