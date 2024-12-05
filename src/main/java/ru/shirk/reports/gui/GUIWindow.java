package ru.shirk.reports.gui;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import org.bukkit.entity.Player;

@Getter
@EqualsAndHashCode
@ToString
public class GUIWindow {
    public static final GUIWindow REPORT_LIST_FOR_MODERS = new GUIWindow("REPORT_LIST_FOR_MODERS", "Жалобы на игроков");
    public static final GUIWindow REPORT_LIST_FOR_ADMINS = new GUIWindow("REPORT_LIST_FOR_ADMINS", "Панель Администратора");
    public static final GUIWindow REMOVE_REPORTS = new GUIWindow("REMOVE_REPORTS", "Удаление жалоб");

    private final String name;
    private String playerName;
    private final String title;

    public GUIWindow(@NonNull String name, @NonNull String title) {
        this.name = name;
        this.title = title;
    }

    public GUIWindow(@NonNull String name, @NonNull String title, @NonNull String playerName) {
        this.name = name;
        this.title = title;
        this.playerName = playerName;
    }

    public static GUIWindow SEARCH_REPORTS(@NonNull String playerName) {
        return new GUIWindow("SEARCH_REPORTS", "Жалобы на " + playerName, playerName);
    }

    public static GUIWindow listWindowFor(@NonNull Player player) {
        return player.hasPermission("reports.admin") ? REPORT_LIST_FOR_ADMINS : REPORT_LIST_FOR_MODERS;
    }
}
