package ru.shirk.reports.users;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;

import java.time.LocalDateTime;

public class User implements IUser {

    @NonNull
    private final String name;
    private int passedChecks;
    private int failedChecks;
    @Nullable
    private LocalDateTime lastCheck;
    private int trueReports;
    private int falseReports;
    @Nullable
    private UserMuteInfo userMuteInfo;

    public User(@NonNull String name, int passedChecks, int failedChecks, @Nullable LocalDateTime lastCheck,
                int trueReports, int falseReports, @Nullable UserMuteInfo userMuteInfo) {
        this.name = name;
        this.passedChecks = passedChecks;
        this.failedChecks = failedChecks;
        this.lastCheck = lastCheck;
        this.trueReports = trueReports;
        this.falseReports = falseReports;
        this.userMuteInfo = userMuteInfo;
    }

    @Override
    public @NonNull String getName() {
        return name;
    }

    @Override
    public int getPassedChecks() {
        return passedChecks;
    }

    @Override
    public void addPassedChecks() {
        passedChecks = passedChecks + 1;
    }

    @Override
    public int getFailedChecks() {
        return failedChecks;
    }

    @Override
    public void addFailedChecks() {
        failedChecks = failedChecks + 1;
    }

    @Override
    public @Nullable LocalDateTime getLastCheck() {
        return lastCheck;
    }

    @Override
    public int getTrueReports() {
        return trueReports;
    }

    @Override
    public void addTrueReports() {
        trueReports = trueReports + 1;
    }

    @Override
    public int getFalseReports() {
        return falseReports;
    }

    @Override
    public void addFalseReports() {
        falseReports = falseReports + 1;
    }

    @Override
    public void setLastCheck(@Nullable LocalDateTime timestamp) {
        this.lastCheck = timestamp;
    }

    @Override
    public void mute(@NonNull UserMuteInfo userMuteInfo) {
        this.userMuteInfo = userMuteInfo;
    }

    @Override
    public void unmute(@NonNull String reason) {
        this.userMuteInfo = null;
    }

    @Override
    public @Nullable UserMuteInfo getMuteInfo() {
        return this.userMuteInfo;
    }

    public @NonNull Moderator toModerator() {
        return new Moderator(name, passedChecks, failedChecks, lastCheck, trueReports, falseReports, userMuteInfo);
    }
}
