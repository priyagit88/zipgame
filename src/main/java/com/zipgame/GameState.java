package com.zipgame;

import java.util.HashMap;
import java.util.Map;

public class GameState {
    public static final int SIZE = 6;
    private int[][] board; // Stores the number in each cell. 0 means empty.
    private boolean[][] fixed; // true if the number is part of the initial puzzle
    private int currentNumber; // The last valid number connected in the sequence
    private boolean gameOver;
    private String message;

    // Walls
    private boolean[][] hWalls;
    private boolean[][] vWalls;

    // Display Mapping
    private Map<Integer, Integer> displayMap;

    // Level Counter
    private int level = 1;
    private int maxLevel = 1;

    // Submission Flag
    private boolean scoreSubmitted = false;

    private int[][] solution; // Full solution for hints

    private boolean isDailyChallenge = false;
    private LevelGenerator.LevelData currentLevelData;

    public GameState() {
        // ... (fields init)
        this.board = new int[SIZE][SIZE];
        this.fixed = new boolean[SIZE][SIZE];
        this.solution = new int[SIZE][SIZE];
        this.hWalls = new boolean[SIZE][SIZE];
        this.vWalls = new boolean[SIZE][SIZE];
        this.displayMap = new HashMap<>();
        initializeLevel();
    }

    private void initializeLevel() {
        this.isDailyChallenge = false;
        // Deterministic generation based on level number
        // We use 'level' as seed. For Level 1, seed 1, etc.
        // This ensures every time player opens Level 1, it's the same.
        LevelGenerator.LevelData data = LevelGenerator.generateLevel(this.level);
        this.currentLevelData = data;
        applyLevelData(data);
    }

    // Helper to populate state from data
    private void applyLevelData(LevelGenerator.LevelData data) {
        this.board = new int[SIZE][SIZE]; // Reset board state, but keep fixed?
        // No, LevelData.board contains the fixed numbers.
        // But wait, LevelGenerator.board logic:
        // "data.board[r][c] = val" for fixed numbers, 0 otherwise.
        // So we can copy it.
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(data.board[i], 0, this.board[i], 0, SIZE);
        }

        this.fixed = data.fixed;
        this.solution = data.solution;
        this.hWalls = data.hWalls;
        this.vWalls = data.vWalls;
        this.displayMap = data.displayMap;

        this.currentNumber = 1;
        this.gameOver = false;
        this.scoreSubmitted = false;
        this.hintsUsed = 0;

        // Message
        int maxCheckpoint = 0;
        for (int val : displayMap.values()) {
            if (val > maxCheckpoint)
                maxCheckpoint = val;
        }
        if (isDailyChallenge) {
            this.message = "Daily Challenge: Connect 1 to " + maxCheckpoint;
        } else {
            this.message = "Draw the path from 1 to " + maxCheckpoint + "!";
        }
    }

    public synchronized void reset() {
        if (isDailyChallenge) {
            // Restart the daily challenge
            applyLevelData(currentLevelData);
            return;
        }

        // Normal play
        if (gameOver) {
            if (level == maxLevel) {
                maxLevel++;
            }
            level++;
        }
        initializeLevel();
    }

    // ... (rest of class)

    public synchronized void loadLevel(LevelGenerator.LevelData data) {
        this.isDailyChallenge = true;
        this.currentLevelData = data;
        applyLevelData(data);
    }

    public void setLevel(int level) {
        this.level = level;
        // If setting to a level, re-initialize
        initializeLevel();
    }

    public void setMaxLevel(int maxLevel) {
        this.maxLevel = maxLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public boolean isScoreSubmitted() {
        return scoreSubmitted;
    }

    public void setScoreSubmitted(boolean submitted) {
        this.scoreSubmitted = submitted;
    }

    public synchronized boolean makeMove(int r, int c) {
        if (gameOver)
            return false;

        if (r < 0 || r >= SIZE || c < 0 || c >= SIZE) {
            this.message = "Invalid position.";
            return false;
        }

        int clickedValue = board[r][c];

        // Find position of currentNumber
        int currR = -1, currC = -1;
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == currentNumber) {
                    currR = i;
                    currC = j;
                    break;
                }
            }
        }

        // Logic for clicking the LAST number (Undo logic or just ignore?)
        // If user clicks the current number, maybe do nothing.
        if (currR == r && currC == c) {
            this.message = "You are at " + currentNumber;
            return true;
        }

        // Undo Logic: Clicking an existing path part
        if (clickedValue > 0 && clickedValue < currentNumber) {
            return backtrack(clickedValue);
        }

        // ZIP LOGIC: Allow straight line moves
        boolean isRow = (currR == r);
        boolean isCol = (currC == c);

        if (!isRow && !isCol) {
            this.message = "Must move in a straight line.";
            return false;
        }

        // Check path and walls
        int dr = Integer.signum(r - currR);
        int dc = Integer.signum(c - currC);
        int dist = Math.abs(r - currR) + Math.abs(c - currC);

        // Validate path is clear (except target) and no walls
        int tempR = currR;
        int tempC = currC;

        for (int i = 0; i < dist; i++) {
            // Check wall before moving
            if (dr != 0) { // Vertical
                int wallR = Math.min(tempR, tempR + dr);
                if (hWalls[wallR][tempC]) {
                    this.message = "Blocked by wall.";
                    return false;
                }
            } else { // Horizontal
                int wallC = Math.min(tempC, tempC + dc);
                if (vWalls[tempR][wallC]) {
                    this.message = "Blocked by wall.";
                    return false;
                }
            }

            // Move
            tempR += dr;
            tempC += dc;

            // Check cell content (only for intermediate cells)
            if (i < dist - 1) {
                if (board[tempR][tempC] != 0) {
                    this.message = "Path blocked by existing number.";
                    return false;
                }
            }
        }

        // Expected value at target
        int expectedValue = currentNumber + dist;

        int pathR = currR;
        int pathC = currC;

        // REMOVED "Cannot skip fixed number" check to allow relaxed gameplay
        // User can make invalid moves and undo later.

        if (fixed[r][c]) {
            if (board[r][c] != expectedValue) {
                this.message = "Target is " + board[r][c] + ", expected " + expectedValue;
                return false;
            }
        } else if (board[r][c] != 0) {
            this.message = "Target cell occupied.";
            return false;
        }

        // Perform the Zip
        tempR = currR;
        tempC = currC;
        for (int i = 1; i <= dist; i++) {
            tempR += dr;
            tempC += dc;
            // Only overwrite if not fixed.
            // If we land on a fixed number that IS expectedValue, we don't need to write
            // (it's already there)
            // If we land on a fixed number that is NOT expectedValue, we would have errored
            // above (target check).
            // BUT what if we pass THROUGH a fixed number?
            // "Path blocked by existing number" (lines 206-210) handles intermediate
            // blocks.
            // Wait, lines 206-210:
            /*
             * if (i < dist - 1) {
             * if (board[tempR][tempC] != 0) {
             * return false;
             * }
             * }
             */
            // This prevents passing *through* any number (fixed or not).
            // This is correct. You cannot zip *through* a number, you zip *to* a number.
            // So intermediate cells MUST be empty.
            // If I skip a fixed number, it means I go *around* it or away from it.
            // So I am not passing through it.

            if (!fixed[tempR][tempC]) {
                board[tempR][tempC] = currentNumber + i;
            }
        }

        currentNumber = expectedValue;
        this.message = "Zipped to " + currentNumber;
        checkWin();
        return true;
    }

    private boolean backtrack(int targetVal) {
        // Undo everything > targetVal
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] > targetVal) {
                    if (!fixed[i][j]) {
                        board[i][j] = 0;
                    }
                    // If fixed, we leave it alone (it remains as a future target)
                }
            }
        }
        currentNumber = targetVal;
        this.message = "Backtracked to path length " + targetVal;
        return true;
    }

    public synchronized boolean undoStep() {
        if (currentNumber <= 1)
            return false;

        // Find current head
        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (board[i][j] == currentNumber) {
                    if (!fixed[i][j]) {
                        board[i][j] = 0;
                    }
                    currentNumber--;
                    this.message = "Undid one step.";

                    // If we undid a win, cancel game over
                    if (gameOver)
                        gameOver = false;
                    return true;
                }
            }
        }
        return false;
    }

    private int hintsUsed = 0;

    public synchronized int[] getHint() {
        if (hintsUsed >= 3) {
            return null; // Limit reached
        }

        // Next target is currentNumber + 1
        int target = currentNumber + 1;
        if (target > SIZE * SIZE)
            return null;

        for (int i = 0; i < SIZE; i++) {
            for (int j = 0; j < SIZE; j++) {
                if (solution[i][j] == target) {
                    hintsUsed++;
                    return new int[] { i, j };
                }
            }
        }
        return null;
    }

    private void checkWin() {
        if (currentNumber == SIZE * SIZE) {
            // Verify no skipped fixed numbers (i.e., total filled cells must be 36)
            int count = 0;
            for (int i = 0; i < SIZE; i++) {
                for (int j = 0; j < SIZE; j++) {
                    if (board[i][j] != 0)
                        count++;
                }
            }
            if (count == SIZE * SIZE) {
                this.message = "Level Complete! Well done!";
                gameOver = true;
            } else {
                this.message = "Puzzle complete, but you missed some fixed numbers!";
                // Don't set gameOver, let them undo.
            }
        }
    }

    // Getters for JSON serialization
    public int[][] getBoard() {
        return board;
    }

    public boolean[][] getFixed() {
        return fixed;
    }

    public boolean[][] getHWalls() {
        return hWalls;
    }

    public boolean[][] getVWalls() {
        return vWalls;
    }

    public Map<Integer, Integer> getDisplayMap() {
        return displayMap;
    }

    public int getCurrentNumber() {
        return currentNumber;
    }

    public String getMessage() {
        return message;
    }

    public boolean isGameOver() {
        return gameOver;
    }

    public int getLevel() {
        return level;
    }

    public boolean getScoreSubmitted() {
        return scoreSubmitted;
    }

    public int[][] getSolution() {
        return solution;
    }

    public int getHintsUsed() {
        return hintsUsed;
    }
}
