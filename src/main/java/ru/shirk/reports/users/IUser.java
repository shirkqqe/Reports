package ru.shirk.reports.users;

import lombok.NonNull;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;
import ru.shirk.reports.Reports;

import java.time.Duration;
import java.time.LocalDateTime;

public interface IUser {
    @NonNull
    String getName();

    int getPassedChecks();

    void addPassedChecks();

    int getFailedChecks();

    void addFailedChecks();

    @Nullable
    LocalDateTime getLastCheck();

    int getTrueReports();

    void addTrueReports();

    int getFalseReports();

    void addFalseReports();

    void setLastCheck(@Nullable LocalDateTime timestamp);

    void mute(@NonNull UserMuteInfo userMuteInfo);

    void unmute(@NonNull String reason);

    @Nullable
    UserMuteInfo getMuteInfo();

    @Nullable
    default Player getBase() {
        return Bukkit.getPlayer(getName());
    }

    default void save() {
        Reports.getUsersManager().saveUser(this);
    }

    record UserMuteInfo(@NonNull LocalDateTime expiryDate, @NonNull String reason) {
        public String formattedDuration() {
            final Duration duration = Duration.between(LocalDateTime.now(), expiryDate);
            final StringBuilder builder = new StringBuilder();
            if (duration.toDaysPart() > 0) builder.append(duration.toDaysPart()).append(" дн. ");
            if (duration.toHoursPart() > 0) builder.append(duration.toHoursPart()).append(" ч. ");
            if (duration.toMinutesPart() > 0) builder.append(duration.toMinutesPart()).append(" мин. ");
            if (duration.toSecondsPart() > 0) builder.append(duration.toSecondsPart()).append(" сек. ");
            return builder.toString();
        }

        public boolean isActive() {
            return Duration.between(LocalDateTime.now(), expiryDate).getSeconds() > 0;
        }
    }
}
