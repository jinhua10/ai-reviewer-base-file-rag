/**
 * 统计信息标签页组件 / Statistics Tab Component
 * 负责显示系统统计信息、索引管理
 */

function StatisticsTab() {
    // 获取React hooks（避免重复声明）
    const { useState, useEffect } = React;

    // 使用语言Hook
    const { t } = window.LanguageModule.useTranslation();

    // 状态管理
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [rebuilding, setRebuilding] = useState(false);
    const [incrementalIndexing, setIncrementalIndexing] = useState(false);
    const [rebuildResult, setRebuildResult] = useState(null);

    useEffect(() => {
        loadStatistics();
    }, []);

    const loadStatistics = async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await window.api.getStatistics();
            setStats(result);
        } catch (err) {
            setError(err.message || t('statsLoadError'));
        } finally {
            setLoading(false);
        }
    };

    const handleRebuild = async () => {
        if (!confirm(t('statsRebuildConfirm'))) {
            return;
        }

        setRebuilding(true);
        setError(null);
        setRebuildResult(null);

        try {
            const result = await window.api.rebuild();
            setRebuildResult(result);

            if (result.success) {
                // 重建成功后刷新统计信息
                setTimeout(() => {
                    loadStatistics();
                }, 1000);
            }
        } catch (err) {
            setError(err.message || t('statsRebuildError'));
        } finally {
            setRebuilding(false);
        }
    };

    const handleIncrementalIndex = async () => {
        if (!confirm(t('statsIncrementalConfirm'))) {
            return;
        }

        setIncrementalIndexing(true);
        setError(null);
        setRebuildResult(null);

        try {
            const result = await window.api.incrementalIndex();
            setRebuildResult(result);

            if (result.success) {
                // 增量索引成功后刷新统计信息
                setTimeout(() => {
                    loadStatistics();
                }, 1000);
            }
        } catch (err) {
            setError(err.message || t('statsIncrementalError'));
        } finally {
            setIncrementalIndexing(false);
        }
    };

    // 加载状态
    if (loading) {
        return React.createElement('div', { className: 'loading' },
            React.createElement('div', { className: 'spinner' }),
            React.createElement('p', null, t('statsLoadingStats'))
        );
    }

    // 错误状态
    if (error) {
        return React.createElement('div', { className: 'error' },
            t('qaErrorPrefix') + ' ' + error,
            React.createElement('button', {
                className: 'btn btn-secondary',
                onClick: loadStatistics,
                style: { marginTop: '10px' }
            }, t('statsRetry'))
        );
    }

    // 主渲染
    return React.createElement('div', null,
        // 统计卡片网格
        React.createElement('div', { className: 'statistics-grid' },
            React.createElement('div', { className: 'stat-card' },
                React.createElement('div', { className: 'stat-value' }, stats.documentCount),
                React.createElement('div', { className: 'stat-label' }, t('statsDocCount'))
            ),
            React.createElement('div', { className: 'stat-card' },
                React.createElement('div', { className: 'stat-value' }, stats.indexedDocumentCount),
                React.createElement('div', { className: 'stat-label' }, t('statsIndexedCount'))
            ),
            React.createElement('div', { className: 'stat-card' },
                React.createElement('div', { className: 'stat-value' },
                    `${stats.documentCount > 0
                        ? Math.round((stats.indexedDocumentCount / stats.documentCount) * 100)
                        : 0}%`
                ),
                React.createElement('div', { className: 'stat-label' }, t('statsIndexProgress'))
            )
        ),

        // 索引进度
        (rebuilding || incrementalIndexing) && React.createElement('div', {
            className: 'loading',
            style: { marginTop: '30px' }
        },
            React.createElement('div', { className: 'spinner' }),
            React.createElement('p', null,
                rebuilding ? t('statsRebuilding') : t('statsIncrementalIndexing')
            ),
            React.createElement('p', {
                style: { fontSize: '14px', color: '#999', marginTop: '10px' }
            }, rebuilding ? t('statsRebuildWait') : t('statsIncrementalWait'))
        ),

        // 重建结果
        rebuildResult && !rebuilding && !incrementalIndexing &&
        React.createElement('div', {
            className: rebuildResult.success ? 'answer-card' : 'error',
            style: { marginTop: '30px' }
        },
            React.createElement('h3', null,
                rebuildResult.success ? t('statsSuccess') : t('statsFailed')
            ),
            React.createElement('div', { style: { marginTop: '10px' } },
                React.createElement('p', null, rebuildResult.message),
                rebuildResult.success && rebuildResult.processedFiles > 0 &&
                React.createElement('div', {
                    style: { marginTop: '15px', fontSize: '14px' }
                },
                    React.createElement('p', null,
                        `${t('statsProcessedFiles')}: ${rebuildResult.processedFiles} ${t('statsCount')}`
                    ),
                    React.createElement('p', null,
                        `${t('statsTotalDocs')}: ${rebuildResult.totalDocuments} ${t('statsCount')}`
                    ),
                    React.createElement('p', null,
                        `${t('statsDuration')}: ${(rebuildResult.durationMs / 1000).toFixed(2)} ${t('statsSeconds')}`
                    )
                ),
                rebuildResult.suggestion && React.createElement('p', {
                    style: { marginTop: '10px', fontSize: '14px', color: '#666' }
                }, `${t('statsSuggestion')} ${rebuildResult.suggestion}`)
            )
        ),

        // 索引指南和操作按钮
        React.createElement('div', { style: { marginTop: '30px' } },
            React.createElement('div', {
                style: {
                    padding: '20px',
                    background: '#f8f9ff',
                    borderRadius: '8px',
                    marginBottom: '20px'
                }
            },
                React.createElement('h4', {
                    style: { marginBottom: '10px', color: '#667eea' }
                }, t('statsIndexGuideTitle')),
                React.createElement('div', {
                    style: { fontSize: '14px', lineHeight: '1.8', color: '#666' }
                },
                    React.createElement('p', {
                        style: { marginBottom: '8px' }
                    }, t('statsIncrementalDesc')),
                    React.createElement('p', null, t('statsRebuildDesc'))
                )
            ),

            // 操作按钮
            React.createElement('div', {
                style: {
                    textAlign: 'center',
                    display: 'flex',
                    gap: '10px',
                    justifyContent: 'center',
                    flexWrap: 'wrap'
                }
            },
                React.createElement('button', {
                    className: 'btn btn-secondary',
                    onClick: loadStatistics,
                    disabled: rebuilding || incrementalIndexing
                }, t('statsRefresh')),
                React.createElement('button', {
                    className: 'btn btn-primary',
                    onClick: handleIncrementalIndex,
                    disabled: rebuilding || incrementalIndexing,
                    style: {
                        background: incrementalIndexing
                            ? '#ccc'
                            : 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)'
                    }
                }, incrementalIndexing ? t('statsIndexing') : t('statsIncrementalIndex')),
                React.createElement('button', {
                    className: 'btn btn-primary',
                    onClick: handleRebuild,
                    disabled: rebuilding || incrementalIndexing
                }, rebuilding ? t('statsIndexingProgress') : t('statsRebuildIndex'))
            )
        )
    );
}

// 导出到全局
if (typeof window !== 'undefined') {
    window.StatisticsTab = StatisticsTab;
}

// 如果支持模块导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = StatisticsTab;
}

