package ru.shirk.reports.storage.database;

import lombok.NonNull;
import org.jetbrains.annotations.Nullable;
import ru.shirk.reports.Reports;
import ru.shirk.reports.reports.Report;
import ru.shirk.reports.reports.ReportStatus;
import ru.shirk.reports.users.IUser;
import ru.shirk.reports.users.Moderator;
import ru.shirk.reports.users.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;

public class DatabaseStorage {
    private final DatabaseManager mySqlManager;

    public DatabaseStorage() {
        mySqlManager = new DatabaseManager(Reports.getConfigurationManager());
    }

    public void addReport(@NonNull Report report) {
        final Connection connection = mySqlManager.getConnection();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO `" + mySqlManager
                    .getBaseName() + "`.`reports` (`username`, `reported`, `reason`, `server-id`) VALUES (?, ?, ?, ?)");

            preparedStatement.setString(1, report.getUser());
            preparedStatement.setString(2, report.getReported());
            preparedStatement.setString(3, report.getReason());
            preparedStatement.setString(4, report.getServerId());

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при добавлении жалобы", e);
        } finally {
            mySqlManager.returnConnection(connection);
        }
    }

    public @NonNull ArrayList<Report> getReports() {
        final ArrayList<Report> reportList = new ArrayList<>();
        final Connection connection = mySqlManager.getConnection();

        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SELECT * FROM `" + mySqlManager.getBaseName() + "`.`reports`")) {
            while (resultSet.next()) {
                final ReportStatus reportStatus = ReportStatus.valueOf(resultSet.getString("status"));
                if (reportStatus == ReportStatus.CLOSED) continue;
                Moderator moderator = null;
                if (resultSet.getString("moderator") != null) {
                    final IUser user = getUser(resultSet.getString("moderator"));
                    if (user instanceof Moderator) moderator = (Moderator) user;
                }
                reportList.add(new Report(
                        resultSet.getInt("id"),
                        resultSet.getString("reported"),
                        resultSet.getString("username"),
                        resultSet.getString("reason"),
                        moderator,
                        resultSet.getObject("report-date", LocalDateTime.class),
                        reportStatus,
                        resultSet.getString("server-id")
                ));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при загрузке жалоб", e);
        } finally {
            mySqlManager.returnConnection(connection);
        }
        return reportList;
    }

    public @Nullable Report getReport(int id) {
        final Connection connection = mySqlManager.getConnection();
        Report report = null;

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `" + mySqlManager.getBaseName()
                    + "`.`reports` WHERE `id` = ?");
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                final ReportStatus reportStatus = ReportStatus.valueOf(resultSet.getString("status"));
                if (reportStatus == ReportStatus.CLOSED) continue;
                Moderator moderator = null;
                if (resultSet.getString("moderator") != null) {
                    final IUser user = getUser(resultSet.getString("moderator"));
                    if (user instanceof Moderator) moderator = (Moderator) user;
                }
                report = new Report(
                        resultSet.getInt("id"),
                        resultSet.getString("reported"),
                        resultSet.getString("username"),
                        resultSet.getString("reason"),
                        moderator,
                        resultSet.getObject("report-date", LocalDateTime.class),
                        ReportStatus.valueOf(resultSet.getString("status")),
                        resultSet.getString("server-id")
                );
            }
            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении жалобы", e);
        } finally {
            mySqlManager.returnConnection(connection);
        }
        return report;
    }

    public boolean containsReport(int id) {
        return getReport(id) != null;
    }

    public @NonNull ArrayList<Report> getReportsFromUser(@NonNull String name) {
        final ArrayList<Report> reportList = new ArrayList<>();
        final Connection connection = mySqlManager.getConnection();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `" + mySqlManager
                    .getBaseName() + "`.`reports` WHERE `reported` = ?");

            preparedStatement.setString(1, name);

            ResultSet resultSet = preparedStatement.executeQuery();
            while (resultSet.next()) {
                final ReportStatus reportStatus = ReportStatus.valueOf(resultSet.getString("status"));
                if (reportStatus == ReportStatus.CLOSED) continue;
                Moderator moderator = null;
                if (resultSet.getString("moderator") != null) {
                    final IUser user = getUser(resultSet.getString("moderator"));
                    if (user instanceof Moderator) moderator = (Moderator) user;
                }
                reportList.add(new Report(
                        resultSet.getInt("id"),
                        resultSet.getString("reported"),
                        resultSet.getString("username"),
                        resultSet.getString("reason"),
                        moderator,
                        resultSet.getObject("report-date", LocalDateTime.class),
                        ReportStatus.valueOf(resultSet.getString("status")),
                        resultSet.getString("server-id")
                ));
            }
            resultSet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Ошибка при загрузке жалоб на пользователя (%s)", name), e);
        } finally {
            mySqlManager.returnConnection(connection);
        }
        return reportList;
    }

    public int getNextId() {
        final Connection connection = mySqlManager.getConnection();
        try {
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SELECT MAX(id) FROM `" + mySqlManager.getBaseName() + "`.`reports`");
            if (resultSet.next()) {
                return resultSet.getInt(1) + 1;
            }
            statement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при получении следующего ID.", e);
        } finally {
            mySqlManager.returnConnection(connection);
        }
        return 1;
    }

    public void updateReport(@NonNull Report report) {
        final Connection connection = mySqlManager.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE `" + mySqlManager.getBaseName() + "`.`reports` SET `username` = ?, `reported` = ?," +
                        " `reason` = ?, `moderator` = ?, `report-date` = ?, `status` = ?, `server-id` = ? WHERE `id` = ?")) {

            preparedStatement.setString(1, report.getUser());
            preparedStatement.setString(2, report.getReported());
            preparedStatement.setString(3, report.getReason());
            preparedStatement.setString(4, report.getModerator() == null ? null : report.getModerator()
                    .getName());
            preparedStatement.setTimestamp(5, Timestamp.valueOf(report.getCreatedDate()));
            preparedStatement.setString(6, report.getStatus().name());
            preparedStatement.setString(7, report.getServerId());
            preparedStatement.setInt(8, report.getNumber());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Произошла ошибка при обновлении жалобы (" + e.getErrorCode() + ")", e);
        } finally {
            mySqlManager.returnConnection(connection);
        }
    }

    public @NonNull HashSet<String> getUniqueReportedUsers() {
        final HashSet<String> uniqueReportedUsers = new HashSet<>();
        final Connection connection = mySqlManager.getConnection();
        try {
            final Statement statement = connection.createStatement();
            final ResultSet resultSet = statement.executeQuery("SELECT DISTINCT `reported` " +
                    "FROM `" + mySqlManager.getBaseName() + "`.`reports`");

            while (resultSet.next()) {
                uniqueReportedUsers.add(resultSet.getString(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("Произошла ошибка при получении уникальных пользователей ", e);
        } finally {
            mySqlManager.returnConnection(connection);
        }
        return uniqueReportedUsers;
    }

    public @Nullable Report getLastUserReport(@NonNull String username) {
        final Connection connection = mySqlManager.getConnection();
        try {
            final PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `" +
                    mySqlManager.getBaseName() + "`.`reports` WHERE `reported` = ? ORDER BY `report-date` DESC LIMIT 1");

            preparedStatement.setString(1, username);

            final ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Moderator moderator = null;
                if (resultSet.getString("moderator") != null) {
                    final IUser user = getUser(resultSet.getString("moderator"));
                    if (user instanceof Moderator) moderator = (Moderator) user;
                }
                return new Report(
                        resultSet.getInt("id"),
                        resultSet.getString("reported"),
                        resultSet.getString("username"),
                        resultSet.getString("reason"),
                        moderator,
                        resultSet.getObject("report-date", LocalDateTime.class),
                        ReportStatus.valueOf(resultSet.getString("status")),
                        resultSet.getString("server-id")
                );
            }
        } catch (SQLException e) {
            throw new RuntimeException("Произошла ошибка при получении последней жалобы на пользователя " + username +
                    " (" + e.getMessage() + ")");
        } finally {
            mySqlManager.returnConnection(connection);
        }
        return null;
    }

    public void removeReport(int id) {
        final Connection connection = mySqlManager.getConnection();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("DELETE FROM `" + mySqlManager
                    .getBaseName() + "`.`reports` WHERE `id` = ?");

            preparedStatement.setInt(1, id);

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при удалении жалобы", e);
        } finally {
            mySqlManager.returnConnection(connection);
        }
    }

    public void addUser(@NonNull IUser user) {
        final Connection connection = mySqlManager.getConnection();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("INSERT INTO users (`username`," +
                    " `passed-checks`, `failed-checks`, `last-check`, `moderator`, `true-reports`, `false-reports`," +
                    " `mute-end-date`, `mute-reason`) VALUES (?,?,?,?,?,?,?,?,?)");

            preparedStatement.setString(1, user.getName());
            preparedStatement.setInt(2, user.getPassedChecks());
            preparedStatement.setInt(3, user.getFailedChecks());
            preparedStatement.setTimestamp(4, user.getLastCheck() == null ? null : Timestamp
                    .valueOf(user.getLastCheck()));
            preparedStatement.setBoolean(5, user instanceof Moderator);
            preparedStatement.setInt(6, user.getTrueReports());
            preparedStatement.setInt(7, user.getFalseReports());
            preparedStatement.setTimestamp(8, user.getMuteInfo() == null ? null
                    : Timestamp.valueOf(user.getMuteInfo().expiryDate()));
            preparedStatement.setString(9, user.getMuteInfo() == null ? null :
                    user.getMuteInfo().reason());

            preparedStatement.executeUpdate();
            preparedStatement.close();
        } catch (SQLException e) {
            throw new RuntimeException("Ошибка при добавлении пользователя", e);
        } finally {
            mySqlManager.returnConnection(connection);
        }
    }

    public @Nullable IUser getUser(@NonNull String name) {
        Connection connection = mySqlManager.getConnection();
        IUser user = null;

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("SELECT * FROM `" + mySqlManager
                    .getBaseName() + "`.`users` WHERE `username` = ?");

            preparedStatement.setString(1, name);

            ResultSet rs = preparedStatement.executeQuery();
            while (rs.next()) {
                user = rs.getBoolean("moderator") ?
                        new Moderator(
                                name,
                                rs.getInt("passed-checks"),
                                rs.getInt("failed-checks"),
                                rs.getTimestamp("last-check") == null ? null : rs
                                        .getTimestamp("last-check").toLocalDateTime(),
                                rs.getInt("true-reports"),
                                rs.getInt("false-reports"),
                                rs.getString("mute-reason") == null ? null : new IUser.UserMuteInfo(
                                        rs.getObject("mute-end-date", LocalDateTime.class),
                                        rs.getString("mute-reason"))) :
                        new User(name,
                                rs.getInt("passed-checks"),
                                rs.getInt("failed-checks"),
                                rs.getTimestamp("last-check") == null ? null : rs
                                        .getTimestamp("last-check").toLocalDateTime(),
                                rs.getInt("true-reports"),
                                rs.getInt("false-reports"),
                                rs.getString("mute-reason") == null ? null : new IUser.UserMuteInfo(
                                        rs.getObject("mute-end-date", LocalDateTime.class),
                                        rs.getString("mute-reason")));
            }
        } catch (SQLException e) {
            throw new RuntimeException(String.format("Ошибка при получении пользователя (%s)", name), e);
        } finally {
            mySqlManager.returnConnection(connection);
        }

        return user;
    }

    public void saveUser(@NonNull IUser user) {
        final Connection connection = mySqlManager.getConnection();

        try (PreparedStatement preparedStatement = connection.prepareStatement(
                "UPDATE `" + mySqlManager.getBaseName() + "`.`users` SET `passed-checks` = ?, `failed-checks` = ?," +
                        " `last-check` = ?, `moderator` = ?, `true-reports` = ?, `false-reports` = ?, `mute-end-date` = ?," +
                        " `mute-reason` = ? WHERE `username` = ?")) {

            preparedStatement.setInt(1, user.getPassedChecks());
            preparedStatement.setInt(2, user.getFailedChecks());
            preparedStatement.setTimestamp(3, user.getLastCheck() == null ? null
                    : Timestamp.valueOf(user.getLastCheck()));
            preparedStatement.setBoolean(4, user instanceof Moderator);
            preparedStatement.setInt(5, user.getTrueReports());
            preparedStatement.setInt(6, user.getFalseReports());
            preparedStatement.setTimestamp(7, user.getMuteInfo() == null ? null : Timestamp
                    .valueOf(user.getMuteInfo().expiryDate()));
            preparedStatement.setString(8, user.getMuteInfo() == null ? null : user.getMuteInfo().reason());
            preparedStatement.setString(9, user.getName());

            preparedStatement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Произошла ошибка при обновлении пользователя (" + e.getErrorCode() + ")", e);
        } finally {
            mySqlManager.returnConnection(connection);
        }
    }

    public boolean containsUser(@NonNull String name) {
        return getUser(name) != null;
    }
}
