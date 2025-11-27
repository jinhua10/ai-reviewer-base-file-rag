/**
 * Documents Tab Component / 文档管理标签页组件
 * JSX 版本 - 使用 Babel 转译
 *
 * @author AI Reviewer Team
 * @since 2025-11-28
 */

function DocumentsTab() {
    const { useState, useEffect, useRef } = React;
    const { t, language } = window.LanguageModule.useTranslation();

    // ============================================================================
    // 状态管理
    // ============================================================================
    const [documents, setDocuments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(null);
    const [filterText, setFilterText] = useState('');

    // 分页状态
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [totalPages, setTotalPages] = useState(0);
    const [totalCount, setTotalCount] = useState(0);

    // 排序状态
    const [sortBy, setSortBy] = useState('date');
    const [sortOrder, setSortOrder] = useState('desc');

    // 滚动位置管理
    const scrollContainerRef = useRef(null);
    const savedScrollPosition = useRef(0);

    // 高级搜索状态
    const [showAdvancedSearch, setShowAdvancedSearch] = useState(false);
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

    // 支持的文件类型
    const [supportedFileTypes, setSupportedFileTypes] = useState([
        'pdf', 'docx', 'doc', 'xlsx', 'xls', 'pptx', 'ppt', 'txt', 'md', 'html', 'xml'
    ]);

    // ============================================================================
    // 副作用 / Effects
    // ============================================================================

    // 加载支持的文件类型
    useEffect(() => {
        const loadSupportedFileTypes = async () => {
            try {
                const response = await fetch('/api/documents/supported-types');
                if (response.ok) {
                    const types = await response.json();
                    if (Array.isArray(types) && types.length > 0) {
                        setSupportedFileTypes(types);
                    }
                }
            } catch (err) {
                console.log('使用默认文件类型列表');
            }
        };
        loadSupportedFileTypes();
    }, []);

    // 首次加载文档
    useEffect(() => {
        loadDocuments();
    }, []);

    // 参数变化时重新加载（注意：filterText 不在这里，改为按回车触发）
    useEffect(() => {
        if (!loading) {
            // 排序、分页等操作保持滚动位置
            loadDocuments(true);
        }
    }, [currentPage, pageSize, sortBy, sortOrder, showAdvancedSearch]);

    // ============================================================================
    // 核心功能函数
    // ============================================================================

    const loadDocuments = async (preserveScroll = false) => {
        // 保存当前滚动位置
        if (preserveScroll && scrollContainerRef.current) {
            savedScrollPosition.current = scrollContainerRef.current.scrollTop;
        }

        setLoading(true);
        setError(null);

        try {
            const filters = showAdvancedSearch ? {
                search: advancedFilters.search,
                searchMode: advancedFilters.searchMode,
                fileTypes: advancedFilters.fileTypes.join(','),
                minSize: advancedFilters.minSize ? parseInt(advancedFilters.minSize) * 1024 * 1024 : 0,
                maxSize: advancedFilters.maxSize ? parseInt(advancedFilters.maxSize) * 1024 * 1024 : 1099511627776,
                indexed: advancedFilters.indexed,
                startDate: advancedFilters.startDate,
                endDate: advancedFilters.endDate
            } : {
                search: filterText || '',
                searchMode: 'contains'
            };

            const result = await window.api.listDocuments(currentPage, pageSize, sortBy, sortOrder, filters);

            if (result.success) {
                setDocuments(result.documents || []);
                setTotalCount(result.total || 0);
                setTotalPages(result.totalPages || 0);
            } else {
                setError(result.message || t('docsGetListError'));
            }
        } catch (err) {
            setError(err.response?.data?.message || err.message || t('docsLoadError'));
        } finally {
            setLoading(false);

            // 恢复滚动位置
            if (preserveScroll && scrollContainerRef.current) {
                // 使用 setTimeout 确保 DOM 已更新
                setTimeout(() => {
                    if (scrollContainerRef.current) {
                        scrollContainerRef.current.scrollTop = savedScrollPosition.current;
                    }
                }, 0);
            }
        }
    };

    const handleFileSelect = async (event) => {
        const files = event.target.files;
        if (!files || files.length === 0) return;

        setUploading(true);
        setUploadProgress({ total: files.length, current: 0, success: 0, failed: 0 });

        for (let i = 0; i < files.length; i++) {
            try {
                await window.api.uploadDocument(files[i]);
                setUploadProgress(prev => ({ ...prev, current: i + 1, success: prev.success + 1 }));
            } catch (err) {
                setUploadProgress(prev => ({ ...prev, current: i + 1, failed: prev.failed + 1 }));
            }
        }

        setUploading(false);
        // 延长显示时间到8秒，让用户看到索引建议
        setTimeout(() => setUploadProgress(null), 8000);
        setCurrentPage(1);
        loadDocuments();
        event.target.value = '';
    };

    const handleDelete = async (fileName) => {
        if (!confirm(t('docsDeleteConfirm'))) return;

        try {
            const result = await window.api.deleteDocument(fileName);
            if (result.success) {
                alert(t('docsDeleteSuccess'));
                loadDocuments();
            } else {
                alert(t('docsDeleteError') + ': ' + result.message);
            }
        } catch (err) {
            alert(t('docsDeleteError') + ': ' + err.message);
        }
    };

    const formatFileSize = (bytes) => {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
        return (bytes / 1024 / 1024).toFixed(2) + ' MB';
    };

    const goToPage = (page) => {
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        setCurrentPage(page);
    };

    const handleSearchChange = (value) => {
        setFilterText(value);
        // 不再立即重置页码，等待用户按回车
    };

    const handleSearchSubmit = () => {
        // 按回车时才触发搜索，保持滚动位置
        setCurrentPage(1);
        loadDocuments(true);
    };

    const handleSearchKeyPress = (e) => {
        if (e.key === 'Enter') {
            handleSearchSubmit();
        }
    };

    const handleSortChange = (field, order) => {
        if (field) setSortBy(field);
        if (order) setSortOrder(order);
        // 不重置页码，保持用户当前位置
        // setCurrentPage(1);
    };

    const handlePageSizeChange = (size) => {
        setPageSize(size);
        setCurrentPage(1);
    };

    const updateFilter = (key, value) => {
        setAdvancedFilters(prev => ({ ...prev, [key]: value }));
    };

    const toggleFileType = (type, checked) => {
        setAdvancedFilters(prev => ({
            ...prev,
            fileTypes: checked ? [...prev.fileTypes, type] : prev.fileTypes.filter(t => t !== type)
        }));
    };

    const applyFilters = () => {
        setCurrentPage(1);
        loadDocuments(true);
    };

    const resetFilters = () => {
        setAdvancedFilters({
            search: '',
            searchMode: 'contains',
            fileTypes: [],
            minSize: '',
            maxSize: '',
            indexed: 'all',
            startDate: '',
            endDate: ''
        });
        setCurrentPage(1);
        // 延迟加载以确保状态更新完成
        setTimeout(() => loadDocuments(true), 0);
    };

    const hasActiveFilters = () => {
        return advancedFilters.search !== '' ||
               advancedFilters.fileTypes.length > 0 ||
               advancedFilters.minSize !== '' ||
               advancedFilters.maxSize !== '' ||
               advancedFilters.indexed !== 'all' ||
               advancedFilters.startDate !== '' ||
               advancedFilters.endDate !== '';
    };

    const getActiveFilterCount = () => {
        let count = 0;
        if (advancedFilters.search) count++;
        if (advancedFilters.fileTypes.length > 0) count++;
        if (advancedFilters.minSize || advancedFilters.maxSize) count++;
        if (advancedFilters.indexed !== 'all') count++;
        if (advancedFilters.startDate || advancedFilters.endDate) count++;
        return count;
    };

    // ============================================================================
    // 子组件引用
    // ============================================================================
    const {
        UploadArea,
        DocumentListHeader,
        SearchFilters,
        DocumentList,
        Pagination
    } = window.DocumentsTabComponents;

    // ============================================================================
    // 主渲染
    // ============================================================================
    return (
        <div>
            {/* 上传区域 */}
            <UploadArea
                uploading={uploading}
                uploadProgress={uploadProgress}
                handleFileSelect={handleFileSelect}
                t={t}
            />

            {/* 文档列表区域 */}
            <div>
                {/* 列表头部 */}
                <DocumentListHeader
                    loading={loading}
                    totalCount={totalCount}
                    documentsLength={documents.length}
                    loadDocuments={loadDocuments}
                    t={t}
                />

                {/* 搜索和筛选区域 */}
                {!loading && (
                    <div className="documents-search-container">
                        <SearchFilters
                            showAdvancedSearch={showAdvancedSearch}
                            setShowAdvancedSearch={setShowAdvancedSearch}
                            filterText={filterText}
                            handleSearchChange={handleSearchChange}
                            handleSearchKeyPress={handleSearchKeyPress}
                            handleSearchSubmit={handleSearchSubmit}
                            advancedFilters={advancedFilters}
                            updateFilter={updateFilter}
                            toggleFileType={toggleFileType}
                            supportedFileTypes={supportedFileTypes}
                            applyFilters={applyFilters}
                            resetFilters={resetFilters}
                            hasActiveFilters={hasActiveFilters}
                            getActiveFilterCount={getActiveFilterCount}
                            language={language}
                            t={t}
                        />

                        {/* 排序和分页控制栏 - 只在有文档时显示 */}
                        {totalCount > 0 && (
                            <div className="documents-controls-bar">
                                {/* 排序方式 */}
                                <div className="documents-control-group">
                                    <label className="documents-control-label">{t('docsSortBy')}:</label>
                                    <select
                                        className="input-field documents-control-select"
                                        value={sortBy}
                                        onChange={(e) => handleSortChange(e.target.value, null)}
                                    >
                                        <option value="date">{t('docsSortByDate')}</option>
                                        <option value="name">{t('docsSortByName')}</option>
                                        <option value="size">{t('docsSortBySize')}</option>
                                        <option value="type">{t('docsSortByType')}</option>
                                    </select>
                                    <select
                                        className="input-field documents-control-select"
                                        value={sortOrder}
                                        onChange={(e) => handleSortChange(null, e.target.value)}
                                    >
                                        <option value="desc">{t('docsSortDesc')}</option>
                                        <option value="asc">{t('docsSortAsc')}</option>
                                    </select>
                                </div>

                                {/* 每页显示数量 */}
                                <div className="documents-control-group">
                                    <label className="documents-control-label">{t('docsPageSize')}:</label>
                                    <select
                                        className="input-field documents-control-select"
                                        value={pageSize}
                                        onChange={(e) => handlePageSizeChange(Number(e.target.value))}
                                    >
                                        <option value={10}>10 {t('docsPageSizeItems')}</option>
                                        <option value={20}>20 {t('docsPageSizeItems')}</option>
                                        <option value={50}>50 {t('docsPageSizeItems')}</option>
                                        <option value={100}>100 {t('docsPageSizeItems')}</option>
                                        <option value={-1}>{t('docsShowAll')}</option>
                                    </select>
                                </div>

                                {/* 统计信息 */}
                                <div className="documents-stats">
                                    {filterText ? (
                                        <>
                                            {t('docsFilterResult')} {totalCount} {t('logDocsCount')}
                                            <button
                                                className="documents-stats-clear-btn"
                                                onClick={() => handleSearchChange('')}
                                            >
                                                {t('docsFilterClear')}
                                            </button>
                                        </>
                                    ) : (
                                        `${t('docsPaginationTotal')} ${totalCount} ${t('logDocsCount')}`
                                    )}
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {/* 加载状态 */}
                {loading && (
                    <div className="loading">
                        <div className="spinner"></div>
                        <p>{t('docsLoadingList')}</p>
                    </div>
                )}

                {/* 错误状态 */}
                {error && !loading && (
                    <div className="error">
                        {t('qaErrorPrefix')} {error}
                        <button
                            className="btn btn-secondary"
                            onClick={loadDocuments}
                            style={{ marginTop: '10px', display: 'block' }}
                        >
                            {t('docsRetry')}
                        </button>
                    </div>
                )}

                {/* 空状态 */}
                {!error && !loading && documents.length === 0 && (
                    <div className="empty-state">
                        <div className="empty-state-icon">📁</div>
                        <p>{t('docsListEmpty')}</p>
                        <p style={{ fontSize: '14px', marginTop: '10px', color: '#ccc' }}>
                            {t('docsEmptyHint')}
                        </p>
                    </div>
                )}

                {/* 文档列表 */}
                {!error && !loading && documents.length > 0 && (
                    <>
                        <DocumentList
                            documents={documents}
                            formatFileSize={formatFileSize}
                            handleDelete={handleDelete}
                            scrollContainerRef={scrollContainerRef}
                            t={t}
                        />

                        {/* 分页控制 */}
                        {pageSize !== -1 && totalPages > 1 && (
                            <Pagination
                                currentPage={currentPage}
                                totalPages={totalPages}
                                goToPage={goToPage}
                                t={t}
                            />
                        )}
                    </>
                )}

                {/* 提示信息 */}
                {!error && !loading && totalCount > 0 && (
                    <div className="documents-tip">
                        {t('docsUploadTip')}
                    </div>
                )}
            </div>
        </div>
    );
}

// 导出到全局
if (typeof window !== 'undefined') {
    window.DocumentsTab = DocumentsTab;
}

