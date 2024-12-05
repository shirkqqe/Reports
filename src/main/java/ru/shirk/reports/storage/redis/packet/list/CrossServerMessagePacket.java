package ru.shirk.reports.storage.redis.packet.list;

import com.google.gson.*;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Nullable;
import ru.shirk.reports.storage.redis.packet.AbstractRedisPacket;

@Getter
@Log
public class CrossServerMessagePacket extends AbstractRedisPacket {

    private String target;
    private String server;
    private String message;

    public CrossServerMessagePacket(String server, String target, String message) {
        super(null);
        this.server = server;
        this.target = target;
        this.message = message;
    }

    public CrossServerMessagePacket() {
        this(null);
    }

    public CrossServerMessagePacket(@Nullable String source) {
        super(source);
    }

    @Override
    public void read() {
        try {
            final JsonElement element = new JsonParser().parse(getSource());
            if (!element.isJsonObject()) return;
            final JsonObject object = element.getAsJsonObject();

            this.server = object.get("server").getAsString();
            this.target = object.get("target").getAsString();
            this.message = object.get("message").getAsString();
        } catch (JsonSyntaxException e) {
            log.warning("Packet parse error: "+ e.getMessage());
        }
    }

    @Override
    public void write() {
        JsonObject jo = new JsonObject();
        jo.add("server", new JsonPrimitive(server));
        jo.add("target", new JsonPrimitive(target));
        jo.add("message", new JsonPrimitive(message));
        this.setSource(jo.toString());
    }
}
