/**
 * 文档管理组件 - UI组件库
 * 包含文档卡片、高级搜索、分页等UI组件
 */

const DocumentsTabComponents = {
    /**
     * 渲染文档卡片
     */
    renderDocumentCard: (doc, onDelete, onIndex, t) => {
        const formatFileSize = (bytes) => {
            if (bytes === 0) return '0 B';
            const k = 1024;
            const sizes = ['B', 'KB', 'MB', 'GB'];
            const i = Math.floor(Math.log(bytes) / Math.log(k));
            return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
        };

        const formatDate = (dateStr) => {
            if (!dateStr) return '-';
            const date = new Date(dateStr);
            return date.toLocaleString();
        };

        return React.createElement('div', {
            key: doc.id,
            className: 'document-card',
            style: window.StyleConstants.CARD.base
        },
            // 文档头部
            React.createElement('div', { className: 'document-header' },
                React.createElement('h3', null, '📄 ' + doc.name),
                React.createElement('span', {
                    className: `status-badge ${doc.indexed ? 'indexed' : 'not-indexed'}`
                }, doc.indexed ? t('docsIndexed') : t('docsNotIndexed'))
            ),

            // 文档信息
            React.createElement('div', { className: 'document-info' },
                React.createElement('div', null, `${t('docsFileSize')}: ${formatFileSize(doc.size)}`),
                React.createElement('div', null, `${t('docsFileType')}: ${doc.type}`),
                React.createElement('div', null, `${t('docsUploadTime')}: ${formatDate(doc.uploadTime)}`)
            ),

            // 操作按钮
            React.createElement('div', {
                className: 'document-actions',
                style: { marginTop: '10px', display: 'flex', gap: '10px' }
            },
                !doc.indexed && React.createElement('button', {
                    className: 'btn-primary',
                    style: window.StyleConstants.createButton('primary', 'gradientBlue'),
                    onClick: () => onIndex(doc.id),
                    onMouseEnter: (e) => window.StyleConstants.onButtonHover(e, 'rgba(79, 172, 254, 0.4)'),
                    onMouseLeave: (e) => window.StyleConstants.onButtonLeave(e, 'rgba(79, 172, 254, 0.3)')
                }, t('docsIndexButton')),

                React.createElement('button', {
                    className: 'btn-danger',
                    style: window.StyleConstants.createButton('primary', 'gradientPink'),
                    onClick: () => onDelete(doc.id),
                    onMouseEnter: (e) => window.StyleConstants.onButtonHover(e, 'rgba(245, 87, 108, 0.4)'),
                    onMouseLeave: (e) => window.StyleConstants.onButtonLeave(e, 'rgba(245, 87, 108, 0.3)')
                }, t('docsDeleteButton'))
            )
        );
    },

    /**
     * 渲染高级搜索面板
     */
    renderAdvancedSearch: (filters, supportedFileTypes, updateFilter, toggleFileType, language, t) => {
        return React.createElement('div', {
            className: 'advanced-search',
            style: {
                ...window.StyleConstants.CARD.base,
                marginBottom: '15px'
            }
        },
            React.createElement('h3', null, t('docsAdvancedSearch')),

            // 搜索关键词
            React.createElement('div', { style: { marginBottom: '15px' } },
                React.createElement('label', null, t('docsSearchKeyword')),
                React.createElement('input', {
                    type: 'text',
                    className: 'input-field',
                    style: window.StyleConstants.INPUT.base,
                    value: filters.search,
                    onChange: (e) => updateFilter('search', e.target.value),
                    placeholder: t('docsSearchPlaceholder')
                })
            ),

            // 搜索模式
            React.createElement('div', { style: { marginBottom: '15px' } },
                React.createElement('label', null, t('docsSearchMode')),
                React.createElement('select', {
                    className: 'input-field',
                    style: window.StyleConstants.INPUT.base,
                    value: filters.searchMode,
                    onChange: (e) => updateFilter('searchMode', e.target.value)
                },
                    React.createElement('option', { value: 'contains' }, t('docsSearchModeContains')),
                    React.createElement('option', { value: 'exact' }, t('docsSearchModeExact')),
                    React.createElement('option', { value: 'prefix' }, t('docsSearchModePrefix'))
                )
            ),

            // 文件类型
            React.createElement('div', { style: { marginBottom: '15px' } },
                React.createElement('label', null,
                    t('docsFileTypeFilter'),
                    filters.fileTypes.length > 0 && React.createElement('span', {
                        style: { marginLeft: '8px', color: '#667eea', fontSize: '14px' }
                    }, `(${filters.fileTypes.length} ${t('docsSelected')})`)
                ),

                React.createElement('div', {
                    style: { display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap' }
                },
                    // 下拉框
                    React.createElement('select', {
                        className: 'input-field',
                        style: window.StyleConstants.SELECT.base,
                        multiple: true,
                        size: 1,
                        value: filters.fileTypes,
                        onChange: (e) => {
                            const selected = Array.from(e.target.selectedOptions, option => option.value);
                            updateFilter('fileTypes', selected);
                        },
                        onFocus: (e) => {
                            e.target.size = Math.min(supportedFileTypes.length, 5);
                            e.target.style.borderColor = '#667eea';
                            e.target.style.boxShadow = '0 0 0 3px rgba(102, 126, 234, 0.1)';
                        },
                        onBlur: (e) => {
                            e.target.size = 1;
                            e.target.style.borderColor = '#e0e7ff';
                            e.target.style.boxShadow = 'none';
                        }
                    },
                        ...supportedFileTypes.map(type =>
                            React.createElement('option', {
                                key: type,
                                value: type,
                                style: {
                                    padding: '6px 10px',
                                    borderRadius: '4px',
                                    margin: '2px',
                                    cursor: 'pointer',
                                    fontWeight: '500',
                                    fontSize: '13px'
                                }
                            }, `📄 ${type.toUpperCase()}`)
                        )
                    ),

                    // 全选/清空按钮
                    React.createElement('button', {
                        type: 'button',
                        style: window.StyleConstants.createButton('primary', 'gradientPurple'),
                        onClick: () => updateFilter('fileTypes', [...supportedFileTypes]),
                        onMouseEnter: (e) => window.StyleConstants.onButtonHover(e, 'rgba(102, 126, 234, 0.4)'),
                        onMouseLeave: (e) => window.StyleConstants.onButtonLeave(e, 'rgba(102, 126, 234, 0.3)')
                    }, `✓ ${t('docsSelectAll')}`),

                    React.createElement('button', {
                        type: 'button',
                        style: window.StyleConstants.createButton('primary', 'gradientPink'),
                        onClick: () => updateFilter('fileTypes', []),
                        onMouseEnter: (e) => window.StyleConstants.onButtonHover(e, 'rgba(245, 87, 108, 0.4)'),
                        onMouseLeave: (e) => window.StyleConstants.onButtonLeave(e, 'rgba(245, 87, 108, 0.3)')
                    }, `✕ ${t('docsClearAll')}`)
                ),

                // 已选择的文件类型标签
                filters.fileTypes.length > 0 && React.createElement('div', {
                    style: { marginTop: '8px', display: 'flex', flexWrap: 'wrap', gap: '6px' }
                },
                    ...filters.fileTypes.map(type =>
                        React.createElement('span', {
                            key: type,
                            style: {
                                display: 'inline-flex',
                                alignItems: 'center',
                                gap: '4px',
                                padding: '4px 8px',
                                background: '#667eea',
                                color: 'white',
                                borderRadius: '4px',
                                fontSize: '12px',
                                fontWeight: '500'
                            }
                        },
                            type.toUpperCase(),
                            React.createElement('button', {
                                type: 'button',
                                onClick: () => toggleFileType(type, false),
                                style: {
                                    background: 'none',
                                    border: 'none',
                                    color: 'white',
                                    cursor: 'pointer',
                                    padding: '0',
                                    marginLeft: '2px',
                                    fontSize: '14px'
                                }
                            }, '×')
                        )
                    )
                )
            ),

            // 日期范围
            React.createElement('div', { style: { marginBottom: '15px' } },
                React.createElement('label', null, t('docsDateFilter')),
                React.createElement('div', {
                    style: { display: 'flex', gap: '10px', alignItems: 'center' }
                },
                    React.createElement(window.DatePicker, {
                        value: filters.startDate,
                        onChange: (date) => updateFilter('startDate', date),
                        placeholder: t('docsDateStart'),
                        language: language
                    }),
                    React.createElement('span', null, '-'),
                    React.createElement(window.DatePicker, {
                        value: filters.endDate,
                        onChange: (date) => updateFilter('endDate', date),
                        placeholder: t('docsDateEnd'),
                        language: language
                    })
                )
            )
        );
    },

    /**
     * 渲染上传进度
     */
    renderUploadProgress: (progress, t) => {
        return React.createElement('div', {
            className: 'upload-progress',
            style: {
                ...window.StyleConstants.CARD.base,
                marginBottom: '15px',
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                color: 'white'
            }
        },
            React.createElement('h4', null, t('docsUploadProgress')),
            React.createElement('div', null,
                `${progress.current} / ${progress.total} - `,
                `✓ ${progress.success} | ✗ ${progress.failed}`
            ),
            React.createElement('div', {
                style: {
                    width: '100%',
                    height: '8px',
                    background: 'rgba(255,255,255,0.3)',
                    borderRadius: '4px',
                    marginTop: '10px',
                    overflow: 'hidden'
                }
            },
                React.createElement('div', {
                    style: {
                        width: `${(progress.current / progress.total) * 100}%`,
                        height: '100%',
                        background: 'white',
                        transition: 'width 0.3s ease'
                    }
                })
            )
        );
    },

    /**
     * 渲染分页组件
     */
    renderPagination: (currentPage, totalPages, pageSize, onPageChange, onPageSizeChange, t) => {
        if (totalPages <= 1) return null;

        const pages = [];
        const maxButtons = 5;
        let startPage = Math.max(1, currentPage - Math.floor(maxButtons / 2));
        let endPage = Math.min(totalPages, startPage + maxButtons - 1);

        if (endPage - startPage < maxButtons - 1) {
            startPage = Math.max(1, endPage - maxButtons + 1);
        }

        for (let i = startPage; i <= endPage; i++) {
            pages.push(i);
        }

        return React.createElement('div', {
            className: 'pagination',
            style: {
                display: 'flex',
                justifyContent: 'center',
                alignItems: 'center',
                gap: '10px',
                marginTop: '20px'
            }
        },
            // 上一页
            React.createElement('button', {
                onClick: () => onPageChange(currentPage - 1),
                disabled: currentPage === 1,
                style: window.StyleConstants.createButton('primary', 'gradientPurple')
            }, '◀'),

            // 页码按钮
            ...pages.map(page =>
                React.createElement('button', {
                    key: page,
                    onClick: () => onPageChange(page),
                    style: {
                        ...window.StyleConstants.createButton('primary', page === currentPage ? 'gradientPurple' : 'gradientBlue'),
                        opacity: page === currentPage ? 1 : 0.6
                    }
                }, page)
            ),

            // 下一页
            React.createElement('button', {
                onClick: () => onPageChange(currentPage + 1),
                disabled: currentPage === totalPages,
                style: window.StyleConstants.createButton('primary', 'gradientPurple')
            }, '▶')
        );
    }
};

// 导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = DocumentsTabComponents;
} else {
    window.DocumentsTabComponents = DocumentsTabComponents;
}

