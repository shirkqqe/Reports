package ru.shirk.reports;

import lombok.Getter;
import lombok.NonNull;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import ru.shirk.reports.commands.Commands;
import ru.shirk.reports.gui.ReportsMenu;
import ru.shirk.reports.impl.Server;
import ru.shirk.reports.impl.ServerImpl;
import ru.shirk.reports.listeners.bukkit.BukkitEvents;
import ru.shirk.reports.reports.ReportManager;
import ru.shirk.reports.storage.configs.ConfigurationManager;
import ru.shirk.reports.storage.database.DatabaseStorage;
import ru.shirk.reports.storage.redis.RedisManager;
import ru.shirk.reports.users.UsersManager;

import java.io.File;
import java.util.Objects;

public final class Reports extends JavaPlugin {

    @Getter
    private static Reports instance;
    @Getter
    private static final ConfigurationManager configurationManager = new ConfigurationManager();
    @Getter
    private static ReportManager reportManager;
    @Getter
    private static UsersManager usersManager;
    @Getter
    private static ReportsMenu reportsMenu;
    @Getter
    private static RedisManager redisManager;

    @Override
    public void onEnable() {
        instance = this;
        loadConfigs();
        DatabaseStorage databaseStorage = new DatabaseStorage();
        redisManager = new RedisManager();
        reportManager = new ReportManager(databaseStorage, redisManager);
        usersManager = new UsersManager(databaseStorage);
        reportsMenu = new ReportsMenu(this);
        final Commands commands = new Commands(redisManager);
        Objects.requireNonNull(this.getCommand("reports")).setExecutor(commands);
        Objects.requireNonNull(this.getCommand("cheatreport")).setExecutor(commands);
        Objects.requireNonNull(this.getCommand("reportlist")).setExecutor(commands);
        Objects.requireNonNull(this.getCommand("reports")).setTabCompleter(commands);
        Objects.requireNonNull(this.getCommand("cheatreport")).setTabCompleter(commands);
        Objects.requireNonNull(this.getCommand("rmute")).setExecutor(commands);
        Objects.requireNonNull(this.getCommand("runmute")).setExecutor(commands);
        Objects.requireNonNull(this.getCommand("rtoggle")).setExecutor(commands);
        this.getServer().getPluginManager().registerEvents(new BukkitEvents(), this);
    }

    @Override
    public void onDisable() {
        if (redisManager != null) {
            redisManager.unload();
        }
        instance = null;
    }

    private void loadConfigs() {
        try {
            if (!(new File(getDataFolder(), "settings.yml")).exists()) {
                getConfigurationManager().createFile("settings.yml");
            }
            if (!(new File(getDataFolder(), "lang.yml")).exists()) {
                getConfigurationManager().createFile("lang.yml");
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static @NonNull Server getCurrentServer() {
        final ConfigurationSection section = configurationManager.getConfig("settings.yml").getFile().getConfigurationSection("properties");
        if (section == null) {
            throw new IllegalStateException("Server properties is not configured!");
        }
        return new ServerImpl();
    }
}
