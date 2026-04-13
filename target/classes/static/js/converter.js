/**
 * converter.js — PDF conversion to DOCX, PPTX, and batch operations
 */
const Converter = {
    init() {
        this.bindEvents();
    },

    bindEvents() {
        document.getElementById('btn-convert-docx').addEventListener('click', () => this.convertToDocx());
        document.getElementById('btn-convert-pptx').addEventListener('click', () => this.convertToPptx());
        document.getElementById('btn-batch-convert').addEventListener('click', () => this.batchConvert());
    },

    requireFile() {
        const fileId = FileManager.getActiveFileId();
        if (!fileId) {
            Toast.warning('Please upload and select a PDF file first');
            return null;
        }
        return fileId;
    },

    async convertToDocx() {
        const fileId = this.requireFile();
        if (!fileId) return;

        Loading.show('Converting to Word...');
        try {
            const blob = await API.blob(`/api/convert/${fileId}/to-docx`, {
                method: 'POST'
            });

            const file = FileManager.files.find(f => f.fileId === fileId);
            const baseName = file ? file.fileName.replace(/\.pdf$/i, '') : 'document';
            downloadBlob(blob, baseName + '.docx');

            Toast.success('DOCX conversion complete!');
        } catch (error) {
            Toast.error('Conversion failed: ' + error.message);
        }
        Loading.hide();
    },

    async convertToPptx() {
        const fileId = this.requireFile();
        if (!fileId) return;

        Loading.show('Converting to PowerPoint...');
        try {
            const blob = await API.blob(`/api/convert/${fileId}/to-pptx`, {
                method: 'POST'
            });

            const file = FileManager.files.find(f => f.fileId === fileId);
            const baseName = file ? file.fileName.replace(/\.pdf$/i, '') : 'document';
            downloadBlob(blob, baseName + '.pptx');

            Toast.success('PPTX conversion complete!');
        } catch (error) {
            Toast.error('Conversion failed: ' + error.message);
        }
        Loading.hide();
    },

    async batchConvert() {
        const fileIds = FileManager.files.map(f => f.fileId);
        if (fileIds.length === 0) {
            Toast.warning('No files to convert');
            return;
        }

        const format = document.getElementById('batch-format').value;

        Loading.show(`Batch converting ${fileIds.length} files to ${format.toUpperCase()}...`);
        try {
            const blob = await API.blob('/api/batch/convert', {
                method: 'POST',
                body: { fileIds, format }
            });

            downloadBlob(blob, `batch_converted_${format}.zip`);
            Toast.success(`Batch conversion complete: ${fileIds.length} files`);
        } catch (error) {
            Toast.error('Batch conversion failed: ' + error.message);
        }
        Loading.hide();
    }
};
