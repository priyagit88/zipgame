# script.js Explanation

1: `let timerInterval;`
- Variable to hold the timer interval ID.

4: `let globalMaxLevel = 1;`
- Tracks the user's maximum unlocked level locally.

5: `let lastState = null;`
- Stores the last received game state for redrawing during resize events.

9: `document.addEventListener('DOMContentLoaded', () => {`
- Runs code when the DOM is fully loaded.

16: `const savedTheme = localStorage.getItem('theme');`
- Retrieves saved theme preference from local storage.

24: `if (document.cookie.indexOf('user=') !== -1) {`
- Checks if a user cookie exists (simple login check).

26: `fetchState(true);`
- If logged in, fetch current game state from server.

28: `document.getElementById('login-overlay').style.display = 'flex';`
- If not logged in, show the login modal.

33: `function toggleTheme() {`
- Function to switch specific CSS variables/classes for theme.

47: `function initAudio() {`
- Initializes the Web Audio API context (must be user-triggered usually).

56: `function playSound(type) {`
- Plays synthesized sounds based on type ('zip', 'win', 'error') using oscillators.

107: `function login() {`
- Handles login form submission.

113: `fetch('/api/login', { ... })`
- Sends POST request to `/api/login` with username/password.

123: `globalMaxLevel = data.level || 1;`
- Updates local max level from response.

125: `showDashboard();`
- Shows the dashboard upon success.

160: `function showDashboard() {`
- Hides game view, shows dashboard view.

174: `function renderDashboard() {`
- Generates the grid of level buttons using `globalMaxLevel`.

181: `for (let i = 1; i <= limit; i++) {`
- Loops to create buttons for unlocked + 4 locked levels.

201: `function selectLevel(lvl) {`
- Handles selecting a level to play.

202: `fetch(/api/select_level?level=${lvl} ...)`
- Calls API to set the active level.

212: `resetTimer();`
- Resets the timer for the new game.

250: `function fetchState(initial = false) {`
- GETs current state from `/api/state`.

265: `function render(state) {`
- **Main Rendering Function**. Updates the UI based on `state`.

267: `if (lastState) { ... }`
- Compares with last state to trigger sound effects (e.g. if currentNumber increased => Zip sound).

287: `if (state.level && currentLevelText !== newLevelText) { ... }`
- Detects if level changed (e.g., auto-advanced) and updates display/leaderboard.

292: `if (state.gameOver) { ... }`
- Handles game win condition (Show modal, save score).

312: `gridContainer.innerHTML = '';`
- Clears the board.

329: `state.board.forEach((row, r) => {`
- Iterates rows.

330: `row.forEach((cell, c) => {`
- Iterates columns (cells).

332: `div.classList.add('cell');`
- Creates cell DIV.

358: `div.addEventListener('click', () => { ... makeMove(r, c) ... });`
- Adds click listener to cells to trigger moves.

369: `function drawConnections(state) {`
- Draws the path lines ("connectors") between numbers.

371: `oldConnectors.forEach(el => el.remove());`
- Clears old path lines.

401: `for (let i = 1; i < state.currentNumber; i++) {`
- Iterates from 1 to the current head of the path.
- Finds position of `i` and `i+1`.
- Draws a line connecting them.

432: `function makeMove(r, c) {`
- Sends move coordinates to `/api/move`.

442: `function restartGame() {`
- Calls `/api/restart`.

451: `function fetchLeaderboard() {`
- Fetches top scores for the current level.

492: `function startDailyChallenge() {`
- Enters daily challenge mode via `/api/daily`.

515: `function undoStep() {`
- Calls `/api/undo`.

531: `function getHint() {`
- Calls `/api/hint` and highlights the returned cell.

558: `function saveScore(time) {`
- Submits the win time to `/api/score`.
