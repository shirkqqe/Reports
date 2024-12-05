package ru.shirk.reports.storage.redis.packet;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

public abstract class AbstractRedisPacket implements RedisPacket {

    @Getter(AccessLevel.PUBLIC)
    @Setter(AccessLevel.PROTECTED)
    private String source;

    public AbstractRedisPacket(@Nullable String source) {
        this.source = source;
        if (source != null) {
            read();
        }
    }
}
