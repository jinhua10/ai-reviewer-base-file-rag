/**
 * LLM Results Tab Component / LLM 结果标签页组件
 * 展示和管理 LLM 分析结果历史
 *
 * @author AI Reviewer Team
 * @since 2025-12-05
 */

function LLMResultsTab() {
    const { useState, useEffect } = React;
    const { t } = window.LanguageModule ? window.LanguageModule.useTranslation() : { t: (k) => k };

    // 状态管理
    const [results, setResults] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [selectedResult, setSelectedResult] = useState(null);
    const [previewContent, setPreviewContent] = useState('');
    const [previewLoading, setPreviewLoading] = useState(false);

    // 加载历史记录
    const loadHistory = async () => {
        setLoading(true);
        setError(null);
        try {
            const response = await window.api.getLLMResultHistory(50);
            if (response.success) {
                setResults(response.documents || []);
            } else {
                setError(response.error || '加载失败');
            }
        } catch (err) {
            console.error('加载 LLM 结果历史失败:', err);
            setError(err.message || '加载失败');
        } finally {
            setLoading(false);
        }
    };

    // 初始加载
    useEffect(() => {
        loadHistory();
    }, []);

    // 预览文档
    const handlePreview = async (result) => {
        setSelectedResult(result);
        setPreviewLoading(true);
        try {
            const content = await window.api.previewLLMResult(result.id);
            setPreviewContent(content);
        } catch (err) {
            console.error('预览失败:', err);
            setPreviewContent('预览加载失败: ' + err.message);
        } finally {
            setPreviewLoading(false);
        }
    };

    // 下载 Markdown
    const handleDownloadMarkdown = async (result) => {
        try {
            await window.api.downloadLLMResultMarkdown(result.id, result.fileName + '.md');
            showToast('下载成功', 'success');
        } catch (err) {
            console.error('下载失败:', err);
            showToast('下载失败: ' + err.message, 'error');
        }
    };

    // 下载 PDF
    const handleDownloadPdf = async (result) => {
        try {
            await window.api.downloadLLMResultPdf(result.id, result.fileName + '.pdf');
            showToast('下载成功', 'success');
        } catch (err) {
            console.error('下载失败:', err);
            showToast('下载失败: ' + err.message, 'error');
        }
    };

    // 删除文档
    const handleDelete = async (result) => {
        if (!confirm(`确定要删除 "${result.fileName}" 吗？`)) {
            return;
        }

        try {
            const response = await window.api.deleteLLMResult(result.id);
            if (response.success) {
                showToast('删除成功', 'success');
                loadHistory(); // 重新加载列表
                if (selectedResult && selectedResult.id === result.id) {
                    setSelectedResult(null);
                    setPreviewContent('');
                }
            } else {
                showToast('删除失败: ' + response.error, 'error');
            }
        } catch (err) {
            console.error('删除失败:', err);
            showToast('删除失败: ' + err.message, 'error');
        }
    };

    // 关闭预览
    const handleClosePreview = () => {
        setSelectedResult(null);
        setPreviewContent('');
    };

    // 格式化时间
    const formatTime = (timeStr) => {
        if (!timeStr) return '-';
        try {
            const date = new Date(timeStr);
            return date.toLocaleString('zh-CN', {
                year: 'numeric',
                month: '2-digit',
                day: '2-digit',
                hour: '2-digit',
                minute: '2-digit'
            });
        } catch (e) {
            return timeStr;
        }
    };

    // 获取分析类型图标
    const getTypeIcon = (type) => {
        switch (type) {
            case '问答': return '💬';
            case '文档分析': return '📄';
            case '图片分析': return '🖼️';
            case '渐进式分析': return '📊';
            default: return '📝';
        }
    };

    // 渲染 Markdown
    const renderMarkdown = (content) => {
        if (typeof marked !== 'undefined') {
            return { __html: marked.parse(content || '') };
        }
        return { __html: content || '' };
    };

    return (
        <div className="llm-results-tab">
            {/* 标题和操作栏 */}
            <div className="llm-results-header">
                <h2>
                    <span className="icon">📚</span>
                    LLM 分析结果历史
                </h2>
                <div className="header-actions">
                    <button
                        className="btn btn-secondary"
                        onClick={loadHistory}
                        disabled={loading}
                    >
                        🔄 刷新
                    </button>
                </div>
            </div>

            {/* 错误提示 */}
            {error && (
                <div className="alert alert-error">
                    <span className="icon">⚠️</span>
                    {error}
                </div>
            )}

            {/* 主内容区 */}
            <div className="llm-results-content">
                {/* 左侧列表 */}
                <div className="results-list-panel">
                    {loading ? (
                        <div className="loading-state">
                            <div className="spinner"></div>
                            <p>加载中...</p>
                        </div>
                    ) : results.length === 0 ? (
                        <div className="empty-state">
                            <span className="icon">📭</span>
                            <p>暂无分析结果</p>
                            <p className="hint">进行问答或文档分析后，结果会自动保存在这里</p>
                        </div>
                    ) : (
                        <div className="results-list">
                            {results.map((result) => (
                                <div
                                    key={result.id}
                                    className={`result-item ${selectedResult?.id === result.id ? 'selected' : ''}`}
                                    onClick={() => handlePreview(result)}
                                >
                                    <div className="result-icon">
                                        {getTypeIcon(result.analysisType)}
                                    </div>
                                    <div className="result-info">
                                        <div className="result-title">
                                            {result.fileName || result.id}
                                        </div>
                                        <div className="result-meta">
                                            <span className="type-badge">{result.analysisType}</span>
                                            <span className="time">{formatTime(result.createdAt)}</span>
                                        </div>
                                        {result.summary && (
                                            <div className="result-summary">
                                                {result.summary.length > 80
                                                    ? result.summary.substring(0, 80) + '...'
                                                    : result.summary}
                                            </div>
                                        )}
                                    </div>
                                    <div className="result-actions">
                                        <button
                                            className="btn-icon"
                                            title="下载 Markdown"
                                            onClick={(e) => { e.stopPropagation(); handleDownloadMarkdown(result); }}
                                        >
                                            📥
                                        </button>
                                        <button
                                            className="btn-icon"
                                            title="删除"
                                            onClick={(e) => { e.stopPropagation(); handleDelete(result); }}
                                        >
                                            🗑️
                                        </button>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                {/* 右侧预览 */}
                <div className="preview-panel">
                    {selectedResult ? (
                        <>
                            <div className="preview-header">
                                <h3>{selectedResult.fileName || selectedResult.id}</h3>
                                <div className="preview-actions">
                                    <button
                                        className="btn btn-primary btn-sm"
                                        onClick={() => handleDownloadMarkdown(selectedResult)}
                                    >
                                        📥 下载 Markdown
                                    </button>
                                    <button
                                        className="btn btn-secondary btn-sm"
                                        onClick={() => handleDownloadPdf(selectedResult)}
                                    >
                                        📄 下载 PDF
                                    </button>
                                    <button
                                        className="btn-icon"
                                        onClick={handleClosePreview}
                                        title="关闭"
                                    >
                                        ✕
                                    </button>
                                </div>
                            </div>
                            <div className="preview-meta">
                                {selectedResult.sourceDocument && (
                                    <span><strong>源文档:</strong> {selectedResult.sourceDocument}</span>
                                )}
                                {selectedResult.question && (
                                    <span><strong>问题:</strong> {selectedResult.question}</span>
                                )}
                                <span><strong>类型:</strong> {selectedResult.analysisType}</span>
                                <span><strong>时间:</strong> {formatTime(selectedResult.createdAt)}</span>
                            </div>
                            <div className="preview-content">
                                {previewLoading ? (
                                    <div className="loading-state">
                                        <div className="spinner"></div>
                                        <p>加载预览...</p>
                                    </div>
                                ) : (
                                    <div
                                        className="markdown-body"
                                        dangerouslySetInnerHTML={renderMarkdown(previewContent)}
                                    />
                                )}
                            </div>
                        </>
                    ) : (
                        <div className="empty-preview">
                            <span className="icon">👈</span>
                            <p>选择左侧的结果查看详情</p>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
}

// 显示 Toast 消息（如果不存在则创建简单版本）
if (typeof showToast === 'undefined') {
    window.showToast = function(message, type) {
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.textContent = message;
        toast.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 12px 24px;
            border-radius: 8px;
            color: white;
            font-weight: 500;
            z-index: 10000;
            animation: slideIn 0.3s ease;
            background: ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : '#3b82f6'};
        `;
        document.body.appendChild(toast);
        setTimeout(() => {
            toast.style.animation = 'slideOut 0.3s ease';
            setTimeout(() => toast.remove(), 300);
        }, 3000);
    };
}

// 导出组件
if (typeof window !== 'undefined') {
    window.LLMResultsTab = LLMResultsTab;
}

