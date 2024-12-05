package ru.shirk.reports.reports;

import lombok.*;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import ru.shirk.reports.Reports;
import ru.shirk.reports.users.Moderator;

import java.time.LocalDateTime;

@Getter
@EqualsAndHashCode
@ToString
public class Report implements Comparable<Report> {

    @Setter
    @NonNull
    private ReportStatus status;
    private final int number;
    @NonNull
    private final String reported;
    @NonNull
    private final String user;
    @NonNull
    private final String reason;
    @Nullable
    private Moderator moderator;
    @NonNull
    private final LocalDateTime createdDate;
    @NonNull
    private final String serverId;

    public Report(int number, @NonNull String reported, @NonNull String user, @NonNull String reason, @Nullable Moderator
            moderator, @NonNull LocalDateTime dateCreated, @NonNull ReportStatus status, @NonNull String serverId) {
        this.status = status;
        this.serverId = serverId;
        this.number = number;
        this.reported = reported;
        this.user = user;
        this.reason = reason;
        this.createdDate = dateCreated;
        this.moderator = moderator;
    }

    public void check(@NonNull Moderator moderator) {
        if (this.moderator != null) return;
        this.moderator = moderator;
        moderator.addActiveReport(this);
        this.status = ReportStatus.CHECK;
    }

    public void complete(@NonNull Moderator moderator, @NonNull String comment, boolean reportStatus) {
        moderator.removeActiveReport(this);
        this.moderator = null;
        this.status = ReportStatus.CLOSED;
        Reports.getReportManager().saveReport(this);
        if (reportStatus) {
            Reports.getUsersManager().addTrueReports(user);
        } else {
            Reports.getUsersManager().addFalseReports(user);
        }
        Reports.getRedisManager().sendMessageToPlayer(Reports.getCurrentServer(), user, Reports.getConfigurationManager()
                .getConfig("lang.yml").c("reportClosed")
                .replace("%comment%", comment)
                .replace("%number%", "" + this.number)
                .replace("%player%", reported)
                .replace("%status%", reportStatus ? "Принята" : "Ложная"));
        final Player moder = Bukkit.getPlayer(moderator.getName());
        if (moder != null) {
            moder.sendMessage(Reports.getConfigurationManager()
                    .getConfig("lang.yml").c("reportClosed")
                    .replace("%comment%", comment)
                    .replace("%number%", "" + this.number)
                    .replace("%player%", reported)
                    .replace("%status%", reportStatus ? "Принята" : "Ложная"));
        }
    }

    public void save() {
        Reports.getReportManager().saveReport(this);
    }

    @Override
    public int compareTo(@NotNull Report o) {
        return createdDate.compareTo(o.getCreatedDate());
    }
}
