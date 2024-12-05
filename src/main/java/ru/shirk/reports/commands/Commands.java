package ru.shirk.reports.commands;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.shirk.reports.Reports;
import ru.shirk.reports.listeners.api.ReportSendingByPlayerEvent;
import ru.shirk.reports.reports.Report;
import ru.shirk.reports.reports.ReportStatus;
import ru.shirk.reports.storage.configs.Configuration;
import ru.shirk.reports.storage.redis.RedisManager;
import ru.shirk.reports.tools.Utils;
import ru.shirk.reports.users.IUser;
import ru.shirk.reports.users.Moderator;

import java.time.LocalDateTime;
import java.util.List;

@AllArgsConstructor
public class Commands implements CommandExecutor, TabCompleter {

    private final Configuration config = Reports.getConfigurationManager().getConfig("lang.yml");
    private final RedisManager redisManager;

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        switch (command.getName().toLowerCase()) {
            case "reports" -> {
                if (!sender.hasPermission("reports.admin")) {
                    sender.sendMessage("§7Команда не найдена или у Вас нет доступа к ней.");
                    return true;
                }
                if (args.length == 0 && !(sender instanceof Player)) {
                    sender.sendMessage("Команда только для игроков!");
                    return true;
                }
                if (args.length == 0) return true;
                switch (args[0].toLowerCase()) {
                    case "add" -> {
                        try {
                            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(args[1]);
                            if (offlinePlayer == null) {
                                sender.sendMessage(config.c("playerNotFound"));
                                return true;
                            }
                            final String reason = Utils.getFinalArg(args, 2);
                            final Report report = new Report(
                                    Reports.getReportManager().getNextReportId(),
                                    args[1],
                                    !(sender instanceof Player) ? "System" : sender.getName(),
                                    reason,
                                    null,
                                    LocalDateTime.now(),
                                    ReportStatus.OPENED,
                                    Reports.getCurrentServer().getName()
                            );
                            Reports.getReportManager().addReport(report, false);
                            sender.sendMessage(config.c("reportCreated").replace("%player%", args[1]));
                        } catch (Exception e) {
                            sender.sendMessage(ChatColor.RED + "Произошла ошибка!");
                        }
                    }
                    case "del" -> {
                        try {
                            final int number = Integer.parseInt(args[1]);
                            if (Reports.getReportManager().removeReport(number)) {
                                sender.sendMessage(config.c("reportDeleted"));
                            } else {
                                sender.sendMessage(config.c("reportNotFound"));
                            }
                        } catch (Exception e) {
                            sender.sendMessage(ChatColor.RED + "Произошла ошибка!");
                        }
                    }
                    case "get" -> {
                        try {
                            try {
                                final int number = Integer.parseInt(args[1]);
                                final Report report = Reports.getReportManager().getReport(number);
                                if (report == null) {
                                    throw new NumberFormatException();
                                }
                                sender.sendMessage(report.toString());
                            } catch (NumberFormatException e) {
                                if (Reports.getReportManager().getReportsFrom(args[1]).isEmpty()) {
                                    sender.sendMessage(config.c("notResults"));
                                    return true;
                                }
                                for (Report report : Reports.getReportManager().getReportsFrom(args[1])) {
                                    sender.sendMessage(report.toString());
                                }
                            }
                        } catch (Exception e) {
                            sender.sendMessage(ChatColor.RED + "Произошла ошибка!");
                        }
                    }
                    case "check" -> {
                        if (!(sender instanceof Player player)) return true;
                        try {
                            final int number = Integer.parseInt(args[1]);
                            Report report = Reports.getReportManager().getReport(number);
                            if (report == null) {
                                sender.sendMessage("Жалобы с таким номером нет.");
                                return true;
                            }

                            final Moderator moderator = Reports.getUsersManager().getModerator(player.getName());
                            if (moderator == null) {
                                sender.sendMessage(ChatColor.RED + "Произошла ошибка!");
                                return true;
                            }
                            report.check(moderator);
                            report.save();
                            sender.sendMessage("Выполнено!");
                        } catch (Exception e) {
                            sender.sendMessage(ChatColor.RED + "Произошла ошибка!");
                        }
                    }
                    case "reload" -> {
                        try {
                            Reports.getConfigurationManager().reloadConfigs();
                            sender.sendMessage(config.c("reloaded"));
                        } catch (Exception e) {
                            sender.sendMessage(ChatColor.RED + "Произошла ошибка!");
                        }
                    }
                }
            }
            case "reportlist" -> {
                if (!haveAccess(sender)) {
                    sender.sendMessage("§7Команда не найдена или у Вас нет доступа к ней.");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Команда только для игроков!");
                    return true;
                }
                Reports.getReportsMenu().open(player);
            }
            case "cheatreport" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Команда только для игроков!");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(config.c("reportUsing"));
                    return true;
                }
                if (args[0].equalsIgnoreCase(player.getName())) {
                    player.sendMessage(config.c("sendError"));
                    return true;
                }
                final IUser user = Reports.getUsersManager().getUser(player.getName());
                if (user != null && user.getBase() != null && user.getMuteInfo() != null && user.getMuteInfo().isActive()) {
                    user.getBase().sendMessage(Reports.getConfigurationManager().getConfig("lang.yml").c("mute.mutedFeedback")
                            .replace("%duration%", user.getMuteInfo().formattedDuration())
                            .replace("%reason%", user.getMuteInfo().reason()));
                    return true;
                }


                final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayerIfCached(args[0]);
                if (offlinePlayer == null) {
                    player.sendMessage(ChatColor.RED + "Игрок не найден!");
                    return true;
                }
                final String reason = Utils.getFinalArg(args, 1);
                final Report report = new Report(
                        Reports.getReportManager().getNextReportId(),
                        args[0],
                        player.getName(),
                        reason,
                        null,
                        LocalDateTime.now(),
                        ReportStatus.OPENED,
                        Reports.getCurrentServer().getProxyName()
                );

                ReportSendingByPlayerEvent event = new ReportSendingByPlayerEvent(
                        report,
                        player,
                        offlinePlayer
                );
                event.callEvent();

                if (event.isCancelled()) return true;

                Reports.getReportManager().addReport(report, false);
                player.sendMessage(config.c("sendFeedback"));
            }
            case "rmute" -> {
                if (!haveAccess(sender)) {
                    sender.sendMessage("§7Команда не найдена или у Вас нет доступа к ней.");
                    return true;
                }
                if (args.length < 3) {
                    sender.sendMessage(config.c("mute.usage"));
                    return true;
                }
                final IUser target = Reports.getUsersManager().getUser(args[0]);
                if (target == null) {
                    sender.sendMessage(config.c("playerNotFound"));
                    return true;
                }

                final LocalDateTime time = Utils.argumentToDateTime(args[1]);
                if (time == null) {
                    sender.sendMessage(config.c("mute.invalidDuration"));
                    return true;
                }

                final IUser.UserMuteInfo userMuteInfo = new IUser.UserMuteInfo(time, Utils.getFinalArg(args, 2));
                Reports.getUsersManager().muteUser(sender instanceof ConsoleCommandSender ? "System" : sender.getName(), target,
                        userMuteInfo);
                sender.sendMessage(config.c("mute.staffFeedback").replace("%mutedPlayer%", args[0])
                        .replace("%duration%", userMuteInfo.formattedDuration()).replace("%reason%", userMuteInfo.reason()));
            }
            case "runmute" -> {
                if (!sender.hasPermission("reports.admin")) {
                    sender.sendMessage("§7Команда не найдена или у Вас нет доступа к ней.");
                    return true;
                }
                if (args.length < 2) {
                    sender.sendMessage(config.c("unmute.usage"));
                    return true;
                }
                final IUser target = Reports.getUsersManager().getUser(args[0]);
                if (target == null) {
                    sender.sendMessage(config.c("unmute.playerNotFound"));
                    return true;
                }
                if (target.getMuteInfo() == null || !target.getMuteInfo().isActive()) {
                    sender.sendMessage(config.c("unmute.error"));
                    return true;
                }

                Reports.getUsersManager().unmuteUser(sender instanceof ConsoleCommandSender ? "System" : sender.getName(),
                        target, Utils.getFinalArg(args, 1));
            }
            case "rtoggle" -> {
                if (!haveAccess(sender)) {
                    sender.sendMessage("§7Команда не найдена или у Вас нет доступа к ней.");
                    return true;
                }
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Команда только для игроков!");
                    return true;
                }

                redisManager.setNotificationToggle(player.getUniqueId());
                if (redisManager.isNotificationsToggled(player.getUniqueId())) {
                    player.sendMessage(config.c("rtoggle.disabled"));
                    return true;
                }
                player.sendMessage(config.c("rtoggle.enabled"));
            }
        }
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (command.getName().equalsIgnoreCase("reports") && sender.hasPermission("reports.admin")) {
            if (args.length == 1) {
                return List.of("add", "del", "get", "reload", "check");
            }
        } else if (command.getName().equalsIgnoreCase("cheatreport")) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
        } else if (command.getName().equalsIgnoreCase("rmute") || command.getName().equalsIgnoreCase("runmute")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }

    private boolean haveAccess(@NonNull CommandSender sender) {
        return sender.hasPermission("reports.admin") || sender.hasPermission("reports.moderator");
    }
}
