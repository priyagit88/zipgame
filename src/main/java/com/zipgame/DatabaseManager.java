package com.zipgame;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:zipgame_v2.db";

    static {
        try {
            // Load driver
            Class.forName("org.sqlite.JDBC");
            initDatabase();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(URL);
                Statement stmt = conn.createStatement()) {

            // Users table: Stores current level for persistence
            String sqlUsers = "CREATE TABLE IF NOT EXISTS users (" +
                    "username TEXT PRIMARY KEY, " +
                    "password_hash TEXT, " +
                    "current_level INTEGER DEFAULT 1)";
            stmt.execute(sqlUsers);

            // Migration: Add password_hash column if it doesn't exist (e.g. legacy DB)
            // SQLite doesn't support IF NOT EXISTS for ADD COLUMN
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN password_hash TEXT");
            } catch (SQLException ignored) {
                // Column likely exists
                System.out.println("Migration Note: " + ignored.getMessage());
            }

            // Scores table: Leaderboard
            String sqlScores = "CREATE TABLE IF NOT EXISTS scores (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "username TEXT, " +
                    "level INTEGER, " +
                    "time INTEGER)";
            stmt.execute(sqlScores);

        } catch (SQLException e) {
            System.out.println("DB Init Error: " + e.getMessage());
        }
    }

    public static int getUserLevel(String username) {
        String sql = "SELECT current_level FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                int lvl = rs.getInt("current_level");
                return (lvl < 1) ? 1 : lvl;
            } else {
                // Create user if not exists (Legacy method called by LoginHandler?)
                // Actually LoginHandler checks validateUser first.
                // But getUserLevel is called AFTER validation.
                // If it's a new user flow via register, this returns 1.
                // If legacy user flow (auto-creation), we might want to ensure they exist.
                // But registerUser handles creation.
                // We keep this for backward compat or direct access.
                updateUserLevel(username, 1);
                return 1;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return 1;
        }
    }

    public static void updateUserLevel(String username, int level) {
        String sql = "INSERT INTO users(username, current_level) VALUES(?,?) " +
                "ON CONFLICT(username) DO UPDATE SET current_level=excluded.current_level";
        try (Connection conn = DriverManager.getConnection(URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setInt(2, level);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void addScore(String username, int level, int time) {
        String sql = "INSERT INTO scores(username, level, time) VALUES(?,?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setInt(2, level);
            pstmt.setInt(3, time);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static String getLeaderboardJson(int level, String username) {
        String sql = "SELECT time FROM scores WHERE level = ? AND username = ? ORDER BY time ASC LIMIT 10";

        StringBuilder json = new StringBuilder("[");
        try (Connection conn = DriverManager.getConnection(URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, level);
            pstmt.setString(2, username);
            ResultSet rs = pstmt.executeQuery();

            boolean first = true;
            while (rs.next()) {
                if (!first)
                    json.append(",");
                json.append("{\"name\":\"").append(username).append("\",");
                json.append("\"time\":").append(rs.getInt("time")).append("}");
                first = false;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        json.append("]");
        return json.toString();
    }

    public static boolean registerUser(String username, String password) {
        String hash = hashPassword(password);
        String sql = "INSERT INTO users(username, password_hash, current_level) VALUES(?,?,1)";
        try (Connection conn = DriverManager.getConnection(URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            pstmt.setString(2, hash);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Duplicate user?
            // Try to CLAIM legacy account (where password_hash is NULL)
            String updateSql = "UPDATE users SET password_hash = ? WHERE username = ? AND password_hash IS NULL";
            try (Connection conn = DriverManager.getConnection(URL);
                    PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
                pstmt.setString(1, hash);
                pstmt.setString(2, username);
                int rows = pstmt.executeUpdate();
                if (rows > 0)
                    return true; // Successfully claimed
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
            return false; // Actually duplicate and already has password
        }
    }

    public static boolean validateUser(String username, String password) {
        String hash = hashPassword(password);
        String sql = "SELECT password_hash FROM users WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(URL);
                PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                String stored = rs.getString("password_hash");
                // Allow legacy users without password (null) to log in?
                // Alternatively, return false and force registration.
                // Let's assume matches only if stored is not null.
                if (stored == null)
                    return false;
                return stored.equals(hash);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedhash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * encodedhash.length);
            for (int i = 0; i < encodedhash.length; i++) {
                String hex = Integer.toHexString(0xff & encodedhash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}
