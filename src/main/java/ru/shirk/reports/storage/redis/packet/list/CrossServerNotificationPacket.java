package ru.shirk.reports.storage.redis.packet.list;

import com.google.gson.*;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Nullable;
import ru.shirk.reports.storage.redis.packet.AbstractRedisPacket;

@Getter
@Log
public class CrossServerNotificationPacket extends AbstractRedisPacket {

    private String server;
    private String message;

    public CrossServerNotificationPacket(String server, String message) {
        super(null);
        this.server = server;
        this.message = message;
    }

    public CrossServerNotificationPacket() {
        this(null);
    }

    public CrossServerNotificationPacket(@Nullable String source) {
        super(source);
    }

    @Override
    public void read() {
        try {
            final JsonElement element = new JsonParser().parse(getSource());
            if (!element.isJsonObject()) return;
            final JsonObject object = element.getAsJsonObject();

            this.server = object.get("server").getAsString();
            this.message = object.get("message").getAsString();
        } catch (JsonSyntaxException e) {
            log.warning("Packet parse error: "+ e.getMessage());
        }
    }

    @Override
    public void write() {
        JsonObject jo = new JsonObject();
        jo.add("server", new JsonPrimitive(server));
        jo.add("message", new JsonPrimitive(message));
        this.setSource(jo.toString());
    }
}
