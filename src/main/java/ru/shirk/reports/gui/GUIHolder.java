package ru.shirk.reports.gui;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.ToString;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.shirk.reports.Reports;
import ru.shirk.reports.reports.Report;
import ru.shirk.reports.reports.ReportStatus;
import ru.shirk.reports.tools.Utils;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Stream;

@EqualsAndHashCode
@ToString
@SuppressWarnings("deprecation")
public class GUIHolder implements InventoryHolder {

    // fields
    @Getter
    private GUIWindow currentWindow;
    private int page;
    @Getter
    @NonNull
    private final UUID owner;
    private Inventory inventory;
    private SortType sortType = SortType.NEW;

    // items
    private final ItemStack decor = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
    private final ItemStack addButton = new ItemStack(Material.LIME_DYE);
    private final ItemStack getButton = new ItemStack(Material.BOOK);
    private final ItemStack delButton = new ItemStack(Material.RED_DYE);
    protected final ItemStack nextPage = new ItemStack(Material.ARROW);
    protected final ItemStack prevPage = new ItemStack(Material.ARROW);
    private final ItemStack back = new ItemStack(Material.BLACK_DYE);
    private ItemStack sort = sortType.buildItem();

    {
        ItemMeta itemMeta = decor.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(" ");
        decor.setItemMeta(itemMeta);

        itemMeta = nextPage.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName("Следующая страница ->");
        nextPage.setItemMeta(itemMeta);

        itemMeta = prevPage.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName("<- Предыдущая страница");
        prevPage.setItemMeta(itemMeta);

        itemMeta = back.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(ChatColor.GRAY + "Закрыть");
        back.setItemMeta(itemMeta);

        itemMeta = addButton.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(ChatColor.GREEN + "Добавить новую жалобу...");
        addButton.setItemMeta(itemMeta);

        itemMeta = getButton.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(ChatColor.GRAY + "Жалобы на игрока...");
        getButton.setItemMeta(itemMeta);

        itemMeta = delButton.getItemMeta();
        assert itemMeta != null;
        itemMeta.setDisplayName(ChatColor.RED + "Удалить жалобу...");
        delButton.setItemMeta(itemMeta);
    }

    public GUIHolder(@NonNull UUID owner) {
        this.owner = owner;
    }

    public void inventory(@NonNull Inventory inventory) {
        this.inventory = inventory;
    }

    public void nextSortType() {
        this.sortType = sortType == SortType.NEW ? SortType.OLD : SortType.NEW;
        this.sort = this.sortType.buildItem();
    }

    public GUIHolder changeWindow(@NonNull GUIWindow window) {
        this.currentWindow = window;
        return this;
    }

    public void build() {
        final Player base = base();
        if (base == null) return;
        if (inventory == null) return;
        inventory = Bukkit.createInventory(this, 54, Component.text(ChatColor.DARK_GRAY + currentWindow.getTitle()));
        Bukkit.getScheduler().runTaskAsynchronously(Reports.getInstance(), () -> {
            inventory.clear();

            setBasicItems();

            Report[] reports = new Report[0];
            int startIndex = page * 45;
            int slot = 0;

            switch (currentWindow.getName().toUpperCase()) {
                case "REPORT_LIST_FOR_ADMINS", "REPORT_LIST_FOR_MODERS" -> {
                    reports = Reports.getReportManager().getAllReports().toArray(new Report[0]);
                    Arrays.sort(reports);
                    switch (sortType) {
                        case NEW -> {
                            for (int i = reports.length - 1; i >= startIndex && slot < 45; i--) {
                                Report report = reports[i];
                                if (report.getModerator() != null && report.getModerator().getName().equalsIgnoreCase(base.getName())) {
                                    inventory.setItem(slot, ownCheckReportItem(report));
                                    slot++;
                                    continue;
                                }
                                inventory.setItem(slot, newReportItemStack(report));
                                slot++;
                            }
                        }
                        case OLD -> {
                            for (int i = startIndex; i < reports.length && slot < 45; i++) {
                                Report report = reports[i];
                                if (report.getModerator() != null && report.getModerator().getName().equalsIgnoreCase(base.getName())) {
                                    inventory.setItem(slot, ownCheckReportItem(report));
                                    slot++;
                                    continue;
                                }
                                inventory.setItem(slot, newReportItemStack(report));
                                slot++;
                            }
                        }
                    }
                }
                case "REMOVE_REPORTS" -> {
                    reports = Reports.getReportManager().getAllReports().toArray(new Report[0]);
                    Arrays.sort(reports);
                    switch (sortType) {
                        case NEW -> {
                            for (int i = reports.length - 1; i >= startIndex && slot < 45; i--) {
                                inventory.setItem(slot, newReportItemStack(reports[i]));
                                slot++;
                            }
                        }
                        case OLD -> {
                            for (int i = startIndex; i < reports.length && slot < 45; i++) {
                                inventory.setItem(slot, newReportItemStack(reports[i]));
                                slot++;
                            }
                        }
                    }
                }
                case "SEARCH_REPORTS" -> {
                    if (Bukkit.getOfflinePlayerIfCached(this.currentWindow.getPlayerName()) == null) {
                        inventory.close();
                        return;
                    }
                    reports = Reports.getReportManager().getReportsFrom(this.currentWindow.getPlayerName()).toArray(new Report[0]);
                    Arrays.sort(reports);
                    switch (sortType) {
                        case NEW -> {
                            for (int i = reports.length - 1; i >= startIndex && slot < 45; i--) {
                                Report report = reports[i];
                                if (report.getModerator() != null && report.getModerator().getName().equalsIgnoreCase(base.getName())) {
                                    inventory.setItem(slot, ownCheckReportItem(report));
                                    slot++;
                                    continue;
                                }
                                inventory.setItem(slot, newReportItemStack(report));
                                slot++;
                            }
                        }
                        case OLD -> {
                            for (int i = startIndex; i < reports.length && slot < 45; i++) {
                                Report report = reports[i];
                                if (report.getModerator() != null && report.getModerator().getName().equalsIgnoreCase(base.getName())) {
                                    inventory.setItem(slot, ownCheckReportItem(report));
                                    slot++;
                                    continue;
                                }
                                inventory.setItem(slot, newReportItemStack(report));
                                slot++;
                            }
                        }
                    }
                }
            }

            setNavigationButtons(startIndex, reports.length);
        });
        base.openInventory(inventory);
    }

    public void update() {
        final Player owner = Bukkit.getPlayer(this.owner);
        if (owner == null) return;
        build();
        owner.updateInventory();
    }

    public void nextPage() {
        this.page++;
    }

    public void prevPage() {
        if (this.page > 0) this.page--;
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }

    private @Nullable Player base() {
        return Bukkit.getPlayer(owner);
    }

    private ItemStack newReportItemStack(final Report report) {
        final ItemStack itemStack = new ItemStack(Reports.getConfigurationManager().getConfig("settings.yml")
                .m("report_item.material"));
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(Reports.getConfigurationManager().getConfig("settings.yml")
                .c("report_item.name").replace("%number%", "" + report.getNumber()));
        itemMeta.getPersistentDataContainer().set(NamespacedKey.minecraft("number"), PersistentDataType.INTEGER,
                report.getNumber());
        if (report.getStatus() == ReportStatus.OPENED) {
            itemMeta.setLore(Utils.replaceLorePlaceholders(Reports.getConfigurationManager().getConfig("settings.yml")
                    .cl("report_item.lore"), report));
        } else if (report.getStatus() == ReportStatus.CHECK) {
            itemMeta.setLore(Utils.replaceLorePlaceholders(Reports.getConfigurationManager().getConfig("settings.yml")
                    .cl("report_item.lore_active"), report));
        }
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private ItemStack ownCheckReportItem(final Report report) {
        final ItemStack itemStack = new ItemStack(Reports.getConfigurationManager().getConfig("settings.yml")
                .m("report_item.material"));
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.setDisplayName(Reports.getConfigurationManager().getConfig("settings.yml")
                .c("report_item.name").replace("%number%", "" + report.getNumber()));
        itemMeta.getPersistentDataContainer().set(NamespacedKey.minecraft("number"), PersistentDataType.INTEGER,
                report.getNumber());
        itemMeta.setLore(Utils.replaceLorePlaceholders(Reports.getConfigurationManager().getConfig("settings.yml")
                .cl("report_item.lore_own"), report));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }

    private void setBasicItems() {
        switch (currentWindow.getName().toUpperCase()) {
            case "REPORT_LIST_FOR_ADMINS" -> {
                inventory.setItem(45, decor);
                inventory.setItem(46, decor);
                inventory.setItem(47, addButton);
                inventory.setItem(48, decor);
                inventory.setItem(49, getButton);
                inventory.setItem(50, decor);
                inventory.setItem(51, delButton);
                inventory.setItem(52, sort);
                inventory.setItem(53, decor);
            }
            case "REPORT_LIST_FOR_MODERS", "REMOVE_REPORTS", "SEARCH_REPORTS" -> {
                inventory.setItem(45, decor);
                inventory.setItem(46, decor);
                inventory.setItem(47, decor);
                inventory.setItem(48, decor);
                inventory.setItem(49, back);
                inventory.setItem(50, sort);
                inventory.setItem(51, decor);
                inventory.setItem(52, decor);
                inventory.setItem(53, decor);
            }
        }
    }

    private void setNavigationButtons(int startIndex, int totalReports) {
        if (startIndex + 36 < totalReports) {
            inventory.setItem(53, nextPage);
        } else {
            inventory.setItem(53, decor);
        }

        if (startIndex > 0) {
            inventory.setItem(45, prevPage);
        } else {
            inventory.setItem(45, decor);
        }
    }

    public enum SortType {
        NEW,
        OLD;

        public ItemStack buildItem() {
            ItemStack itemStack = new ItemStack(Material.NETHER_STAR);
            ItemMeta itemMeta = itemStack.getItemMeta();
            assert itemMeta != null;
            itemMeta.setDisplayName(ChatColor.GOLD + "Сортировка");
            switch (this) {
                case NEW -> {
                    itemMeta.setLore(Stream.of(
                                    " ",
                                    " &6●&f Сначала новые",
                                    " &7●&f Сначала старые",
                                    " ").map(line -> line.replace('&', '§'))
                            .toList());
                }
                case OLD -> {
                    itemMeta.setLore(Stream.of(
                                    " ",
                                    " &7●&f Сначала новые",
                                    " &6●&f Сначала старые",
                                    " ").map(line -> line.replace('&', '§'))
                            .toList());
                }
            }
            itemStack.setItemMeta(itemMeta);
            return itemStack;
        }
    }
}
