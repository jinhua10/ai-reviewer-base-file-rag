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

    // AI分析面板状态
    const [showAIAnalysis, setShowAIAnalysis] = useState(false);
    const [uploadedFilesCache, setUploadedFilesCache] = useState([]);
    const [selectedDocs, setSelectedDocs] = useState(new Set());
    const [splitPosition, setSplitPosition] = useState(() => {
        const saved = localStorage.getItem('aiAnalysisSplitPosition');
        return saved ? parseFloat(saved) : 50;
    });
    const [isDragging, setIsDragging] = useState(false);

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
                        // 更新文件图标映射
                        if (window.updateFileIconMap) {
                            window.updateFileIconMap(types);
                        }
                    }
                }
            } catch (err) {
                console.log(t('logUseDefaultFileTypes'));
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

    // 处理分隔线拖拽
    useEffect(() => {
        if (!isDragging) return;

        const handleMouseMove = (e) => {
            const newPosition = (e.clientX / window.innerWidth) * 100;
            if (newPosition > 20 && newPosition < 80) {
                setSplitPosition(newPosition);
                localStorage.setItem('aiAnalysisSplitPosition', newPosition.toString());
            }
        };

        const handleMouseUp = () => {
            setIsDragging(false);
        };

        document.addEventListener('mousemove', handleMouseMove);
        document.addEventListener('mouseup', handleMouseUp);

        return () => {
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
        };
    }, [isDragging]);

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

        const successFiles = [];

        for (let i = 0; i < files.length; i++) {
            try {
                await window.api.uploadDocument(files[i]);
                setUploadProgress(prev => ({ ...prev, current: i + 1, success: prev.success + 1 }));
                // 缓存成功上传的文件
                successFiles.push(files[i]);
            } catch (err) {
                setUploadProgress(prev => ({ ...prev, current: i + 1, failed: prev.failed + 1 }));
            }
        }

        // 更新上传文件缓存
        if (successFiles.length > 0) {
            setUploadedFilesCache(prev => [...successFiles, ...prev].slice(0, 50)); // 保留最近50个
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

    // 切换文档选择
    const toggleDocSelection = (fileName) => {
        setSelectedDocs(prev => {
            const newSet = new Set(prev);
            if (newSet.has(fileName)) {
                newSet.delete(fileName);
            } else {
                newSet.add(fileName);
            }
            return newSet;
        });
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
        // 如果没有过滤文本，返回原始列表
        if (!localFilterText && !filterText) {
            return documents;
        }

        // 使用本地过滤文本（如果有），否则使用后端过滤文本
        const searchText = localFilterText || filterText;
        if (!searchText) {
            return documents;
        }

        // 前端实时过滤（在allDocuments或documents中过滤）
        const searchLower = searchText.toLowerCase();
        const sourceList = allDocuments.length > 0 ? allDocuments : documents;
        return sourceList.filter(doc =>
            doc.fileName.toLowerCase().includes(searchLower)
        );
    };

    // 高级搜索 - 前端实时过滤（支持所有筛选条件）
    const getAdvancedFilteredDocuments = () => {
        // 检查是否有任何过滤条件
        const hasAnyFilter =
            localAdvancedFilters.search ||
            localAdvancedFilters.fileTypes.length > 0 ||
            localAdvancedFilters.minSize ||
            localAdvancedFilters.maxSize ||
            localAdvancedFilters.indexed !== 'all' ||
            localAdvancedFilters.startDate ||
            localAdvancedFilters.endDate;

        // 如果没有任何过滤条件，返回后端数据
        if (!hasAnyFilter) {
            return documents;
        }

        // 使用 allDocuments（后端数据）或 documents 进行过滤
        const sourceList = allDocuments.length > 0 ? allDocuments : documents;

        // 前端实时过滤所有条件
        return sourceList.filter(doc => {
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
                const docType = doc.fileType.toLowerCase();
                if (!localAdvancedFilters.fileTypes.some(type => type.toLowerCase() === docType)) {
                    return false;
                }
            }

            // 3. 文件大小过滤
            if (localAdvancedFilters.minSize) {
                const minBytes = parseFloat(localAdvancedFilters.minSize) * 1024 * 1024;
                if (doc.fileSize < minBytes) return false;
            }
            if (localAdvancedFilters.maxSize) {
                const maxBytes = parseFloat(localAdvancedFilters.maxSize) * 1024 * 1024;
                if (doc.fileSize > maxBytes) return false;
            }

            // 4. 索引状态过滤
            if (localAdvancedFilters.indexed !== 'all') {
                const isIndexed = localAdvancedFilters.indexed === 'true';
                if (!!doc.indexed !== isIndexed) return false;
            }

            // 5. 日期范围过滤
            if (localAdvancedFilters.startDate || localAdvancedFilters.endDate) {
                try {
                    const docDate = new Date(doc.uploadTime);

                    if (isNaN(docDate.getTime())) {
                        // 如果日期解析失败，跳过此文档
                        return false;
                    }

                    if (localAdvancedFilters.startDate) {
                        const startDate = new Date(localAdvancedFilters.startDate);
                        startDate.setHours(0, 0, 0, 0); // 从开始日期的 00:00:00 开始
                        if (docDate < startDate) return false;
                    }

                    if (localAdvancedFilters.endDate) {
                        const endDate = new Date(localAdvancedFilters.endDate);
                        endDate.setHours(23, 59, 59, 999); // 包含结束日期的整天
                        if (docDate > endDate) return false;
                    }
                } catch (e) {
                    console.error(t('logDateFilterError'), e);
                    return false;
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
        // 检查本地过滤条件是否与后端过滤条件不同（即正在进行实时过滤）
        const isDifferent =
            localAdvancedFilters.search !== advancedFilters.search ||
            localAdvancedFilters.searchMode !== advancedFilters.searchMode ||
            JSON.stringify(localAdvancedFilters.fileTypes) !== JSON.stringify(advancedFilters.fileTypes) ||
            localAdvancedFilters.minSize !== advancedFilters.minSize ||
            localAdvancedFilters.maxSize !== advancedFilters.maxSize ||
            localAdvancedFilters.indexed !== advancedFilters.indexed ||
            localAdvancedFilters.startDate !== advancedFilters.startDate ||
            localAdvancedFilters.endDate !== advancedFilters.endDate;

        // 只有当有差异时才显示提示
        return isDifferent;
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
        <div style={{ position: 'relative', height: '100%' }}>
            {/* 主文档管理区域 */}
            <div style={{
                width: showAIAnalysis ? `${splitPosition}%` : '100%',
                height: '100%',
                overflow: 'auto',
                transition: showAIAnalysis ? 'none' : 'width 0.3s'
            }}>
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
                                    <button
                                        className="btn btn-primary"
                                        onClick={() => setShowAIAnalysis(!showAIAnalysis)}
                                        style={{
                                            marginRight: '15px',
                                            backgroundColor: showAIAnalysis ? '#f44336' : '#9C27B0',
                                            display: 'flex',
                                            alignItems: 'center',
                                            gap: '6px'
                                        }}
                                        title={showAIAnalysis ? '关闭AI分析' : '打开AI分析'}
                                    >
                                        <span style={{ fontSize: '16px' }}>
                                            {showAIAnalysis ? '✕' : '🤖'}
                                        </span>
                                        <span>
                                            {showAIAnalysis ? (t('close') || '关闭') : (t('aiAnalysis') || 'AI分析')}
                                        </span>
                                        {!showAIAnalysis && selectedDocs.size > 0 && (
                                            <span style={{
                                                backgroundColor: '#fff',
                                                color: '#9C27B0',
                                                padding: '2px 6px',
                                                borderRadius: '10px',
                                                fontSize: '11px',
                                                fontWeight: 'bold'
                                            }}>
                                                {selectedDocs.size}
                                            </span>
                                        )}
                                    </button>

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
                                        {(localFilterText || filterText || (showAdvancedSearch && (
                                            localAdvancedFilters.search ||
                                            localAdvancedFilters.fileTypes.length > 0 ||
                                            localAdvancedFilters.minSize ||
                                            localAdvancedFilters.maxSize ||
                                            localAdvancedFilters.indexed !== 'all' ||
                                            localAdvancedFilters.startDate ||
                                            localAdvancedFilters.endDate
                                        ))) ? (
                                            <>
                                                {t('docsFilterResult')} {
                                                    (showAdvancedSearch ?
                                                        getAdvancedFilteredDocuments() :
                                                        getFilteredDocuments()
                                                    ).length
                                                } / {allDocuments.length > 0 ? allDocuments.length : totalCount} {t('logDocsCount')}
                                                <button
                                                    className="documents-stats-clear-btn"
                                                    onClick={() => {
                                                        if (showAdvancedSearch) {
                                                            resetFilters();
                                                        } else {
                                                            handleSearchChange('');
                                                            setLocalFilterText('');
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
                            <button className="btn btn-secondary" onClick={loadDocuments}>
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
                                showAIAnalysis={showAIAnalysis}
                                selectedDocs={selectedDocs}
                                onToggleDoc={toggleDocSelection}
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

                    {/* 实时过滤后的空状态 */}
                    {!error && !loading && documents.length > 0 &&
                     (showAdvancedSearch ? getAdvancedFilteredDocuments() : getFilteredDocuments()).length === 0 && (
                        <div className="empty-state">
                            <div className="empty-state-icon">🔍</div>
                            <p>{t('docsNoMatchFound')}</p>
                            <p style={{ fontSize: '14px', marginTop: '10px', color: '#ccc' }}>
                                {t('docsTryDifferentKeyword')}
                            </p>
                        </div>
                    )}

                    {/* 提示信息 */}
                    {!error && !loading && totalCount > 0 && !showAIAnalysis && (
                        <div className="documents-tip">
                            {t('docsUploadTip')}
                        </div>
                    )}
                </div>
            </div>

            {/* 拖拽分隔线 */}
            {showAIAnalysis && (
                <div
                    style={{
                        position: 'fixed',
                        left: `${splitPosition}%`,
                        top: 0,
                        bottom: 0,
                        width: '6px',
                        backgroundColor: isDragging ? '#2196F3' : '#e0e0e0',
                        cursor: 'ew-resize',
                        zIndex: 1000,
                        transition: isDragging ? 'none' : 'background-color 0.2s',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                    }}
                    onMouseDown={() => setIsDragging(true)}
                >
                    <div style={{
                        width: '20px',
                        height: '60px',
                        backgroundColor: isDragging ? '#2196F3' : '#999',
                        borderRadius: '4px',
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center',
                        color: 'white',
                        fontSize: '16px',
                        boxShadow: '0 2px 8px rgba(0,0,0,0.15)'
                    }}>
                        ⋮
                    </div>
                </div>
            )}

            {/* AI分析右侧面板 */}
            {showAIAnalysis && (
                <div style={{
                    position: 'fixed',
                    left: `calc(${splitPosition}% + 6px)`,
                    top: 0,
                    right: 0,
                    bottom: 0,
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    overflowY: 'auto',
                    boxShadow: '-2px 0 12px rgba(0,0,0,0.3)',
                    zIndex: 999,
                    padding: '20px'
                }}>
                    <div style={{
                        marginBottom: '20px',
                        paddingBottom: '15px',
                        borderBottom: '2px solid rgba(255,255,255,0.3)'
                    }}>
                        <h2 style={{
                            margin: 0,
                            color: '#ffffff',
                            fontSize: '24px',
                            textShadow: '0 2px 4px rgba(0,0,0,0.2)'
                        }}>
                            🤖 {t('aiAnalysis') || 'AI分析'}
                        </h2>
                        <p style={{
                            margin: '8px 0 0 0',
                            color: 'rgba(255,255,255,0.9)',
                            fontSize: '14px'
                        }}>
                            {selectedDocs.size > 0
                                ? `已选择 ${selectedDocs.size} 个文档`
                                : '请在左侧勾选要分析的文档'}
                        </p>
                    </div>

                    {window.EmbeddedAIAnalysisPanel && React.createElement(window.EmbeddedAIAnalysisPanel, {
                        selectedDocuments: documents
                            .filter(d => selectedDocs.has(d.fileName))
                            .map(d => ({
                                title: d.fileName,
                                name: d.fileName,
                                path: d.fileName,
                                size: d.fileSize
                            })),
                        onClose: () => setShowAIAnalysis(false)
                    })}
                </div>
            )}
        </div>
    );
}

// 导出到全局
if (typeof window !== 'undefined') {
    window.DocumentsTab = DocumentsTab;
}
