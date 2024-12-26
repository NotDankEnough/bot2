package kz.ilotterytea.bot.api.commands;

import java.util.List;

public class Response {
    private final List<String> multiple;
    private final String single;

    private Response(String single, List<String> multiple) {
        this.single = single;
        this.multiple = multiple;
    }

    public static Response ofSingle(String single) {
        return new Response(single, null);
    }

    public static Response ofMultiple(List<String> multiple) {
        return new Response(null, multiple);
    }

    public static Response ofNothing() {
        return new Response(null, null);
    }

    public String getSingle() {
        assert single != null;
        return single;
    }

    public List<String> getMultiple() {
        assert multiple != null;
        return multiple;
    }

    public boolean isSingle() {
        return this.single != null;
    }

    public boolean isMultiple() {
        return this.multiple != null;
    }

    public boolean isNothing() {
        return this.single == null && this.multiple == null;
    }
}
