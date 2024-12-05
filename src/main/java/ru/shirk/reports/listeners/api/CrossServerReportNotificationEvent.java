package ru.shirk.reports.listeners.api;

import lombok.Getter;
import lombok.Setter;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

@Getter
@Setter
public class CrossServerReportNotificationEvent extends Event implements Cancellable {

    private String server;
    private String message;

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled;

    public CrossServerReportNotificationEvent(String server, String message) {
        super(true);
        this.server = server;
        this.message = message;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
