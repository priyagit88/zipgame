# DatabaseManager.java Explanation

1: `package com.zipgame;`
- Declares the package name.

3-11: `import java.sql...;`
- Imports standard Java SQL classes for JDBC connectivity (Connection, DriverManager, etc.) and security classes for hashing.

13: `public class DatabaseManager {`
- Defines the `DatabaseManager` class for handling SQLite database operations.

14: `private static final String URL = "jdbc:sqlite:zipgame_v2.db";`
- Defines the connection URL for the SQLite database file named `zipgame_v2.db`.

16: `static {`
- Static initialization block, executed when the class is loaded.

19: `Class.forName("org.sqlite.JDBC");`
- Loads the SQLite JDBC driver.

20: `initDatabase();`
- Key method call to ensure tables exist on startup.

26: `private static void initDatabase() {`
- Method to initialize the database schema.

27: `try (Connection conn = DriverManager.getConnection(URL);`
- Establishes a connection to the database. Uses try-with-resources to ensure it closes.

28: `Statement stmt = conn.createStatement()) {`
- Creates a SQL statement object.

31: `String sqlUsers = "CREATE TABLE IF NOT EXISTS users ...`
- Defines SQL to create the `users` table with `username` (Primary Key), `password_hash`, and `current_level`.

35: `stmt.execute(sqlUsers);`
- Executes the table creation SQL.

40: `stmt.execute("ALTER TABLE users ADD COLUMN password_hash TEXT");`
- Attempts to add `password_hash` column to support migrations from older versions of the schema.

47: `String sqlScores = "CREATE TABLE IF NOT EXISTS scores ...`
- Defines SQL to create the `scores` table with `username`, `level`, and `time` columns.

59: `public static int getUserLevel(String username) {`
- Method to get a user's current maximum level.

60: `String sql = "SELECT current_level FROM users WHERE username = ?";`
- SQL query to fetch the level.

66: `int lvl = rs.getInt("current_level");`
- Extracts the level from result set.

76: `updateUserLevel(username, 1);`
- If user not found (in this specific flow), defaults to creating/resetting them to level 1.

85: `public static void updateUserLevel(String username, int level) {`
- Updates a user's level or inserts a new record if they don't exist.

86: `String sql = "INSERT INTO users... ON CONFLICT(username) DO UPDATE ...";`
- Uses SQLite `UPSERT` syntax: Insert, but if username exists, update the `current_level`.

98: `public static void addScore(String username, int level, int time) {`
- Inserts a new score record for a user.

111: `public static String getLeaderboardJson(int level, String username) {`
- Retrieves top 10 scores for a level and formats them as a JSON string.

112: `String sql = "SELECT time FROM scores WHERE level = ? AND username = ? ORDER BY time ASC LIMIT 10";`
- Queries the top 10 fastest times for the given level. Note: The SQL as written `AND username = ?` actually only gets that specific user's scores, which seems to serve a "personal bests" list rather than a global leaderboard, or it might be a bug/feature of this specific implementation.

137: `public static boolean registerUser(String username, String password) {`
- Registers a new user.

138: `String hash = hashPassword(password);`
- Hashes the password before storage.

139: `String sql = "INSERT INTO users(username, password_hash, current_level) VALUES(?,?,1)";`
- SQL to insert the new user.

145: `return true;`
- Returns true if successful.

149-156: `String updateSql = "UPDATE users ...`
- Handles logic to claim "legacy" accounts that might exist without a password hash.

164: `public static boolean validateUser(String username, String password) {`
- Checks if username/password are correct.

166: `String sql = "SELECT password_hash FROM users WHERE username = ?";`
- Gets the stored hash for the user.

178: `return stored.equals(hash);`
- Returns true if the stored hash matches the hash of the provided password.

186: `private static String hashPassword(String password) {`
- Helper method to perform SHA-256 hashing.

188: `MessageDigest digest = MessageDigest.getInstance("SHA-256");`
- Uses generic SHA-256 digest.

201: `throw new RuntimeException(e);`
- Crashes if SHA-256 is somehow missing (unlikely).
