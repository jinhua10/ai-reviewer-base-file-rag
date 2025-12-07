/**
 * HOPE Dashboard Panel Component (HOPE 仪表盘面板组件)
 * 
 * 显示 HOPE 系统状态、性能指标、知识质量和优化建议 (Displays HOPE system status, performance metrics, knowledge quality, and optimization suggestions)
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */

function HOPEDashboardPanel({ collapsed = false, onToggle }) {
    const { useState, useEffect, useCallback } = React;
    const { t } = window.LanguageModule.useTranslation();

    // 状态 (State)
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [dashboard, setDashboard] = useState(null);
    const [autoRefresh, setAutoRefresh] = useState(true);
    const [testQuestion, setTestQuestion] = useState('');
    const [testResult, setTestResult] = useState(null);
    const [testLoading, setTestLoading] = useState(false);

    // 加载仪表盘数据 (Load dashboard data)
    const loadDashboard = useCallback(async () => {
        try {
            setError(null);
            const data = await window.api.getHOPEDashboard();
            setDashboard(data);
        } catch (err) {
            setError(err.message || t('hopeLoadError'));
            console.error(t('logHOPEDashboardError'), err);
        } finally {
            setLoading(false);
        }
    }, []);

    // 初始加载和自动刷新 (Initial load and auto-refresh)
    useEffect(() => {
        loadDashboard();

        let interval;
        if (autoRefresh) {
            interval = setInterval(loadDashboard, 30000); // 30秒刷新 (30s refresh)
        }

        return () => {
            if (interval) clearInterval(interval);
        };
    }, [loadDashboard, autoRefresh]);

    // 测试 HOPE 查询 (Test HOPE query)
    const handleTestQuery = async () => {
        if (!testQuestion.trim()) return;

        setTestLoading(true);
        setTestResult(null);

        try {
            const result = await window.api.testHOPEQuery(testQuestion);
            setTestResult(result);
        } catch (err) {
            setTestResult({ error: err.message });
        } finally {
            setTestLoading(false);
        }
    };

    // 重置指标 (Reset metrics)
    const handleResetMetrics = async () => {
        if (!confirm(t('hopeResetConfirm'))) return;

        try {
            await window.api.resetHOPEMetrics();
            await loadDashboard();
            showToast(t('hopeResetSuccess'), 'success');
        } catch (err) {
            showToast(t('hopeResetFailed') + ': ' + err.message, 'error');
        }
    };

    // 获取健康状态颜色 (Get health status color)
    const getHealthColor = (status) => {
        switch (status) {
            case 'healthy': return '#10b981';
            case 'warning': return '#f59e0b';
            case 'unhealthy': return '#ef4444';
            default: return '#6b7280';
        }
    };

    // 格式化百分比 (Format percentage)
    const formatPercent = (value) => {
        return (value * 100).toFixed(1) + '%';
    };

    // 格式化时间 (Format time)
    const formatTime = (ms) => {
        if (ms < 1000) return ms.toFixed(0) + 'ms';
        return (ms / 1000).toFixed(2) + 's';
    };

    if (collapsed) {
        return (
            <div className="hope-dashboard-collapsed" onClick={onToggle}>
                <span>🧠 HOPE</span>
                {dashboard?.health && (
                    <span
                        className="hope-health-indicator"
                        style={{ backgroundColor: getHealthColor(dashboard.health.status) }}
                    />
                )}
            </div>
        );
    }

    return (
        <div className="hope-dashboard-panel">
            {/* 头部 (Header) */}
            <div className="hope-dashboard-header">
                <div>
                    <h3 className="hope-dashboard-title">
                        🧠 {t('hopeTitle')}
                    </h3>
                    <p className="hope-dashboard-subtitle">
                        {t('hopeSubtitle')}
                    </p>
                </div>
                <div className="hope-dashboard-controls">
                    <label className="hope-dashboard-label">
                        <input
                            type="checkbox"
                            checked={autoRefresh}
                            onChange={(e) => setAutoRefresh(e.target.checked)}
                        />
                        {t('hopeAutoRefresh')}
                    </label>
                    <button onClick={loadDashboard} className="hope-dashboard-btn">
                        🔄
                    </button>
                    {onToggle && (
                        <button onClick={onToggle} className="hope-dashboard-btn">
                            ✕
                        </button>
                    )}
                </div>
            </div>

            {/* 内容区 (Content area) */}
            <div className="hope-dashboard-content">
                {loading ? (
                    <div className="hope-dashboard-loading">
                        {t('loading')}
                    </div>
                ) : error ? (
                    <div className="hope-dashboard-error">
                        {error}
                    </div>
                ) : !dashboard?.enabled ? (
                    <div className="hope-dashboard-disabled">
                        {t('hopeDisabled')}
                    </div>
                ) : (
                    <>
                        {/* 健康状态 (Health status) */}
                        <div className="hope-health-card">
                            <div
                                className="hope-health-badge"
                                style={{ background: getHealthColor(dashboard.health?.status) }}
                            >
                                {dashboard.health?.status === 'healthy' ? '✓' :
                                 dashboard.health?.status === 'warning' ? '!' : '✕'}
                            </div>
                            <div>
                                <div className="hope-health-status">
                                    {dashboard.health?.status || 'Unknown'}
                                </div>
                                {dashboard.health?.issues?.length > 0 && (
                                    <div className="hope-health-issues">
                                        {dashboard.health.issues.join(', ')}
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* 核心指标 (Core metrics) */}
                        <div className="hope-metrics-grid">
                            {/* LLM 节省率 (LLM Savings Rate) */}
                            <div className="hope-metric-card savings">
                                <div className="hope-metric-value">
                                    {formatPercent(dashboard.metrics?.llmSavingsRate || 0)}
                                </div>
                                <div className="hope-metric-label">{t('hopeLLMSavings')}</div>
                            </div>

                            {/* 直接回答 (Direct Answers) */}
                            <div className="hope-metric-card answers">
                                <div className="hope-metric-value">
                                    {dashboard.metrics?.directAnswers || 0}
                                </div>
                                <div className="hope-metric-label">{t('hopeDirectAnswers')}</div>
                            </div>

                            {/* 平均响应时间 (Average Response Time) */}
                            <div className="hope-metric-card response">
                                <div className="hope-metric-value">
                                    {formatTime(dashboard.metrics?.avgResponseTimeMs || 0)}
                                </div>
                                <div className="hope-metric-label">{t('hopeAvgResponse')}</div>
                            </div>

                            {/* 总查询数 (Total Queries) */}
                            <div className="hope-metric-card queries">
                                <div className="hope-metric-value">
                                    {dashboard.metrics?.totalQueries || 0}
                                </div>
                                <div className="hope-metric-label">{t('hopeTotalQueries')}</div>
                            </div>
                        </div>

                        {/* 三层统计 (Three-tier statistics) */}
                        <div className="hope-section">
                            <h4 className="hope-section-title">
                                📊 {t('hopeLayerStats')}
                            </h4>
                            <div className="hope-layers-grid">
                                <div className="hope-layer-card permanent">
                                    <div className="hope-layer-value">
                                        {dashboard.metrics?.permanentHits || 0}
                                    </div>
                                    <div className="hope-layer-label">{t('hopePermanent')}</div>
                                </div>
                                <div className="hope-layer-card ordinary">
                                    <div className="hope-layer-value">
                                        {dashboard.metrics?.ordinaryHits || 0}
                                    </div>
                                    <div className="hope-layer-label">{t('hopeOrdinary')}</div>
                                </div>
                                <div className="hope-layer-card high-freq">
                                    <div className="hope-layer-value">
                                        {dashboard.metrics?.highFreqHits || 0}
                                    </div>
                                    <div className="hope-layer-label">{t('hopeHighFreq')}</div>
                                </div>
                            </div>
                        </div>

                        {/* 优化建议 (Optimization suggestions) */}
                        {dashboard.suggestions?.length > 0 && (
                            <div className="hope-section">
                                <h4 className="hope-section-title">
                                    💡 {t('hopeSuggestions')}
                                </h4>
                                <div className="hope-suggestions">
                                    {dashboard.suggestions.map((suggestion, index) => (
                                        <div key={index} className="hope-suggestion-item">
                                            {suggestion}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* 测试查询 (Test query) */}
                        <div className="hope-test-section">
                            <h4 className="hope-section-title">
                                🧪 {t('hopeTestQuery')}
                            </h4>
                            <div className="hope-test-controls">
                                <input
                                    type="text"
                                    value={testQuestion}
                                    onChange={(e) => setTestQuestion(e.target.value)}
                                    placeholder={t('hopeTestPlaceholder')}
                                    onKeyPress={(e) => e.key === 'Enter' && handleTestQuery()}
                                    className="hope-test-input"
                                />
                                <button
                                    onClick={handleTestQuery}
                                    disabled={testLoading || !testQuestion.trim()}
                                    className="hope-test-button"
                                >
                                    {testLoading ? '...' : t('hopeTestButton')}
                                </button>
                            </div>

                            {testResult && (
                                <div className={`hope-test-result ${testResult.error ? 'error' : 'success'}`}>
                                    {testResult.error ? (
                                        <span className="hope-test-error-text">{testResult.error}</span>
                                    ) : (
                                        <>
                                            <div className="hope-test-result-field"><strong>{t('hopeNeedsLLM')}:</strong> {testResult.needsLLM ? t('yes') : t('no')}</div>
                                            <div className="hope-test-result-field"><strong>{t('hopeSourceLayer')}:</strong> {testResult.sourceLayer || '-'}</div>
                                            <div className="hope-test-result-field"><strong>{t('hopeConfidence')}:</strong> {testResult.confidence?.toFixed(2) || '-'}</div>
                                            <div className="hope-test-result-field"><strong>{t('hopeStrategy')}:</strong> {testResult.strategy}</div>
                                            <div className="hope-test-result-field"><strong>{t('hopeProcessTime')}:</strong> {testResult.processingTimeMs}ms</div>
                                            {testResult.answer && (
                                                <div className="hope-test-result-answer">
                                                    <strong>{t('hopeAnswer')}:</strong> {testResult.answer.substring(0, 200)}
                                                    {testResult.answer.length > 200 ? '...' : ''}
                                                </div>
                                            )}
                                        </>
                                    )}
                                </div>
                            )}
                        </div>

                        {/* 操作按钮 (Action buttons) */}
                        <div className="hope-actions">
                            <button onClick={handleResetMetrics} className="hope-action-button">
                                {t('hopeResetMetrics')}
                            </button>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}

// 导出到全局
if (typeof window !== 'undefined') {
    window.HOPEDashboardPanel = HOPEDashboardPanel;
}

