package ru.shirk.reports.reports;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import ru.shirk.reports.Reports;
import ru.shirk.reports.storage.database.DatabaseStorage;
import ru.shirk.reports.storage.redis.RedisManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

@AllArgsConstructor
public class ReportManager {

    private final DatabaseStorage databaseStorage;
    private final RedisManager redisManager;

    public void addReport(final Report report, boolean dontLog) {
        if(report.getStatus() == ReportStatus.CLOSED) return;
        databaseStorage.addReport(report);
        if(!dontLog) {
            redisManager.sendMessage(
                    Reports.getCurrentServer(),
                    Reports.getConfigurationManager().getConfig("lang.yml").c("newReport")
                            .replace("%suspected%", report.getReported())
                            .replace("%count%", String.valueOf(getReportsFrom(report.getReported()).size()))
                            .replace("%server%", Reports.getCurrentServer().getName())
            );
        }
    }

    public @NonNull List<Report> getReportsFrom(@NonNull String reported) {
        return databaseStorage.getReportsFromUser(reported);
    }

    public void saveReport(@NonNull Report report) {
        databaseStorage.updateReport(report);
    }

    public @Nullable Report getReport(final int number) {
        return databaseStorage.getReport(number);
    }


    public List<Report> getAllReports() {
        return databaseStorage.getReports();
    }

    public @Nullable Report getLastReport(@NonNull String user) {
        return databaseStorage.getLastUserReport(user);
    }

    public @NonNull HashMap<String, Report> getUsersReports() {
        final HashMap<String, Report> map = new HashMap<>();
        final HashSet<String> uniqueUsers = databaseStorage.getUniqueReportedUsers();

        for (String user : uniqueUsers) {
            if (user == null) continue;
            final Report report = databaseStorage.getLastUserReport(user);
            if (report == null) continue;
            map.put(user, report);
        }

        return map;
    }

    public boolean removeReport(final int reportId) {
        if (!databaseStorage.containsReport(reportId)) return false;
        try {
            databaseStorage.removeReport(reportId);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public int getNextReportId() {
        return databaseStorage.getNextId();
    }
}
