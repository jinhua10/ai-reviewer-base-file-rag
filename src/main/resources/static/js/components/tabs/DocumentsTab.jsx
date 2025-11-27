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
    const [documents, setDocuments] = useState([]); // 后端返回的原始文档列表
    const [allDocuments, setAllDocuments] = useState([]); // 用于前端过滤的完整文档列表
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(null);
    const [filterText, setFilterText] = useState('');
    const [localFilterText, setLocalFilterText] = useState(''); // 用于前端实时过滤的文本

    // 分页状态
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [totalPages, setTotalPages] = useState(0);
    const [totalCount, setTotalCount] = useState(0);

    // 排序状态
    const [sortBy, setSortBy] = useState('date');
    const [sortOrder, setSortOrder] = useState('desc');


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

    // 本地高级搜索状态（用于前端实时过滤）
    const [localAdvancedFilters, setLocalAdvancedFilters] = useState({
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
            loadDocuments();
        }
    }, [currentPage, pageSize, sortBy, sortOrder, showAdvancedSearch]);

    // ============================================================================
    // 核心功能函数
    // ============================================================================

    const loadDocuments = async () => {
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
                const docs = result.documents || [];
                setDocuments(docs);
                setAllDocuments(docs); // 保存完整列表用于前端过滤
                setTotalCount(result.total || 0);
                setTotalPages(result.totalPages || 0);
                // 同步本地过滤状态
                setLocalFilterText(filterText);
                setLocalAdvancedFilters({ ...advancedFilters });
            } else {
                setError(result.message || t('docsGetListError'));
            }
        } catch (err) {
            setError(err.response?.data?.message || err.message || t('docsLoadError'));
        } finally {
            setLoading(false);
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
        setLocalFilterText(value); // 同时更新本地过滤文本，触发实时过滤
    };

    const handleSearchSubmit = () => {
        // 按回车或点击搜索按钮时才触发后台搜索
        setCurrentPage(1);
        loadDocuments();
    };

    const handleSearchKeyPress = (e) => {
        if (e.key === 'Enter') {
            handleSearchSubmit();
        }
    };

    // 前端实时过滤文档列表
    const getFilteredDocuments = () => {
        if (!localFilterText || localFilterText === filterText) {
            // 如果没有本地过滤文本，或者本地过滤文本等于后端搜索文本，返回原始列表
            return documents;
        }

        // 前端实时过滤
        const searchLower = localFilterText.toLowerCase();
        return allDocuments.filter(doc =>
            doc.fileName.toLowerCase().includes(searchLower)
        );
    };

    // 高级搜索 - 前端实时过滤（支持所有筛选条件）
    const getAdvancedFilteredDocuments = () => {
        // 检查是否有任何本地过滤条件与后端过滤条件不同
        const hasLocalFilters =
            localAdvancedFilters.search !== advancedFilters.search ||
            localAdvancedFilters.searchMode !== advancedFilters.searchMode ||
            JSON.stringify(localAdvancedFilters.fileTypes) !== JSON.stringify(advancedFilters.fileTypes) ||
            localAdvancedFilters.minSize !== advancedFilters.minSize ||
            localAdvancedFilters.maxSize !== advancedFilters.maxSize ||
            localAdvancedFilters.indexed !== advancedFilters.indexed ||
            localAdvancedFilters.startDate !== advancedFilters.startDate ||
            localAdvancedFilters.endDate !== advancedFilters.endDate;

        // 如果没有本地过滤，返回后端数据
        if (!hasLocalFilters) {
            return documents;
        }

        // 前端实时过滤所有条件
        return allDocuments.filter(doc => {
            // 1. 文件名搜索过滤
            if (localAdvancedFilters.search) {
                const searchLower = localAdvancedFilters.search.toLowerCase();
                let matchSearch = false;

                switch (localAdvancedFilters.searchMode) {
                    case 'exact':
                        matchSearch = doc.fileName.toLowerCase() === searchLower;
                        break;
                    case 'regex':
                        try {
                            const regex = new RegExp(localAdvancedFilters.search, 'i');
                            matchSearch = regex.test(doc.fileName);
                        } catch (e) {
                            matchSearch = doc.fileName.toLowerCase().includes(searchLower);
                        }
                        break;
                    case 'contains':
                    default:
                        matchSearch = doc.fileName.toLowerCase().includes(searchLower);
                }

                if (!matchSearch) return false;
            }

            // 2. 文件类型过滤
            if (localAdvancedFilters.fileTypes.length > 0) {
                if (!localAdvancedFilters.fileTypes.includes(doc.fileType.toLowerCase())) {
                    return false;
                }
            }

            // 3. 文件大小过滤
            if (localAdvancedFilters.minSize) {
                const minBytes = parseInt(localAdvancedFilters.minSize) * 1024 * 1024;
                if (doc.fileSize < minBytes) return false;
            }
            if (localAdvancedFilters.maxSize) {
                const maxBytes = parseInt(localAdvancedFilters.maxSize) * 1024 * 1024;
                if (doc.fileSize > maxBytes) return false;
            }

            // 4. 索引状态过滤
            if (localAdvancedFilters.indexed !== 'all') {
                const isIndexed = localAdvancedFilters.indexed === 'true';
                if (doc.indexed !== isIndexed) return false;
            }

            // 5. 日期范围过滤
            if (localAdvancedFilters.startDate || localAdvancedFilters.endDate) {
                const docDate = new Date(doc.uploadTime);

                if (localAdvancedFilters.startDate) {
                    const startDate = new Date(localAdvancedFilters.startDate);
                    if (docDate < startDate) return false;
                }

                if (localAdvancedFilters.endDate) {
                    const endDate = new Date(localAdvancedFilters.endDate);
                    endDate.setHours(23, 59, 59, 999); // 包含结束日期的整天
                    if (docDate > endDate) return false;
                }
            }

            return true;
        });
    };

    const handleSortChange = (field, order) => {
        if (field) setSortBy(field);
        if (order) setSortOrder(order);
    };

    const handlePageSizeChange = (size) => {
        setPageSize(size);
        setCurrentPage(1);
    };

    const updateFilter = (key, value) => {
        setAdvancedFilters(prev => ({ ...prev, [key]: value }));
        setLocalAdvancedFilters(prev => ({ ...prev, [key]: value })); // 同时更新本地状态，触发实时过滤
    };

    const toggleFileType = (type, checked) => {
        const newFileTypes = checked ?
            [...advancedFilters.fileTypes, type] :
            advancedFilters.fileTypes.filter(t => t !== type);

        setAdvancedFilters(prev => ({
            ...prev,
            fileTypes: newFileTypes
        }));
        setLocalAdvancedFilters(prev => ({
            ...prev,
            fileTypes: newFileTypes
        }));
    };

    const applyFilters = () => {
        setCurrentPage(1);
        loadDocuments(); // 发送后端请求
    };

    const resetFilters = () => {
        const emptyFilters = {
            search: '',
            searchMode: 'contains',
            fileTypes: [],
            minSize: '',
            maxSize: '',
            indexed: 'all',
            startDate: '',
            endDate: ''
        };
        setAdvancedFilters(emptyFilters);
        setLocalAdvancedFilters(emptyFilters);
        setCurrentPage(1);
        // 延迟加载以确保状态更新完成
        setTimeout(() => loadDocuments(), 0);
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

    // 检查是否有本地过滤条件（用于显示实时过滤提示）
    const hasLocalAdvancedFilters = () => {
        return localAdvancedFilters.search !== advancedFilters.search ||
               localAdvancedFilters.searchMode !== advancedFilters.searchMode ||
               JSON.stringify(localAdvancedFilters.fileTypes) !== JSON.stringify(advancedFilters.fileTypes) ||
               localAdvancedFilters.minSize !== advancedFilters.minSize ||
               localAdvancedFilters.maxSize !== advancedFilters.maxSize ||
               localAdvancedFilters.indexed !== advancedFilters.indexed ||
               localAdvancedFilters.startDate !== advancedFilters.startDate ||
               localAdvancedFilters.endDate !== advancedFilters.endDate;
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
                                    {localFilterText || (showAdvancedSearch && hasLocalAdvancedFilters()) ? (
                                        <>
                                            {t('docsFilterResult')} {
                                                (showAdvancedSearch ?
                                                    getAdvancedFilteredDocuments() :
                                                    getFilteredDocuments()
                                                ).length
                                            } / {allDocuments.length} {t('logDocsCount')}
                                            <button
                                                className="documents-stats-clear-btn"
                                                onClick={() => {
                                                    if (showAdvancedSearch) {
                                                        resetFilters();
                                                    } else {
                                                        handleSearchChange('');
                                                    }
                                                }}
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
                            documents={showAdvancedSearch ? getAdvancedFilteredDocuments() : getFilteredDocuments()}
                            formatFileSize={formatFileSize}
                            handleDelete={handleDelete}
                            t={t}
                        />

                        {/* 实时过滤提示 */}
                        {((localFilterText && localFilterText !== filterText) ||
                          (showAdvancedSearch && hasLocalAdvancedFilters())) && (
                            <div style={{
                                marginTop: '10px',
                                padding: '10px',
                                background: '#e3f2fd',
                                borderRadius: '6px',
                                fontSize: '13px',
                                color: '#1976d2',
                                textAlign: 'center'
                            }}>
                                💡 {t('docsLocalFilterHint') || '正在前端实时过滤，点击"应用筛选"按钮进行完整搜索'}
                            </div>
                        )}

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

                {/* 实时过滤后的空状态 */}
                {!error && !loading && documents.length > 0 &&
                 (showAdvancedSearch ? getAdvancedFilteredDocuments() : getFilteredDocuments()).length === 0 && (
                    <div className="empty-state">
                        <div className="empty-state-icon">🔍</div>
                        <p>{t('docsNoMatchFound') || '没有找到匹配的文档'}</p>
                        <p style={{ fontSize: '14px', marginTop: '10px', color: '#ccc' }}>
                            {t('docsTryDifferentKeyword') || '尝试使用不同的关键词或点击搜索按钮进行完整搜索'}
                        </p>
                    </div>
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

