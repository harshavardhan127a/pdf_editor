/**
 * pdfViewer.js — PDF rendering using PDF.js with navigation and zoom
 */
const PdfViewer = {
    pdfDoc: null,
    currentPage: 1,
    totalPages: 0,
    scale: 1.0,
    currentFileId: null,
    rendering: false,
    pendingPage: null,
    pdfjsLib: null,

    async init() {
        // Load PDF.js from CDN
        this.pdfjsLib = await this.loadPdfJs();
        this.bindEvents();
    },

    async loadPdfJs() {
        // Dynamic import of PDF.js
        const pdfjsLib = await import('https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.7.76/pdf.min.mjs');
        pdfjsLib.GlobalWorkerOptions.workerSrc = 'https://cdnjs.cloudflare.com/ajax/libs/pdf.js/4.7.76/pdf.worker.min.mjs';
        return pdfjsLib;
    },

    bindEvents() {
        document.getElementById('btn-prev-page').addEventListener('click', () => this.prevPage());
        document.getElementById('btn-next-page').addEventListener('click', () => this.nextPage());
        document.getElementById('btn-zoom-in').addEventListener('click', () => this.zoomIn());
        document.getElementById('btn-zoom-out').addEventListener('click', () => this.zoomOut());
        document.getElementById('btn-zoom-fit').addEventListener('click', () => this.zoomFit());

        document.getElementById('input-page-number').addEventListener('change', (e) => {
            const page = parseInt(e.target.value);
            if (page >= 1 && page <= this.totalPages) {
                this.goToPage(page);
            }
        });

        document.getElementById('btn-download').addEventListener('click', () => this.downloadCurrent());
        document.getElementById('btn-extract-text').addEventListener('click', () => this.extractText());
        document.getElementById('btn-search').addEventListener('click', () => this.searchText());

        document.getElementById('search-input').addEventListener('keydown', (e) => {
            if (e.key === 'Enter') this.searchText();
        });

        // Keyboard shortcuts
        document.addEventListener('keydown', (e) => {
            if (e.target.tagName === 'INPUT' || e.target.tagName === 'TEXTAREA') return;
            if (!this.pdfDoc) return;

            switch(e.key) {
                case 'ArrowLeft':
                    e.preventDefault();
                    this.prevPage();
                    break;
                case 'ArrowRight':
                    e.preventDefault();
                    this.nextPage();
                    break;
                case '+':
                case '=':
                    if (e.ctrlKey) { e.preventDefault(); this.zoomIn(); }
                    break;
                case '-':
                    if (e.ctrlKey) { e.preventDefault(); this.zoomOut(); }
                    break;
            }
        });
    },

    async loadPdf(fileId) {
        if (!this.pdfjsLib) {
            await this.init();
        }

        this.currentFileId = fileId;

        try {
            const url = `/api/pdf/${fileId}/view`;
            this.pdfDoc = await this.pdfjsLib.getDocument(url).promise;
            this.totalPages = this.pdfDoc.numPages;
            this.currentPage = 1;
            this.scale = 1.0;

            document.getElementById('total-pages').textContent = this.totalPages;
            document.getElementById('input-page-number').max = this.totalPages;

            // Show viewer tab
            document.getElementById('tab-viewer').style.display = 'flex';

            // Show thumbnails
            document.getElementById('thumbnails-section').style.display = 'block';
            this.renderThumbnails();

            // Render first page
            await this.renderPage(this.currentPage);
            this.updateZoomDisplay();

        } catch (error) {
            Toast.error('Failed to load PDF: ' + error.message);
            console.error('PDF load error:', error);
        }
    },

    async renderPage(pageNum) {
        if (this.rendering) {
            this.pendingPage = pageNum;
            return;
        }

        if (!this.pdfDoc) return;

        this.rendering = true;

        try {
            const page = await this.pdfDoc.getPage(pageNum);
            const viewport = page.getViewport({ scale: this.scale });

            const canvas = document.getElementById('pdf-canvas');
            const context = canvas.getContext('2d');

            canvas.height = viewport.height;
            canvas.width = viewport.width;

            const renderContext = {
                canvasContext: context,
                viewport: viewport
            };

            await page.render(renderContext).promise;

            this.currentPage = pageNum;
            document.getElementById('input-page-number').value = pageNum;
            this.updateActiveThumbnail();

        } catch (error) {
            console.error('Render error:', error);
        }

        this.rendering = false;

        if (this.pendingPage !== null) {
            const pending = this.pendingPage;
            this.pendingPage = null;
            this.renderPage(pending);
        }
    },

    prevPage() {
        if (this.currentPage > 1) {
            this.renderPage(this.currentPage - 1);
        }
    },

    nextPage() {
        if (this.currentPage < this.totalPages) {
            this.renderPage(this.currentPage + 1);
        }
    },

    goToPage(pageNum) {
        if (pageNum >= 1 && pageNum <= this.totalPages) {
            this.renderPage(pageNum);
        }
    },

    zoomIn() {
        this.scale = Math.min(this.scale + 0.25, 4.0);
        this.renderPage(this.currentPage);
        this.updateZoomDisplay();
    },

    zoomOut() {
        this.scale = Math.max(this.scale - 0.25, 0.25);
        this.renderPage(this.currentPage);
        this.updateZoomDisplay();
    },

    async zoomFit() {
        if (!this.pdfDoc) return;
        const page = await this.pdfDoc.getPage(this.currentPage);
        const viewport = page.getViewport({ scale: 1.0 });
        const container = document.getElementById('viewer-container');
        const containerWidth = container.clientWidth - 40;
        this.scale = containerWidth / viewport.width;
        this.renderPage(this.currentPage);
        this.updateZoomDisplay();
    },

    updateZoomDisplay() {
        document.getElementById('zoom-level').textContent = Math.round(this.scale * 100) + '%';
    },

    renderThumbnails() {
        const container = document.getElementById('page-thumbnails');
        container.innerHTML = '';

        for (let i = 1; i <= this.totalPages; i++) {
            const el = document.createElement('div');
            el.className = `thumb-item${i === this.currentPage ? ' active' : ''}`;
            el.dataset.page = i;
            el.innerHTML = `
                <span class="thumb-number">${i}</span>
                <span>Page ${i}</span>
            `;
            el.addEventListener('click', () => this.goToPage(i));
            container.appendChild(el);
        }
    },

    updateActiveThumbnail() {
        document.querySelectorAll('.thumb-item').forEach(el => {
            el.classList.toggle('active', parseInt(el.dataset.page) === this.currentPage);
        });
    },

    async downloadCurrent() {
        if (!this.currentFileId) {
            Toast.warning('No file selected');
            return;
        }

        Loading.show('Preparing download...');
        try {
            const blob = await API.blob(`/api/pdf/${this.currentFileId}/download`);
            const file = FileManager.files.find(f => f.fileId === this.currentFileId);
            downloadBlob(blob, file ? file.fileName : 'document.pdf');
            Toast.success('Download started');
        } catch (error) {
            Toast.error('Download failed: ' + error.message);
        }
        Loading.hide();
    },

    async extractText() {
        if (!this.currentFileId) {
            Toast.warning('No file selected');
            return;
        }

        Loading.show('Extracting text...');
        try {
            const result = await API.json(`/api/pdf/${this.currentFileId}/extract-text`);
            if (result.success) {
                document.getElementById('extracted-text').value = result.data.text;
                document.getElementById('modal-overlay').style.display = 'flex';
            }
        } catch (error) {
            Toast.error('Text extraction failed: ' + error.message);
        }
        Loading.hide();
    },

    async searchText() {
        if (!this.currentFileId) {
            Toast.warning('No file selected');
            return;
        }

        const query = document.getElementById('search-input').value.trim();
        if (!query) {
            Toast.warning('Enter a search term');
            return;
        }

        Loading.show('Searching...');
        try {
            const result = await API.json(`/api/pdf/${this.currentFileId}/search`, {
                method: 'POST',
                body: { query, caseSensitive: false }
            });

            if (result.success) {
                this.showSearchResults(result.data);
            }
        } catch (error) {
            Toast.error('Search failed: ' + error.message);
        }
        Loading.hide();
    },

    showSearchResults(searchResult) {
        const body = document.getElementById('search-results-body');

        if (searchResult.totalMatches === 0) {
            body.innerHTML = `
                <div style="text-align:center; padding: 30px; color: var(--text-muted);">
                    <p style="font-size:1.5rem; margin-bottom: 8px;">🔍</p>
                    <p>No matches found for "<strong>${searchResult.query}</strong>"</p>
                </div>
            `;
        } else {
            body.innerHTML = `
                <p style="margin-bottom: 16px; color: var(--text-secondary);">
                    Found <strong style="color: var(--accent-tertiary);">${searchResult.totalMatches}</strong> 
                    match${searchResult.totalMatches > 1 ? 'es' : ''} across 
                    <strong>${searchResult.matches.length}</strong> page${searchResult.matches.length > 1 ? 's' : ''}
                </p>
                ${searchResult.matches.map(m => `
                    <div class="search-result-item" style="cursor:pointer" data-page="${m.pageNumber}">
                        <div class="search-result-page">Page ${m.pageNumber} — ${m.count} match${m.count > 1 ? 'es' : ''}</div>
                        <div class="search-result-snippet">${m.contextSnippet || ''}</div>
                    </div>
                `).join('')}
            `;

            // Click to navigate to page
            body.querySelectorAll('.search-result-item').forEach(el => {
                el.addEventListener('click', () => {
                    const page = parseInt(el.dataset.page);
                    this.goToPage(page);
                    document.getElementById('search-modal-overlay').style.display = 'none';
                });
            });
        }

        document.getElementById('search-modal-overlay').style.display = 'flex';
    },

    clear() {
        this.pdfDoc = null;
        this.currentPage = 1;
        this.totalPages = 0;
        this.currentFileId = null;
        const canvas = document.getElementById('pdf-canvas');
        const ctx = canvas.getContext('2d');
        ctx.clearRect(0, 0, canvas.width, canvas.height);
        canvas.width = 0;
        canvas.height = 0;
        document.getElementById('thumbnails-section').style.display = 'none';
    }
};
