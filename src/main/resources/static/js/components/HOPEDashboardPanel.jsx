/**
 * HOPE Dashboard Panel Component
 * HOPE 三层记忆架构监控仪表盘组件
 *
 * 显示 HOPE 系统状态、性能指标、知识质量和优化建议
 *
 * @author AI Reviewer Team
 * @since 2025-12-07
 */

function HOPEDashboardPanel({ collapsed = false, onToggle }) {
    const { useState, useEffect, useCallback } = React;
    const { t } = window.LanguageModule.useTranslation();

    // 状态
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [dashboard, setDashboard] = useState(null);
    const [autoRefresh, setAutoRefresh] = useState(true);
    const [testQuestion, setTestQuestion] = useState('');
    const [testResult, setTestResult] = useState(null);
    const [testLoading, setTestLoading] = useState(false);

    // 加载仪表盘数据
    const loadDashboard = useCallback(async () => {
        try {
            setError(null);
            const data = await window.api.getHOPEDashboard();
            setDashboard(data);
        } catch (err) {
            setError(err.message || '加载 HOPE 仪表盘失败');
            console.error('Failed to load HOPE dashboard:', err);
        } finally {
            setLoading(false);
        }
    }, []);

    // 初始加载和自动刷新
    useEffect(() => {
        loadDashboard();

        let interval;
        if (autoRefresh) {
            interval = setInterval(loadDashboard, 30000); // 30秒刷新
        }

        return () => {
            if (interval) clearInterval(interval);
        };
    }, [loadDashboard, autoRefresh]);

    // 测试 HOPE 查询
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

    // 重置指标
    const handleResetMetrics = async () => {
        if (!confirm('确定要重置 HOPE 监控指标吗？')) return;

        try {
            await window.api.resetHOPEMetrics();
            await loadDashboard();
            showToast('指标已重置', 'success');
        } catch (err) {
            showToast('重置失败: ' + err.message, 'error');
        }
    };

    // 获取健康状态颜色
    const getHealthColor = (status) => {
        switch (status) {
            case 'healthy': return '#10b981';
            case 'warning': return '#f59e0b';
            case 'unhealthy': return '#ef4444';
            default: return '#6b7280';
        }
    };

    // 格式化百分比
    const formatPercent = (value) => {
        return (value * 100).toFixed(1) + '%';
    };

    // 格式化时间
    const formatTime = (ms) => {
        if (ms < 1000) return ms.toFixed(0) + 'ms';
        return (ms / 1000).toFixed(2) + 's';
    };

    if (collapsed) {
        return (
            <div
                className="hope-dashboard-collapsed"
                onClick={onToggle}
                style={{
                    padding: '10px',
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    color: 'white',
                    borderRadius: '8px',
                    cursor: 'pointer',
                    textAlign: 'center'
                }}
            >
                <span>🧠 HOPE</span>
                {dashboard?.health && (
                    <span
                        style={{
                            display: 'inline-block',
                            width: '8px',
                            height: '8px',
                            borderRadius: '50%',
                            backgroundColor: getHealthColor(dashboard.health.status),
                            marginLeft: '8px'
                        }}
                    />
                )}
            </div>
        );
    }

    return (
        <div className="hope-dashboard-panel" style={{
            background: 'white',
            borderRadius: '12px',
            boxShadow: '0 4px 6px -1px rgba(0, 0, 0, 0.1)',
            overflow: 'hidden'
        }}>
            {/* 头部 */}
            <div style={{
                background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                padding: '16px 20px',
                color: 'white',
                display: 'flex',
                justifyContent: 'space-between',
                alignItems: 'center'
            }}>
                <div>
                    <h3 style={{ margin: 0, fontSize: '18px', fontWeight: '600' }}>
                        🧠 HOPE 三层记忆架构
                    </h3>
                    <p style={{ margin: '4px 0 0', fontSize: '12px', opacity: 0.9 }}>
                        智能知识缓存与学习系统
                    </p>
                </div>
                <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
                    <label style={{ fontSize: '12px', cursor: 'pointer' }}>
                        <input
                            type="checkbox"
                            checked={autoRefresh}
                            onChange={(e) => setAutoRefresh(e.target.checked)}
                            style={{ marginRight: '4px' }}
                        />
                        自动刷新
                    </label>
                    <button
                        onClick={loadDashboard}
                        style={{
                            background: 'rgba(255,255,255,0.2)',
                            border: 'none',
                            borderRadius: '4px',
                            padding: '4px 8px',
                            color: 'white',
                            cursor: 'pointer'
                        }}
                    >
                        🔄
                    </button>
                    {onToggle && (
                        <button
                            onClick={onToggle}
                            style={{
                                background: 'rgba(255,255,255,0.2)',
                                border: 'none',
                                borderRadius: '4px',
                                padding: '4px 8px',
                                color: 'white',
                                cursor: 'pointer'
                            }}
                        >
                            ✕
                        </button>
                    )}
                </div>
            </div>

            {/* 内容区 */}
            <div style={{ padding: '16px' }}>
                {loading ? (
                    <div style={{ textAlign: 'center', padding: '40px', color: '#6b7280' }}>
                        加载中...
                    </div>
                ) : error ? (
                    <div style={{
                        textAlign: 'center',
                        padding: '20px',
                        color: '#ef4444',
                        background: '#fef2f2',
                        borderRadius: '8px'
                    }}>
                        {error}
                    </div>
                ) : !dashboard?.enabled ? (
                    <div style={{
                        textAlign: 'center',
                        padding: '40px',
                        color: '#6b7280'
                    }}>
                        HOPE 系统未启用
                    </div>
                ) : (
                    <>
                        {/* 健康状态 */}
                        <div style={{
                            display: 'flex',
                            alignItems: 'center',
                            gap: '12px',
                            marginBottom: '16px',
                            padding: '12px',
                            background: '#f9fafb',
                            borderRadius: '8px'
                        }}>
                            <div style={{
                                width: '48px',
                                height: '48px',
                                borderRadius: '50%',
                                background: getHealthColor(dashboard.health?.status),
                                display: 'flex',
                                alignItems: 'center',
                                justifyContent: 'center',
                                color: 'white',
                                fontSize: '24px'
                            }}>
                                {dashboard.health?.status === 'healthy' ? '✓' :
                                 dashboard.health?.status === 'warning' ? '!' : '✕'}
                            </div>
                            <div>
                                <div style={{ fontWeight: '600', textTransform: 'capitalize' }}>
                                    {dashboard.health?.status || 'Unknown'}
                                </div>
                                {dashboard.health?.issues?.length > 0 && (
                                    <div style={{ fontSize: '12px', color: '#f59e0b' }}>
                                        {dashboard.health.issues.join(', ')}
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* 核心指标 */}
                        <div style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(4, 1fr)',
                            gap: '12px',
                            marginBottom: '16px'
                        }}>
                            {/* LLM 节省率 */}
                            <div style={{
                                background: 'linear-gradient(135deg, #10b981 0%, #059669 100%)',
                                borderRadius: '8px',
                                padding: '12px',
                                color: 'white',
                                textAlign: 'center'
                            }}>
                                <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                                    {formatPercent(dashboard.metrics?.llmSavingsRate || 0)}
                                </div>
                                <div style={{ fontSize: '11px', opacity: 0.9 }}>LLM 节省率</div>
                            </div>

                            {/* 直接回答 */}
                            <div style={{
                                background: 'linear-gradient(135deg, #3b82f6 0%, #2563eb 100%)',
                                borderRadius: '8px',
                                padding: '12px',
                                color: 'white',
                                textAlign: 'center'
                            }}>
                                <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                                    {dashboard.metrics?.directAnswers || 0}
                                </div>
                                <div style={{ fontSize: '11px', opacity: 0.9 }}>直接回答</div>
                            </div>

                            {/* 平均响应时间 */}
                            <div style={{
                                background: 'linear-gradient(135deg, #8b5cf6 0%, #7c3aed 100%)',
                                borderRadius: '8px',
                                padding: '12px',
                                color: 'white',
                                textAlign: 'center'
                            }}>
                                <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                                    {formatTime(dashboard.metrics?.avgResponseTimeMs || 0)}
                                </div>
                                <div style={{ fontSize: '11px', opacity: 0.9 }}>平均响应</div>
                            </div>

                            {/* 总查询数 */}
                            <div style={{
                                background: 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
                                borderRadius: '8px',
                                padding: '12px',
                                color: 'white',
                                textAlign: 'center'
                            }}>
                                <div style={{ fontSize: '24px', fontWeight: 'bold' }}>
                                    {dashboard.metrics?.totalQueries || 0}
                                </div>
                                <div style={{ fontSize: '11px', opacity: 0.9 }}>总查询数</div>
                            </div>
                        </div>

                        {/* 三层统计 */}
                        <div style={{ marginBottom: '16px' }}>
                            <h4 style={{ margin: '0 0 8px', fontSize: '14px', color: '#374151' }}>
                                📊 三层命中统计
                            </h4>
                            <div style={{
                                display: 'grid',
                                gridTemplateColumns: 'repeat(3, 1fr)',
                                gap: '8px'
                            }}>
                                <div style={{
                                    padding: '10px',
                                    background: '#fef3c7',
                                    borderRadius: '6px',
                                    textAlign: 'center'
                                }}>
                                    <div style={{ fontSize: '18px', fontWeight: 'bold', color: '#d97706' }}>
                                        {dashboard.metrics?.permanentHits || 0}
                                    </div>
                                    <div style={{ fontSize: '11px', color: '#92400e' }}>低频层</div>
                                </div>
                                <div style={{
                                    padding: '10px',
                                    background: '#dbeafe',
                                    borderRadius: '6px',
                                    textAlign: 'center'
                                }}>
                                    <div style={{ fontSize: '18px', fontWeight: 'bold', color: '#2563eb' }}>
                                        {dashboard.metrics?.ordinaryHits || 0}
                                    </div>
                                    <div style={{ fontSize: '11px', color: '#1e40af' }}>中频层</div>
                                </div>
                                <div style={{
                                    padding: '10px',
                                    background: '#dcfce7',
                                    borderRadius: '6px',
                                    textAlign: 'center'
                                }}>
                                    <div style={{ fontSize: '18px', fontWeight: 'bold', color: '#16a34a' }}>
                                        {dashboard.metrics?.highFreqHits || 0}
                                    </div>
                                    <div style={{ fontSize: '11px', color: '#166534' }}>高频层</div>
                                </div>
                            </div>
                        </div>

                        {/* 优化建议 */}
                        {dashboard.suggestions?.length > 0 && (
                            <div style={{ marginBottom: '16px' }}>
                                <h4 style={{ margin: '0 0 8px', fontSize: '14px', color: '#374151' }}>
                                    💡 优化建议
                                </h4>
                                <div style={{
                                    background: '#f0fdf4',
                                    borderRadius: '6px',
                                    padding: '10px'
                                }}>
                                    {dashboard.suggestions.map((suggestion, index) => (
                                        <div key={index} style={{
                                            fontSize: '12px',
                                            color: '#166534',
                                            marginBottom: index < dashboard.suggestions.length - 1 ? '6px' : 0
                                        }}>
                                            {suggestion}
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* 测试查询 */}
                        <div style={{
                            borderTop: '1px solid #e5e7eb',
                            paddingTop: '16px'
                        }}>
                            <h4 style={{ margin: '0 0 8px', fontSize: '14px', color: '#374151' }}>
                                🧪 测试 HOPE 查询
                            </h4>
                            <div style={{ display: 'flex', gap: '8px' }}>
                                <input
                                    type="text"
                                    value={testQuestion}
                                    onChange={(e) => setTestQuestion(e.target.value)}
                                    placeholder="输入测试问题..."
                                    onKeyPress={(e) => e.key === 'Enter' && handleTestQuery()}
                                    style={{
                                        flex: 1,
                                        padding: '8px 12px',
                                        border: '1px solid #d1d5db',
                                        borderRadius: '6px',
                                        fontSize: '13px'
                                    }}
                                />
                                <button
                                    onClick={handleTestQuery}
                                    disabled={testLoading || !testQuestion.trim()}
                                    style={{
                                        padding: '8px 16px',
                                        background: testLoading ? '#9ca3af' : '#667eea',
                                        color: 'white',
                                        border: 'none',
                                        borderRadius: '6px',
                                        cursor: testLoading ? 'not-allowed' : 'pointer',
                                        fontSize: '13px'
                                    }}
                                >
                                    {testLoading ? '...' : '测试'}
                                </button>
                            </div>

                            {testResult && (
                                <div style={{
                                    marginTop: '8px',
                                    padding: '10px',
                                    background: testResult.error ? '#fef2f2' : '#f0fdf4',
                                    borderRadius: '6px',
                                    fontSize: '12px'
                                }}>
                                    {testResult.error ? (
                                        <span style={{ color: '#dc2626' }}>{testResult.error}</span>
                                    ) : (
                                        <>
                                            <div><strong>需要 LLM:</strong> {testResult.needsLLM ? '是' : '否'}</div>
                                            <div><strong>来源层:</strong> {testResult.sourceLayer || '-'}</div>
                                            <div><strong>置信度:</strong> {testResult.confidence?.toFixed(2) || '-'}</div>
                                            <div><strong>策略:</strong> {testResult.strategy}</div>
                                            <div><strong>耗时:</strong> {testResult.processingTimeMs}ms</div>
                                            {testResult.answer && (
                                                <div style={{
                                                    marginTop: '6px',
                                                    paddingTop: '6px',
                                                    borderTop: '1px solid #d1d5db'
                                                }}>
                                                    <strong>答案:</strong> {testResult.answer.substring(0, 200)}
                                                    {testResult.answer.length > 200 ? '...' : ''}
                                                </div>
                                            )}
                                        </>
                                    )}
                                </div>
                            )}
                        </div>

                        {/* 操作按钮 */}
                        <div style={{
                            marginTop: '16px',
                            paddingTop: '16px',
                            borderTop: '1px solid #e5e7eb',
                            display: 'flex',
                            justifyContent: 'flex-end',
                            gap: '8px'
                        }}>
                            <button
                                onClick={handleResetMetrics}
                                style={{
                                    padding: '6px 12px',
                                    background: '#f3f4f6',
                                    color: '#374151',
                                    border: '1px solid #d1d5db',
                                    borderRadius: '4px',
                                    cursor: 'pointer',
                                    fontSize: '12px'
                                }}
                            >
                                重置指标
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

