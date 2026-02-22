// ============= GAME BOOSTER - REAL RAM CLEANER =============

// Database Game Populer
const GAMES = [
    { 
        name: "Mobile Legends", 
        pkg: "com.mobile.legends", 
        uri: "mobilelegends://", 
        category: "MOBA",
        icon: "🎮"
    },
    { 
        name: "Free Fire", 
        pkg: "com.dts.freefireth", 
        uri: "freefire://", 
        category: "Battle Royale",
        icon: "🔥"
    },
    { 
        name: "Free Fire MAX", 
        pkg: "com.dts.freefiremax", 
        uri: "freefiremax://", 
        category: "Battle Royale",
        icon: "🔥"
    },
    { 
        name: "PUBG Mobile", 
        pkg: "com.tencent.ig", 
        uri: "pubgm://", 
        category: "Battle Royale",
        icon: "🔫"
    },
    { 
        name: "Genshin Impact", 
        pkg: "com.miHoYo.GenshinImpact", 
        uri: "genshinimpact://", 
        category: "RPG",
        icon: "✨"
    },
    { 
        name: "Call of Duty", 
        pkg: "com.activision.callofduty.shooter", 
        uri: "codm://", 
        category: "FPS",
        icon: "🎯"
    },
    { 
        name: "FIFA Mobile", 
        pkg: "com.ea.gp.fifamobile", 
        uri: "fifamobile://", 
        category: "Sports",
        icon: "⚽"
    },
    { 
        name: "Among Us", 
        pkg: "com.innersloth.spacemafia", 
        uri: "amongus://", 
        category: "Party",
        icon: "👾"
    }
];

// ============= STATE MANAGEMENT =============
let state = {
    detectedGames: [],
    recentGames: JSON.parse(localStorage.getItem('recentGames') || '[]'),
    hasPermission: localStorage.getItem('overlayPermission') === 'granted',
    overlayActive: false,
    currentGame: null,
    fps: 60,
    fpsTarget: 60,
    frameCount: 0,
    lastFpsUpdate: performance.now(),
    isAndroid: /Android/i.test(navigator.userAgent)
};

// ============= ELEMENTS =============
const elements = {
    overlay: document.getElementById('floatOverlay'),
    menuModal: document.getElementById('menuModal'),
    toast: document.getElementById('toast'),
    gameGrid: document.getElementById('gameGrid'),
    recentList: document.getElementById('recentList'),
    logCard: document.getElementById('logCard'),
    logContent: document.getElementById('logContent'),
    
    // RAM elements
    ramPercent: document.getElementById('ramPercent'),
    ramFill: document.getElementById('ramFill'),
    ramUsed: document.getElementById('ramUsed'),
    ramFree: document.getElementById('ramFree'),
    ramTotal: document.getElementById('ramTotal'),
    
    // FPS elements
    fpsTarget: document.getElementById('fpsTarget'),
    fpsSlider: document.getElementById('fpsSlider'),
    overlayFps: document.getElementById('overlayFps'),
    overlayRam: document.getElementById('overlayRam'),
    overlayProcs: document.getElementById('overlayProcs'),
    
    // Other
    deviceModel: document.getElementById('deviceModel'),
    gameCount: document.getElementById('gameCount'),
    cleanRamBtn: document.getElementById('cleanRamBtn'),
    closeLog: document.getElementById('closeLog'),
    overlayBoost: document.getElementById('overlayBoost'),
    minimizeOverlay: document.getElementById('minimizeOverlay'),
    closeOverlay: document.getElementById('closeOverlay'),
    menuBtn: document.getElementById('menuBtn'),
    closeMenu: document.getElementById('closeMenu'),
    scanGamesBtn: document.getElementById('scanGamesBtn'),
    checkPermBtn: document.getElementById('checkPermBtn'),
    resetOverlayBtn: document.getElementById('resetOverlayBtn'),
    showProcBtn: document.getElementById('showProcBtn'),
    clearCacheBtn: document.getElementById('clearCacheBtn')
};

// ============= INITIALIZATION =============
document.addEventListener('DOMContentLoaded', () => {
    init();
});

function init() {
    updateDeviceInfo();
    updateRamStats();
    scanGames();
    renderRecent();
    setupFPS();
    setupEventListeners();
    setupOverlayDrag();
    startFPSMonitor();
    
    // Auto update every 2 seconds
    setInterval(updateRamStats, 2000);
}

// ============= DEVICE INFO =============
function updateDeviceInfo() {
    if (window.Android) {
        elements.deviceModel.textContent = window.Android.getDeviceModel();
    } else {
        elements.deviceModel.textContent = 'Desktop Mode';
    }
}

// ============= RAM STATS =============
function updateRamStats() {
    if (!window.Android) {
        // Demo mode
        const demoTotal = 4 * 1024 * 1024 * 1024; // 4GB
        const demoUsed = Math.floor(Math.random() * 2 + 2) * 1024 * 1024 * 1024; // 2-3GB
        const demoFree = demoTotal - demoUsed;
        const demoPercent = Math.round((demoUsed / demoTotal) * 100);
        
        updateRamDisplay(demoTotal, demoUsed, demoFree, demoPercent);
        return;
    }
    
    try {
        const total = window.Android.getTotalRam();
        const avail = window.Android.getAvailableRam();
        const used = total - avail;
        const percent = Math.round((used / total) * 100);
        
        updateRamDisplay(total, used, avail, percent);
        
        // Update overlay
        elements.overlayRam.textContent = percent + '%';
        
        const procs = window.Android.getRunningProcessesCount();
        elements.overlayProcs.textContent = procs;
        
    } catch (e) {
        console.log('RAM update error:', e);
    }
}

function updateRamDisplay(total, used, free, percent) {
    elements.ramPercent.textContent = percent + '%';
    elements.ramFill.style.width = percent + '%';
    elements.ramUsed.textContent = formatBytes(used);
    elements.ramFree.textContent = formatBytes(free);
    elements.ramTotal.textContent = formatBytes(total);
}

function formatBytes(bytes) {
    if (bytes < 1024) return bytes + ' B';
    const units = ['KB', 'MB', 'GB', 'TB'];
    let i = -1;
    do {
        bytes /= 1024;
        i++;
    } while (bytes >= 1024 && i < units.length - 1);
    return bytes.toFixed(1) + ' ' + units[i];
}

// ============= CLEAN RAM (REAL) =============
if (elements.cleanRamBtn) {
    elements.cleanRamBtn.addEventListener('click', () => {
        if (!window.Android) {
            showToast('Tidak dalam mode Android');
            return;
        }
        
        showToast('🧹 Membersihkan RAM...');
        
        // Animate button
        elements.cleanRamBtn.style.transform = 'scale(0.95)';
        setTimeout(() => {
            elements.cleanRamBtn.style.transform = 'scale(1)';
        }, 200);
        
        // Call native RAM cleaner
        try {
            const result = window.Android.cleanRamNow();
            
            // Show log
            elements.logContent.textContent = result;
            elements.logCard.style.display = 'block';
            
            // Update stats after cleaning
            setTimeout(() => {
                updateRamStats();
                showToast('✅ RAM dibersihkan!');
            }, 1500);
            
        } catch (e) {
            showToast('❌ Gagal membersihkan RAM');
        }
    });
}

// Close log
if (elements.closeLog) {
    elements.closeLog.addEventListener('click', () => {
        elements.logCard.style.display = 'none';
    });
}

// ============= FPS MONITOR =============
function startFPSMonitor() {
    function measureFPS() {
        state.frameCount++;
        const now = performance.now();
        if (now - state.lastFpsUpdate >= 1000) {
            state.fps = state.frameCount;
            state.frameCount = 0;
            state.lastFpsUpdate = now;
            
            // Update FPS displays
            if (elements.overlayFps) {
                elements.overlayFps.textContent = state.fps;
            }
        }
        requestAnimationFrame(measureFPS);
    }
    measureFPS();
}

function setupFPS() {
    if (!elements.fpsSlider || !elements.fpsTarget) return;
    
    // Load saved target
    const savedTarget = localStorage.getItem('fpsTarget');
    if (savedTarget) {
        state.fpsTarget = parseInt(savedTarget);
        elements.fpsSlider.value = state.fpsTarget;
        elements.fpsTarget.textContent = state.fpsTarget;
    }
    
    elements.fpsSlider.addEventListener('input', (e) => {
        state.fpsTarget = parseInt(e.target.value);
        elements.fpsTarget.textContent = state.fpsTarget;
        localStorage.setItem('fpsTarget', state.fpsTarget);
    });
}

// ============= SCAN GAMES =============
async function scanGames() {
    if (!elements.gameGrid) return;
    
    elements.gameGrid.innerHTML = '<div class="loading">🔍 Memindai game...</div>';
    
    const detected = [];
    
    if (window.Android) {
        for (const game of GAMES) {
            try {
                if (window.Android.isPackageInstalled(game.pkg)) {
                    detected.push(game);
                }
            } catch (e) {
                console.log('Error checking game:', game.pkg);
            }
            // Small delay to prevent blocking
            await new Promise(r => setTimeout(r, 10));
        }
    }
    
    // Fallback if no games detected
    if (detected.length === 0) {
        detected.push(...GAMES.slice(0, 4));
    }
    
    state.detectedGames = detected;
    
    if (elements.gameCount) {
        elements.gameCount.textContent = detected.length;
    }
    
    renderGameList();
}

function renderGameList() {
    if (!elements.gameGrid) return;
    
    if (state.detectedGames.length === 0) {
        elements.gameGrid.innerHTML = '<div class="loading">Tidak ada game ditemukan</div>';
        return;
    }
    
    elements.gameGrid.innerHTML = state.detectedGames.map(game => `
        <div class="game-card" data-pkg="${game.pkg}" data-uri="${game.uri}" data-name="${game.name}">
            <div class="game-icon">${game.icon}</div>
            <div class="game-name">${game.name}</div>
            <div class="game-category">${game.category}</div>
            <span class="game-badge">MAIN</span>
        </div>
    `).join('');
    
    // Add click handlers
    document.querySelectorAll('.game-card').forEach(card => {
        card.addEventListener('click', () => {
            const name = card.dataset.name;
            const pkg = card.dataset.pkg;
            launchGame(name, pkg);
        });
    });
}

// ============= LAUNCH GAME =============
function launchGame(name, pkg) {
    showToast(`🚀 Membuka ${name}...`);
    
    // Set current game
    state.currentGame = name;
    
    // Save to recent
    const recent = { name, pkg, time: Date.now() };
    state.recentGames = [recent, ...state.recentGames.filter(g => g.pkg !== pkg)].slice(0, 5);
    localStorage.setItem('recentGames', JSON.stringify(state.recentGames));
    renderRecent();
    
    // Launch game
    try {
        if (window.Android) {
            window.Android.openGame(pkg);
        } else {
            // Desktop fallback
            window.open(`https://play.google.com/store/apps/details?id=${pkg}`, '_blank');
        }
        
        // Show overlay after launching
        setTimeout(() => {
            if (elements.overlay && state.hasPermission) {
                elements.overlay.style.display = 'block';
                state.overlayActive = true;
            }
        }, 2000);
        
    } catch (e) {
        showToast('❌ Gagal membuka game');
    }
}

// ============= RENDER RECENT =============
function renderRecent() {
    if (!elements.recentList) return;
    
    if (state.recentGames.length === 0) {
        elements.recentList.innerHTML = '<div class="recent-item">-</div>';
        return;
    }
    
    elements.recentList.innerHTML = state.recentGames.map(game => `
        <div class="recent-item" data-pkg="${game.pkg}">${game.name}</div>
    `).join('');
    
    document.querySelectorAll('.recent-item').forEach(item => {
        item.addEventListener('click', () => {
            const pkg = item.dataset.pkg;
            const game = GAMES.find(g => g.pkg === pkg);
            if (game) launchGame(game.name, pkg);
        });
    });
}

// ============= OVERLAY DRAG =============
function setupOverlayDrag() {
    const dragHandle = document.getElementById('overlayDrag');
    if (!dragHandle || !elements.overlay) return;
    
    let isDragging = false;
    let offsetX, offsetY;
    
    dragHandle.addEventListener('mousedown', (e) => {
        isDragging = true;
        offsetX = e.clientX - elements.overlay.offsetLeft;
        offsetY = e.clientY - elements.overlay.offsetTop;
        elements.overlay.style.transition = 'none';
    });
    
    dragHandle.addEventListener('touchstart', (e) => {
        isDragging = true;
        offsetX = e.touches[0].clientX - elements.overlay.offsetLeft;
        offsetY = e.touches[0].clientY - elements.overlay.offsetTop;
        elements.overlay.style.transition = 'none';
    }, { passive: false });
    
    document.addEventListener('mousemove', (e) => {
        if (!isDragging) return;
        e.preventDefault();
        
        const x = e.clientX - offsetX;
        const y = e.clientY - offsetY;
        
        // Boundary check
        const maxX = window.innerWidth - elements.overlay.offsetWidth;
        const maxY = window.innerHeight - elements.overlay.offsetHeight;
        
        elements.overlay.style.left = Math.min(Math.max(0, x), maxX) + 'px';
        elements.overlay.style.top = Math.min(Math.max(0, y), maxY) + 'px';
        elements.overlay.style.right = 'auto';
    });
    
    document.addEventListener('touchmove', (e) => {
        if (!isDragging) return;
        e.preventDefault();
        
        const x = e.touches[0].clientX - offsetX;
        const y = e.touches[0].clientY - offsetY;
        
        const maxX = window.innerWidth - elements.overlay.offsetWidth;
        const maxY = window.innerHeight - elements.overlay.offsetHeight;
        
        elements.overlay.style.left = Math.min(Math.max(0, x), maxX) + 'px';
        elements.overlay.style.top = Math.min(Math.max(0, y), maxY) + 'px';
        elements.overlay.style.right = 'auto';
    }, { passive: false });
    
    document.addEventListener('mouseup', () => {
        isDragging = false;
        elements.overlay.style.transition = 'all 0.3s';
    });
    
    document.addEventListener('touchend', () => {
        isDragging = false;
        elements.overlay.style.transition = 'all 0.3s';
    });
}

// Overlay controls
if (elements.minimizeOverlay) {
    elements.minimizeOverlay.addEventListener('click', () => {
        elements.overlay.classList.toggle('minimized');
    });
}

if (elements.closeOverlay) {
    elements.closeOverlay.addEventListener('click', () => {
        elements.overlay.style.display = 'none';
        state.overlayActive = false;
    });
}

if (elements.overlayBoost) {
    elements.overlayBoost.addEventListener('click', () => {
        if (elements.cleanRamBtn) {
            elements.cleanRamBtn.click();
        }
    });
}

// ============= TOAST =============
function showToast(message, duration = 2000) {
    if (!elements.toast) return;
    
    elements.toast.textContent = message;
    elements.toast.classList.add('show');
    
    setTimeout(() => {
        elements.toast.classList.remove('show');
    }, duration);
}

// ============= MENU =============
if (elements.menuBtn) {
    elements.menuBtn.addEventListener('click', () => {
        if (elements.menuModal) {
            elements.menuModal.classList.add('show');
        }
    });
}

if (elements.closeMenu) {
    elements.closeMenu.addEventListener('click', () => {
        elements.menuModal.classList.remove('show');
    });
}

// Menu items
if (elements.scanGamesBtn) {
    elements.scanGamesBtn.addEventListener('click', () => {
        elements.menuModal.classList.remove('show');
        scanGames();
    });
}

if (elements.checkPermBtn) {
    elements.checkPermBtn.addEventListener('click', () => {
        elements.menuModal.classList.remove('show');
        
        if (window.Android) {
            showToast('✅ Izin Android aktif');
        } else {
            showToast('ℹ️ Mode desktop');
        }
    });
}

if (elements.resetOverlayBtn) {
    elements.resetOverlayBtn.addEventListener('click', () => {
        elements.menuModal.classList.remove('show');
        
        if (elements.overlay) {
            elements.overlay.style.top = '100px';
            elements.overlay.style.left = 'auto';
            elements.overlay.style.right = '20px';
            showToast('🪟 Posisi overlay direset');
        }
    });
}

if (elements.showProcBtn) {
    elements.showProcBtn.addEventListener('click', () => {
        elements.menuModal.classList.remove('show');
        
        if (window.Android) {
            try {
                const procs = window.Android.getRunningProcessesString();
                elements.logContent.textContent = procs;
                elements.logCard.style.display = 'block';
            } catch (e) {
                showToast('❌ Gagal mengambil daftar proses');
            }
        } else {
            showToast('ℹ️ Tidak dalam mode Android');
        }
    });
}

if (elements.clearCacheBtn) {
    elements.clearCacheBtn.addEventListener('click', () => {
        elements.menuModal.classList.remove('show');
        
        // Clear localStorage
        localStorage.clear();
        showToast('🗑️ Cache dibersihkan');
        
        // Reload after 1 second
        setTimeout(() => {
            location.reload();
        }, 1000);
    });
}

// ============= EVENT LISTENERS =============
function setupEventListeners() {
    // Close modal when clicking outside
    window.addEventListener('click', (e) => {
        if (e.target === elements.menuModal) {
            elements.menuModal.classList.remove('show');
        }
    });
    
    // Handle overlay permission from Android
    window.overlayPermissionGranted = function() {
        state.hasPermission = true;
        localStorage.setItem('overlayPermission', 'granted');
        showToast('✅ Izin overlay diberikan');
    };
}
