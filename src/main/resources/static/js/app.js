/**
 * app.js — Main application controller
 */
const App = {
    currentTab: 'viewer',

    init() {
        this.bindNavigation();
        this.bindModals();

        // Initialize modules
        FileManager.init();
        PdfViewer.init();
        Editor.init();
        Converter.init();
        Toast.init();

        // Bind tool-specific buttons
        this.bindToolButtons();

        console.log('PDF Editor Pro initialized');
    },

    bindNavigation() {
        const navBtns = document.querySelectorAll('.nav-btn');
        navBtns.forEach(btn => {
            btn.addEventListener('click', () => {
                const tab = btn.dataset.tab;
                this.switchTab(tab);
            });
        });
    },

    switchTab(tabName) {
        this.currentTab = tabName;

        // Update nav buttons
        document.querySelectorAll('.nav-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tabName);
        });

        // Hide all tab contents
        document.querySelectorAll('.tab-content').forEach(tab => {
            tab.style.display = 'none';
        });

        // Hide welcome screen if we have a file
        if (FileManager.getActiveFileId()) {
            document.getElementById('welcome-screen').style.display = 'none';
        }

        // Show selected tab
        const tabEl = document.getElementById('tab-' + tabName);
        if (tabEl) {
            tabEl.style.display = tabName === 'viewer' ? 'flex' : 'block';
        }

        // Refresh metadata if tools tab
        if (tabName === 'tools' && FileManager.getActiveFileId()) {
            this.loadMetadata();
        }
    },

    bindModals() {
        // Text extraction modal
        document.getElementById('modal-close').addEventListener('click', () => {
            document.getElementById('modal-overlay').style.display = 'none';
        });
        document.getElementById('btn-close-modal').addEventListener('click', () => {
            document.getElementById('modal-overlay').style.display = 'none';
        });
        document.getElementById('btn-copy-text').addEventListener('click', () => {
            const textArea = document.getElementById('extracted-text');
            textArea.select();
            navigator.clipboard.writeText(textArea.value).then(() => {
                Toast.success('Text copied to clipboard');
            });
        });

        // Search modal
        document.getElementById('search-modal-close').addEventListener('click', () => {
            document.getElementById('search-modal-overlay').style.display = 'none';
        });
        document.getElementById('btn-close-search-modal').addEventListener('click', () => {
            document.getElementById('search-modal-overlay').style.display = 'none';
        });

        // Close modals on overlay click
        document.getElementById('modal-overlay').addEventListener('click', (e) => {
            if (e.target === e.currentTarget) {
                e.currentTarget.style.display = 'none';
            }
        });
        document.getElementById('search-modal-overlay').addEventListener('click', (e) => {
            if (e.target === e.currentTarget) {
                e.currentTarget.style.display = 'none';
            }
        });

        // ESC to close modals
        document.addEventListener('keydown', (e) => {
            if (e.key === 'Escape') {
                document.getElementById('modal-overlay').style.display = 'none';
                document.getElementById('search-modal-overlay').style.display = 'none';
            }
        });
    },

    bindToolButtons() {
        // Merge
        document.getElementById('btn-merge').addEventListener('click', async () => {
            const fileIds = FileManager.getSelectedMergeFileIds();
            if (fileIds.length < 2) {
                Toast.warning('Select at least 2 files to merge');
                return;
            }

            const outputName = document.getElementById('merge-output-name').value || 'merged.pdf';

            Loading.show('Merging PDFs...');
            try {
                const blob = await API.blob('/api/pdf/merge', {
                    method: 'POST',
                    body: { fileIds, outputFileName: outputName }
                });

                downloadBlob(blob, outputName);
                Toast.success(`Merged ${fileIds.length} PDFs`);
            } catch (error) {
                Toast.error('Merge failed: ' + error.message);
            }
            Loading.hide();
        });

        // Split
        document.getElementById('btn-split').addEventListener('click', async () => {
            const fileId = FileManager.getActiveFileId();
            if (!fileId) {
                Toast.warning('Select a PDF to split');
                return;
            }

            const rangeInput = document.getElementById('split-ranges').value.trim();
            let pageRanges = null;

            if (rangeInput) {
                pageRanges = rangeInput.split(',').map(r => {
                    const parts = r.trim().split('-').map(s => parseInt(s.trim()));
                    return parts.length === 2 ? parts : [parts[0], parts[0]];
                });
            }

            Loading.show('Splitting PDF...');
            try {
                const result = await API.json(`/api/pdf/${fileId}/split`, {
                    method: 'POST',
                    body: pageRanges ? { pageRanges } : {}
                });

                if (result.success) {
                    const partIds = result.data;
                    partIds.forEach((id, i) => {
                        FileManager.files.push({
                            fileId: id,
                            fileName: `split_part_${i + 1}.pdf`,
                            fileSize: 0
                        });
                    });
                    FileManager.renderFileList();
                    Toast.success(`PDF split into ${partIds.length} parts`);
                }
            } catch (error) {
                Toast.error('Split failed: ' + error.message);
            }
            Loading.hide();
        });

        // Metadata refresh
        document.getElementById('btn-refresh-metadata').addEventListener('click', () => this.loadMetadata());
    },

    async loadMetadata() {
        const fileId = FileManager.getActiveFileId();
        if (!fileId) return;

        try {
            const result = await API.json(`/api/files/${fileId}/metadata`);
            if (result.success) {
                this.displayMetadata(result.data);
            }
        } catch (error) {
            console.error('Metadata load error:', error);
        }
    },

    displayMetadata(metadata) {
        const container = document.getElementById('metadata-display');
        let html = '';

        const addItem = (key, value) => {
            html += `<div class="metadata-item">
                <span class="metadata-key">${key}</span>
                <span class="metadata-value">${value || '—'}</span>
            </div>`;
        };

        addItem('File Name', metadata.fileName);
        addItem('File Size', metadata.fileSizeFormatted);
        addItem('Pages', metadata.pageCount);
        addItem('Encrypted', metadata.encrypted ? 'Yes' : 'No');

        if (metadata.documentInfo) {
            const info = metadata.documentInfo;
            if (info.title) addItem('Title', info.title);
            if (info.author) addItem('Author', info.author);
            if (info.subject) addItem('Subject', info.subject);
            if (info.creator) addItem('Creator', info.creator);
            if (info.producer) addItem('Producer', info.producer);
            if (info.creationDate) addItem('Created', info.creationDate);
            if (info.modificationDate) addItem('Modified', info.modificationDate);
        }

        if (metadata.pages && metadata.pages.length > 0) {
            const p = metadata.pages[0];
            addItem('Page Size', `${Math.round(p.width)} × ${Math.round(p.height)} pt`);
            if (p.rotation) addItem('Rotation', p.rotation + '°');
        }

        container.innerHTML = html;
    }
};

// ===== Initialize on DOM ready =====
document.addEventListener('DOMContentLoaded', () => App.init());
