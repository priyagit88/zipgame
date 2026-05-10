let timerInterval;
let startTime;
let isTimerRunning = false;
let globalMaxLevel = 1;
let lastState = null; // Store for resize redraw
let soundEnabled = true;
let audioCtx = null;

document.addEventListener('DOMContentLoaded', () => {
    // Resize listener for responsive connectors
    window.addEventListener('resize', () => {
        if (lastState) drawConnections(lastState);
    });

    // Initialize Theme
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme) {
        document.body.setAttribute('data-theme', savedTheme);
    } else if (window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches) {
        document.body.setAttribute('data-theme', 'dark');
    }

    // Check for login
    if (document.cookie.indexOf('user=') !== -1) {
        // Fetch state to get maxLevel
        fetchState(true); // true = initial load
    } else {
        document.getElementById('login-overlay').style.display = 'flex';
    }
});

// --- Theme & Sound ---
function toggleTheme() {
    const body = document.body;
    const current = body.getAttribute('data-theme');
    const next = current === 'dark' ? 'light' : 'dark';
    body.setAttribute('data-theme', next);
    localStorage.setItem('theme', next);
}

function toggleSound() {
    soundEnabled = !soundEnabled;
    const btn = document.querySelector('button[title="Toggle Sound"]');
    if (btn) btn.textContent = soundEnabled ? "🔊" : "🔇";
}

function initAudio() {
    if (!audioCtx) {
        audioCtx = new (window.AudioContext || window.webkitAudioContext)();
    }
    if (audioCtx.state === 'suspended') {
        audioCtx.resume();
    }
}

function playSound(type) {
    if (!soundEnabled) return;
    initAudio();

    const osc = audioCtx.createOscillator();
    const gain = audioCtx.createGain();
    osc.connect(gain);
    gain.connect(audioCtx.destination);

    const now = audioCtx.currentTime;

    if (type === 'zip') {
        osc.type = 'sine';
        osc.frequency.setValueAtTime(400, now);
        osc.frequency.exponentialRampToValueAtTime(1000, now + 0.1);
        gain.gain.setValueAtTime(0.1, now);
        gain.gain.exponentialRampToValueAtTime(0.01, now + 0.1);
        osc.start(now);
        osc.stop(now + 0.1);
    } else if (type === 'win') {
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(500, now);
        osc.frequency.setValueAtTime(600, now + 0.1);
        osc.frequency.setValueAtTime(800, now + 0.2);
        gain.gain.setValueAtTime(0.2, now);
        gain.gain.linearRampToValueAtTime(0, now + 0.6);
        osc.start(now);
        osc.stop(now + 0.6);
    } else if (type === 'error') {
        osc.type = 'sawtooth';
        osc.frequency.setValueAtTime(200, now);
        osc.frequency.linearRampToValueAtTime(100, now + 0.2);
        gain.gain.setValueAtTime(0.1, now);
        gain.gain.linearRampToValueAtTime(0, now + 0.2);
        osc.start(now);
        osc.stop(now + 0.2);
    }
}

// --- Auth ---
// --- Auth ---
function toggleRegister(showRegister) {
    if (showRegister) {
        document.getElementById('login-form').style.display = 'none';
        document.getElementById('register-form').style.display = 'block';
    } else {
        document.getElementById('login-form').style.display = 'block';
        document.getElementById('register-form').style.display = 'none';
    }
}

function login() {
    const username = document.getElementById('username-input').value;
    const password = document.getElementById('password-input').value; // New password field
    if (!username) return alert("Please enter a username");
    // We allow empty password for legacy or testing, but backend checks validation.

    fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, password: password })
    })
        .then(res => res.json())
        .then(data => {
            if (data.status === 'error') {
                alert(data.message);
            } else {
                globalMaxLevel = data.level || 1;
                document.getElementById('login-overlay').style.display = 'none';
                showDashboard();
            }
        })
        .catch(err => console.error(err));
}

function register() {
    const username = document.getElementById('reg-username').value;
    const password = document.getElementById('reg-password').value;

    if (!username || !password) return alert("Please fill all fields");

    fetch('/api/register', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ username: username, password: password })
    })
        .then(res => res.json())
        .then(data => {
            if (data.status === 'ok') {
                alert("Registration successful! Please login.");
                toggleRegister(false);
            } else {
                alert(data.message);
            }
        })
        .catch(err => console.error(err));
}

function logout() {
    // Clear cookie
    document.cookie = "user=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;";
    location.reload();
}

function showDashboard() {
    stopTimer();
    document.getElementById('game-container').style.display = 'none';
    document.getElementById('dashboard-container').style.display = 'block';

    const match = document.cookie.match(/user=([^;]+)/);
    if (match) {
        document.getElementById('dash-user-display').textContent = match[1];
        document.getElementById('user-display').textContent = match[1];
    }

    renderDashboard();
}

function renderDashboard() {
    const grid = document.getElementById('level-grid');
    grid.innerHTML = '';

    // Generate some levels (e.g. up to Max + 4 to show upcoming)
    const limit = globalMaxLevel + 4;

    for (let i = 1; i <= limit; i++) {
        const btn = document.createElement('button');
        btn.classList.add('level-btn');
        btn.textContent = i;

        if (i < globalMaxLevel) {
            btn.classList.add('completed');
            btn.onclick = () => selectLevel(i);
        } else if (i === globalMaxLevel) {
            btn.classList.add('current');
            btn.onclick = () => selectLevel(i);
        } else {
            btn.classList.add('locked');
            btn.title = "Complete previous levels to unlock";
        }

        grid.appendChild(btn);
    }
}

function selectLevel(lvl) {
    fetch(`/api/select_level?level=${lvl}`, { method: 'POST' })
        .then(async res => {
            if (res.ok) return res.json();
            // Try to get text, default to status text
            const text = await res.text().catch(() => res.statusText);
            throw new Error(`${res.status}: ${text} at ${res.url}`);
        })
        .then(data => {
            showGame();
            render(data);
            resetTimer(); // Start new timer for this level
        })
        .catch(err => alert(err.message));
}

function showGame() {
    document.getElementById('dashboard-container').style.display = 'none';
    document.getElementById('game-container').style.display = 'block';
    // Reset level text to force update
    document.getElementById('level-display').textContent = "";
}

function startTimer() {
    if (isTimerRunning) return;
    startTime = Date.now();
    isTimerRunning = true;
    timerInterval = setInterval(updateTimer, 1000);
}

function stopTimer() {
    clearInterval(timerInterval);
    isTimerRunning = false;
}

function resetTimer() {
    stopTimer();
    document.getElementById('timer').textContent = "00:00";
    startTimer();
}

function updateTimer() {
    const elapsed = Math.floor((Date.now() - startTime) / 1000);
    const m = Math.floor(elapsed / 60);
    const s = elapsed % 60;
    document.getElementById('timer').textContent =
        (m < 10 ? "0" + m : m) + ":" + (s < 10 ? "0" + s : s);
}

function fetchState(initial = false) {
    fetch('/api/state')
        .then(response => response.json())
        .then(data => {
            globalMaxLevel = data.maxLevel || 1;
            if (initial) {
                document.getElementById('login-overlay').style.display = 'none';
                showDashboard(); // Start at dashboard
            } else {
                render(data);
            }
        })
        .catch(err => console.error('Error fetching state:', err));
}

function render(state) {
    // Sound Trigger Logic
    if (lastState) {
        if (state.gameOver && !lastState.gameOver) {
            playSound('win');
        } else if (state.currentNumber > lastState.currentNumber) {
            playSound('zip');
        } else if (state.message !== lastState.message && state.message.toLowerCase().includes('invalid')) {
            playSound('error'); // Maybe?
        }
    }

    lastState = state;
    // Update global max level if changed
    if (state.maxLevel && state.maxLevel > globalMaxLevel) {
        globalMaxLevel = state.maxLevel;
    }

    // Detect Level Change for Leaderboard
    const currentLevelText = document.getElementById('level-display').textContent;
    const newLevelText = "Level " + state.level;

    if (state.level && currentLevelText !== newLevelText) {
        document.getElementById('level-display').textContent = newLevelText;
        fetchLeaderboard();
    }

    if (state.gameOver) {
        stopTimer();
        // Show Modal
        document.getElementById('completion-overlay').style.display = 'flex';
        document.getElementById('comp-level').innerText = state.level;
        document.getElementById('comp-time').innerText = document.getElementById('timer').innerText;

        // Auto-save score if not already submitted
        if (!state.scoreSubmitted) {
            const timeText = document.getElementById('timer').textContent;
            const parts = timeText.split(":");
            const seconds = parseInt(parts[0]) * 60 + parseInt(parts[1]);
            saveScore(seconds);
        }
    } else {
        document.getElementById('completion-overlay').style.display = 'none';
        document.getElementById('restart-btn').textContent = 'Restart Level';
    }

    const gridContainer = document.getElementById('grid-container');
    gridContainer.innerHTML = '';

    document.getElementById('message').textContent = state.message;
    document.getElementById('current-number').textContent = "Path Length: " + state.currentNumber;

    // Status color
    if (state.gameOver) {
        document.getElementById('message').style.color = 'var(--success-color)';
    } else {
        // Check for negative messages
        if (state.message.includes('Invalid') || state.message.includes('Blocked')) {
            document.getElementById('message').style.color = '#ef4444';
        } else {
            document.getElementById('message').style.color = 'var(--subtext-color)';
        }
    }

    state.board.forEach((row, r) => {
        row.forEach((cell, c) => {
            const div = document.createElement('div');
            div.classList.add('cell');

            if (cell.value > 0) {
                // If it's fixed or filled
                // Fixed numbers show value
                // Filled numbers (path) don't typically show value unless we want them to?
                // The original code only showed value for fixed cells.

                if (cell.fixed) {
                    // For fixed cells, we use data attribute or inner text. 
                    // The CSS uses content: attr(data-display) logic? No, I reverted to textContent in CSS comments.
                    // Let's use textContent but maybe inside a span?
                    // CSS handles .cell text centering.
                    div.textContent = cell.display;
                }
                div.classList.add('filled');
                if (cell.value <= state.currentNumber) {
                    div.classList.add('active-path');
                }
            }
            if (cell.fixed) div.classList.add('fixed');
            if (cell.value === state.currentNumber) div.classList.add('current');

            if (cell.hWall) div.classList.add('wall-bottom');
            if (cell.vWall) div.classList.add('wall-right');

            div.addEventListener('click', () => {
                if (!state.gameOver) makeMove(r, c);
            });

            gridContainer.appendChild(div);
        });
    });

    drawConnections(state);
}

function drawConnections(state) {
    const gridContainer = document.getElementById('grid-container');
    const oldConnectors = gridContainer.querySelectorAll('.connector');
    oldConnectors.forEach(el => el.remove());

    const cells = gridContainer.querySelectorAll('.cell');
    if (cells.length === 0) return;

    // Helper to get center
    const getCellCenter = (r, c) => {
        const index = r * 6 + c;
        if (index >= cells.length) return { x: 0, y: 0 };
        const cell = cells[index];
        // We need position relative to the grid container
        return {
            x: cell.offsetLeft + cell.offsetWidth / 2,
            y: cell.offsetTop + cell.offsetHeight / 2
        };
    };

    const findPos = (val) => {
        for (let r = 0; r < 6; r++) {
            for (let c = 0; c < 6; c++) {
                if (state.board[r][c].value === val) return { r, c };
            }
        }
        return null;
    };

    const THICKNESS = 8; // Slightly thinner for elegance
    const HALF_THICK = THICKNESS / 2;

    for (let i = 1; i < state.currentNumber; i++) {
        const start = findPos(i);
        const end = findPos(i + 1);

        if (start && end) {
            const connector = document.createElement('div');
            connector.classList.add('connector');

            const p1 = getCellCenter(start.r, start.c);
            const p2 = getCellCenter(end.r, end.c);

            if (start.r === end.r) {
                // Horizontal
                const width = Math.abs(p2.x - p1.x);
                connector.style.width = (width + THICKNESS / 2) + 'px'; // Overlap slightly to close gaps
                connector.style.height = THICKNESS + 'px';
                connector.style.left = (Math.min(p1.x, p2.x) - THICKNESS / 4) + 'px'; // Centering fix
                connector.style.top = (p1.y - HALF_THICK) + 'px';
            } else {
                // Vertical
                const height = Math.abs(p2.y - p1.y);
                connector.style.width = THICKNESS + 'px';
                connector.style.height = (height + THICKNESS / 2) + 'px';
                connector.style.left = (p1.x - HALF_THICK) + 'px';
                connector.style.top = (Math.min(p1.y, p2.y) - THICKNESS / 4) + 'px';
            }
            gridContainer.appendChild(connector);
        }
    }
}

function makeMove(r, c) {
    fetch(`/api/move?r=${r}&c=${c}`, { method: 'POST' })
        .then(response => response.json())
        .then(data => render(data))
        .catch(err => {
            console.error('Error making move:', err);
            playSound('error');
        });
}

function restartGame() {
    // If text is "Next Level", logic is handled by backend reset (increments level)
    resetTimer();
    fetch('/api/restart', { method: 'POST' })
        .then(response => response.json())
        .then(data => render(data))
        .catch(err => console.error('Error restarting:', err));
}

function fetchLeaderboard() {
    let level = 1;
    const levelText = document.getElementById('level-display').textContent;
    if (levelText) {
        const parts = levelText.split(" ");
        if (parts.length > 1) level = parseInt(parts[1]);
    }

    fetch(`/api/leaderboard?level=${level}`)
        .then(res => res.json())
        .then(data => {
            const list = document.getElementById('leaderboard-list');
            list.innerHTML = '';

            const header = document.createElement('li');
            header.innerHTML = `<strong>Your Best Times (Level ${level})</strong>`;
            header.style.marginBottom = '5px';
            header.style.color = 'var(--text-color)';
            list.appendChild(header);

            if (data.length === 0) {
                const empty = document.createElement('li');
                empty.textContent = 'No scores yet';
                list.appendChild(empty);
                return;
            }
            data.forEach((score, index) => {
                const li = document.createElement('li');
                const m = Math.floor(score.time / 60);
                const s = score.time % 60;
                const timeStr = (m < 10 ? "0" + m : m) + ":" + (s < 10 ? "0" + s : s);
                li.textContent = `${index + 1}. ${timeStr}`;
                list.appendChild(li);
            });
        })
        .catch(err => console.error(err));
}

// ... (previous functions)


function startDailyChallenge() {
    fetch('/api/daily', { method: 'POST' })
        .then(response => {
            if (response.status === 401) {
                document.getElementById('login-overlay').style.display = 'flex';
                return null;
            }
            return response.json();
        })
        .then(data => {
            if (data) {
                render(data);
                document.getElementById('dashboard-container').style.display = 'none';
                document.getElementById('game-container').style.display = 'block';
                document.getElementById('level-display').innerText = "Daily Challenge";
                startTimer();
                fetchLeaderboard(1); // Daily leaderboard? Or just level 1?
                // Ideally backend should support daily leaderboard.
                // For now, let's just show top scores generally or hide it.
            }
        });
}

function undoStep() {
    fetch('/api/undo', { method: 'POST' })
        .then(res => {
            if (res.ok) return res.json();
            throw new Error('Cannot undo');
        })
        .then(data => {
            render(data);
            playSound('zip'); // Reuse zip sound for feedback
        })
        .catch(err => {
            console.log(err);
            playSound('error');
        });
}

function getHint() {
    fetch('/api/hint', { method: 'POST' })
        .then(async res => {
            if (res.ok) return res.json();
            const text = await res.text();
            throw new Error(text || 'No hint available');
        })
        .then(hint => {
            // Highlight the hint cell
            const grid = document.getElementById('grid-container');
            const index = hint.r * 6 + hint.c;
            if (grid.children[index]) {
                const cell = grid.children[index];
                cell.style.backgroundColor = '#fef08a'; // Yellow-200
                cell.style.transition = 'background-color 0.5s';
                setTimeout(() => {
                    cell.style.backgroundColor = '';
                }, 1000);
            }
        })
        .catch(err => {
            console.log(err);
            alert(err.message); // Show "Hint limit reached" or "No hint available"
            playSound('error');
        });
}

function saveScore(time) {
    // Name is handled by cookie in backend
    fetch('/api/score', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ time: time })
    })
        .then(res => res.json())
        .then(() => fetchLeaderboard())
        .catch(err => console.error(err));
}

function handleNextLevel() {
    document.getElementById('completion-overlay').style.display = 'none';
    restartGame(); // Backend advances level on reset if gameover
}

function handleReplay() {
    document.getElementById('completion-overlay').style.display = 'none';
    // To replay without advancing, we must re-select the current level explicitly.
    if (lastState) {
        selectLevel(lastState.level);
    }
}
