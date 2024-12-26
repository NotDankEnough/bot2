package kz.ilotterytea.bot.builtin.user;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kz.ilotterytea.bot.SharedConstants;
import kz.ilotterytea.bot.api.commands.*;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.models.serverresponse.ivr.UserInfo;
import kz.ilotterytea.bot.utils.ParsedMessage;
import okhttp3.OkHttpClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A command for checking if user is banned.
 *
 * @author ilotterytea
 * @since 1.5
 */
public class UserIdCheckCommand implements Command {
    @Override
    public String getNameId() {
        return "userid";
    }

    @Override
    public List<String> getOptions() {
        return Collections.singletonList("login");
    }

    @Override
    public List<String> getAliases() {
        return List.of("uid", "isbanned");
    }

    @Override
    public Response run(Request request) throws Exception {
        ParsedMessage message = request.getMessage();
        Channel channel = request.getChannel();

        if (message.getMessage().isEmpty()) {
            throw CommandException.notEnoughArguments(request, CommandArgument.TARGET);
        }

        // Parsing ids and logins:
        String[] s = message.getMessage().get().split(",");
        ArrayList<String> loginQuery = new ArrayList<>();
        ArrayList<String> idQuery = new ArrayList<>();

        for (String id : s) {
            id = id.trim();

            try {
                int parsedId = Integer.parseInt(id);
                idQuery.add(id);
            } catch (NumberFormatException e) {
                loginQuery.add(id);
            }
        }

        // Building the query:
        String query = "?";

        if (!idQuery.isEmpty()) {
            query = query + "id=" + String.join(",", idQuery);
        }

        if (!loginQuery.isEmpty()) {
            if (query.length() > 1) {
                query = query + "&";
            }

            query = query + "login=" + String.join(",", loginQuery);
        }

        // Requesting:
        OkHttpClient client = new OkHttpClient();
        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .get()
                .url(SharedConstants.IVR_USER_ENDPOINT + query)
                .build();

        ArrayList<String> results = new ArrayList<>();

        okhttp3.Response response = client.newCall(httpRequest).execute();
        if (response.body() == null || response.code() != 200) {
            throw CommandException.externalAPIError(request, response.code(), "IVR API");
        }

        List<UserInfo> userInfos = new Gson().fromJson(response.body().string(), new TypeToken<List<UserInfo>>() {
        }.getType());

        if (userInfos.isEmpty()) {
            throw CommandException.notFound(request, request.getMessage().getMessage().get());
        }

        for (UserInfo userInfo : userInfos) {
            results.add(String.format(
                    "%s: %s %s",
                    (message.getUsedOptions().contains("login")) ? userInfo.getLogin() : userInfo.getDisplayName(),
                    userInfo.getId(),
                    (userInfo.getBanned()) ? ("[⛔ " + userInfo.getBanReason() + "]") : ""
            ));
        }

        return Response.ofSingle(String.join(" | ", results));
    }
}
