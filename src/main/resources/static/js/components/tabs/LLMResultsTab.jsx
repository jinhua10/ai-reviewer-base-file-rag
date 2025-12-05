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
                setError(response.error || t('llmResultsLoadError'));
            }
        } catch (err) {
            console.error(t('llmResultsLogLoadHistoryError'), err);
            setError(err.message || t('llmResultsLoadError'));
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
            console.error(t('llmResultsLogPreviewError'), err);
            setPreviewContent(t('llmResultsPreviewError') + err.message);
        } finally {
            setPreviewLoading(false);
        }
    };

    // 下载 Markdown
    const handleDownloadMarkdown = async (result) => {
        try {
            await window.api.downloadLLMResultMarkdown(result.id, result.fileName + '.md');
            showToast(t('llmResultsDownloadSuccess'), 'success');
        } catch (err) {
            console.error(t('llmResultsLogDownloadError'), err);
            showToast(t('llmResultsDownloadError') + err.message, 'error');
        }
    };

    // 下载 PDF（前端生成）
    const handleDownloadPdf = async (result) => {
        try {
            // 检查 html2pdf 是否已加载
            if (typeof html2pdf === 'undefined') {
                showToast('PDF 生成库未加载，请刷新页面重试', 'error');
                return;
            }

            showToast('正在生成 PDF...', 'info');

            // 获取 Markdown 内容
            let markdownContent = previewContent;
            if (!markdownContent || selectedResult?.id !== result.id) {
                markdownContent = await window.api.previewLLMResult(result.id);
            }

            // 将 Markdown 转换为 HTML
            const htmlContent = marked.parse(markdownContent);

            // 创建临时容器
            const container = document.createElement('div');
            container.innerHTML = `
                <div style="padding: 20px; font-family: 'Microsoft YaHei', 'SimSun', sans-serif; line-height: 1.8;">
                    <style>
                        h1 { color: #333; border-bottom: 2px solid #4CAF50; padding-bottom: 10px; }
                        h2 { color: #555; margin-top: 20px; }
                        h3 { color: #666; }
                        blockquote { background: #f9f9f9; border-left: 4px solid #4CAF50; padding: 10px 15px; margin: 10px 0; }
                        code { background: #f4f4f4; padding: 2px 6px; border-radius: 3px; }
                        pre { background: #f4f4f4; padding: 15px; border-radius: 5px; overflow-x: auto; }
                        ul, ol { padding-left: 20px; }
                        hr { border: none; border-top: 1px solid #ddd; margin: 20px 0; }
                        table { border-collapse: collapse; width: 100%; margin: 10px 0; }
                        th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }
                        th { background: #f5f5f5; }
                        img { max-width: 100%; height: auto; }
                    </style>
                    ${htmlContent}
                </div>
            `;

            // 配置 PDF 选项
            const opt = {
                margin: [10, 10, 10, 10],
                filename: (result.fileName || 'llm-result') + '.pdf',
                image: { type: 'jpeg', quality: 0.98 },
                html2canvas: {
                    scale: 2,
                    useCORS: true,
                    letterRendering: true
                },
                jsPDF: {
                    unit: 'mm',
                    format: 'a4',
                    orientation: 'portrait'
                },
                pagebreak: { mode: ['avoid-all', 'css', 'legacy'] }
            };

            // 生成 PDF
            await html2pdf().set(opt).from(container).save();

            showToast(t('llmResultsDownloadSuccess'), 'success');
        } catch (err) {
            console.error(t('llmResultsLogDownloadError'), err);
            showToast(t('llmResultsDownloadError') + err.message, 'error');
        }
    };

    // 删除文档
    const handleDelete = async (result) => {
        if (!confirm(t('llmResultsDeleteConfirm').replace('{0}', result.fileName))) {
            return;
        }

        try {
            const response = await window.api.deleteLLMResult(result.id);
            if (response.success) {
                showToast(t('llmResultsDeleteSuccess'), 'success');
                loadHistory(); // 重新加载列表
                if (selectedResult && selectedResult.id === result.id) {
                    setSelectedResult(null);
                    setPreviewContent('');
                }
            } else {
                showToast(t('llmResultsDeleteError') + response.error, 'error');
            }
        } catch (err) {
            console.error(t('llmResultsLogDeleteError'), err);
            showToast(t('llmResultsDeleteError') + err.message, 'error');
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
        if (!type) return '📝';
        
        const normalizedType = type.toLowerCase().trim();
        
        // 问答类型
        if (normalizedType.includes('问答') || normalizedType.includes('qa') || 
            normalizedType.includes('q&a') || normalizedType.includes('question')) {
            return '💬';
        }
        // 文档分析
        if (normalizedType.includes('文档') || normalizedType.includes('document') || 
            normalizedType.includes('分析') || normalizedType.includes('analysis')) {
            return '📄';
        }
        // 图片分析
        if (normalizedType.includes('图片') || normalizedType.includes('image') || 
            normalizedType.includes('图像') || normalizedType.includes('picture')) {
            return '🖼️';
        }
        // 渐进式分析
        if (normalizedType.includes('渐进') || normalizedType.includes('progressive') || 
            normalizedType.includes('incremental')) {
            return '📊';
        }
        
        return '📝';
    };
    
    // 获取翻译后的类型名称
    const getTranslatedType = (type) => {
        if (!type) return t('llmResultsTypeDefault');
        
        const normalizedType = type.toLowerCase().trim();
        
        if (normalizedType.includes('问答') || normalizedType.includes('qa') || 
            normalizedType.includes('q&a') || normalizedType.includes('question')) {
            return t('llmResultsTypeQA');
        }
        if (normalizedType.includes('文档') || normalizedType.includes('document') || 
            normalizedType.includes('分析') || normalizedType.includes('analysis')) {
            return t('llmResultsTypeDocument');
        }
        if (normalizedType.includes('图片') || normalizedType.includes('image') || 
            normalizedType.includes('图像') || normalizedType.includes('picture')) {
            return t('llmResultsTypeImage');
        }
        if (normalizedType.includes('渐进') || normalizedType.includes('progressive') || 
            normalizedType.includes('incremental')) {
            return t('llmResultsTypeProgressive');
        }
        
        return type || t('llmResultsTypeDefault');
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
                    {t('llmResultsTitle')}
                </h2>
                <div className="header-actions">
                    <button
                        className="btn btn-secondary"
                        onClick={loadHistory}
                        disabled={loading}
                    >
                        🔄 {t('llmResultsRefresh')}
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
                            <p>{t('llmResultsLoading')}</p>
                        </div>
                    ) : results.length === 0 ? (
                        <div className="empty-state">
                            <span className="icon">📭</span>
                            <p>{t('llmResultsEmpty')}</p>
                            <p className="hint">{t('llmResultsEmptyHint')}</p>
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
                                            <span className="type-badge">{getTranslatedType(result.analysisType)}</span>
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
                                            title={t('llmResultsDownloadMarkdown')}
                                            onClick={(e) => { e.stopPropagation(); handleDownloadMarkdown(result); }}
                                        >
                                            📥
                                        </button>
                                        <button
                                            className="btn-icon"
                                            title={t('llmResultsDelete')}
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
                                        📥 {t('llmResultsDownloadMarkdown')}
                                    </button>
                                    <button
                                        className="btn btn-secondary btn-sm"
                                        onClick={() => handleDownloadPdf(selectedResult)}
                                    >
                                        📄 {t('llmResultsDownloadPdf')}
                                    </button>
                                    <button
                                        className="btn-icon"
                                        onClick={handleClosePreview}
                                        title={t('llmResultsClose')}
                                    >
                                        ✕
                                    </button>
                                </div>
                            </div>
                            <div className="preview-meta">
                                {selectedResult.sourceDocument && (
                                    <span><strong>{t('llmResultsSourceDoc')}</strong> {selectedResult.sourceDocument}</span>
                                )}
                                {selectedResult.question && (
                                    <span><strong>{t('llmResultsQuestion')}</strong> {selectedResult.question}</span>
                                )}
                                <span><strong>{t('llmResultsType')}</strong> {getTranslatedType(selectedResult.analysisType)}</span>
                                <span><strong>{t('llmResultsTime')}</strong> {formatTime(selectedResult.createdAt)}</span>
                            </div>
                            <div className="preview-content">
                                {previewLoading ? (
                                    <div className="loading-state">
                                        <div className="spinner"></div>
                                        <p>{t('llmResultsPreviewLoading')}</p>
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
                            <p>{t('llmResultsSelectHint')}</p>
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

