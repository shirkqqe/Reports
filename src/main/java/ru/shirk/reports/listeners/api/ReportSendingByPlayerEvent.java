package ru.shirk.reports.listeners.api;

import lombok.Getter;
import lombok.NonNull;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import ru.shirk.reports.reports.Report;

@Getter
public class ReportSendingByPlayerEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    @NonNull
    private final Report report;
    @NonNull
    private final Player sender;
    @NonNull
    private final OfflinePlayer target;
    private boolean cancelled = false;

    public ReportSendingByPlayerEvent(@NonNull Report report, @NonNull Player sender, @NonNull OfflinePlayer target) {
        super();
        this.report = report;
        this.sender = sender;
        this.target = target;
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
        return this.cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }
}
