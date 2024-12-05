package ru.shirk.reports.gui;

import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;
import ru.shirk.reports.Reports;
import ru.shirk.reports.reports.Report;
import ru.shirk.reports.reports.ReportStatus;
import ru.shirk.reports.tools.AnvilDialog;
import ru.shirk.reports.tools.Utils;
import ru.shirk.reports.users.Moderator;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ReportsMenu implements Listener {

    private final Map<UUID, GUIHolder> viewers = new HashMap<>();

    public ReportsMenu(@NonNull JavaPlugin plugin) {
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @SuppressWarnings("deprecation")
    public void open(@NonNull Player player) {
        final GUIHolder guiHolder = new GUIHolder(player.getUniqueId());
        guiHolder.changeWindow(GUIWindow.listWindowFor(player));
        final Inventory inventory = Bukkit.createInventory(guiHolder, 54, guiHolder.getCurrentWindow().getTitle());
        guiHolder.inventory(inventory);
        guiHolder.build();
        viewers.put(player.getUniqueId(), guiHolder);
    }

    @EventHandler
    private void onOpenInventory(InventoryOpenEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (viewers.get(player.getUniqueId()) != null) return;
        if (!(event.getInventory().getHolder() != null && event.getInventory().getHolder() instanceof GUIHolder holder))
            return;
        viewers.put(player.getUniqueId(), holder);
    }

    @EventHandler
    private void onClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (viewers.get(player.getUniqueId()) == null) return;

        final GUIHolder guiHolder = viewers.get(player.getUniqueId());
        if (!(event.getInventory().getHolder() != null && event.getInventory().getHolder() instanceof GUIHolder holder
                && guiHolder == holder)) return;
        event.setCancelled(true);

        switch (holder.getCurrentWindow().getName().toLowerCase()) {
            case "report_list_for_admins" -> {
                switch (event.getSlot()) {
                    case 52 -> {
                        holder.nextSortType();
                        holder.update();
                    }
                    case 47 -> {
                        player.closeInventory();
                        player.sendMessage(Reports.getConfigurationManager().getConfig("lang.yml").c("enterNameAndComment"));
                        new AnvilDialog(player, Reports.getInstance(), "Создание новой жалобы", Reports.getConfigurationManager()
                                .getConfig("lang.yml").c("enterNameAndComment")) {
                            @Override
                            protected void onResponse(@NonNull Player player, @NonNull String response) {
                                final String[] message = response.split(" ");
                                if (message.length < 2) {
                                    player.sendMessage(Reports.getConfigurationManager().getConfig("lang.yml")
                                            .c("formatError"));
                                    return;
                                }
                                if (Bukkit.getPlayer(message[0]) == null || Bukkit.getOfflinePlayerIfCached(message[0]) == null) {
                                    player.sendMessage(ChatColor.RED + "Игрок не найден!");
                                    return;
                                }
                                final Report report = new Report(
                                        Reports.getReportManager().getNextReportId() != -1 ?
                                                Reports.getReportManager().getNextReportId() + 1 : 1,
                                        message[0],
                                        "System", Utils.getFinalArg(message, 1),
                                        null,
                                        LocalDateTime.now(),
                                        ReportStatus.OPENED,
                                        Reports.getCurrentServer().getName()
                                );
                                Reports.getReportManager().addReport(report, false);
                                player.sendMessage(Reports.getConfigurationManager().getConfig("lang.yml")
                                        .c("reportCreated").replace("%player%", message[0]));
                            }
                        }.show();
                    }
                    case 49 -> {
                        player.sendMessage(Reports.getConfigurationManager().getConfig("lang.yml").c("enterName"));
                        new AnvilDialog(player, Reports.getInstance(), "Поиск жалоб", Reports.getConfigurationManager()
                                .getConfig("lang.yml").c("enterName")) {
                            @Override
                            protected void onResponse(@NonNull Player player, @NonNull String response) {
                                holder.changeWindow(GUIWindow.SEARCH_REPORTS(response)).build();
                            }
                        }.show();
                    }
                    case 51 -> holder.changeWindow(GUIWindow.REMOVE_REPORTS).build();
                    default -> {
                        if (event.getCurrentItem() != null && event.getCurrentItem().isSimilar(holder.nextPage)) {
                            holder.nextPage();
                            holder.update();
                            return;
                        }
                        if (event.getCurrentItem() != null && event.getCurrentItem().isSimilar(holder.prevPage)) {
                            holder.prevPage();
                            holder.update();
                            return;
                        }

                        final ItemStack itemStack = event.getCurrentItem();
                        if (itemStack == null || itemStack.getItemMeta() == null) return;
                        final Integer reportNumber = itemStack.getItemMeta().getPersistentDataContainer()
                                .get(NamespacedKey.minecraft("number"), PersistentDataType.INTEGER);
                        if (reportNumber == null) return;
                        final Report report = Reports.getReportManager().getReport(reportNumber);
                        if (report == null) return;

                        switch (report.getStatus()) {
                            case OPENED -> {
                                if (!event.getClick().isRightClick() && !event.getClick().isLeftClick()) return;
                                if (event.getClick().isLeftClick()) {
                                    final Moderator moderator = Reports.getUsersManager().getModerator(player.getName());
                                    if (moderator == null) return;
                                    report.check(moderator);
                                    report.save();
                                    updateAll();
                                    return;
                                }
                                holder.changeWindow(GUIWindow.SEARCH_REPORTS(report.getReported())).build();
                            }
                            case CHECK -> {
                                if (!event.getClick().isRightClick() && !event.getClick().isLeftClick()) return;
                                if (event.getClick().isRightClick()) {
                                    if (report.getModerator() != null && report.getModerator().getName()
                                            .equalsIgnoreCase(player.getName())) {
                                        player.closeInventory();
                                        new AnvilDialog(player, Reports.getInstance(), "Закрытие жалобы: Ложная",
                                                Reports.getConfigurationManager().getConfig("lang.yml").c("enterComment")) {
                                            @Override
                                            protected void onResponse(@NonNull Player player, @NonNull String response) {
                                                if (!player.hasPermission("reports.moderator")) return;
                                                for (Report report : Reports.getReportManager().getAllReports()) {
                                                    if (report.getModerator() == null || !report.getModerator().getName()
                                                            .equalsIgnoreCase(player.getName())) continue;
                                                    report.complete(Reports.getUsersManager().getModerator(player.getName()),
                                                            response, false);
                                                }
                                            }
                                        }.show();
                                        return;
                                    }
                                    holder.changeWindow(GUIWindow.SEARCH_REPORTS(report.getReported())).build();
                                    return;
                                }
                                if (report.getModerator() == null || !report.getModerator().getName()
                                        .equalsIgnoreCase(player.getName())) return;
                                if (event.getClick().isLeftClick()) {
                                    player.closeInventory();
                                    new AnvilDialog(
                                            player,
                                            Reports.getInstance(),
                                            "Закрытие жалобы: Принята",
                                            Reports.getConfigurationManager().getConfig("lang.yml")
                                                    .c("enterComment")) {
                                        @Override
                                        protected void onResponse(@NonNull Player player, @NonNull String response) {
                                            if (!player.hasPermission("reports.moderator")) return;
                                            for (Report report : Reports.getReportManager().getAllReports()) {
                                                if (report.getModerator() == null || !report.getModerator().getName()
                                                        .equalsIgnoreCase(player.getName())) continue;
                                                report.complete(Reports.getUsersManager().getModerator(player.getName()),
                                                        response, true);
                                            }
                                        }
                                    }.show();
                                }
                            }
                        }
                    }
                }
            }
            case "report_list_for_moders" -> {
                if (event.getSlot() == 49) {
                    player.closeInventory();
                    return;
                }
                if (event.getSlot() == 50) {
                    holder.nextSortType();
                    holder.update();
                    return;
                }
                if (event.getCurrentItem() != null && event.getCurrentItem().isSimilar(holder.nextPage)) {
                    holder.nextPage();
                    holder.update();
                    return;
                }
                if (event.getCurrentItem() != null && event.getCurrentItem().isSimilar(holder.prevPage)) {
                    holder.prevPage();
                    holder.update();
                    return;
                }

                final ItemStack itemStack = event.getCurrentItem();
                if (itemStack == null || itemStack.getItemMeta() == null) return;
                Integer reportNumber = itemStack.getItemMeta().getPersistentDataContainer()
                        .get(NamespacedKey.minecraft("number"), PersistentDataType.INTEGER);
                if (reportNumber == null) return;
                final Report report = Reports.getReportManager().getReport(reportNumber);
                if (report == null) return;

                switch (report.getStatus()) {
                    case OPENED -> {
                        if (!event.getClick().isRightClick() && !event.getClick().isLeftClick()) return;
                        if (event.getClick().isLeftClick()) {
                            final Moderator moderator = Reports.getUsersManager().getModerator(player.getName());
                            if (moderator == null) return;
                            report.check(moderator);
                            report.save();
                            updateAll();
                            return;
                        }
                        holder.changeWindow(GUIWindow.SEARCH_REPORTS(report.getReported())).build();
                    }
                    case CHECK -> {
                        if (!event.getClick().isRightClick() && !event.getClick().isLeftClick()) return;
                        if (event.getClick().isRightClick()) {
                            if (report.getModerator() != null && report.getModerator().getName()
                                    .equalsIgnoreCase(player.getName())) {
                                player.closeInventory();
                                new AnvilDialog(player, Reports.getInstance(), "Закрытие жалобы: Ложная",
                                        Reports.getConfigurationManager().getConfig("lang.yml").c("enterComment")) {
                                    @Override
                                    protected void onResponse(@NonNull Player player, @NonNull String response) {
                                        if (!player.hasPermission("reports.moderator")) return;
                                        for (Report report : Reports.getReportManager().getAllReports()) {
                                            if (report.getModerator() == null || !report.getModerator().getName()
                                                    .equalsIgnoreCase(player.getName())) continue;
                                            report.complete(Reports.getUsersManager().getModerator(player.getName()),
                                                    response, false);
                                        }
                                    }
                                }.show();
                                return;
                            }
                            holder.changeWindow(GUIWindow.SEARCH_REPORTS(report.getReported())).build();
                            return;
                        }
                        if (report.getModerator() == null || !report.getModerator().getName()
                                .equalsIgnoreCase(player.getName())) return;
                        if (event.getClick().isLeftClick()) {
                            player.closeInventory();
                            new AnvilDialog(
                                    player,
                                    Reports.getInstance(),
                                    "Закрытие жалобы: Принята",
                                    Reports.getConfigurationManager().getConfig("lang.yml")
                                            .c("enterComment")) {
                                @Override
                                protected void onResponse(@NonNull Player player, @NonNull String response) {
                                    if (!player.hasPermission("reports.moderator")) return;
                                    for (Report report : Reports.getReportManager().getAllReports()) {
                                        if (report.getModerator() == null || !report.getModerator().getName()
                                                .equalsIgnoreCase(player.getName())) continue;
                                        report.complete(Reports.getUsersManager().getModerator(player.getName()),
                                                response, true);
                                    }
                                }
                            }.show();
                        }
                    }
                }
            }
            case "remove_reports" -> {
                if (event.getSlot() == 49) {
                    holder.changeWindow(GUIWindow.REPORT_LIST_FOR_ADMINS).build();
                    return;
                }
                if (event.getSlot() == 50) {
                    holder.nextSortType();
                    holder.update();
                    return;
                }
                if (event.getCurrentItem() != null && event.getCurrentItem().isSimilar(holder.nextPage)) {
                    holder.nextPage();
                    holder.update();
                    return;
                }
                if (event.getCurrentItem() != null && event.getCurrentItem().isSimilar(holder.prevPage)) {
                    holder.prevPage();
                    holder.update();
                    return;
                }

                final Inventory inventory = event.getInventory();
                final int slot = event.getSlot();
                final ItemStack itemStack = inventory.getItem(slot);

                if (itemStack == null || itemStack.getItemMeta() == null) return;
                final Integer number = itemStack.getItemMeta().getPersistentDataContainer()
                        .get(NamespacedKey.minecraft("number"), PersistentDataType.INTEGER);
                if (number == null) return;
                Reports.getReportManager().removeReport(number);
                inventory.removeItem(itemStack);
                holder.update();
            }
            case "search_reports" -> {
                if (event.getSlot() == 49) {
                    holder.changeWindow(player.hasPermission("reports.admin") ? GUIWindow.REPORT_LIST_FOR_ADMINS :
                            GUIWindow.REPORT_LIST_FOR_MODERS).build();
                    return;
                }
                if (event.getSlot() == 50) {
                    holder.nextSortType();
                    holder.update();
                    return;
                }
                if (event.getCurrentItem() != null && event.getCurrentItem().isSimilar(holder.nextPage)) {
                    holder.nextPage();
                    holder.update();
                    return;
                }
                if (event.getCurrentItem() != null && event.getCurrentItem().isSimilar(holder.prevPage)) {
                    holder.prevPage();
                    holder.update();
                }
            }
        }
    }

    private void updateAll() {
        for (final Map.Entry<UUID, GUIHolder> entry : viewers.entrySet()) {
            final Player p = Bukkit.getPlayer(entry.getKey());
            if (p == null) continue;
            entry.getValue().update();
        }
    }

    @EventHandler
    private void onClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player player)) return;
        if (viewers.get(player.getUniqueId()) == null) return;
        final GUIHolder guiHolder = viewers.get(player.getUniqueId());
        if (!(event.getInventory().getHolder() != null && event.getInventory().getHolder() instanceof GUIHolder holder
                && guiHolder == holder)) return;
        if (event.getReason() == InventoryCloseEvent.Reason.OPEN_NEW) return;
        viewers.remove(player.getUniqueId());
    }
}
