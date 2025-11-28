/**
 * Documents Tab UI Components / 文档管理标签页UI组件库
 * JSX 版本 - 使用 Babel 转译
 *
 * @author AI Reviewer Team
 * @since 2025-11-28
 */

// ============================================================================
// 工具函数 - 根据文件类型返回对应图标
// ============================================================================

// 全局文件类型图标映射
window.fileIconMap = window.fileIconMap || {
    // Word 文档
    'doc': '📝',
    'docx': '📝',

    // Excel 表格
    'xls': '📊',
    'xlsx': '📊',
    'csv': '📊',

    // PowerPoint 演示
    'ppt': '📽️',
    'pptx': '📽️',

    // PDF 文档
    'pdf': '📕',

    // 文本文件
    'txt': '📃',
    'md': '📋',
    'markdown': '📋',

    // 代码文件
    'html': '🌐',
    'htm': '🌐',
    'xml': '📰',
    'json': '📰',
    'yaml': '📰',
    'yml': '📰',

    // 图片
    'jpg': '🖼️',
    'jpeg': '🖼️',
    'png': '🖼️',
    'gif': '🖼️',
    'bmp': '🖼️',
    'svg': '🖼️',

    // 压缩文件
    'zip': '🗜️',
    'rar': '🗜️',
    '7z': '🗜️',
    'tar': '🗜️',
    'gz': '🗜️',

    // 音频文件
    'mp3': '🎵',
    'wav': '🎵',
    'ogg': '🎵',
    'flac': '🎵',
    'aac': '🎵',
    'm4a': '🎵',
    'wma': '🎵',

    // 视频文件
    'mp4': '🎬',
    'avi': '🎬',
    'mkv': '🎬',
    'mov': '🎬',
    'wmv': '🎬',
    'flv': '🎬',
    'webm': '🎬',
    'm4v': '🎬',

    // 其他常见类型
    'rtf': '📝',
    'odt': '📝',
    'ods': '📊',
    'odp': '📽️',
    'tex': '📄',
    'log': '📋',
};

// 根据文件扩展名智能推断图标
function guessFileIcon(fileType) {
    const type = fileType.toLowerCase();
    
    // 文档类型
    if (['doc', 'docx', 'rtf', 'odt'].includes(type)) return '📝';
    
    // 表格类型
    if (['xls', 'xlsx', 'csv', 'ods'].includes(type)) return '📊';
    
    // 演示类型
    if (['ppt', 'pptx', 'odp'].includes(type)) return '📽️';
    
    // PDF
    if (type === 'pdf') return '📕';
    
    // 文本和标记语言
    if (['txt', 'text', 'log'].includes(type)) return '📃';
    if (['md', 'markdown', 'rst'].includes(type)) return '📋';
    
    // 网页和结构化数据
    if (['html', 'htm', 'xhtml'].includes(type)) return '🌐';
    if (['xml', 'json', 'yaml', 'yml', 'toml'].includes(type)) return '📰';
    
    // 图片
    if (['jpg', 'jpeg', 'png', 'gif', 'bmp', 'svg', 'webp', 'ico'].includes(type)) return '🖼️';
    
    // 压缩文件
    if (['zip', 'rar', '7z', 'tar', 'gz', 'bz2', 'xz'].includes(type)) return '🗜️';
    
    // 音视频
    if (['mp3', 'wav', 'ogg', 'flac', 'aac'].includes(type)) return '🎵';
    if (['mp4', 'avi', 'mkv', 'mov', 'wmv', 'flv'].includes(type)) return '🎬';
    
    // 代码文件
    if (['js', 'jsx', 'ts', 'tsx', 'py', 'java', 'c', 'cpp', 'cs', 'go', 'rb', 'php', 'swift', 'kt'].includes(type)) return '💻';
    
    // 默认文档图标
    return '📄';
}

function getFileIcon(fileType) {
    const type = fileType.toLowerCase();
    
    // 优先使用映射表中的图标
    if (window.fileIconMap[type]) {
        return window.fileIconMap[type];
    }
    
    // 如果映射表中没有，使用智能推断
    const guessedIcon = guessFileIcon(type);
    
    // 将推断的图标添加到映射表中（缓存）
    window.fileIconMap[type] = guessedIcon;
    
    return guessedIcon;
}

// 更新文件类型图标映射的工具函数
window.updateFileIconMap = function(fileTypes) {
    if (!Array.isArray(fileTypes)) return;
    
    fileTypes.forEach(type => {
        const lowerType = type.toLowerCase();
        if (!window.fileIconMap[lowerType]) {
            // 使用智能推断为新类型分配图标
            window.fileIconMap[lowerType] = guessFileIcon(lowerType);
        }
    });
};

// ============================================================================
// 通用自定义下拉选择器组件
// ============================================================================
function CustomSelect({ value, onChange, options, style, className }) {
    const { useState, useEffect, useRef } = React;
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef(null);
    
    // 点击外部关闭下拉菜单
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        
        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }
        
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isOpen]);
    
    const selectedOption = options.find(opt => opt.value === value);
    
    return (
        <div className={`custom-select-wrapper ${className || ''}`} style={style} ref={dropdownRef}>
            <div 
                className="custom-select-trigger"
                onClick={() => setIsOpen(!isOpen)}
            >
                <span>{selectedOption ? selectedOption.label : ''}</span>
                <span className="custom-select-arrow">{isOpen ? '▲' : '▼'}</span>
            </div>
            
            {isOpen && (
                <div className="custom-select-menu">
                    {options.map(option => (
                        <div
                            key={option.value}
                            className={`custom-select-option ${option.value === value ? 'selected' : ''}`}
                            onClick={() => {
                                onChange(option.value);
                                setIsOpen(false);
                            }}
                        >
                            {option.label}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

// ============================================================================
// 上传区域组件
// ============================================================================
function UploadArea({ uploading, uploadProgress, handleFileSelect, t }) {
    return (
        <div className="documents-upload-area">
            <h3>{t('docsUploadArea')}</h3>
            <input
                type="file"
                id="fileInput"
                className="documents-upload-input"
                multiple
                accept=".xlsx,.xls,.docx,.doc,.pptx,.ppt,.pdf,.txt,.md,.html,.xml"
                onChange={handleFileSelect}
                disabled={uploading}
            />
            <label
                htmlFor="fileInput"
                className={`documents-upload-label btn btn-primary ${uploading ? 'disabled' : ''}`}
            >
                {uploading ? t('docsUploading') : t('docsUploadButton')}
            </label>
            <p className="documents-upload-hint">
                {t('docsUploadFormats')}
            </p>

            {uploadProgress && (
                <div className="documents-upload-progress">
                    <div>
                        {t('docsUploadProgress')} {uploadProgress.current}/{uploadProgress.total}
                    </div>
                    <div className="documents-upload-progress-text">
                        {t('docsUploadSuccessCount')} {uploadProgress.success} |
                        {t('docsUploadFailedCount')} {uploadProgress.failed}
                    </div>
                    {/* 上传完成后显示索引建议 */}
                    {uploadProgress.current === uploadProgress.total && uploadProgress.success > 0 && (
                        <div style={{
                            marginTop: '10px',
                            padding: '12px',
                            background: '#fff3cd',
                            borderRadius: '6px',
                            border: '1px solid #ffc107',
                            fontSize: '14px',
                            lineHeight: '1.6'
                        }}>
                            <div style={{ display: 'flex', alignItems: 'center', marginBottom: '5px' }}>
                                <span style={{ fontSize: '16px', marginRight: '6px' }}>💡</span>
                                <strong style={{ color: '#856404' }}>{t('docsUploadIndexTip')}</strong>
                            </div>
                            <div style={{ color: '#856404', fontSize: '13px' }}>
                                {t('docsUploadIndexDesc')}
                            </div>
                        </div>
                    )}
                </div>
            )}
        </div>
    );
}

// ============================================================================
// 列表头部组件
// ============================================================================
function DocumentListHeader({ loading, totalCount, documentsLength, loadDocuments, t }) {
    return (
        <div className="documents-list-header">
            <h3 className="documents-list-title">
                {t('docsListTitle')}
                {!loading && totalCount > 0 && ` (${documentsLength}/${totalCount})`}
            </h3>
            <button
                className="btn btn-secondary"
                onClick={loadDocuments}
                disabled={loading}
            >
                {loading ? t('docsListRefreshing') : t('docsListRefresh')}
            </button>
        </div>
    );
}

// ============================================================================
// 搜索过滤组件
// ============================================================================
function SearchFilters({
    showAdvancedSearch,
    setShowAdvancedSearch,
    filterText,
    handleSearchChange,
    handleSearchKeyPress,
    handleSearchSubmit,
    advancedFilters,
    updateFilter,
    toggleFileType,
    supportedFileTypes,
    applyFilters,
    resetFilters,
    hasActiveFilters,
    getActiveFilterCount,
    language,
    t
}) {
    return (
        <div style={{ marginBottom: '15px' }}>
            {/* 搜索模式切换按钮 */}
            <div className="documents-search-toggle">
                <button
                    className="btn btn-secondary"
                    onClick={() => setShowAdvancedSearch(!showAdvancedSearch)}
                    style={{ marginRight: '10px' }}
                >
                    {showAdvancedSearch ? t('docsSimpleSearch') : t('docsAdvancedSearch')}
                </button>
            </div>

            {/* 简单搜索 */}
            {!showAdvancedSearch && (
                <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    <input
                        type="text"
                        className="input-field documents-simple-search"
                        placeholder={t('docsFilterPlaceholder')}
                        value={filterText}
                        onChange={(e) => handleSearchChange(e.target.value)}
                        onKeyPress={handleSearchKeyPress}
                        style={{ flex: 1 }}
                    />
                    <button
                        className="btn btn-primary"
                        onClick={handleSearchSubmit}
                        style={{ whiteSpace: 'nowrap' }}
                    >
                        🔍 {t('docsSearchButton')}
                    </button>
                    {filterText && (
                        <button
                            className="btn btn-secondary"
                            onClick={() => {
                                handleSearchChange('');
                                handleSearchSubmit();
                            }}
                            style={{ whiteSpace: 'nowrap' }}
                            title={t('docsClearSearch')}
                        >
                            ✕ {t('docsClearButton')}
                        </button>
                    )}
                </div>
            )}

            {/* 高级搜索面板 */}
            {showAdvancedSearch && (
                <AdvancedSearchPanel
                    advancedFilters={advancedFilters}
                    updateFilter={updateFilter}
                    toggleFileType={toggleFileType}
                    supportedFileTypes={supportedFileTypes}
                    applyFilters={applyFilters}
                    resetFilters={resetFilters}
                    language={language}
                    t={t}
                />
            )}

            {/* 当前激活的筛选条件显示 */}
            {showAdvancedSearch && hasActiveFilters() && (
                <div className="documents-active-filters">
                    <span className="documents-active-filters-text">
                        {t('docsActiveFilters')}: {getActiveFilterCount()} {t('docsFilterCount')}
                    </span>
                </div>
            )}
        </div>
    );
}

// ============================================================================
// 高级搜索面板组件
// ============================================================================
function AdvancedSearchPanel({
    advancedFilters,
    updateFilter,
    toggleFileType,
    supportedFileTypes,
    applyFilters,
    resetFilters,
    language,
    t
}) {
    const DatePicker = window.DatePicker;

    return (
        <div className="documents-advanced-search-panel">
            {/* 文件名搜索 + 搜索模式 */}
            <div className="documents-search-row">
                <label className="documents-search-label">{t('docsFilterPlaceholder')}</label>
                <div style={{ position: 'relative', flex: 1 }}>
                    <input
                        type="text"
                        className="input-field documents-search-input"
                        placeholder={t('docsFilterPlaceholder')}
                        value={advancedFilters.search}
                        onChange={(e) => updateFilter('search', e.target.value)}
                        onKeyPress={(e) => {
                            if (e.key === 'Enter') {
                                applyFilters();
                            }
                        }}
                        style={{ width: '100%', paddingRight: advancedFilters.search ? '35px' : '12px' }}
                    />
                    {advancedFilters.search && (
                        <button
                            onClick={() => updateFilter('search', '')}
                            style={{
                                position: 'absolute',
                                right: '8px',
                                top: '50%',
                                transform: 'translateY(-50%)',
                                background: 'none',
                                border: 'none',
                                color: '#999',
                                cursor: 'pointer',
                                fontSize: '18px',
                                padding: '0',
                                width: '20px',
                                height: '20px',
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                borderRadius: '50%',
                                transition: 'all 0.2s'
                            }}
                            onMouseEnter={(e) => {
                                e.target.style.background = '#f0f0f0';
                                e.target.style.color = '#666';
                            }}
                            onMouseLeave={(e) => {
                                e.target.style.background = 'none';
                                e.target.style.color = '#999';
                            }}
                            title={t('docsClearSearch')}
                        >
                        ✕
                    </button>
                )}
            </div>
            <CustomSelect
                value={advancedFilters.searchMode}
                onChange={(val) => updateFilter('searchMode', val)}
                options={[
                    { value: 'contains', label: t('docsSearchModeContains') },
                    { value: 'exact', label: t('docsSearchModeExact') },
                    { value: 'regex', label: t('docsSearchModeRegex') }
                ]}
                style={{ width: 'auto', minWidth: '150px' }}
            />
        </div>            {/* 文件类型多选 */}
            <FileTypeSelector
                advancedFilters={advancedFilters}
                updateFilter={updateFilter}
                toggleFileType={toggleFileType}
                supportedFileTypes={supportedFileTypes}
                t={t}
            />

            {/* 文件大小范围 + 索引状态 */}
            <div className="documents-search-row">
                {/* 文件大小 */}
                <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                    <label style={{ fontWeight: '600', whiteSpace: 'nowrap' }}>
                        {t('docsFileSizeFilter')}
                    </label>
                    <input
                        type="number"
                        className="input-field"
                        style={{ width: '100px' }}
                        placeholder={t('docsFileSizeMin')}
                        value={advancedFilters.minSize}
                        onChange={(e) => updateFilter('minSize', e.target.value)}
                        min="0"
                    />
                    <span>-</span>
                    <input
                        type="number"
                        className="input-field"
                        style={{ width: '100px' }}
                        placeholder={t('docsFileSizeMax')}
                        value={advancedFilters.maxSize}
                        onChange={(e) => updateFilter('maxSize', e.target.value)}
                        min="0"
                    />
                    <span>{t('docsFileSizeUnit')}</span>
                </div>

            {/* 索引状态 */}
            <div style={{ display: 'flex', gap: '10px', alignItems: 'center' }}>
                <label style={{ fontWeight: '600', whiteSpace: 'nowrap' }}>
                    {t('docsIndexedFilter')}
                </label>
                <CustomSelect
                    value={advancedFilters.indexed}
                    onChange={(val) => updateFilter('indexed', val)}
                    options={[
                        { value: 'all', label: t('docsIndexedAll') },
                        { value: 'true', label: t('docsIndexedYes') },
                        { value: 'false', label: t('docsIndexedNo') }
                    ]}
                    style={{ width: '150px' }}
                />
            </div>
        </div>            {/* 日期范围 */}
            <div className="documents-search-row documents-date-range-row">
                <label style={{ fontWeight: '600', whiteSpace: 'nowrap' }}>
                    {t('docsDateFilter')}
                </label>
                <div className="documents-date-picker-container">
                    <DatePicker
                        value={advancedFilters.startDate}
                        onChange={(date) => updateFilter('startDate', date)}
                        placeholder={t('docsDateStart')}
                        language={language}
                    />
                </div>
                <span style={{ margin: '0 10px', color: '#999' }}>-</span>
                <div className="documents-date-picker-container">
                    <DatePicker
                        value={advancedFilters.endDate}
                        onChange={(date) => updateFilter('endDate', date)}
                        placeholder={t('docsDateEnd')}
                        language={language}
                    />
                </div>
            </div>

            {/* 操作按钮 */}
            <div className="documents-action-buttons">
                <button onClick={applyFilters} className="btn btn-primary">
                    {t('docsApplyFilter')}
                </button>
                <button onClick={resetFilters} className="btn btn-secondary">
                    {t('docsResetFilter')}
                </button>
            </div>
        </div>
    );
}

// ============================================================================
// 文件类型选择器组件
// ============================================================================
function FileTypeSelector({ advancedFilters, updateFilter, toggleFileType, supportedFileTypes, t }) {
    const { useState, useEffect, useRef } = React;
    const [isOpen, setIsOpen] = useState(false);
    const dropdownRef = useRef(null);
    
    // 对文件类型进行排序
    const sortedFileTypes = [...supportedFileTypes].sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()));
    
    // 将文件类型分成3列
    const columns = 3;
    const itemsPerColumn = Math.ceil(sortedFileTypes.length / columns);
    const columnData = [];
    for (let i = 0; i < columns; i++) {
        columnData.push(sortedFileTypes.slice(i * itemsPerColumn, (i + 1) * itemsPerColumn));
    }
    
    // 点击外部关闭下拉菜单
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (dropdownRef.current && !dropdownRef.current.contains(event.target)) {
                setIsOpen(false);
            }
        };
        
        if (isOpen) {
            document.addEventListener('mousedown', handleClickOutside);
        }
        
        return () => {
            document.removeEventListener('mousedown', handleClickOutside);
        };
    }, [isOpen]);
    
    return (
        <div className="documents-file-type-selector">
            <label className="documents-file-type-header">
                {t('docsFileTypeFilter')}
                {advancedFilters.fileTypes.length > 0 && (
                    <span className="documents-file-type-count">
                        ({advancedFilters.fileTypes.length} {t('docsSelected')})
                    </span>
                )}
            </label>

            <div className="documents-file-type-controls">
                {/* 自定义下拉选择器 */}
                <div className="documents-file-type-dropdown" ref={dropdownRef}>
                    <div 
                        className="documents-file-type-dropdown-trigger"
                        onClick={() => setIsOpen(!isOpen)}
                    >
                        <span>
                            {advancedFilters.fileTypes.length > 0 
                                ? `${advancedFilters.fileTypes.length} ${t('docsSelected')}`
                                : t('docsSelectFileTypes') || '选择文件类型'}
                        </span>
                        <span className="documents-dropdown-arrow">{isOpen ? '▲' : '▼'}</span>
                    </div>
                    
                    {isOpen && (
                        <div className="documents-file-type-dropdown-menu">
                            <div className="documents-file-type-grid-3col">
                                {columnData.map((column, colIndex) => (
                                    <div key={colIndex} className="documents-file-type-column">
                                        {column.map(type => (
                                            <label key={type} className="documents-file-type-checkbox-item">
                                                <input
                                                    type="checkbox"
                                                    checked={advancedFilters.fileTypes.includes(type)}
                                                    onChange={(e) => {
                                                        e.stopPropagation();
                                                        toggleFileType(type, e.target.checked);
                                                    }}
                                                />
                                                <span className="documents-file-type-label">
                                                    <span className="documents-file-type-icon">{getFileIcon(type)}</span>
                                                    <span className="documents-file-type-name">{type.toUpperCase()}</span>
                                                </span>
                                            </label>
                                        ))}
                                    </div>
                                ))}
                            </div>
                        </div>
                    )}
                </div>

                {/* 快捷操作按钮 */}
                <button
                    type="button"
                    className="documents-btn-primary documents-btn-gradient-purple"
                    onClick={() => updateFilter('fileTypes', [...supportedFileTypes])}
                >
                    ✓ {t('docsSelectAll')}
                </button>
                <button
                    type="button"
                    className="documents-btn-primary documents-btn-gradient-pink"
                    onClick={() => updateFilter('fileTypes', [])}
                >
                    ✕ {t('docsClearAll')}
                </button>
            </div>

            {/* 已选择的文件类型标签显示 */}
            {advancedFilters.fileTypes.length > 0 && (
                <div className="documents-file-type-tags">
                    {advancedFilters.fileTypes.map(type => (
                        <span key={type} className="documents-file-type-tag">
                            {getFileIcon(type)} {type.toUpperCase()}
                            <button
                                type="button"
                                className="documents-file-type-tag-remove"
                                onClick={() => toggleFileType(type, false)}
                            >
                                ×
                            </button>
                        </span>
                    ))}
                </div>
            )}
        </div>
    );
}

// ============================================================================
// 文档列表组件
// ============================================================================
const DocumentList = React.memo(function DocumentList({ documents, formatFileSize, handleDelete, t }) {
    return (
        <div className="documents-list">
            {documents.map((doc) => (
                <div key={doc.fileName + doc.uploadTime} className="document-card">
                    <div className="document-info">
                        <div className="document-title">
                            {getFileIcon(doc.fileType)} {doc.fileName}
                        </div>
                        <div className="document-meta">
                            📦 {formatFileSize(doc.fileSize)} |
                            📅 {doc.uploadTime} |
                            🏷️ {doc.fileType.toUpperCase()}
                            {doc.indexed && <span style={{ marginLeft: '5px' }}>| ✅ {t('docsIndexed')}</span>}
                        </div>
                    </div>
                    <button
                        className="document-delete-btn btn btn-secondary"
                        onClick={() => handleDelete(doc.fileName)}
                    >
                        {t('docsListDelete')}
                    </button>
                </div>
            ))}
        </div>
    );
});

// ============================================================================
// 分页组件
// ============================================================================
function Pagination({ currentPage, totalPages, goToPage, t }) {
    return (
        <div className="pagination-container">
            <button
                className="pagination-btn"
                onClick={() => goToPage(currentPage - 1)}
                disabled={currentPage === 1}
            >
                {t('docsPaginationPrev')}
            </button>

            <div className="pagination-info">
                <span>
                    {t('docsPagination')} {currentPage} {t('docsPaginationPage')} /
                    {t('docsPaginationTotal')} {totalPages} {t('docsPaginationPage')}
                </span>
                <span style={{ margin: '0 10px', color: '#ccc' }}>|</span>
                <input
                    type="number"
                    className="pagination-jump-input"
                    min="1"
                    max={totalPages}
                    placeholder={currentPage.toString()}
                    onKeyPress={(e) => {
                        if (e.key === 'Enter') {
                            const page = parseInt(e.target.value);
                            if (page && page >= 1 && page <= totalPages) {
                                goToPage(page);
                                e.target.value = '';
                            }
                        }
                    }}
                />
                <button
                    className="pagination-jump-btn"
                    onClick={(e) => {
                        const input = e.target.previousElementSibling;
                        const page = parseInt(input.value);
                        if (page && page >= 1 && page <= totalPages) {
                            goToPage(page);
                            input.value = '';
                        }
                    }}
                >
                    {t('docsPaginationJump')}
                </button>
            </div>

            <button
                className="pagination-btn"
                onClick={() => goToPage(currentPage + 1)}
                disabled={currentPage === totalPages}
            >
                {t('docsPaginationNext')}
            </button>
        </div>
    );
}

// ============================================================================
// 导出组件
// ============================================================================
const DocumentsTabComponents = {
    CustomSelect,
    UploadArea,
    DocumentListHeader,
    SearchFilters,
    AdvancedSearchPanel,
    FileTypeSelector,
    DocumentList,
    Pagination
};

// 导出到全局
if (typeof window !== 'undefined') {
    window.DocumentsTabComponents = DocumentsTabComponents;
}

// 如果支持模块导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = DocumentsTabComponents;
}

