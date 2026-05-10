# index.html Explanation

1: `<!DOCTYPE html>`
- Standard HTML5 declaration.

2: `<html lang="en">`
- Root element with English language attribute.

4: `<head>`
- Head section for metadata and resource links.

5: `<meta charset="UTF-8">`
- Sets character encoding to UTF-8.

6: `<meta name="viewport" content="width=device-width, initial-scale=1.0">`
- Ensures the page scales correctly on mobile devices.

7: `<title>Zip Puzzle Game</title>`
- Sets the page title shown in the browser tab.

8: `<link rel="stylesheet" href="style.css">`
- Links to the external CSS stylesheet `style.css`.

11: `<body>`
- Body section containing the visible page content.

14: `<div id="login-overlay" ...>`
- A modal overlay for the login/register screen. Initially visible/flexed via inline CSS (though inline styles might be better moved to CSS).

18: `<div id="login-form">`
- Container for the login form inputs and buttons.

21: `<input type="text" id="username-input" ...>`
- Text input for the username.

23: `<input type="password" id="password-input" ...>`
- Password input for the password.

26: `<button onclick="login()" ...>`
- Login button that calls `login()` JavaScript function when clicked.

31: `<div id="register-form" style="display: none;">`
- Container for the registration form (initially hidden).

48: `<div id="completion-overlay" ...>`
- Modal overlay shown when a level is completed (Success screen).

53: `<p>You completed Level <span id="comp-level"></span>!</p>`
- Displays the level number just completed.

56: `<button onclick="handleNextLevel()" ...>`
- Button to proceed to the next level.

67: `<div class="container" id="dashboard-container" style="display: none;">`
- Main dashboard view showing level selection grid. Hidden until logged in.

73: `<div id="level-grid" class="level-grid">`
- Container where level buttons will be dynamically inserted by JavaScript.

77: `<button onclick="startDailyChallenge()" ...>`
- Button to start the daily challenge mode.

80: `<button id="logout-btn" onclick="logout()" ...>`
- Logout button.

83: `<div class="container" id="game-container" style="display: none;">`
- Main game interface container. Hidden until a level is started.

85: `<button class="icon-btn" onclick="toggleTheme()" ...>`
- Button to switch between light and dark themes.

86: `<button class="icon-btn" onclick="toggleSound()" ...>`
- Button to toggle sound effects.

91: `<button onclick="showDashboard()" ...>`
- Button to return to the dashboard.

99: `<div id="level-display" ...>`
- Displays current level number.

101: `<div id="timer" ...>`
- Displays elapsed time.

104: `<span id="message">Loading...</span>`
- Status message area (e.g., "Invalid move", "Well done!").

107: `<div id="grid-container" class="grid-container">`
- The game board container. JavaScript will generate the 6x6 grid of cells here.

112: `<button id="restart-btn" ... onclick="restartGame()">`
- Restarts the current level.

113: `<button id="undo-btn" ... onclick="undoStep()">`
- Undoes the last move.

114: `<button id="hint-btn" ... onclick="getHint()">`
- Request a hint.

124: `<ul id="leaderboard-list" ...>`
- List container for displaying top scores (leaderboard).

131: `<script src="script.js"></script>`
- Links to the external JavaScript file `script.js` which contains the game logic.
