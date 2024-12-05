package ru.shirk.reports.users;

import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.shirk.reports.Reports;
import ru.shirk.reports.storage.database.DatabaseStorage;

import java.time.LocalDateTime;

public class UsersManager {

    private final DatabaseStorage databaseStorage;

    public UsersManager(DatabaseStorage databaseStorage) {
        this.databaseStorage = databaseStorage;
    }

    public void loadUser(final boolean isModerator, final String name, final int passedChecks,
                         final int failedChecks, final LocalDateTime lastCheck, final int trueReports, final int falseReports,
                         final IUser.UserMuteInfo muteInfo) {
        final IUser iUser = getUser(name);
        if (iUser != null && iUser.getBase() != null) {
            if (iUser.getBase() == null) return;
            if ((iUser.getBase().hasPermission("reports.moderator")
                    || iUser.getBase().hasPermission("reports.admin")) && iUser instanceof User user) {
                Bukkit.getScheduler().runTaskAsynchronously(Reports.getInstance(), () -> user.toModerator().save());
            } else if (!(iUser.getBase().hasPermission("reports.moderator")
                    || iUser.getBase().hasPermission("reports.admin")) && iUser instanceof Moderator moderator) {
                moderator.save();
            }
            return;
        }

        final IUser user = isModerator ? new Moderator(
                name,
                passedChecks,
                failedChecks,
                lastCheck,
                trueReports,
                falseReports,
                muteInfo
        ) : new User(
                name,
                passedChecks,
                failedChecks,
                lastCheck,
                trueReports,
                falseReports,
                muteInfo
        );

        databaseStorage.addUser(user);
        Reports.getInstance().getSLF4JLogger().info("added user {}", user.getName());
    }

    public @Nullable IUser getUser(final String username) {
        return databaseStorage.getUser(username);
    }

    public Moderator getModerator(final String username) {
        final IUser user = getUser(username);
        if (!(user instanceof Moderator moderator)) {
            return null;
        }
        return moderator;
    }

    public void muteUser(@NonNull String executor, @NonNull IUser user, @NonNull IUser.UserMuteInfo userMuteInfo) {
        if (user.getBase() != null && user.getBase().hasPermission("reports.admin")) return;
        user.mute(userMuteInfo);
        if (user.getMuteInfo() == null) return;
        if (user.getBase() != null && user.getBase().isOnline()) {
            user.getBase().sendMessage(Reports.getConfigurationManager().getConfig("lang.yml").c("mute.mutedFeedback")
                    .replace("%duration%", user.getMuteInfo().formattedDuration())
                    .replace("%reason%", user.getMuteInfo().reason()));
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission("reports.moderator") && !player.hasPermission("reports.admin")) continue;
                player.sendMessage(Reports.getConfigurationManager().getConfig("lang.yml").c("mute.staffBroadcast")
                        .replace("%executor%", executor).replace("%mutedPlayer%", user.getName())
                        .replace("%duration%", user.getMuteInfo().formattedDuration())
                        .replace("%reason%", user.getMuteInfo().reason()));
            }
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("reports.moderator") && !player.hasPermission("reports.admin")) continue;
            player.sendMessage(Reports.getConfigurationManager().getConfig("lang.yml").c("mute.staffBroadcastOffline")
                    .replace("%executor%", executor).replace("%mutedPlayer%", user.getName())
                    .replace("%duration%", user.getMuteInfo().formattedDuration())
                    .replace("%reason%", user.getMuteInfo().reason()));
        }
        user.save();
    }

    public void unmuteUser(@NonNull String executor, @NonNull IUser user, @NonNull String reason) {
        if (user.getBase() != null && user.getBase().hasPermission("reports.admin")) return;
        user.unmute(reason);
        if (user.getBase() != null && user.getBase().isOnline()) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!player.hasPermission("reports.moderator") && !player.hasPermission("reports.admin")) continue;
                player.sendMessage(Reports.getConfigurationManager().getConfig("lang.yml").c("unmute.staffBroadcast")
                        .replace("%executor%", executor).replace("%mutedPlayer%", user.getName())
                        .replace("%reason%", reason));
            }
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!player.hasPermission("reports.moderator") && !player.hasPermission("reports.admin")) return;
            player.sendMessage(Reports.getConfigurationManager().getConfig("lang.yml").c("unmute.staffBroadcastOffline")
                    .replace("%executor%", executor).replace("%mutedPlayer%", user.getName())
                    .replace("%reason%", reason));
        }
        user.save();
    }

    public void saveUser(final IUser user) {
        if (!databaseStorage.containsUser(user.getName())) return;
        databaseStorage.saveUser(user);
    }

    public void addTrueReports(final String username) {
        final IUser user = databaseStorage.getUser(username);
        if (user == null) return;
        user.addTrueReports();
        user.save();
    }

    public void addFalseReports(final String username) {
        final IUser user = databaseStorage.getUser(username);
        if (user == null) return;
        user.addFalseReports();
        user.setLastCheck(LocalDateTime.now());
        user.save();
    }

    @SuppressWarnings("unused")
    public void addPassedChecks(final String username) {
        final IUser user = databaseStorage.getUser(username);
        if (user == null) return;
        user.addPassedChecks();
        user.save();
    }

    @SuppressWarnings("unused")
    public void addFailedChecks(final String username) {
        final IUser user = databaseStorage.getUser(username);
        if (user == null) return;
        user.addFailedChecks();
        user.setLastCheck(LocalDateTime.now());
        user.save();
    }
}
