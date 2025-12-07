/**
 * Statistics Tab Component / 统计信息标签页组件
 * JSX 版本 - 使用 Babel 转译
 * 负责显示系统统计信息、索引管理
 *
 * @author AI Reviewer Team
 * @since 2025-11-28
 */

function StatisticsTab() {
    const { useState, useEffect } = React;
    const { t } = window.LanguageModule.useTranslation();

    // 状态管理
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [rebuilding, setRebuilding] = useState(false);
    const [incrementalIndexing, setIncrementalIndexing] = useState(false);
    const [rebuildResult, setRebuildResult] = useState(null);
    const [isIndexing, setIsIndexing] = useState(false); // 服务器端索引状态

    useEffect(() => {
        loadStatistics();
        // 启动索引状态轮询
        const intervalId = setInterval(checkIndexingStatus, 3000); // 每3秒检查一次
        return () => clearInterval(intervalId); // 清理定时器
    }, []);

    // 检查索引状态
    const checkIndexingStatus = async () => {
        try {
            const result = await window.api.checkIndexingStatus();
            setIsIndexing(result.indexing);

            // 如果索引状态变化，更新统计信息
            if (result.indexing !== isIndexing) {
                if (!result.indexing && isIndexing) {
                    // 索引刚完成，刷新统计
                    setTimeout(() => loadStatistics(), 500);
                }
            }
        } catch (err) {
            console.error(t('logStatisticsIndexingError'), err);
        }
    };

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
        if (!confirm(t('statsRebuildConfirm'))) return;

        // 如果正在索引，禁止操作
        if (isIndexing) {
            alert(t('statsIndexingInProgress'));
            return;
        }

        setRebuilding(true);
        setError(null);
        setRebuildResult(null);

        try {
            const result = await window.api.rebuild();
            setRebuildResult(result);

            if (result.success) {
                setTimeout(() => loadStatistics(), 1000);
            }
        } catch (err) {
            setError(err.message || t('statsRebuildError'));
        } finally {
            setRebuilding(false);
        }
    };

    const handleIncrementalIndex = async () => {
        if (!confirm(t('statsIncrementalConfirm'))) return;

        // 如果正在索引，禁止操作
        if (isIndexing) {
            alert(t('statsIndexingInProgress'));
            return;
        }

        setIncrementalIndexing(true);
        setError(null);
        setRebuildResult(null);

        try {
            const result = await window.api.incrementalIndex();
            setRebuildResult(result);

            if (result.success) {
                setTimeout(() => loadStatistics(), 1000);
            }
        } catch (err) {
            setError(err.message || t('statsIncrementalError'));
        } finally {
            setIncrementalIndexing(false);
        }
    };

    // 加载状态
    if (loading) {
        return (
            <div className="loading">
                <div className="spinner"></div>
                <p>{t('statsLoadingStats')}</p>
            </div>
        );
    }

    // 错误状态
    if (error) {
        return (
            <div className="error">
                {t('qaErrorPrefix')} {error}
                <button
                    className="btn btn-secondary"
                    onClick={loadStatistics}
                    style={{ marginTop: '10px' }}
                >
                    {t('statsRetry')}
                </button>
            </div>
        );
    }

    // 主渲染
    return (
        <div>
            {/* 统计卡片网格 */}
            <div className="statistics-grid">
                <div className="stat-card">
                    <div className="stat-value">{stats.documentCount}</div>
                    <div className="stat-label">{t('statsDocCount')}</div>
                    <div style={{ fontSize: '12px', color: '#999', marginTop: '5px' }}>
                        {t('statsDocCountDesc')}
                    </div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">{stats.indexedDocumentCount}</div>
                    <div className="stat-label">{t('statsIndexedCount')}</div>
                    <div style={{ fontSize: '12px', color: '#999', marginTop: '5px' }}>
                        {t('statsIndexedCountDesc')}
                    </div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">
                        {stats.indexProgress !== undefined ? stats.indexProgress :
                            (stats.documentCount > 0
                                ? Math.round((stats.indexedDocumentCount / stats.documentCount) * 100)
                                : 100)}%
                    </div>
                    <div className="stat-label">{t('statsIndexProgress')}</div>
                    <div style={{ fontSize: '12px', color: '#999', marginTop: '5px' }}>
                        {t('statsIndexProgressDesc')}
                    </div>
                </div>
            </div>

            {/* 如果有未索引的文档，显示警告提示 */}
            {stats.needsIndexing && stats.unindexedCount > 0 && (
                <div style={{
                    marginTop: '20px',
                    padding: '15px',
                    background: '#fff3cd',
                    borderRadius: '8px',
                    border: '1px solid #ffc107'
                }}>
                    <div style={{ display: 'flex', alignItems: 'center', marginBottom: '8px' }}>
                        <span style={{ fontSize: '18px', marginRight: '8px' }}>⚠️</span>
                        <strong style={{ color: '#856404' }}>{t('statsOutOfSync')}</strong>
                    </div>
                    <div style={{ fontSize: '14px', color: '#856404', lineHeight: '1.6' }}>
                        {stats.message || t('statsOutOfSyncDesc', {
                            total: stats.documentCount,
                            indexed: stats.indexedDocumentCount,
                            unindexed: stats.unindexedCount
                        })}
                    </div>
                    <button
                        className="btn btn-primary"
                        onClick={handleIncrementalIndex}
                        disabled={rebuilding || incrementalIndexing}
                        style={{
                            marginTop: '10px',
                            background: incrementalIndexing
                                ? '#ccc'
                                : 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)'
                        }}
                    >
                        {incrementalIndexing ? t('statsIndexing') : t('statsIncrementalIndexNow')}
                    </button>
                </div>
            )}

            {/* 如果所有文档都已索引，显示成功提示 */}
            {!stats.needsIndexing && stats.documentCount > 0 && (
                <div style={{
                    marginTop: '20px',
                    padding: '15px',
                    background: '#d4edda',
                    borderRadius: '8px',
                    border: '1px solid #28a745'
                }}>
                    <div style={{ display: 'flex', alignItems: 'center' }}>
                        <span style={{ fontSize: '18px', marginRight: '8px' }}>✅</span>
                        <span style={{ color: '#155724', fontSize: '14px' }}>
                            {stats.message || t('statsAllIndexed')}
                        </span>
                    </div>
                </div>
            )}


            {/* 索引进度 */}
            {(rebuilding || incrementalIndexing) && (
                <div className="loading" style={{ marginTop: '30px' }}>
                    <div className="spinner"></div>
                    <p>
                        {rebuilding ? t('statsRebuilding') : t('statsIncrementalIndexing')}
                    </p>
                    <p style={{ fontSize: '14px', color: '#999', marginTop: '10px' }}>
                        {rebuilding ? t('statsRebuildWait') : t('statsIncrementalWait')}
                    </p>
                </div>
            )}

            {/* 重建结果 */}
            {rebuildResult && !rebuilding && !incrementalIndexing && (
                <div
                    className={rebuildResult.success ? 'answer-card' : 'error'}
                    style={{ marginTop: '30px' }}
                >
                    <h3>
                        {rebuildResult.success ? t('statsSuccess') : t('statsFailed')}
                    </h3>
                    <div style={{ marginTop: '10px' }}>
                        <p>{rebuildResult.message}</p>
                        {rebuildResult.success && rebuildResult.processedFiles > 0 && (
                            <div style={{ marginTop: '15px', fontSize: '14px' }}>
                                <p>
                                    {t('statsProcessedFiles')}: {rebuildResult.processedFiles} {t('statsCount')}
                                </p>
                                <p>
                                    {t('statsTotalDocs')}: {rebuildResult.totalDocuments} {t('statsCount')}
                                </p>
                                <p>
                                    {t('statsDuration')}: {(rebuildResult.durationMs / 1000).toFixed(2)} {t('statsSeconds')}
                                </p>
                            </div>
                        )}
                        {rebuildResult.suggestion && (
                            <p style={{ marginTop: '10px', fontSize: '14px', color: '#666' }}>
                                {t('statsSuggestion')} {rebuildResult.suggestion}
                            </p>
                        )}
                    </div>
                </div>
            )}

            {/* 索引指南和操作按钮 */}
            <div style={{ marginTop: '30px' }}>
                <div
                    style={{
                        padding: '20px',
                        background: '#f8f9ff',
                        borderRadius: '8px',
                        marginBottom: '20px'
                    }}
                >
                    <h4 style={{ marginBottom: '10px', color: '#667eea' }}>
                        {t('statsIndexGuideTitle')}
                    </h4>
                    <div style={{ fontSize: '14px', lineHeight: '1.8', color: '#666' }}>
                        <p style={{ marginBottom: '8px' }}>
                            {t('statsIncrementalDesc')}
                        </p>
                        <p>{t('statsRebuildDesc')}</p>
                    </div>
                </div>

                {/* 操作按钮 */}
                <div
                    style={{
                        textAlign: 'center',
                        display: 'flex',
                        gap: '10px',
                        justifyContent: 'center',
                        flexWrap: 'wrap'
                    }}
                >
                    <button
                        className="btn btn-secondary"
                        onClick={loadStatistics}
                        disabled={rebuilding || incrementalIndexing || isIndexing}
                    >
                        {t('statsRefresh')}
                    </button>
                    <button
                        className="btn btn-primary"
                        onClick={handleIncrementalIndex}
                        disabled={rebuilding || incrementalIndexing || isIndexing}
                        style={{
                            background: (incrementalIndexing || isIndexing)
                                ? '#ccc'
                                : 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)'
                        }}
                    >
                        {(incrementalIndexing || isIndexing) ? t('statsIndexing') : t('statsIncrementalIndex')}
                    </button>
                    <button
                        className="btn btn-primary"
                        onClick={handleRebuild}
                        disabled={rebuilding || incrementalIndexing || isIndexing}
                    >
                        {(rebuilding || isIndexing) ? t('statsIndexingProgress') : t('statsRebuildIndex')}
                    </button>
                </div>
            </div>
        </div>
    );
}

// 导出到全局
if (typeof window !== 'undefined') {
    window.StatisticsTab = StatisticsTab;
}

// 如果支持模块导出
if (typeof module !== 'undefined') {
    module.exports = StatisticsTab;
}

