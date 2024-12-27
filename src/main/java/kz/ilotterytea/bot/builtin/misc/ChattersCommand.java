package kz.ilotterytea.bot.builtin.misc;

import com.github.twitch4j.helix.domain.Chatter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.SharedConstants;
import kz.ilotterytea.bot.api.commands.Command;
import kz.ilotterytea.bot.api.commands.CommandException;
import kz.ilotterytea.bot.api.commands.Request;
import kz.ilotterytea.bot.api.commands.Response;
import kz.ilotterytea.bot.i18n.LineIds;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.List;

public class ChattersCommand implements Command {
    @Override
    public String getNameId() {
        return "chatters";
    }

    @Override
    public int getDelay() {
        return 30000;
    }

    @Override
    public List<String> getAliases() {
        return List.of("cha", "chat", "чаттеры", "список", "lurkers");
    }

    @Override
    public Response run(Request request) throws Exception {
        List<Chatter> chatters;

        try {
            chatters = Huinyabot.getInstance().getClient().getHelix().getChatters(
                    SharedConstants.TWITCH_ACCESS_TOKEN,
                    request.getChannel().getAliasId().toString(),
                    Huinyabot.getInstance().getCredential().getUserId(),
                    null,
                    null
            ).execute().getChatters();
        } catch (Exception ignored) {
            throw CommandException.insufficientRights(request);
        }

        String paste = "total chatters: " + chatters.size() + '\n';
        paste += "----------------------\n\n";
        paste += String.join("\n", chatters.stream().map(Chatter::getUserLogin).toList());

        String title = String.format("%s's chatter list on %s",
                request.getChannel().getAliasName(),
                new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date.from(Instant.now()))
        );

        OkHttpClient client = new OkHttpClient();

        MultipartBody body = new MultipartBody.Builder()
                .addFormDataPart("paste", paste)
                .addFormDataPart("title", title)
                .build();

        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .post(body)
                .url(SharedConstants.PASTEA_URL + "/paste")
                .build();

        okhttp3.Response response = client.newCall(httpRequest).execute();

        if (response.code() != 201 || response.body() == null) {
            throw CommandException.externalAPIError(request, response.code(), "Pastea API");
        }

        String contents = response.body().string();
        JsonObject json = JsonParser.parseString(contents).getAsJsonObject();

        String outputUrl = String.format("%s/%s",
                SharedConstants.PASTEA_URL,
                json.get("data").getAsJsonObject().get("id").getAsString()
        );

        return Response.ofSingle(Huinyabot.getInstance()
                .getLocale()
                .formattedText(request.getChannel().getPreferences().getLanguage(),
                        LineIds.C_CHATTERS,
                        request.getUser().getAliasName(),
                        outputUrl
                )
        );
    }
}
