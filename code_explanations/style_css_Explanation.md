# style.css Explanation

1: `:root {`
- Defines CSS variables (custom properties) for the global theme (colors, spacing).

3-19: `--bg-color: ...`
- Defines light theme colors (backgrounds, text, primary colors).

21: `[data-theme="dark"] {`
- Overrides variables when the body has `data-theme="dark"`.

22-35: `--bg-color: ...`
- Defines dark theme colors.

38: `body {`
- Styles for the body element.

41: `display: flex; ...`
- Uses Flexbox to center the content on the screen (`justify-content: center`, `align-items: center`).

51: `.container {`
- Styles for the main content cards (Dashboard, Game).

55: `box-shadow: ...`
- Adds a drop shadow for a floating effect.

108: `.grid-container {`
- Styles for the 6x6 game board.

109: `display: grid;`
- Uses CSS Grid layout.

110: `grid-template-columns: repeat(6, 60px);`
- Defines 6 columns of 60px width.

111: `grid-template-rows: repeat(6, 60px);`
- Defines 6 rows of 60px height.

121: `.cell {`
- Styles for individual grid cells.

136: `transition: all 0.2s ...`
- Adds smooth animations for interactions.

150: `.cell.wall-bottom {`
- Adds a thick bottom border to represent a horizontal wall.

158: `.cell.fixed {`
- Styles for "fixed" numbers (hints provided at start).

164: `.cell.fixed::before {`
- Uses a pseudo-element to create a circular border/background specifically for fixed numbers.

187: `.cell.current::before {`
- Styles the current "head" of the path with a highlighted circle.

197: `animation: pop 0.3s ...`
- Plays a "pop" animation when the current cell updates.

207: `.connector {`
- Styles for the connecting lines ("zips") between numbers.

208: `position: absolute;`
- Positioned absolutely relative to the grid container to span across cells.

225: `button {`
- General styles for buttons (padding, colors, rounded corners).

265: `.level-grid {`
- Grid layout for the level selection dashboard.

272: `.level-btn {`
- Styles for individual level selection buttons.

292: `.level-btn.completed {`
- Highlights completed levels with a success color.

297: `.level-btn.current {`
- Highlights the current unlocked level more prominently.

304: `.level-btn.locked {`
- Dims and disables locked levels.

312: `#login-overlay {`
- Styles for the login modal background (blur effect).

324: `@media (max-width: 480px) {`
- Responsive design for small screens (mobile).

326: `grid-template-columns: repeat(6, 1fr);`
- Changes grid to use fractional width instead of fixed 60px to fit screen width.
