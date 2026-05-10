package com.zipgame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LevelGenerator {

    private static final int SIZE = 6;

    public static class LevelData {
        public int[][] board; // Solution board (full 1-36)
        public int[][] solution; // Full solution path
        public boolean[][] fixed; // Which ones are revealed
        public boolean[][] hWalls;
        public boolean[][] vWalls;
        public Map<Integer, Integer> displayMap; // Internal Value -> Display Rank

        public LevelData() {
            board = new int[SIZE][SIZE];
            solution = new int[SIZE][SIZE];
            fixed = new boolean[SIZE][SIZE];
            hWalls = new boolean[SIZE][SIZE];
            vWalls = new boolean[SIZE][SIZE];
            displayMap = new HashMap<>();
        }
    }

    public static LevelData generateLevel() {
        return generateLevel(System.currentTimeMillis());
    }

    public static LevelData generateLevel(long seed) {
        Random rng = new Random(seed);
        LevelData data = new LevelData();

        // 1. Generate Hamiltonian Path
        int[][] pathGrid = new int[SIZE][SIZE];
        boolean found = false;

        for (int attempt = 0; attempt < 100; attempt++) {
            resetGrid(pathGrid);
            int startR = rng.nextInt(SIZE);
            int startC = rng.nextInt(SIZE);
            pathGrid[startR][startC] = 1;

            if (solve(pathGrid, startR, startC, 2, rng)) {
                found = true;
                break;
            }
        }

        if (!found) {
            System.out.println("Warning: Level generation failed, using fallback.");
            return generateFallback();
        }

        // Store solution (Deep Copy)
        for (int i = 0; i < SIZE; i++) {
            System.arraycopy(pathGrid[i], 0, data.solution[i], 0, SIZE);
        }

        // 2. Setup Board and Fixed Numbers
        int hintsCount = 8 + rng.nextInt(5); // 8 to 12 hints
        List<Integer> toReveal = new ArrayList<>();
        toReveal.add(1);
        toReveal.add(SIZE * SIZE); // 36

        while (toReveal.size() < hintsCount) {
            int val = 2 + rng.nextInt(SIZE * SIZE - 2);
            if (!toReveal.contains(val))
                toReveal.add(val);
        }
        Collections.sort(toReveal);

        int displayCounter = 1;
        for (int val : toReveal) {
            for (int r = 0; r < SIZE; r++) {
                for (int c = 0; c < SIZE; c++) {
                    if (pathGrid[r][c] == val) {
                        data.board[r][c] = val;
                        data.fixed[r][c] = true;
                        data.displayMap.put(val, displayCounter++);
                    }
                }
            }
        }

        // 3. Generate Walls
        generateWalls(data, pathGrid, rng);

        return data;
    }

    private static LevelData generateFallback() {
        LevelData data = new LevelData();
        // Return a dummy safe level if generation fails
        // Simple horizontal snake
        int val = 1;
        for (int r = 0; r < SIZE; r++) {
            if (r % 2 == 0) {
                for (int c = 0; c < SIZE; c++)
                    data.solution[r][c] = val++;
            } else {
                for (int c = SIZE - 1; c >= 0; c--)
                    data.solution[r][c] = val++;
            }
        }
        data.board[0][0] = 1;
        data.fixed[0][0] = true;
        data.displayMap.put(1, 1);

        data.board[SIZE - 1][(SIZE % 2 == 0) ? 0 : SIZE - 1] = SIZE * SIZE;
        data.fixed[SIZE - 1][(SIZE % 2 == 0) ? 0 : SIZE - 1] = true;
        data.displayMap.put(SIZE * SIZE, 2);

        return data;
    }

    private static void resetGrid(int[][] grid) {
        for (int i = 0; i < SIZE; i++) {
            Arrays.fill(grid[i], 0);
        }
    }

    private static boolean solve(int[][] grid, int r, int c, int count, Random rng) {
        if (count > SIZE * SIZE)
            return true;

        List<int[]> moves = getValidMoves(grid, r, c);

        // Shuffle for randomness
        Collections.shuffle(moves, rng);

        // Sort by degree (heuristic)
        moves.sort(Comparator.comparingInt(m -> getDegree(grid, m[0], m[1])));

        for (int[] move : moves) {
            grid[move[0]][move[1]] = count;
            if (solve(grid, move[0], move[1], count + 1, rng))
                return true;
            grid[move[0]][move[1]] = 0;
        }

        return false;
    }

    private static int getDegree(int[][] grid, int r, int c) {
        return getValidMoves(grid, r, c).size();
    }

    private static List<int[]> getValidMoves(int[][] grid, int r, int c) {
        List<int[]> moves = new ArrayList<>();
        int[] dr = { -1, 1, 0, 0 };
        int[] dc = { 0, 0, -1, 1 };

        for (int i = 0; i < 4; i++) {
            int nr = r + dr[i];
            int nc = c + dc[i];

            if (nr >= 0 && nr < SIZE && nc >= 0 && nc < SIZE && grid[nr][nc] == 0) {
                moves.add(new int[] { nr, nc });
            }
        }
        return moves;
    }

    private static void generateWalls(LevelData data, int[][] pathGrid, Random rng) {
        for (int r = 0; r < SIZE; r++) {
            for (int c = 0; c < SIZE; c++) {
                int val = pathGrid[r][c];

                // Right Wall
                if (c < SIZE - 1) {
                    int neighborVal = pathGrid[r][c + 1];
                    boolean connected = (Math.abs(val - neighborVal) == 1);
                    if (!connected) {
                        if (rng.nextFloat() < 0.3)
                            data.vWalls[r][c] = true;
                    }
                }

                // Bottom Wall
                if (r < SIZE - 1) {
                    int neighborVal = pathGrid[r + 1][c];
                    boolean connected = (Math.abs(val - neighborVal) == 1);
                    if (!connected) {
                        if (rng.nextFloat() < 0.3)
                            data.hWalls[r][c] = true;
                    }
                }
            }
        }
    }
}
