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
import java.util.*;

/**
 * Emote count command.
 *
 * @author ilotterytea
 * @since 1.1
 */
public class EmoteCountCommand implements Command {
    @Override
    public String getNameId() {
        return "ecount";
    }

    @Override
    public List<String> getAliases() {
        return List.of("count", "emote", "колво", "кол-во", "эмоут");
    }

    @Override
    public Response run(Request request) {
        ParsedMessage message = request.getMessage();
        Channel channel = request.getChannel();

        if (message.getMessage().isEmpty()) {
            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_ECOUNT_NOEMOTEPROVIDED,
                    Huinyabot.getInstance().getLocale().literalText(
                            channel.getPreferences().getLanguage(),
                            LineIds.STV
                    )
            ));
        }

        String[] s = message.getMessage().get().split(" ");

        String name = s[0];
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

        // Get the emote:
        Optional<Emote> optionalEmote = emotes.stream().filter(e -> e.getName().equals(name) && e.getDeletionTimestamp() == null).findFirst();

        if (optionalEmote.isEmpty()) {
            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_ECOUNT_NOEMOTEFOUND,
                    Huinyabot.getInstance().getLocale().literalText(
                            channel.getPreferences().getLanguage(),
                            LineIds.STV
                    ),
                    name
            ));
        }

        Emote emote = optionalEmote.get();

        // Sort the emote list:
        emotes.sort(Comparator.comparingInt(Emote::getUsedTimes));
        Collections.reverse(emotes);

        int position = emotes.indexOf(emote);

        return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                channel.getPreferences().getLanguage(),
                LineIds.C_ECOUNT_SUCCESS,
                Huinyabot.getInstance().getLocale().literalText(
                        channel.getPreferences().getLanguage(),
                        LineIds.STV
                ),
                emote.getName(),
                (emote.getGlobal() ? " *" : ""),
                String.valueOf(emote.getUsedTimes()),
                (position < 0 ? "N/A" : String.valueOf(position + 1)),
                String.valueOf(emotes.size())
        ));
    }
}
