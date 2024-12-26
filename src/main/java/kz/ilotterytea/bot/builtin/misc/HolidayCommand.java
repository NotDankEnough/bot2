package kz.ilotterytea.bot.builtin.misc;

import com.github.twitch4j.tmi.domain.Chatters;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.SharedConstants;
import kz.ilotterytea.bot.api.commands.Command;
import kz.ilotterytea.bot.api.commands.CommandException;
import kz.ilotterytea.bot.api.commands.Request;
import kz.ilotterytea.bot.api.commands.Response;
import kz.ilotterytea.bot.entities.channels.Channel;
import kz.ilotterytea.bot.i18n.LineIds;
import kz.ilotterytea.bot.models.HolidayModel;
import kz.ilotterytea.bot.models.serverresponse.Emote;
import kz.ilotterytea.bot.models.serverresponse.ServerPayload;
import kz.ilotterytea.bot.utils.ParsedMessage;
import kz.ilotterytea.bot.utils.StringUtils;
import okhttp3.OkHttpClient;

import java.util.*;

/**
 * Holiday command.
 *
 * @author ilotterytea
 * @since 1.0
 */
public class HolidayCommand implements Command {
    @Override
    public String getNameId() {
        return "holiday";
    }

    @Override
    public int getDelay() {
        return 10000;
    }

    @Override
    public List<String> getOptions() {
        return List.of("тык", "massping", "all", "все", "no-emotes", "без-эмоутов");
    }

    @Override
    public List<String> getSubcommands() {
        return Collections.singletonList("search");
    }

    @Override
    public List<String> getAliases() {
        return List.of("праздник", "hld");
    }

    @Override
    public Response run(Request request) throws Exception {
        ParsedMessage message = request.getMessage();
        Channel channel = request.getChannel();

        if (message.getSubcommandId().isPresent() && message.getSubcommandId().get().equals("search")) {
            if (message.getMessage().isEmpty()) {
                return Response.ofSingle(Huinyabot.getInstance().getLocale().literalText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_HOLIDAY_NOSEARCHQUERY
                ));
            }

            okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                    .url(String.format(SharedConstants.HOLIDAY_SEARCH_URL, message.getMessage().get()))
                    .build();

            ArrayList<HolidayModel> holidays;

            okhttp3.Response response = new OkHttpClient().newCall(httpRequest).execute();

            if (response.code() == 200) {
                assert response.body() != null;
                holidays = new Gson().fromJson(response.body().string(), new TypeToken<ArrayList<HolidayModel>>() {
                }.getType());
            } else {
                throw CommandException.externalAPIError(request, response.code(), "Holiday");
            }

            if (holidays.isEmpty()) {
                return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                        channel.getPreferences().getLanguage(),
                        LineIds.C_HOLIDAY_QUERYNOTFOUND,
                        message.getMessage().get()
                ));
            }

            ArrayList<String> _holidays = new ArrayList<>();

            for (HolidayModel model : holidays) {
                _holidays.add(String.format("%s (%s/%s)", model.getName(), model.getDate().get(1), model.getDate().get(0)));
            }

            ArrayList<String> msgs = new ArrayList<>();
            msgs.add("");
            int index = 0;

            for (String hol : _holidays) {
                if (
                        Huinyabot.getInstance().getLocale().formattedText(
                                channel.getPreferences().getLanguage(),
                                LineIds.C_HOLIDAY_QUERYSUCCESS,
                                String.valueOf(holidays.size()),
                                message.getMessage().get(),
                                msgs.get(index) + hol + ", "
                        ).length() > 500
                ) {
                    index++;
                    msgs.add(hol + ", ");
                } else {
                    String c = msgs.get(index);

                    msgs.remove(index);
                    msgs.add(index, c + hol + ", ");
                }
            }

            return Response.ofMultiple(msgs.stream().map((msg) -> Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_HOLIDAY_QUERYSUCCESS,
                    String.valueOf(holidays.size()),
                    message.getMessage().get(),
                    msg
            )).toList());
        }

        int month;
        int day;

        ArrayList<String> s = new ArrayList<>(Arrays.asList(message.getMessage().get().split(" ")));

        if (s.size() == 0) {
            month = Calendar.getInstance().get(Calendar.MONTH) + 1;
            day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
        } else {
            ArrayList<String> date = new ArrayList<>(Arrays.asList(s.get(0).split("/")));

            if (date.size() >= 2) {
                try {
                    month = Integer.parseInt(date.get(1));
                    day = Integer.parseInt(date.get(0));
                } catch (NumberFormatException e) {
                    month = Calendar.getInstance().get(Calendar.MONTH) + 1;
                    day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                }
            } else {
                try {
                    day = Integer.parseInt(date.get(0));
                    month = Calendar.getInstance().get(Calendar.MONTH) + 1;
                } catch (NumberFormatException e) {
                    month = Calendar.getInstance().get(Calendar.MONTH) + 1;
                    day = Calendar.getInstance().get(Calendar.DAY_OF_MONTH);
                }
            }
        }

        okhttp3.Request httpRequest = new okhttp3.Request.Builder()
                .url(String.format(SharedConstants.HOLIDAY_URL, month, day))
                .build();

        ArrayList<String> holidays;

        okhttp3.Response response = new OkHttpClient().newCall(httpRequest).execute();

        if (response.code() == 200) {
            assert response.body() != null;
            holidays = new Gson().fromJson(response.body().string(), new TypeToken<ArrayList<String>>() {
            }.getType());
        } else {
            throw CommandException.externalAPIError(request, response.code(), "Holiday");
        }

        if (holidays.size() == 0) {
            return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_HOLIDAY_NOHOLIDAYS,
                    StringUtils.pad(day) + "/" + StringUtils.pad(month)
            ));
        }

        String name = holidays.get((int) Math.floor(Math.random() * holidays.size() - 1));

        if (message.getUsedOptions().contains("all") || message.getUsedOptions().contains("все")) {
            return Response.ofSingle(String.join(", ", holidays));
        }

        OkHttpClient client = new OkHttpClient.Builder().build();
        okhttp3.Request statsRequest = new okhttp3.Request.Builder()
                .get()
                .url(SharedConstants.STATS_URL + "/api/v1/channel/" + channel.getAliasId().toString() + "/emotes")
                .build();

        ArrayList<Emote> emotes;

        response = client.newCall(statsRequest).execute();
        if (response.code() != 200) {
            throw CommandException.externalAPIError(request, response.code(), "Stats API");
        }

        if (response.body() == null) {
            throw CommandException.somethingWentWrong(request);
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

        if (message.getUsedOptions().contains("massping") || message.getUsedOptions().contains("тык")) {
            ArrayList<String> msgs = new ArrayList<>();
            int index = 0;
            Chatters chatters = Huinyabot.getInstance().getClient().getMessagingInterface().getChatters(channel.getAliasName()).execute();

            msgs.add("");

            for (String uName : chatters.getAllViewers()) {
                StringBuilder sb = new StringBuilder();

                if (!message.getUsedOptions().contains("no-emotes") || !message.getUsedOptions().contains("без-эмоутов")) {
                    String finalUName = uName;
                    Optional<Emote> optionalEmote = emotes.stream().filter(e -> e.getName().equalsIgnoreCase(finalUName)).findFirst();

                    if (optionalEmote.isPresent()) {
                        uName = optionalEmote.get().getName();
                    }
                }

                if (
                        Huinyabot.getInstance().getLocale().formattedText(
                                channel.getPreferences().getLanguage(),
                                LineIds.C_HOLIDAY_SUCCESS,
                                msgs.get(index) + uName + " ",
                                StringUtils.pad(day) + "/" + StringUtils.pad(month),
                                String.valueOf(holidays.indexOf(name) + 1),
                                String.valueOf(holidays.size()),
                                name
                        ).length() < 500
                ) {
                    sb.append(msgs.get(index)).append(uName).append(" ");
                    msgs.remove(index);
                    msgs.add(index, sb.toString());
                } else {
                    msgs.add("");
                    index++;
                }
            }

            int finalMonth = month;
            int finalDay = day;
            return Response.ofMultiple(msgs.stream().map((msg) -> Huinyabot.getInstance().getLocale().formattedText(
                    channel.getPreferences().getLanguage(),
                    LineIds.C_HOLIDAY_SUCCESS,
                    msg,
                    StringUtils.pad(finalDay) + "/" + StringUtils.pad(finalMonth),
                    String.valueOf(holidays.indexOf(name) + 1),
                    String.valueOf(holidays.size()),
                    name
            )).toList());
        }

        return Response.ofSingle(Huinyabot.getInstance().getLocale().formattedText(
                channel.getPreferences().getLanguage(),
                LineIds.C_HOLIDAY_SUCCESS,
                "",
                StringUtils.pad(day) + "/" + StringUtils.pad(month),
                String.valueOf(holidays.indexOf(name) + 1),
                String.valueOf(holidays.size()),
                name
        ));
    }
}
