# LevelGenerator.java Explanation

1: `package com.zipgame;`
- Declares the package.

3-10: `import java.util...;`
- Imports `ArrayList`, `Arrays`, `Collections` (for shuffling), `Random`, etc.

12: `public class LevelGenerator {`
- Defines the `LevelGenerator` class, responsible for creating game levels.

14: `private static final int SIZE = 6;`
- Fixed board size of 6x6.

16: `public static class LevelData {`
- Inner static class to hold the generated data structure.

17-22: `public int[][] board; ...`
- Fields for the generated level: `board` (initial state), `solution` (answer key), `fixed` (clues), walls, and display map.

34: `public static LevelData generateLevel() {`
- Overload to generate level with a random seed based on current time.

38: `public static LevelData generateLevel(long seed) {`
- Main generation method using a specific seed.

39: `Random rng = new Random(seed);`
- Initializes random number generator with the seed (ensures reproducibility).

43: `int[][] pathGrid = new int[SIZE][SIZE];`
- Temporary grid to store the generated Hamiltonian path.

46: `for (int attempt = 0; attempt < 100; attempt++) {`
- Tries up to 100 times to generate a valid path (since it's randomized and can fail).

47: `resetGrid(pathGrid);`
- Clears the grid for a new attempt.

48-50: `int startR = rng.nextInt(SIZE); ...`
- Picks a random starting position.

52: `if (solve(pathGrid, startR, startC, 2, rng)) {`
- calls recursive `solve` to find a path starting from the random point. The `2` indicates the next number to place (since 1 is placed at start).

58-61: `if (!found) { ... }`
- If 100 attempts fail, generate a fallback "snake" level so the game doesn't crash.

64: `for (int i = 0; i < SIZE; i++) {`
- Copies the successfully generated path into `data.solution`.

69: `int hintsCount = 8 + rng.nextInt(5);`
- Decides how many "hints" (fixed numbers) to reveal (between 8 and 12).

70-79: `List<Integer> toReveal ...`
- Algorithm to pick which numbers to reveal.
- Always reveals 1 and 36 (Start and End).
- Randomly selects other numbers to reveal.
- Sorts them.

82-92: `for (int val : toReveal) { ... }`
- Marks the selected numbers as `fixed` on the board.
- Also populates `displayMap` to give them logical "Checkpoints" (e.g. 1st hint is 1, 2nd is 2, etc., used for display purposes if needed).

95: `generateWalls(data, pathGrid, rng);`
- Adds walls based on the path layout.

100: `private static LevelData generateFallback() {`
- Generates a simple, guaranteed solvable "snake" pattern (left-right-left-right) as a failsafe.

131: `private static boolean solve(int[][] grid, int r, int c, int count, Random rng) {`
- Recursive backtracking algorithm to find a Hamiltonian path.

132: `if (count > SIZE * SIZE)`
- Base case: If count exceeds 36, we have filled the grid. Success!

135: `List<int[]> moves = getValidMoves(grid, r, c);`
- Gets all valid empty neighbors.

138: `Collections.shuffle(moves, rng);`
- Randomizes move order to ensure variety.

141: `moves.sort(Comparator.comparingInt(m -> getDegree(grid, m[0], m[1])));`
- **Warnsdorff's Rule**: Sorts moves by their "degree" (number of valid future moves). Prioritizing squares with fewer options helps avoid dead ends.

143: `for (int[] move : moves) {`
- Iterates through the sorted moves.

144: `grid[move[0]][move[1]] = count;`
- "Do": Place the number.

145: `if (solve(grid, move[0], move[1], count + 1, rng))`
- "Recurse": Try to solve the rest.

147: `grid[move[0]][move[1]] = 0;`
- "Undo": If recursion failed, backtrack (reset cell to 0).

153: `private static int getDegree(int[][] grid, int r, int c) {`
- Calculates how many empty neighbors a cell has. Used for the heuristic.

157: `private static List<int[]> getValidMoves(int[][] grid, int r, int c) {`
- Finds neighbors (Up, Down, Left, Right) that are within bounds and empty (`grid[nr][nc] == 0`).

173: `private static void generateWalls(LevelData data, int[][] pathGrid, Random rng) {`
- Generates walls between cells that are NOT connected in the path.

174-198: `for ...`
- Iterates through the grid.
- Checks right neighbor: If `val` and `neighborVal` are NOT sequential (`Math.abs(val - neighborVal) == 1`), they are not connected in the path.
- `if (rng.nextFloat() < 0.3)`: 30% chance to place a wall between unconnected neighbors. This makes the path more constrained and obvious.
