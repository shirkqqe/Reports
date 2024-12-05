package ru.shirk.reports.tools;

import lombok.NonNull;
import net.kyori.adventure.text.Component;
import net.wesjd.anvilgui.AnvilGUI;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.Collections;

public abstract class AnvilDialog {

    private final Player player;
    private final AnvilGUI.Builder anvil;

    public AnvilDialog(@NonNull Player player, @NonNull JavaPlugin plugin, @NonNull String title, @NonNull String message) {
        this.player = player;
        this.anvil = new AnvilGUI.Builder();
        anvil.plugin(plugin);
        anvil.title(title);
        anvil.itemLeft(buildItemStack(message));
        anvil.onClick((slot, stateSnapshot) -> {
            if (slot != AnvilGUI.Slot.OUTPUT) {
                return Collections.emptyList();
            }

            if (stateSnapshot.getText().equalsIgnoreCase(ChatColor.translateAlternateColorCodes('&', message))) {
                return Collections.emptyList();
            }

            return Arrays.asList(
                    AnvilGUI.ResponseAction.close(),
                    AnvilGUI.ResponseAction.run(() -> onResponse(stateSnapshot.getPlayer(), stateSnapshot.getText()))
            );
        });
    }

    public void show() {
        anvil.open(player);
    }

    protected abstract void onResponse(@NonNull Player player, @NonNull String response);

    private ItemStack buildItemStack(@NonNull String message) {
        final ItemStack itemStack = new ItemStack(Material.NAME_TAG, 1);
        final ItemMeta itemMeta = itemStack.getItemMeta();
        itemMeta.displayName(Component.text(ChatColor.translateAlternateColorCodes('&', message)));
        itemStack.setItemMeta(itemMeta);
        return itemStack;
    }
}
