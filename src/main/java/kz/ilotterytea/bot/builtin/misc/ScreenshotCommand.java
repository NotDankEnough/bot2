package kz.ilotterytea.bot.builtin.misc;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.SharedConstants;
import kz.ilotterytea.bot.api.commands.*;
import kz.ilotterytea.bot.i18n.LineIds;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;

import java.util.List;

public class ScreenshotCommand implements Command {
    @Override
    public String getNameId() {
        return "screenshot";
    }

    @Override
    public int getDelay() {
        return 20000;
    }

    @Override
    public List<String> getAliases() {
        return List.of("scr", "скрин", "screenshot", "shot", "%{np}ttours");
    }

    @Override
    public Response run(Request request) throws Exception {
        if (request.getEvent().getReplyInfo() == null) {
            throw CommandException.notEnoughArguments(request, CommandArgument.MESSAGE);
        }

        String messageId = request.getEvent().getReplyInfo().getMessageId();
        String channelLogin = request.getChannel().getAliasName();

        OkHttpClient client = new OkHttpClient();
        MultipartBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("messageId", messageId)
                .addFormDataPart("channelLogin", channelLogin)
                .build();

        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .url(SharedConstants.SCREENSHOT_URL + "/api/v1/screenshot")
                .post(body)
                .build();

        okhttp3.Response httpResponse = client.newCall(httpRequest).execute();
        if (httpResponse.code() != 201 || httpResponse.body() == null) {
            throw CommandException.externalAPIError(request, httpResponse.code(), "Screenshot API");
        }

        JsonObject json = JsonParser.parseString(httpResponse.body().string()).getAsJsonObject();
        String screenshotUrl = json.getAsJsonObject("data").getAsJsonObject("screenshot").get("url").getAsString();
        return Response.ofSingle(Huinyabot.getInstance().getLocale()
                .formattedText(request.getChannel().getPreferences().getLanguage(),
                        LineIds.C_SCREENSHOT,
                        request.getUser().getAliasName(),
                        screenshotUrl));
    }
}
