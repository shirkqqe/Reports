package ru.shirk.reports.storage.redis;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.pubsub.StatefulRedisPubSubConnection;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import ru.shirk.reports.Reports;
import ru.shirk.reports.impl.Server;
import ru.shirk.reports.listeners.api.CrossServerReportNotificationEvent;
import ru.shirk.reports.storage.redis.packet.list.CrossServerMessagePacket;
import ru.shirk.reports.storage.redis.packet.list.CrossServerNotificationPacket;

import java.util.HashMap;
import java.util.UUID;

public class RedisManager {

    private static final String CHANNEL = "CROSS_SERVER_MESSAGES";
    private final RedisClient client;
    private final StatefulRedisConnection<String, String> connection;
    @SuppressWarnings("all")
    private final StatefulRedisPubSubConnection<String, String> pubSubConnection;

    private final HashMap<UUID, Boolean> notificationsToggle = new HashMap<>();


    public RedisManager() {
        final ConfigurationSection section = Reports.getConfigurationManager().getConfig("settings.yml")
                .getFile().getConfigurationSection("redis");
        if (section == null) {
            throw new IllegalStateException("Redis is not configured!");
        }
        this.client = RedisClient.create("redis://" + section.getString("host") + ":" + section.getString("port"));
        connection = client.connect();
        pubSubConnection = client.connectPubSub();

        pubSubConnection.addListener(new RedisMessageListener() {
            @Override
            public void message(String channel, String message) {
                try {
                    final CrossServerMessagePacket packet = new CrossServerMessagePacket(message);

                    Bukkit.getOnlinePlayers().forEach(player -> {
                        if (player.getName().equals(packet.getTarget())) {
                            player.sendMessage(packet.getMessage());
                            Bukkit.getLogger().info(String.format("[CSM to %s] %s", packet.getTarget(),
                                    packet.getMessage()));
                        }
                    });
                } catch (Exception e) {
                    final CrossServerNotificationPacket packet = new CrossServerNotificationPacket(message);

                    final CrossServerReportNotificationEvent event = new CrossServerReportNotificationEvent(
                            packet.getServer(),
                            packet.getMessage()
                    );
                    event.callEvent();
                    if (event.isCancelled()) return;

                    Bukkit.getOnlinePlayers().forEach(player -> {
                        if ((player.hasPermission("reports.moderator") || player.hasPermission("reports.admin"))
                                && !isNotificationsToggled(player.getUniqueId())) {
                            player.sendMessage(packet.getMessage());
                        }
                    });
                    Bukkit.getLogger().info(packet.getMessage());
                }
            }
        });

        pubSubConnection.async().subscribe(CHANNEL);
    }

    public void unload() {
        client.shutdown();
    }

    public void sendMessage(Server server, String message) {
        RedisAsyncCommands<String, String> commands = connection.async();
        final var packet = new CrossServerNotificationPacket(server.getProxyName(), message);
        packet.write();
        commands.publish(CHANNEL, packet.getSource());
    }

    public void sendMessageToPlayer(Server server, String target, String message) {
        RedisAsyncCommands<String, String> commands = connection.async();
        final var packet = new CrossServerMessagePacket(server.getProxyName(), target, message);
        packet.write();
        commands.publish(CHANNEL, packet.getSource());
    }

    public boolean isNotificationsToggled(UUID uuid) {
        return notificationsToggle.containsKey(uuid);
    }

    public void setNotificationToggle(UUID uuid) {
        if (notificationsToggle.containsKey(uuid)) {
            notificationsToggle.remove(uuid);
            return;
        }
        notificationsToggle.put(uuid, true);
    }
}
