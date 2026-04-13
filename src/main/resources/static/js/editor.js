/**
 * editor.js — PDF editing: text/image insertion, page operations
 */
const Editor = {
    init() {
        this.bindEvents();
    },

    bindEvents() {
        document.getElementById('btn-insert-text').addEventListener('click', () => this.insertText());
        document.getElementById('btn-insert-image').addEventListener('click', () => this.insertImage());
        document.getElementById('btn-delete-pages').addEventListener('click', () => this.deletePages());
        document.getElementById('btn-add-blank-page').addEventListener('click', () => this.addBlankPage());
        document.getElementById('btn-reorder-pages').addEventListener('click', () => this.reorderPages());
    },

    requireFile() {
        const fileId = FileManager.getActiveFileId();
        if (!fileId) {
            Toast.warning('Please upload and select a PDF file first');
            return null;
        }
        return fileId;
    },

    async insertText() {
        const fileId = this.requireFile();
        if (!fileId) return;

        const text = document.getElementById('edit-text-content').value.trim();
        if (!text) {
            Toast.warning('Please enter text to insert');
            return;
        }

        const request = {
            pageNumber: parseInt(document.getElementById('edit-text-page').value) || 0,
            text: text,
            x: parseFloat(document.getElementById('edit-text-x').value) || 100,
            y: parseFloat(document.getElementById('edit-text-y').value) || 700,
            fontSize: parseFloat(document.getElementById('edit-text-size').value) || 12,
            fontName: document.getElementById('edit-text-font').value,
            color: document.getElementById('edit-text-color').value
        };

        Loading.show('Inserting text...');
        try {
            const result = await API.json(`/api/edit/${fileId}/text`, {
                method: 'POST',
                body: request
            });

            if (result.success) {
                Toast.success('Text inserted successfully');
                this.handleNewFile(result.data.fileId, 'text_added.pdf');
            }
        } catch (error) {
            Toast.error('Text insertion failed: ' + error.message);
        }
        Loading.hide();
    },

    async insertImage() {
        const fileId = this.requireFile();
        if (!fileId) return;

        const imageInput = document.getElementById('edit-image-file');
        if (!imageInput.files.length) {
            Toast.warning('Please select an image file');
            return;
        }

        const formData = new FormData();
        formData.append('image', imageInput.files[0]);
        formData.append('pageNumber', document.getElementById('edit-image-page').value || '0');
        formData.append('x', document.getElementById('edit-image-x').value || '100');
        formData.append('y', document.getElementById('edit-image-y').value || '500');
        formData.append('width', document.getElementById('edit-image-width').value || '200');
        formData.append('height', document.getElementById('edit-image-height').value || '200');

        Loading.show('Inserting image...');
        try {
            const result = await API.json(`/api/edit/${fileId}/image`, {
                method: 'POST',
                body: formData
            });

            if (result.success) {
                Toast.success('Image inserted successfully');
                this.handleNewFile(result.data.fileId, 'image_added.pdf');
            }
        } catch (error) {
            Toast.error('Image insertion failed: ' + error.message);
        }
        Loading.hide();
    },

    async deletePages() {
        const fileId = this.requireFile();
        if (!fileId) return;

        const input = document.getElementById('edit-delete-pages').value.trim();
        if (!input) {
            Toast.warning('Enter page numbers to delete');
            return;
        }

        const pageNumbers = input.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n));
        if (pageNumbers.length === 0) {
            Toast.warning('Invalid page numbers');
            return;
        }

        Loading.show('Deleting pages...');
        try {
            const result = await API.json(`/api/pdf/${fileId}/delete-pages`, {
                method: 'POST',
                body: { pageNumbers }
            });

            if (result.success) {
                Toast.success('Pages deleted');
                this.handleNewFile(result.data.fileId, 'pages_deleted.pdf');
            }
        } catch (error) {
            Toast.error('Page deletion failed: ' + error.message);
        }
        Loading.hide();
    },

    async addBlankPage() {
        const fileId = this.requireFile();
        if (!fileId) return;

        const afterPage = parseInt(document.getElementById('edit-add-page-after').value);

        Loading.show('Adding page...');
        try {
            const result = await API.json(`/api/pdf/${fileId}/add-blank-page?afterPage=${afterPage}`, {
                method: 'POST'
            });

            if (result.success) {
                Toast.success('Blank page added');
                this.handleNewFile(result.data.fileId, 'page_added.pdf');
            }
        } catch (error) {
            Toast.error('Failed to add page: ' + error.message);
        }
        Loading.hide();
    },

    async reorderPages() {
        const fileId = this.requireFile();
        if (!fileId) return;

        const input = document.getElementById('edit-reorder-pages').value.trim();
        if (!input) {
            Toast.warning('Enter new page order');
            return;
        }

        const pageOrder = input.split(',').map(s => parseInt(s.trim())).filter(n => !isNaN(n));
        if (pageOrder.length === 0) {
            Toast.warning('Invalid page order');
            return;
        }

        Loading.show('Reordering pages...');
        try {
            const result = await API.json(`/api/pdf/${fileId}/reorder`, {
                method: 'POST',
                body: { pageOrder }
            });

            if (result.success) {
                Toast.success('Pages reordered');
                this.handleNewFile(result.data.fileId, 'reordered.pdf');
            }
        } catch (error) {
            Toast.error('Reorder failed: ' + error.message);
        }
        Loading.hide();
    },

    handleNewFile(newFileId, fileName) {
        // Add to file list and select it
        FileManager.files.push({
            fileId: newFileId,
            fileName: fileName,
            fileSize: 0
        });
        FileManager.selectFile(newFileId);
    }
};
