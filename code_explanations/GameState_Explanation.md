# GameState.java Explanation

1: `package com.zipgame;`
- Declares the package.

3: `import java.util.HashMap;`
- Imports `HashMap`.

4: `import java.util.Map;`
- Imports `Map`.

6: `public class GameState {`
- Defines the `GameState` class, representing the state of a single game session.

7: `public static final int SIZE = 6;`
- Defines the board size as 6x6.

8: `private int[][] board;`
- 2D array storing the number in each cell. 0 represents an empty cell.

9: `private boolean[][] fixed;`
- 2D array marking which cells are fixed (part of the initial puzzle) and cannot be changed/deleted.

10: `private int currentNumber;`
- Tracks the last valid number connected in the user's current path (e.g., if user has connected 1->2->3, this is 3).

11: `private boolean gameOver;`
- Flag indicating if the game is finished.

12: `private String message;`
- Stores a message to be displayed to the user (e.g., "Invalid move").

15: `private boolean[][] hWalls;`
- Stores horizontal walls (barriers between rows).

16: `private boolean[][] vWalls;`
- Stores vertical walls (barriers between columns).

19: `private Map<Integer, Integer> displayMap;`
- Maps the internal logical value (1..36) to the display rank. Used for "checkpoints" where only certain numbers (e.g., 1, 10, 20, 36) are shown initially.

22: `private int level = 1;`
- Current level number.

23: `private int maxLevel = 1;`
- Highest level unlocked.

26: `private boolean scoreSubmitted = false;`
- Tracks if the score for the current level has been submitted to the leaderboard.

28: `private int[][] solution;`
- Stores the full solution grid for providing hints.

30: `private boolean isDailyChallenge = false;`
- Flag for daily challenge mode.

31: `private LevelGenerator.LevelData currentLevelData;`
- Stores the raw level data to allow restarting/resetting.

33: `public GameState() {`
- Constructor. Starts the game.

35-40: `this.board = new int[SIZE][SIZE]; ...`
- Initializes all the arrays and maps.

41: `initializeLevel();`
- Sets up the first level.

44: `private void initializeLevel() {`
- Prepares a level.

45: `this.isDailyChallenge = false;`
- Defaults to normal mode.

49: `LevelGenerator.LevelData data = LevelGenerator.generateLevel(this.level);`
- Generates a level using the current level number as the seed for determinism (Level 1 is always the same).

50: `this.currentLevelData = data;`
- Saves the data.

51: `applyLevelData(data);`
- Applies the generated data to the game state.

55: `private void applyLevelData(LevelGenerator.LevelData data) {`
- Copies data from the `LevelData` object to the `GameState` fields.

61-63: `for (int i = 0; i < SIZE; i++) { ... }`
- Deep copies the board array.

65-69: `this.fixed = data.fixed; ...`
- Copies references for other arrays (assuming they are not mutated in `GameState`, or `LevelData` is disposable).

71: `this.currentNumber = 1;`
- Resets the player's progress to just the number 1.

82: `if (isDailyChallenge) { ... }`
- Sets the initial welcome message based on the mode.

89: `public synchronized void reset() {`
- Resets the current level.

90: `if (isDailyChallenge) { ... }`
- If daily challenge, re-apply the stored daily data.

97-101: `if (gameOver) { ... }`
- If game was over (player won), advance to the next level before initializing.

108: `public synchronized void loadLevel(LevelGenerator.LevelData data) {`
- Loads a specific level data (used for Daily Challenge logic from `SimpleServer`).

114: `public void setLevel(int level) {`
- Sets the specific level number and re-initializes.

136: `public synchronized boolean makeMove(int r, int c) {`
- Core logic for handling a player's click/interaction.

140: `if (r < 0 || r >= SIZE ...)`
- Bounds check.

145: `int clickedValue = board[r][c];`
- Gets the value of the clicked cell.

148-157: `for ... if (board[i][j] == currentNumber) ...`
- Finds the coordinates (`currR`, `currC`) of the `currentNumber` (the "head" of the path).

167: `if (clickedValue > 0 && clickedValue < currentNumber) {`
- If user clicks a number already in the path (less than current head), treat it as an undo/backtrack to that point.

168: `return backtrack(clickedValue);`
- Executes backtrack.

172: `boolean isRow = (currR == r);`
- Checks if the move is in the same row.

173: `boolean isCol = (currC == c);`
- Checks if the move is in the same column.

175: `if (!isRow && !isCol) {`
- Enforces straight-line movement (no diagonals, no L-shapes).

181: `int dr = Integer.signum(r - currR);`
- Calculates direction (-1, 0, 1) for Row.

182: `int dc = Integer.signum(c - currC);`
- Calculates direction (-1, 0, 1) for Column.

183: `int dist = Math.abs(r - currR) + Math.abs(c - currC);`
- Calculates distance.

189: `for (int i = 0; i < dist; i++) {`
- Iterates through every cell in the path to the target.

192: `int wallR = Math.min(tempR, tempR + dr);`
- Calculates wall check coordinates.

193: `if (hWalls[wallR][tempC]) {`
- Checks for horizontal wall blocking the path.

199: `if (vWalls[tempR][wallC]) {`
- Checks for vertical wall blocking the path.

211: `if (board[tempR][tempC] != 0) {`
- Ensures no numbers are being jumped over/passed through.

219: `int expectedValue = currentNumber + dist;`
- Calculates what the value at the target cell should be.

227: `if (fixed[r][c]) {`
- If the target cell is a "fixed" number...

228: `if (board[r][c] != expectedValue) {`
- Checks if we arrived at the fixed number with the correct path length.

232: `} else if (board[r][c] != 0) {`
- If target is not fixed but IS occupied (should have been caught by backtrack or block check, but good safety).

240: `for (int i = 1; i <= dist; i++) {`
- Performs the actual "Zip" (filling in the numbers).

266: `board[tempR][tempC] = currentNumber + i;`
- Sets the board values.

270: `currentNumber = expectedValue;`
- Updates the head of the path.

272: `checkWin();`
- Checks if the level is complete.

276: `private boolean backtrack(int targetVal) {`
- Handles undoing moves.

278-287: `for ... if (board[i][j] > targetVal) ...`
- Clears all numbers on the board greater than the `targetVal`.

293: `public synchronized boolean undoStep() {`
- Undoes just one step (one number).

319: `public synchronized int[] getHint() {`
- Provides the next correct coordinate from the solution.

325: `int target = currentNumber + 1;`
- We want to find where `currentNumber + 1` is located in the solution.

331: `if (solution[i][j] == target) {`
- Search the solution grid.

340: `private void checkWin() {`
- Checks if the game is won.

341: `if (currentNumber == SIZE * SIZE) {`
- If path length equals 36 (6x6).

344-349: `for ...`
- Double checks that all cells are filled (validation).

352: `gameOver = true;`
- Sets game over flag.

361-407: `public ... get...()`
- Getters for fields, used for JSON serialization in `SimpleServer`.
