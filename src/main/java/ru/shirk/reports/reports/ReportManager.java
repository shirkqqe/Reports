package ru.shirk.reports.reports;

import lombok.AllArgsConstructor;
import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import ru.shirk.reports.Reports;
import ru.shirk.reports.storage.database.DatabaseStorage;
import ru.shirk.reports.storage.redis.RedisManager;

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
