package com.zipgame;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

public class SimpleServer {

    // Simple Session Management: Token -> GameState
    // In real app, Token -> UserId -> DB state
    // Here: Token -> GameState (User's memory state)
    // We will persist 'level' to DB when it changes.
    private static Map<String, GameState> sessions = new HashMap<>();

    public static void main(String[] args) throws IOException {
        int port = 8080;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number, defaulting to " + port);
            }
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // Serve static files
        server.createContext("/", new StaticHandler());

        // API endpoints (Central Dispatcher)
        server.createContext("/api", new ApiDispatcher());

        System.out.println("API Dispatcher registered at /api");

        server.setExecutor(null); // creates a default executor
        System.out.println("Server started on port " + port);
        server.start();
    }

    private static String getCookieValue(HttpExchange t, String key) {
        String cookieHeader = t.getRequestHeaders().getFirst("Cookie");
        if (cookieHeader == null)
            return null;
        String[] cookies = cookieHeader.split(";");
        for (String cookie : cookies) {
            String[] parts = cookie.trim().split("=");
            if (parts.length >= 2 && parts[0].equals(key)) {
                return parts[1];
            }
        }
        return null;
    }

    private static GameState getSession(HttpExchange t) {
        String user = getCookieValue(t, "user");
        if (user != null && !user.isEmpty()) {
            return sessions.computeIfAbsent(user, k -> {
                // Load level from DB
                int level = DatabaseManager.getUserLevel(k);
                GameState gs = new GameState();
                gs.setMaxLevel(level);
                gs.setLevel(level); // Start at highest unlocked level
                return gs;
            });
        }
        return null;
    }

    static class ApiDispatcher implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath();
            // Normalize path (remove trailing slash)
            if (path.endsWith("/") && path.length() > 1)
                path = path.substring(0, path.length() - 1);

            System.out.println("API Dispatch: " + path);

            switch (path) {
                case "/api/login":
                    new LoginHandler().handle(t);
                    break;
                case "/api/register":
                    new RegisterHandler().handle(t);
                    break;
                case "/api/state":
                    new StateHandler().handle(t);
                    break;
                case "/api/move":
                    new MoveHandler().handle(t);
                    break;
                case "/api/restart":
                    new RestartHandler().handle(t);
                    break;
                case "/api/select_level":
                    new SelectLevelHandler().handle(t);
                    break;
                case "/api/leaderboard":
                    new LeaderboardHandler().handle(t);
                    break;
                case "/api/score":
                    new ScoreHandler().handle(t);
                    break;
                case "/api/undo":
                    new UndoHandler().handle(t);
                    break;
                case "/api/daily":
                    new DailyHandler().handle(t);
                    break;
                case "/api/hint":
                    new HintHandler().handle(t);
                    break;
                default:
                    sendResponse(t, 404, "API Endpoint Not Found: " + path);
            }
        }
    }

    static class DailyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                GameState gs = getSession(t);
                if (gs == null) {
                    sendResponse(t, 401, "Unauthorized");
                    return;
                }

                // Seed based on current date (UTC)
                long seed = System.currentTimeMillis() / (1000 * 60 * 60 * 24);

                // We need to re-initialize the game state with this seed.
                // But GameState.initializeLevel() calls empty generateLevel().
                // Better approach: Modify GameState to accept LevelData or Seed
                // OR: Create a new GameState or reset with seed.

                LevelGenerator.LevelData data = LevelGenerator.generateLevel(seed);
                gs.loadLevel(data);

                sendResponse(t, 200, toJson(gs));
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    static class UndoHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            GameState gs = getSession(t);
            if (gs == null) {
                sendResponse(t, 401, "Unauthorized");
                return;
            }

            if ("POST".equals(t.getRequestMethod())) {
                boolean changed = gs.undoStep();
                if (changed) {
                    sendResponse(t, 200, toJson(gs));
                } else {
                    sendResponse(t, 400, "Cannot undo further");
                }
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    static class HintHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            GameState gs = getSession(t);
            if (gs == null) {
                sendResponse(t, 401, "Unauthorized");
                return;
            }

            if ("POST".equals(t.getRequestMethod())) {
                int[] hint = gs.getHint();
                if (hint != null) {
                    // Return raw JSON { "r":1, "c":2 }
                    String json = "{\"r\":" + hint[0] + ",\"c\":" + hint[1] + "}";
                    sendResponse(t, 200, json);
                } else {
                    // Check if limit reached (assuming we add accessor or infer)
                    // Since I haven't added getHintsUsed() yet, I will use a generic message for
                    // now
                    // OR I can quickly add `getHintsUsed` in GameState in next step.
                    // Let's bet on adding it.
                    if (gs.getHintsUsed() >= 3) {
                        sendResponse(t, 400, "Hint limit reached (Max 3)");
                    } else {
                        sendResponse(t, 400, "No hint available");
                    }
                }
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    static class StaticHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            String path = t.getRequestURI().getPath();
            if (path.equals("/"))
                path = "/index.html";

            // Security: Prevent directory traversal
            if (path.contains("..")) {
                sendResponse(t, 403, "Forbidden");
                return;
            }

            File file = new File("static" + path);
            if (file.exists() && !file.isDirectory()) {
                String contentType = "text/plain";
                if (path.endsWith(".html"))
                    contentType = "text/html";
                else if (path.endsWith(".css"))
                    contentType = "text/css";
                else if (path.endsWith(".js"))
                    contentType = "application/javascript";

                t.getResponseHeaders().set("Content-Type", contentType);
                t.sendResponseHeaders(200, file.length());
                OutputStream os = t.getResponseBody();
                Files.copy(file.toPath(), os);
                os.close();
            } else {
                sendResponse(t, 404, "Not Found");
            }
        }
    }

    static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                String body = new String(t.getRequestBody().readAllBytes());
                String username = "";
                String password = "";

                // Manual JSON parsing (very simple)
                body = body.replace("{", "").replace("}", "").replace("\"", "");
                String[] parts = body.split(",");
                for (String part : parts) {
                    String[] kv = part.split(":");
                    if (kv.length >= 2) {
                        String key = kv[0].trim();
                        String val = kv[1].trim();
                        if (key.equals("username"))
                            username = val;
                        if (key.equals("password"))
                            password = val;
                    }
                }

                System.out.println("Login attempt: " + username);

                if (DatabaseManager.validateUser(username, password)) {
                    int level = DatabaseManager.getUserLevel(username);
                    t.getResponseHeaders().add("Set-Cookie", "user=" + username + "; Path=/; HttpOnly; Max-Age=86400");
                    sendResponse(t, 200, "{\"status\":\"ok\", \"level\":" + level + "}");
                } else {
                    sendResponse(t, 401, "{\"status\":\"error\", \"message\":\"Invalid credentials\"}");
                }
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("POST".equals(t.getRequestMethod())) {
                String body = new String(t.getRequestBody().readAllBytes());
                String username = "";
                String password = "";

                body = body.replace("{", "").replace("}", "").replace("\"", "");
                String[] parts = body.split(",");
                for (String part : parts) {
                    String[] kv = part.split(":");
                    if (kv.length >= 2) {
                        String key = kv[0].trim();
                        String val = kv[1].trim();
                        if (key.equals("username"))
                            username = val;
                        if (key.equals("password"))
                            password = val;
                    }
                }

                if (username.isEmpty() || password.isEmpty()) {
                    sendResponse(t, 400, "{\"status\":\"error\", \"message\":\"Missing fields\"}");
                    return;
                }

                if (DatabaseManager.registerUser(username, password)) {
                    sendResponse(t, 200, "{\"status\":\"ok\", \"message\":\"Registered\"}");
                } else {
                    sendResponse(t, 409, "{\"status\":\"error\", \"message\":\"User exists\"}");
                }
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    static class StateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            GameState gs = getSession(t);
            if (gs == null) {
                sendResponse(t, 401, "Unauthorized");
                return;
            }

            if ("GET".equals(t.getRequestMethod())) {
                String json = toJson(gs);
                sendResponse(t, 200, json);
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    static class MoveHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            GameState gs = getSession(t);
            if (gs == null) {
                sendResponse(t, 401, "Unauthorized");
                return;
            }

            if ("POST".equals(t.getRequestMethod())) {
                Map<String, String> params = queryToMap(t.getRequestURI().getQuery());
                if (params.containsKey("r") && params.containsKey("c")) {
                    try {
                        int r = Integer.parseInt(params.get("r").trim());
                        int c = Integer.parseInt(params.get("c").trim());
                        gs.makeMove(r, c);
                        String json = toJson(gs);
                        sendResponse(t, 200, json);
                    } catch (NumberFormatException e) {
                        sendResponse(t, 400, "Invalid parameters");
                    }
                } else {
                    sendResponse(t, 400, "Missing parameters");
                }
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    static class RestartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            GameState gs = getSession(t);
            if (gs == null) {
                sendResponse(t, 401, "Unauthorized");
                return;
            }

            if ("POST".equals(t.getRequestMethod())) {
                boolean wasGameOver = gs.isGameOver();
                gs.reset();

                // If we advanced a level (maxLevel increased), save to DB
                if (wasGameOver) {
                    String user = getCookieValue(t, "user");
                    if (user != null) {
                        DatabaseManager.updateUserLevel(user, gs.getMaxLevel());
                    }
                }

                String json = toJson(gs);
                sendResponse(t, 200, json);
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    static class SelectLevelHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            GameState gs = getSession(t);
            if (gs == null) {
                sendResponse(t, 401, "Unauthorized");
                return;
            }

            if ("POST".equals(t.getRequestMethod())) {
                Map<String, String> params = queryToMap(t.getRequestURI().getQuery());
                if (params.containsKey("level")) {
                    try {
                        int requestedLevel = Integer.parseInt(params.get("level").trim());
                        String user = getCookieValue(t, "user");
                        System.out.println(
                                "SelectLevel: user=" + user + " req=" + requestedLevel + " max=" + gs.getMaxLevel());

                        // Level 1 is always unlocked. Others check maxLevel.
                        if (requestedLevel == 1 || (requestedLevel <= gs.getMaxLevel() && requestedLevel > 0)) {
                            gs.setLevel(requestedLevel);
                            String json = toJson(gs);
                            sendResponse(t, 200, json);
                        } else {
                            sendResponse(t, 403, "Level locked");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        sendResponse(t, 400, "Invalid level");
                    }
                } else {
                    sendResponse(t, 400, "Missing level parameter");
                }
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    // --- Leaderboard Logic ---

    static class LeaderboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            if ("GET".equals(t.getRequestMethod())) {
                Map<String, String> params = queryToMap(t.getRequestURI().getQuery());
                int level = 1; // default
                if (params.containsKey("level")) {
                    try {
                        level = Integer.parseInt(params.get("level").trim());
                    } catch (Exception e) {
                    }
                }

                String user = getCookieValue(t, "user");
                if (user == null)
                    user = "Guest";

                String json = DatabaseManager.getLeaderboardJson(level, user);
                sendResponse(t, 200, json);
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    static class ScoreHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange t) throws IOException {
            GameState gs = getSession(t);
            if (gs == null) {
                sendResponse(t, 401, "Unauthorized");
                return;
            }

            if ("POST".equals(t.getRequestMethod())) {
                if (gs.isScoreSubmitted()) {
                    // Already submitted
                    sendResponse(t, 200, "{\"status\":\"ok\", \"message\":\"already_submitted\"}");
                    return;
                }

                String body = new String(t.getRequestBody().readAllBytes());
                String name = getCookieValue(t, "user");
                if (name == null)
                    name = "Anonymous";

                int time = 9999;

                try {
                    // Manual JSON Parse: {"time":12}
                    body = body.replace("{", "").replace("}", "").replace("\"", "");
                    String[] parts = body.split(",");
                    for (String part : parts) {
                        String[] kv = part.split(":");
                        String key = kv[0].trim();
                        String val = kv[1].trim();
                        if (key.equals("name"))
                            name = val; // Allow override if needed, but usually not
                        if (key.equals("time"))
                            time = Integer.parseInt(val);
                    }

                    int currentLevel = gs.getLevel();
                    // Save to DB
                    DatabaseManager.addScore(name, currentLevel, time);
                    gs.setScoreSubmitted(true);

                    sendResponse(t, 200, "{\"status\":\"ok\"}");
                } catch (Exception e) {
                    e.printStackTrace();
                    sendResponse(t, 400, "Invalid JSON format");
                }
            } else {
                sendResponse(t, 405, "Method Not Allowed");
            }
        }
    }

    private static void sendResponse(HttpExchange t, int statusCode, String response) throws IOException {
        t.getResponseHeaders().set("Content-Type", "application/json");
        // Add CORS headers for local testing if needed, but not strictly necessary if
        // serving from same origin
        t.sendResponseHeaders(statusCode, response.length());
        OutputStream os = t.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null)
            return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0].trim(), entry[1].trim());
            }
        }
        return result;
    }

    // Manual JSON serialization to avoid dependencies like Jackson/Gson for
    // simplicity
    private static String toJson(GameState state) {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        sb.append("\"level\":").append(state.getLevel()).append(",");
        sb.append("\"maxLevel\":").append(state.getMaxLevel()).append(",");
        sb.append("\"currentNumber\":").append(state.getCurrentNumber()).append(",");
        sb.append("\"message\":\"").append(state.getMessage()).append("\",");
        sb.append("\"gameOver\":").append(state.isGameOver()).append(",");
        sb.append("\"submitted\":").append(state.getScoreSubmitted()).append(",");

        sb.append("\"board\":[");
        int[][] board = state.getBoard();
        boolean[][] fixed = state.getFixed();
        boolean[][] hWalls = state.getHWalls();
        boolean[][] vWalls = state.getVWalls();
        Map<Integer, Integer> displayMap = state.getDisplayMap();

        for (int i = 0; i < GameState.SIZE; i++) {
            sb.append("[");
            for (int j = 0; j < GameState.SIZE; j++) {
                sb.append("{");
                sb.append("\"value\":").append(board[i][j]).append(",");
                sb.append("\"fixed\":").append(fixed[i][j]).append(",");
                sb.append("\"hWall\":").append(hWalls[i][j]).append(",");
                sb.append("\"vWall\":").append(vWalls[i][j]).append(",");

                int val = board[i][j];
                // Only send display value for fixed cells
                if (fixed[i][j] && displayMap.containsKey(val)) {
                    sb.append("\"display\":").append(displayMap.get(val));
                } else {
                    sb.append("\"display\":null");
                }

                sb.append("}");
                if (j < GameState.SIZE - 1)
                    sb.append(",");
            }
            sb.append("]");
            if (i < GameState.SIZE - 1)
                sb.append(",");
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }
}
