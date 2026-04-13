/**
 * utils.js — Shared utility functions and API client
 */
const API = {
    baseUrl: '',

    async request(url, options = {}) {
        const defaultOptions = {
            headers: {},
        };

        if (options.body && !(options.body instanceof FormData)) {
            defaultOptions.headers['Content-Type'] = 'application/json';
            options.body = JSON.stringify(options.body);
        }

        const mergedOptions = {
            ...defaultOptions,
            ...options,
            headers: { ...defaultOptions.headers, ...options.headers }
        };

        try {
            const response = await fetch(this.baseUrl + url, mergedOptions);

            if (!response.ok) {
                let errorMsg;
                try {
                    const errData = await response.json();
                    errorMsg = errData.message || `HTTP ${response.status}`;
                } catch {
                    errorMsg = `HTTP ${response.status}: ${response.statusText}`;
                }
                throw new Error(errorMsg);
            }

            return response;
        } catch (error) {
            if (error.message.includes('Failed to fetch')) {
                throw new Error('Network error: Could not connect to server');
            }
            throw error;
        }
    },

    async json(url, options = {}) {
        const response = await this.request(url, options);
        return response.json();
    },

    async blob(url, options = {}) {
        const response = await this.request(url, options);
        return response.blob();
    }
};

// ===== Toast Notifications =====
const Toast = {
    container: null,

    init() {
        this.container = document.getElementById('toast-container');
    },

    show(message, type = 'info', duration = 4000) {
        if (!this.container) this.init();

        const icons = {
            success: '✅',
            error: '❌',
            warning: '⚠️',
            info: 'ℹ️'
        };

        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `
            <span class="toast-icon">${icons[type] || icons.info}</span>
            <span>${message}</span>
        `;

        this.container.appendChild(toast);

        setTimeout(() => {
            toast.classList.add('toast-exit');
            setTimeout(() => toast.remove(), 200);
        }, duration);
    },

    success(msg) { this.show(msg, 'success'); },
    error(msg) { this.show(msg, 'error', 6000); },
    warning(msg) { this.show(msg, 'warning'); },
    info(msg) { this.show(msg, 'info'); }
};

// ===== Loading Overlay =====
const Loading = {
    overlay: null,
    messageEl: null,

    init() {
        this.overlay = document.getElementById('loading-overlay');
        this.messageEl = document.getElementById('loading-message');
    },

    show(message = 'Processing...') {
        if (!this.overlay) this.init();
        this.messageEl.textContent = message;
        this.overlay.style.display = 'flex';
    },

    hide() {
        if (!this.overlay) this.init();
        this.overlay.style.display = 'none';
    }
};

// ===== Utility Functions =====
function formatFileSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const units = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(1024));
    return (bytes / Math.pow(1024, i)).toFixed(i > 0 ? 1 : 0) + ' ' + units[i];
}

function formatDate(dateStr) {
    return new Date(dateStr).toLocaleString();
}

function debounce(fn, delay = 300) {
    let timer;
    return (...args) => {
        clearTimeout(timer);
        timer = setTimeout(() => fn(...args), delay);
    };
}

function downloadBlob(blob, filename) {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    document.body.removeChild(a);
    URL.revokeObjectURL(url);
}
