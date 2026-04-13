/**
 * fileManager.js — File upload, listing, and management
 */
const FileManager = {
    files: [],
    activeFileId: null,

    init() {
        this.bindEvents();
    },

    bindEvents() {
        const uploadZone = document.getElementById('upload-zone');
        const fileInput = document.getElementById('file-input');
        const btnUploadWelcome = document.getElementById('btn-upload-welcome');

        // Click to upload
        uploadZone.addEventListener('click', () => fileInput.click());
        btnUploadWelcome.addEventListener('click', () => fileInput.click());

        // File input change
        fileInput.addEventListener('change', (e) => {
            if (e.target.files.length > 0) {
                this.uploadFiles(e.target.files);
                e.target.value = ''; // Reset
            }
        });

        // Drag and drop
        uploadZone.addEventListener('dragover', (e) => {
            e.preventDefault();
            uploadZone.classList.add('drag-over');
        });

        uploadZone.addEventListener('dragleave', () => {
            uploadZone.classList.remove('drag-over');
        });

        uploadZone.addEventListener('drop', (e) => {
            e.preventDefault();
            uploadZone.classList.remove('drag-over');
            const files = Array.from(e.dataTransfer.files).filter(f => f.type === 'application/pdf');
            if (files.length > 0) {
                this.uploadFiles(files);
            } else {
                Toast.warning('Please drop PDF files only');
            }
        });

        // Global drag-and-drop on body
        document.body.addEventListener('dragover', (e) => e.preventDefault());
        document.body.addEventListener('drop', (e) => e.preventDefault());
    },

    async uploadFiles(fileList) {
        const files = Array.from(fileList);

        for (const file of files) {
            if (!file.name.toLowerCase().endsWith('.pdf')) {
                Toast.warning(`Skipped "${file.name}" — not a PDF`);
                continue;
            }

            Loading.show(`Uploading ${file.name}...`);

            try {
                const formData = new FormData();
                formData.append('file', file);

                const result = await API.json('/api/pdf/upload', {
                    method: 'POST',
                    body: formData
                });

                if (result.success) {
                    const fileRecord = {
                        fileId: result.data.fileId,
                        fileName: result.data.fileName,
                        fileSize: file.size
                    };
                    this.files.push(fileRecord);
                    this.renderFileList();
                    Toast.success(`Uploaded: ${file.name}`);

                    // Auto-select first uploaded file
                    if (this.files.length === 1) {
                        this.selectFile(fileRecord.fileId);
                    }
                }
            } catch (error) {
                Toast.error(`Upload failed: ${error.message}`);
            }
        }

        Loading.hide();
    },

    renderFileList() {
        const container = document.getElementById('file-list');
        container.innerHTML = '';

        if (this.files.length === 0) {
            return;
        }

        this.files.forEach(file => {
            const el = document.createElement('div');
            el.className = `file-item${file.fileId === this.activeFileId ? ' active' : ''}`;
            el.dataset.fileId = file.fileId;
            el.innerHTML = `
                <div class="file-item-icon">PDF</div>
                <div class="file-item-info">
                    <div class="file-item-name" title="${file.fileName}">${file.fileName}</div>
                    <div class="file-item-size">${formatFileSize(file.fileSize)}</div>
                </div>
                <div class="file-item-actions">
                    <button class="file-item-btn" data-action="delete" title="Delete">
                        <svg viewBox="0 0 20 20" fill="currentColor"><path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9z" clip-rule="evenodd"/></svg>
                    </button>
                </div>
            `;

            // Click to select
            el.addEventListener('click', (e) => {
                if (e.target.closest('[data-action="delete"]')) {
                    this.deleteFile(file.fileId);
                    return;
                }
                this.selectFile(file.fileId);
            });

            container.appendChild(el);
        });

        // Update merge file list in tools tab
        this.updateMergeList();
    },

    selectFile(fileId) {
        this.activeFileId = fileId;
        this.renderFileList();

        // Hide welcome, show viewer
        document.getElementById('welcome-screen').style.display = 'none';

        // Switch to viewer tab
        App.switchTab('viewer');

        // Load PDF in viewer
        PdfViewer.loadPdf(fileId);
    },

    async deleteFile(fileId) {
        try {
            await API.json(`/api/files/${fileId}`, { method: 'DELETE' });
            this.files = this.files.filter(f => f.fileId !== fileId);

            if (this.activeFileId === fileId) {
                this.activeFileId = null;
                PdfViewer.clear();
                if (this.files.length > 0) {
                    this.selectFile(this.files[0].fileId);
                } else {
                    document.getElementById('welcome-screen').style.display = 'flex';
                    document.getElementById('tab-viewer').style.display = 'none';
                }
            }

            this.renderFileList();
            Toast.success('File deleted');
        } catch (error) {
            Toast.error(`Delete failed: ${error.message}`);
        }
    },

    updateMergeList() {
        const container = document.getElementById('merge-file-list');
        container.innerHTML = '';

        this.files.forEach(file => {
            const el = document.createElement('div');
            el.className = 'merge-file-item';
            el.innerHTML = `
                <input type="checkbox" id="merge-${file.fileId}" value="${file.fileId}" checked>
                <label for="merge-${file.fileId}">${file.fileName}</label>
            `;
            container.appendChild(el);
        });
    },

    getSelectedMergeFileIds() {
        const checkboxes = document.querySelectorAll('#merge-file-list input[type="checkbox"]:checked');
        return Array.from(checkboxes).map(cb => cb.value);
    },

    getActiveFileId() {
        return this.activeFileId;
    },

    refreshFiles() {
        // Re-fetch from server
        API.json('/api/files').then(result => {
            if (result.success && result.data) {
                this.files = result.data.map(f => ({
                    fileId: f.fileId,
                    fileName: f.fileName,
                    fileSize: f.fileSize
                }));
                this.renderFileList();
            }
        }).catch(() => {});
    }
};
