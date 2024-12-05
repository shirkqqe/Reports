package ru.shirk.reports.storage.redis.packet;

public interface RedisPacket {
    void read();
    void write();
}
