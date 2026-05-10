# SimpleServer.java Explanation

1: `package com.zipgame;`
- Declares that this class belongs to the `com.zipgame` package.

3: `import com.sun.net.httpserver.HttpExchange;`
- Imports `HttpExchange` class, which represents an HTTP request/response exchange.

4: `import com.sun.net.httpserver.HttpHandler;`
- Imports `HttpHandler` interface, used to handle HTTP requests.

5: `import com.sun.net.httpserver.HttpServer;`
- Imports `HttpServer` class, which implements a simple HTTP server.

7: `import java.io.File;`
- Imports `File` class for file system operations.

8: `import java.io.IOException;`
- Imports `IOException` for handling input/output errors.

9: `import java.io.OutputStream;`
- Imports `OutputStream` for writing data to the response body.

10: `import java.net.InetSocketAddress;`
- Imports `InetSocketAddress` to specify IP address and port number.

11: `import java.nio.file.Files;`
- Imports `Files` utility class for file operations (like copying).

12: `import java.util.HashMap;`
- Imports `HashMap`, a key-value map implementation.

13: `import java.util.Map;`
- Imports `Map` interface.

15: `public class SimpleServer {`
- Defines the public class `SimpleServer`.

21: `private static Map<String, GameState> sessions = new HashMap<>();`
- Creates a static map to store user sessions, mapping a session token (or username) to a `GameState` object. This acts as a simple in-memory session store.

23: `public static void main(String[] args) throws IOException {`
- The main entry point of the application. It can throw `IOException` if server startup fails.

24: `int port = 8080;`
- Sets the default port number to 8080.

25: `if (args.length > 0) {`
- Checks if any command-line arguments were passed.

26: `try {`
- Starts a try block to handle potential parsing errors.

27: `port = Integer.parseInt(args[0]);`
- Attempts to parse the first argument as the port number.

28: `} catch (NumberFormatException e) {`
- Catches the exception if the argument is not a valid number.

29: `System.err.println("Invalid port number, defaulting to " + port);`
- Prints an error message to standard error but keeps the default port.

33: `HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);`
- Creates an `HttpServer` instance bound to the specified port. The `0` indicates the default backlog size.

36: `server.createContext("/", new StaticHandler());`
- Registers a context handler for the root path `"/"`, using `StaticHandler` to serve static files (like HTML, CSS).

39: `server.createContext("/api", new ApiDispatcher());`
- Registers a context handler for `"/api"`. All requests starting with `/api` go to `ApiDispatcher`.

41: `System.out.println("API Dispatcher registered at /api");`
- Prints a confirmation message to the console.

43: `server.setExecutor(null);`
- Sets the executor to `null`, which creates a default executor for handling requests.

44: `System.out.println("Server started on port " + port);`
- Prints a message indicating the server has successfully started.

45: `server.start();`
- Starts the server in a background thread.

48: `private static String getCookieValue(HttpExchange t, String key) {`
- Helper method to extract a specific cookie value from the request.

49: `String cookieHeader = t.getRequestHeaders().getFirst("Cookie");`
- Gets the "Cookie" header from the request.

50: `if (cookieHeader == null)`
- Checks if the cookie header is missing.

51: `return null;`
- Returns null if no cookies are present.

52: `String[] cookies = cookieHeader.split(";");`
- Splits the cookie string into individual cookies (separated by semicolons).

53: `for (String cookie : cookies) {`
- Iterates through each cookie string.

54: `String[] parts = cookie.trim().split("=");`
- Splits the cookie into name and value.

55: `if (parts.length >= 2 && parts[0].equals(key)) {`
- Checks if the cookie name matches the requested key.

56: `return parts[1];`
- Returns the cookie value.

62: `private static GameState getSession(HttpExchange t) {`
- Helper method to retrieve the `GameState` associated with the current user's session.

63: `String user = getCookieValue(t, "user");`
- Gets the "user" cookie value (username).

64: `if (user != null && !user.isEmpty()) {`
- Checks if a valid username was found.

65: `return sessions.computeIfAbsent(user, k -> {`
- Returns the existing `GameState` for the user, or creates a new one if it doesn't exist (`computeIfAbsent`).

67: `int level = DatabaseManager.getUserLevel(k);`
- Retrieves the user's saved level from the database.

68: `GameState gs = new GameState();`
- Creates a new `GameState` object.

69: `gs.setMaxLevel(level);`
- Sets the user's maximum unlocked level.

70: `gs.setLevel(level);`
- Sets the current level to the highest unlocked level.

71: `return gs;`
- Returns the initialized `GameState`.

77: `static class ApiDispatcher implements HttpHandler {`
- Inner class `ApiDispatcher` that handles all API requests.

79: `public void handle(HttpExchange t) throws IOException {`
- The `handle` method required by `HttpHandler`.

80: `String path = t.getRequestURI().getPath();`
- Gets the request URI path.

82: `if (path.endsWith("/") && path.length() > 1)`
- Checks if the path has a trailing slash.

83: `path = path.substring(0, path.length() - 1);`
- Removes the trailing slash for consistency.

85: `System.out.println("API Dispatch: " + path);`
- Logs the API request path.

87: `switch (path) {`
- Switches on the path to route specific endpoints.

88-120: `case "/api/..." ... break;`
- Routes requests like `/api/login`, `/api/move`, etc., to their specific handlers (`LoginHandler`, `MoveHandler`, etc.).

122: `sendResponse(t, 404, "API Endpoint Not Found: " + path);`
- Sends a 404 error if the API endpoint doesn't exist.

127: `static class DailyHandler implements HttpHandler {`
- Handler for daily challenge requests.

130: `if ("POST".equals(t.getRequestMethod())) {`
- Checks if the request method is POST.

131: `GameState gs = getSession(t);`
- Retrieves the user session.

132: `if (gs == null) {`
- returns 401 Unauthorized if no session found.

138: `long seed = System.currentTimeMillis() / (1000 * 60 * 60 * 24);`
- Generates a seed based on the current date (one seed per day).

145: `LevelGenerator.LevelData data = LevelGenerator.generateLevel(seed);`
- Generates a level using the daily seed.

146: `gs.loadLevel(data);`
- Loads the generated daily level into the game state.

148: `sendResponse(t, 200, toJson(gs));`
- Sends the new game state back to the client as JSON.

155: `static class UndoHandler implements HttpHandler {`
- Handler for undoing a move.

165: `boolean changed = gs.undoStep();`
- Calls `undoStep` on the game state.

166: `if (changed) {`
- If the undo was successful...

167: `sendResponse(t, 200, toJson(gs));`
- Return updated state.

177: `static class HintHandler implements HttpHandler {`
- Handler for requesting hints.

187: `int[] hint = gs.getHint();`
- Gets a hint from the game state.

188: `if (hint != null) {`
- If a hint is available...

190: `String json = "{\"r\":" + hint[0] + ",\"c\":" + hint[1] + "}";`
- Constructs a JSON object with the hint coordinates.

210: `static class StaticHandler implements HttpHandler {`
- Handler for serving static files (HTML, CSS, JS).

214: `if (path.equals("/")) path = "/index.html";`
- Defaults root path requests to `index.html`.

218: `if (path.contains("..")) {`
- Security check: prevents directory traversal attacks accessing files outside the static directory.

223: `File file = new File("static" + path);`
- Creates a `File` object pointing to the requested resource in the `static` folder.

224: `if (file.exists() && !file.isDirectory()) {`
- Checks if the file exists and is not a directory.

230: `contentType = "application/javascript";`
- Sets content type for JS, CSS, or HTML based on extension.

234: `t.sendResponseHeaders(200, file.length());`
- Sends execution with 200 OK status and file length.

236: `Files.copy(file.toPath(), os);`
- Copies the file content to the response output stream.

244: `static class LoginHandler implements HttpHandler {`
- Handler for user login.

248: `String body = new String(t.getRequestBody().readAllBytes());`
- Reads the request body (username/password JSON).

253: `body = body.replace("{", "").replace("}", "").replace("\"", "");`
- Rudimentary JSON parsing (stripping braces and quotes).

269: `if (DatabaseManager.validateUser(username, password)) {`
- Validates credentials against the database.

271: `t.getResponseHeaders().add("Set-Cookie", "user=" + username + "; Path=/; HttpOnly; Max-Age=86400");`
- Sets a session cookie if login is successful.

282: `static class RegisterHandler implements HttpHandler {`
- Handler for user registration.

309: `if (DatabaseManager.registerUser(username, password)) {`
- Attempts to register the user in the database.

320: `static class StateHandler implements HttpHandler {`
- Handler to get current game state.

338: `static class MoveHandler implements HttpHandler {`
- Handler for player moves.

348: `Map<String, String> params = queryToMap(t.getRequestURI().getQuery());`
- Parses query parameters (e.g., `?r=1&c=2`).

353: `gs.makeMove(r, c);`
- Executes the move on the game state.

368: `static class RestartHandler implements HttpHandler {`
- Handler to restart the level.

379: `gs.reset();`
- Resets the game state.

397: `static class SelectLevelHandler implements HttpHandler {`
- Handler to select a specific level.

416: `if (requestedLevel == 1 || (requestedLevel <= gs.getMaxLevel() && requestedLevel > 0)) {`
- Validates if the user is allowed to access the requested level.

438: `static class LeaderboardHandler implements HttpHandler {`
- Handler for fetching leaderboard data.

455: `String json = DatabaseManager.getLeaderboardJson(level, user);`
- Fetches leaderboard JSON from the database.

463: `static class ScoreHandler implements HttpHandler {`
- Handler for submitting scores.

502: `DatabaseManager.addScore(name, currentLevel, time);`
- Adds the score to the database.

516: `private static void sendResponse(HttpExchange t, int statusCode, String response) throws IOException {`
- Helper to send an HTTP response with JSON content type.

526: `private static Map<String, String> queryToMap(String query) {`
- Helper to parse URL query strings into a Map.

541: `private static String toJson(GameState state) {`
- Manually constructs a JSON string representation of the `GameState` object.
