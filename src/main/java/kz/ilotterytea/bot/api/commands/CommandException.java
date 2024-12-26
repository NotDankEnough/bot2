package kz.ilotterytea.bot.api.commands;

import kz.ilotterytea.bot.Huinyabot;
import kz.ilotterytea.bot.i18n.LineIds;

import java.util.Arrays;

public class CommandException extends RuntimeException {
    private enum CommandExceptionCode {
        NOT_ENOUGH_ARGUMENTS(LineIds.ERROR_NOT_ENOUGH_ARGUMENTS),
        INCORRECT_ARGUMENT(LineIds.ERROR_INCORRECT_ARGUMENT),
        INCOMPATIBLE_NAME(LineIds.ERROR_INCOMPATIBLE_NAME),
        NAMESAKE_CREATION(LineIds.ERROR_NAMESAKE_CREATION),
        NOT_FOUND(LineIds.ERROR_NOT_FOUND),
        SOMETHING_WENT_WRONG(LineIds.ERROR_SOMETHING_WENT_WRONG),
        EXTERNAL_API_ERROR(LineIds.ERROR_EXTERNAL_API),
        INSUFFICIENT_RIGHTS(LineIds.ERROR_INSUFFICIENT_RIGHTS);

        private final LineIds id;

        CommandExceptionCode(LineIds id) {
            this.id = id;
        }
    }

    private final CommandExceptionCode code;

    private CommandException(Request request, CommandExceptionCode code, String... arguments) {
        super(Huinyabot.getInstance().getLocale().formattedText(
                request.getChannel().getPreferences().getLanguage(),
                code.id,
                arguments
        ));
        this.code = code;
    }

    public static CommandException notEnoughArguments(Request request, CommandArgument... arguments) {
        return new CommandException(request, CommandExceptionCode.NOT_ENOUGH_ARGUMENTS,
                String.join(", ", Arrays.stream(arguments)
                        .map(Enum::toString)
                        .toList()
                )
        );
    }

    public static CommandException incorrectArgument(Request request, CommandArgument argument, String value) {
        return new CommandException(request, CommandExceptionCode.INCORRECT_ARGUMENT, argument.toString(), value);
    }

    public static CommandException incompatibleName(Request request, String value) {
        return new CommandException(request, CommandExceptionCode.INCOMPATIBLE_NAME, value);
    }

    public static CommandException namesakeCreation(Request request, String value) {
        return new CommandException(request, CommandExceptionCode.NAMESAKE_CREATION, value);
    }

    public static CommandException notFound(Request request, String value) {
        return new CommandException(request, CommandExceptionCode.NOT_FOUND, value);
    }

    public static CommandException externalAPIError(Request request, Integer code, String value) {
        return new CommandException(request, CommandExceptionCode.EXTERNAL_API_ERROR, code.toString(), value != null ? " " + value : "");
    }

    public static CommandException insufficientRights(Request request) {
        return new CommandException(request, CommandExceptionCode.INSUFFICIENT_RIGHTS);
    }

    public static CommandException somethingWentWrong(Request request) {
        return new CommandException(request, CommandExceptionCode.SOMETHING_WENT_WRONG);
    }
}
