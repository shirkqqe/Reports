package ru.shirk.reports.tools;

import lombok.NonNull;
import ru.shirk.reports.Reports;
import ru.shirk.reports.reports.Report;
import ru.shirk.reports.users.IUser;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Utils {

    public static String getFinalArg(final String[] args, final int start) {
        final StringBuilder builder = new StringBuilder();
        for (int i = start; i < args.length; i++) {
            if (i != start) {
                builder.append(" ");
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    public static String dateToString(final LocalDateTime time) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return time.format(formatter);
    }

    public static List<String> replaceLorePlaceholders(final List<String> input, final Report report) {
        final IUser source = Reports.getUsersManager().getUser(report.getUser());
        final IUser target = Reports.getUsersManager().getUser(report.getReported());
        if (source == null || target == null) {
            Reports.getInstance().getSLF4JLogger().info("(source || target) == null");
            return List.of();
        }
        if (report.getModerator() == null) {
            return input.stream()
                    .map(line -> line.replace("%sender%", report.getUser())
                            .replace("%comment%", report.getReason())
                            .replace("%count%", String.valueOf(Reports.getReportManager()
                                    .getReportsFrom(report.getReported()).size()))
                            .replace("%suspected%", report.getReported())
                            .replace("%date%", Utils.dateToString(report.getCreatedDate()))
                            .replace("%server%", report.getServerId())
                            .replace("%source_true%", "" + source.getTrueReports())
                            .replace("%source_false%", "" + source.getFalseReports())
                            .replace("%source_passed%", "" + source.getPassedChecks())
                            .replace("%source_failed%", "" + source.getFailedChecks())
                            .replace("%target_true%", "" + target.getTrueReports())
                            .replace("%target_false%", "" + target.getFalseReports())
                            .replace("%target_passed%", "" + target.getPassedChecks())
                            .replace("%target_failed%", "" + target.getFailedChecks())
                            .replace("%last_check%", target.getLastCheck() == null ? "-"
                                    : dateToString(target.getLastCheck()))

                    ).toList();
        } else {
            return input.stream().map(line -> line.replace("%sender%", report.getUser()).replace("%comment%", report.getReason())
                    .replace("%moderator%", report.getModerator().getName())
                    .replace("%count%", String.valueOf(Reports.getReportManager()
                            .getReportsFrom(report.getReported()).size()))
                    .replace("%suspected%", report.getReported())
                    .replace("%date%", Utils.dateToString(report.getCreatedDate()))
                    .replace("%server%", report.getServerId())
                    .replace("%source_true%", "" + source.getTrueReports())
                    .replace("%source_false%", "" + source.getFalseReports())
                    .replace("%source_passed%", "" + source.getPassedChecks())
                    .replace("%source_failed%", "" + source.getFailedChecks())
                    .replace("%target_true%", "" + target.getTrueReports())
                    .replace("%target_false%", "" + target.getFalseReports())
                    .replace("%target_passed%", "" + target.getPassedChecks())
                    .replace("%target_failed%", "" + target.getFailedChecks())
                    .replace("%last_check%", target.getLastCheck() == null ? "-"
                            : dateToString(target.getLastCheck()))).toList();
        }
    }

    public static LocalDateTime argumentToDateTime(@NonNull String argument) {
        if (argument.endsWith("d")) {
            try {
                int i = Integer.parseInt(argument.replace("d", ""));
                return LocalDateTime.now().plusDays(i);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (argument.endsWith("h")) {
            try {
                int i = Integer.parseInt(argument.replace("h", ""));
                return LocalDateTime.now().plusHours(i);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (argument.endsWith("m")) {
            try {
                int i = Integer.parseInt(argument.replace("m", ""));
                return LocalDateTime.now().plusMinutes(i);
            } catch (NumberFormatException e) {
                return null;
            }
        } else if (argument.endsWith("s")) {
            try {
                int i = Integer.parseInt(argument.replace("s", ""));
                return LocalDateTime.now().plusSeconds(i);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
