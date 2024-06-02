package top.speedcubing.mcproxy.server;

import com.google.gson.JsonArray;

public class Setting {
    private final Object o;
    private final boolean requireStart;

    public Setting(Object o, boolean requireStart) {
        this.o = o;
        this.requireStart = requireStart;
    }

    public Object value() {
        return o;
    }

    public boolean requireStart() {
        return requireStart;
    }

    public Boolean getAsBoolean() {
        return (Boolean) value();
    }

    public String getAsString() {
        return (String) value();
    }

    public Integer getAsInteger() {
        return (Integer) value();
    }

    public JsonArray getAsJsonArray() {
        return (JsonArray) value();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Setting) {
            return value().equals(((Setting) other).value());
        }
        return false;
    }
}
