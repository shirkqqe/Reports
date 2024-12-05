package ru.shirk.reports.listeners.bukkit;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import ru.shirk.reports.Reports;

public class BukkitEvents implements Listener {
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Reports.getUsersManager().loadUser(
                event.getPlayer().hasPermission("reports.moderator")
                        || event.getPlayer().hasPermission("reports.admin"),
                event.getPlayer().getName(),
                0,
                0,
                null,
                0,
                0,
                null
        );
    }
}
