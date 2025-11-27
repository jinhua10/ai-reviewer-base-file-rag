/**
 * 文档管理组件 / Documents Management Component
 * 负责文档的上传、列表、搜索、过滤等功能
 */


function DocumentsTab() {
    // 获取React hooks（避免重复声明）
    const { useState, useEffect } = React;

    // 使用语言Hook
    const { t, language } = window.LanguageModule.useTranslation();

    // 状态管理
    const [state, setState] = useState({
        documents: [],
        loading: true,
        error: null,
        uploading: false,
        uploadProgress: null,
        filterText: '',
        currentPage: 1,
        pageSize: 20,
        totalPages: 0,
        totalCount: 0,
        sortBy: 'date',
        sortOrder: 'desc',
        showAdvancedSearch: false,
        supportedFileTypes: ['pdf', 'docx', 'doc', 'xlsx', 'xls', 'pptx', 'ppt', 'txt', 'md', 'html', 'xml']
    });

    const [advancedFilters, setAdvancedFilters] = useState({
        search: '',
        searchMode: 'contains',
        fileTypes: [],
        minSize: '',
        maxSize: '',
        indexed: 'all',
        startDate: '',
        endDate: ''
    });

    // 更新状态的辅助函数
    const updateState = (updates) => setState(prev => ({ ...prev, ...updates }));

    // 加载文档列表
    const loadDocuments = async () => {
        updateState({ loading: true, error: null });

        try {
            const filters = state.showAdvancedSearch ? {
                search: advancedFilters.search,
                searchMode: advancedFilters.searchMode,
                fileTypes: advancedFilters.fileTypes.join(','),
                minSize: advancedFilters.minSize ? parseInt(advancedFilters.minSize) * 1024 * 1024 : 0,
                maxSize: advancedFilters.maxSize ? parseInt(advancedFilters.maxSize) * 1024 * 1024 : 1099511627776,
                indexed: advancedFilters.indexed,
                startDate: advancedFilters.startDate,
                endDate: advancedFilters.endDate
            } : {
                search: state.filterText || '',
                searchMode: 'contains'
            };

            const result = await window.api.listDocuments(
                state.currentPage,
                state.pageSize,
                state.sortBy,
                state.sortOrder,
                filters
            );

            if (result.success) {
                updateState({
                    documents: result.documents || [],
                    totalCount: result.total || 0,
                    totalPages: result.totalPages || 0,
                    loading: false
                });
            } else {
                updateState({
                    error: result.message || t('docsGetListError'),
                    loading: false
                });
            }
        } catch (err) {
            updateState({
                error: err.response?.data?.message || err.message || t('docsLoadError'),
                loading: false
            });
        }
    };

    // 文件上传处理
    const handleFileSelect = async (event) => {
        const files = event.target.files;
        if (!files || files.length === 0) return;

        updateState({
            uploading: true,
            uploadProgress: { total: files.length, current: 0, success: 0, failed: 0 }
        });

        for (let i = 0; i < files.length; i++) {
            const file = files[i];
            try {
                await window.api.uploadDocument(file);
                updateState({
                    uploadProgress: {
                        ...state.uploadProgress,
                        current: i + 1,
                        success: state.uploadProgress.success + 1
                    }
                });
            } catch (err) {
                updateState({
                    uploadProgress: {
                        ...state.uploadProgress,
                        current: i + 1,
                        failed: state.uploadProgress.failed + 1
                    }
                });
            }
        }

        updateState({ uploading: false });
        setTimeout(() => {
            updateState({ uploadProgress: null });
            loadDocuments();
        }, 2000);
    };

    // 删除文档
    const handleDelete = async (docId) => {
        if (!confirm(t('docsDeleteConfirm'))) return;

        try {
            await window.api.deleteDocument(docId);
            loadDocuments();
        } catch (err) {
            alert(t('docsDeleteError') + ': ' + (err.message || 'Unknown error'));
        }
    };

    // 索引文档
    const handleIndex = async (docId) => {
        try {
            await window.api.indexDocument(docId);
            alert(t('docsIndexSuccess'));
            loadDocuments();
        } catch (err) {
            alert(t('docsIndexError') + ': ' + (err.message || 'Unknown error'));
        }
    };

    // 批量索引
    const handleBatchIndex = async () => {
        if (!confirm(t('docsBatchIndexConfirm'))) return;

        try {
            await window.api.batchIndexDocuments();
            alert(t('docsBatchIndexSuccess'));
            loadDocuments();
        } catch (err) {
            alert(t('docsBatchIndexError') + ': ' + (err.message || 'Unknown error'));
        }
    };

    // 更新过滤器
    const updateFilter = (key, value) => {
        setAdvancedFilters(prev => ({ ...prev, [key]: value }));
    };

    // 切换文件类型
    const toggleFileType = (type, selected) => {
        setAdvancedFilters(prev => ({
            ...prev,
            fileTypes: selected
                ? [...prev.fileTypes, type]
                : prev.fileTypes.filter(t => t !== type)
        }));
    };

    // 生命周期
    useEffect(() => {
        loadDocuments();
    }, []);

    useEffect(() => {
        if (!state.loading) {
            loadDocuments();
        }
    }, [state.currentPage, state.pageSize, state.sortBy, state.sortOrder, state.filterText, state.showAdvancedSearch]);

    // 渲染工具栏
    const renderToolbar = () => {
        return React.createElement('div', { className: 'toolbar', style: { marginBottom: '20px' } },
            // 上传按钮
            React.createElement('input', {
                type: 'file',
                id: 'fileInput',
                multiple: true,
                style: { display: 'none' },
                onChange: handleFileSelect
            }),
            React.createElement('button', {
                className: 'btn-primary',
                onClick: () => document.getElementById('fileInput').click(),
                disabled: state.uploading
            }, state.uploading ? t('docsUploading') : t('docsUploadButton')),

            // 批量索引按钮
            React.createElement('button', {
                className: 'btn-secondary',
                onClick: handleBatchIndex,
                style: { marginLeft: '10px' }
            }, t('docsBatchIndex')),

            // 高级搜索切换
            React.createElement('button', {
                className: 'btn-secondary',
                onClick: () => updateState({ showAdvancedSearch: !state.showAdvancedSearch }),
                style: { marginLeft: '10px' }
            }, state.showAdvancedSearch ? t('docsHideAdvanced') : t('docsShowAdvanced'))
        );
    };

    // 渲染搜索区域
    const renderSearchArea = () => {
        if (!state.showAdvancedSearch) {
            return React.createElement('div', { style: { marginBottom: '15px' } },
                React.createElement('input', {
                    type: 'text',
                    className: 'input-field',
                    placeholder: t('docsSearchPlaceholder'),
                    value: state.filterText,
                    onChange: (e) => updateState({ filterText: e.target.value })
                })
            );
        }

        return DocumentsTabComponents.renderAdvancedSearch(
            advancedFilters,
            state.supportedFileTypes,
            updateFilter,
            toggleFileType,
            language,
            t
        );
    };

    // 渲染���档列表
    const renderDocumentList = () => {
        if (state.loading) {
            return React.createElement('div', { className: 'loading' },
                React.createElement('div', { className: 'spinner' }),
                React.createElement('p', null, t('docsLoading'))
            );
        }

        if (state.error) {
            return React.createElement('div', { className: 'error' }, state.error);
        }

        if (state.documents.length === 0) {
            return React.createElement('div', { className: 'empty-state' },
                React.createElement('div', { className: 'empty-state-icon' }, '📄'),
                React.createElement('p', null, t('docsEmpty'))
            );
        }

        return React.createElement('div', { className: 'documents-list' },
            ...state.documents.map(doc =>
                DocumentsTabComponents.renderDocumentCard(doc, handleDelete, handleIndex, t)
            )
        );
    };

    // 渲染分页
    const renderPagination = () => {
        return DocumentsTabComponents.renderPagination(
            state.currentPage,
            state.totalPages,
            state.pageSize,
            (page) => updateState({ currentPage: page }),
            (size) => updateState({ pageSize: size }),
            t
        );
    };

    // 主渲染
    return React.createElement('div', { className: 'documents-tab' },
        renderToolbar(),
        state.uploadProgress && DocumentsTabComponents.renderUploadProgress(state.uploadProgress, t),
        renderSearchArea(),
        renderDocumentList(),
        renderPagination()
    );
}

// 导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = DocumentsTab;
} else {
    window.DocumentsTab = DocumentsTab;
}

