/**
 * QA Tab Component / QA 标签页组件
 * JSX 版本 - 使用 Babel 转译
 * 负责问答功能、文档下载、反馈系统
 *
 * @author AI Reviewer Team
 * @since 2025-11-28
 */

function QATab() {
    const { useState, useEffect } = React;
    const { t } = window.LanguageModule.useTranslation();

    // 状态管理
    const [question, setQuestion] = useState('');
    const [answer, setAnswer] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // 反馈相关状态
    const [feedbackRating, setFeedbackRating] = useState(0);
    const [feedbackComment, setFeedbackComment] = useState('');
    const [feedbackSubmitted, setFeedbackSubmitted] = useState(false);
    const [documentFeedbacks, setDocumentFeedbacks] = useState({});
    const [showReasonModal, setShowReasonModal] = useState(false);
    const [currentFeedbackDoc, setCurrentFeedbackDoc] = useState(null);

    // ============================================================================
    // 副作用 / Effects
    // ============================================================================

    // 配置 marked
    useEffect(() => {
        if (typeof marked !== 'undefined') {
            marked.setOptions({
                breaks: true,
                gfm: true,
                headerIds: true,
                mangle: false
            });
        }
    }, []);

    // 当答案更新时，高亮代码块
    useEffect(() => {
        if (answer && typeof hljs !== 'undefined') {
            document.querySelectorAll('.answer-text pre code').forEach((block) => {
                hljs.highlightElement(block);
            });
        }
    }, [answer]);

    // 图片点击放大
    useEffect(() => {
        const handleImageClick = (e) => {
            if (e.target.tagName === 'IMG' && e.target.closest('.answer-text')) {
                showImageModal(e.target.src, e.target.alt);
            }
        };

        document.addEventListener('click', handleImageClick);
        return () => document.removeEventListener('click', handleImageClick);
    }, []);

    // ============================================================================
    // 核心功能函数
    // ============================================================================

    const handleAsk = async () => {
        if (!question.trim()) {
            alert(t('qaInputError'));
            return;
        }

        setLoading(true);
        setError(null);
        setAnswer(null);
        setFeedbackSubmitted(false);
        setFeedbackRating(0);
        setFeedbackComment('');
        setDocumentFeedbacks({});

        try {
            const result = await window.api.ask(question);
            setAnswer(result);
        } catch (err) {
            setError(err.message || t('qaRequestError'));
        } finally {
            setLoading(false);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleAsk();
        }
    };

    const handleDownload = async (fileName) => {
        try {
            const blob = await window.api.downloadDocument(fileName);
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (err) {
            alert(t('qaDownloadError') + ': ' + err.message);
        }
    };

    const handleBatchDownload = async () => {
        if (!answer || !answer.sources || answer.sources.length === 0) return;

        try {
            const blob = await window.api.downloadBatch(answer.sources);
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `references_${new Date().getTime()}.zip`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (err) {
            alert(t('qaBatchDownloadError') + ': ' + err.message);
        }
    };

    const handleChunkDownload = async (documentId, chunkId, buttonElement) => {
        try {
            buttonElement.classList.add('downloading');

            const response = await fetch(`/api/chunks/download/${encodeURIComponent(documentId)}/${encodeURIComponent(chunkId)}`);
            if (!response.ok) throw new Error(t('qaChunkDownloadError'));

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `${chunkId}.md`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            setTimeout(() => buttonElement.classList.remove('downloading'), 600);
        } catch (err) {
            buttonElement.classList.remove('downloading');
            alert(t('qaChunkDownloadError') + ': ' + err.message);
        }
    };

    const handleBatchDownloadChunks = async () => {
        if (!answer || !answer.chunks || answer.chunks.length === 0) {
            alert(t('qaChunkDownloadError'));
            return;
        }

        try {
            let successCount = 0;
            let failCount = 0;

            for (let i = 0; i < answer.chunks.length; i++) {
                const chunk = answer.chunks[i];
                try {
                    const response = await fetch(`/api/chunks/download/${encodeURIComponent(chunk.documentId)}/${encodeURIComponent(chunk.chunkId)}`);
                    if (!response.ok) throw new Error('Download failed');

                    const blob = await response.blob();
                    const url = window.URL.createObjectURL(blob);
                    const link = document.createElement('a');
                    link.href = url;
                    link.download = `${chunk.title || 'chunk_' + (chunk.chunkIndex + 1)}.md`;
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);

                    successCount++;

                    if (i < answer.chunks.length - 1) {
                        await new Promise(resolve => setTimeout(resolve, 300));
                    }
                } catch (err) {
                    console.error(`Failed to download chunk ${chunk.chunkId}:`, err);
                    failCount++;
                }
            }

            if (failCount > 0) {
                alert(`${successCount}/${answer.chunks.length} ${t('docsUploadSuccess')}, ${failCount} ${t('docsUploadError')}`);
            } else {
                alert(`${successCount}/${answer.chunks.length} ${t('docsUploadSuccess')}`);
            }
        } catch (err) {
            alert(t('qaChunkDownloadError') + ': ' + err.message);
        }
    };

    const showImageModal = (src, alt) => {
        const modal = document.createElement('div');
        modal.className = 'image-modal active';
        modal.innerHTML = `
            <div class="image-modal-content">
                <button class="image-modal-close" aria-label="${t('qaImageClose')}">&times;</button>
                <img src="${src}" alt="${alt || t('qaImageAlt')}" />
                ${alt ? `<div class="image-caption">${alt}</div>` : ''}
            </div>
        `;

        modal.onclick = (e) => {
            if (e.target === modal || e.target.classList.contains('image-modal-close')) {
                modal.classList.remove('active');
                setTimeout(() => modal.remove(), 300);
            }
        };

        const handleEsc = (e) => {
            if (e.key === 'Escape') {
                modal.classList.remove('active');
                setTimeout(() => modal.remove(), 300);
                document.removeEventListener('keydown', handleEsc);
            }
        };
        document.addEventListener('keydown', handleEsc);

        document.body.appendChild(modal);
    };

    // ============================================================================
    // 反馈功能函数
    // ============================================================================

    const handleSubmitFeedback = async () => {
        if (feedbackRating === 0) {
            alert(t('feedbackPleaseRate'));
            return;
        }

        try {
            const result = await window.api.submitOverallFeedback(
                answer.recordId || Date.now().toString(),
                feedbackRating,
                feedbackComment
            );

            if (result.success) {
                setFeedbackSubmitted(true);
            } else {
                alert(t('feedbackError'));
            }
        } catch (err) {
            console.error('提交反馈失败:', err);
            alert(t('feedbackError'));
        }
    };

    const handleDocumentHelpful = async (docName) => {
        if (documentFeedbacks[docName]) return;

        try {
            const result = await window.api.submitDocumentFeedback(
                answer.recordId || Date.now().toString(),
                docName,
                'HELPFUL',
                null
            );

            if (result.success) {
                setDocumentFeedbacks(prev => ({ ...prev, [docName]: 'HELPFUL' }));
            }
        } catch (err) {
            console.error('提交文档反馈失败:', err);
        }
    };

    const handleDocumentNotHelpful = (docName) => {
        setCurrentFeedbackDoc(docName);
        setShowReasonModal(true);
    };

    const submitDocumentNotHelpfulReason = async (reason) => {
        if (!currentFeedbackDoc) return;

        try {
            const result = await window.api.submitDocumentFeedback(
                answer.recordId || Date.now().toString(),
                currentFeedbackDoc,
                'NOT_HELPFUL',
                reason
            );

            if (result.success) {
                setDocumentFeedbacks(prev => ({ ...prev, [currentFeedbackDoc]: 'NOT_HELPFUL' }));
            }
        } catch (err) {
            console.error('提交文档反馈失败:', err);
        } finally {
            setShowReasonModal(false);
            setCurrentFeedbackDoc(null);
        }
    };

    // ============================================================================
    // 主渲染
    // ============================================================================

    return (
        <div className="qa-section qa-input-section">
            {/* 问题输入区域 */}
            <div className="qa-button-group" style={{marginBottom: '20px'}}>
                <textarea
                    className="qa-question-input input-field"
                    placeholder={t('qaPlaceholder')}
                    value={question}
                    onChange={(e) => setQuestion(e.target.value)}
                    onKeyPress={handleKeyPress}
                    disabled={loading}
                />
                <div className="qa-button-group">
                    <button
                        className="qa-ask-button btn btn-primary"
                        onClick={handleAsk}
                        disabled={loading}
                    >
                        {loading ? t('qaThinking') : t('qaButton')}
                    </button>
                    <button
                        className="qa-clear-button"
                        onClick={() => {
                            setQuestion('');
                            setAnswer(null);
                            setError(null);
                            setFeedbackRating(0);
                            setFeedbackComment('');
                            setFeedbackSubmitted(false);
                            setDocumentFeedbacks({});
                        }}
                        disabled={loading}
                    >
                        {t('clearButton') || '清空'}
                    </button>
                </div>
            </div>

            {/* 加载状态 */}
            {loading && (
                <div className="loading">
                    <div className="spinner"></div>
                    <p>{t('qaAIThinking')}</p>
                </div>
            )}

            {/* 错误状态 */}
            {error && (
                <div className="error">
                    {t('qaErrorPrefix')} {error}
                </div>
            )}

            {/* 答案显示区域 */}
            {answer && !loading && (
                <div className="qa-answer-section">
                    <div className="qa-answer-card answer-card">
                        <div className="qa-answer-header">
                            <h3 className="qa-answer-title">{t('qaAnswer')}</h3>
                            {answer.sources && answer.sources.length > 1 && (
                                <button
                                    className="qa-download-all-btn"
                                    onClick={handleBatchDownload}
                                >
                                    {t('qaBatchDownload')} ({answer.sources.length})
                                </button>
                            )}
                        </div>

                        <div
                            className="qa-answer-text answer-text"
                            dangerouslySetInnerHTML={{
                                __html: typeof marked !== 'undefined'
                                    ? marked.parse(answer.answer)
                                    : answer.answer
                            }}
                        />

                        {/* 参考来源 */}
                        {answer.sources && answer.sources.length > 0 && (
                            <div className="qa-sources-section">
                                <h4 className="qa-sources-title">{t('qaSources')}</h4>
                                <div className="qa-sources-list">
                                    {answer.sources.map((source, index) => (
                                        <div key={index} className="qa-source-item">
                                            <div className="qa-source-info">
                                                <span className="qa-source-name">
                                                    {index + 1}. {source}
                                                </span>
                                            </div>
                                            <div className="qa-source-actions">
                                                <button
                                                    className="qa-source-download-btn"
                                                    onClick={() => handleDownload(source)}
                                                    title={t('qaDownload')}
                                                >
                                                    {t('qaDownload')}
                                                </button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            </div>
                        )}

                        {/* 文档切分块和反馈区域 */}
                        {((answer.chunks && answer.chunks.length > 0) || (answer.sources && answer.sources.length > 0)) && (
                            <div className="chunks-section" style={{marginTop: '20px'}}>
                                <h4>📦 {t('qaChunksAndFeedback')}</h4>

                                {/* 文档切分块 */}
                                {answer.chunks && answer.chunks.length > 0 && (
                                    <>
                                        <div style={{
                                            marginBottom: '10px',
                                            display: 'flex',
                                            justifyContent: 'space-between',
                                            alignItems: 'center'
                                        }}>
                                            <span style={{fontSize: '14px', color: '#666'}}>
                                                {answer.chunks.length} {t('qaChunksAvailable')}
                                            </span>
                                            <button
                                                className="btn-batch-download-chunks"
                                                onClick={handleBatchDownloadChunks}
                                                title={t('qaChunksDownloadAll')}
                                            >
                                                {t('qaChunksDownloadAll')}
                                            </button>
                                        </div>
                                        <div className="chunks-grid">
                                            {answer.chunks.map((chunk) => (
                                                <button
                                                    key={chunk.chunkId}
                                                    className="chunk-button"
                                                    onClick={(e) => handleChunkDownload(chunk.documentId, chunk.chunkId, e.currentTarget)}
                                                    title={`${t('qaChunkDownload')}: ${chunk.title || t('qaChunkTitle') + ' ' + (chunk.chunkIndex + 1)}`}
                                                >
                                                    <div className="chunk-title">
                                                        📄 {chunk.title || `${t('qaChunkTitle')} ${chunk.chunkIndex + 1}`}
                                                    </div>
                                                    <div className="chunk-info">
                                                        <span className="chunk-index">
                                                            {chunk.chunkIndex + 1}/{chunk.totalChunks || answer.chunks.length}
                                                        </span>
                                                        <span className="chunk-size">
                                                            {(chunk.contentLength / 1024).toFixed(1)} KB
                                                        </span>
                                                    </div>
                                                </button>
                                            ))}
                                        </div>
                                    </>
                                )}

                                {/* 文档反馈区域 */}
                                {answer.sources && answer.sources.length > 0 && (
                                    <div
                                        className="document-feedback-area"
                                        style={{marginTop: answer.chunks && answer.chunks.length > 0 ? '20px' : '0'}}
                                    >
                                        <h5 style={{marginBottom: '15px', color: '#1565c0', fontSize: '15px'}}>
                                            📚 {t('feedbackDocumentQuestion')}
                                        </h5>
                                        {answer.sources.map((source, index) => (
                                            <div key={index} className="document-feedback-item">
                                                <div className="document-feedback-name">
                                                    {index + 1}. {source}
                                                </div>
                                                <div className="document-feedback-buttons">
                                                    <button
                                                        className={`qa-feedback-button helpful ${documentFeedbacks[source] === 'HELPFUL' ? 'active' : ''}`}
                                                        onClick={() => handleDocumentHelpful(source)}
                                                        disabled={documentFeedbacks[source] !== undefined}
                                                    >
                                                        {documentFeedbacks[source] === 'HELPFUL'
                                                            ? t('feedbackDocumentSubmitted')
                                                            : t('feedbackDocumentHelpful')}
                                                    </button>
                                                    <button
                                                        className={`qa-feedback-button not-helpful ${documentFeedbacks[source] === 'NOT_HELPFUL' ? 'active' : ''}`}
                                                        onClick={() => handleDocumentNotHelpful(source)}
                                                        disabled={documentFeedbacks[source] !== undefined}
                                                    >
                                                        {documentFeedbacks[source] === 'NOT_HELPFUL'
                                                            ? t('feedbackDocumentSubmitted')
                                                            : t('feedbackDocumentNotHelpful')}
                                                    </button>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}
                            </div>
                        )}

                        {/* 用户反馈区域 */}
                        <div className="qa-feedback-section">
                            {!feedbackSubmitted ? (
                                <>
                                    <h4 className="qa-feedback-title">{t('feedbackQuestion')}</h4>
                                    <div className="qa-rating-stars">
                                        {[1, 2, 3, 4, 5].map(star => (
                                            <span
                                                key={star}
                                                className={`qa-star ${star <= feedbackRating ? 'filled' : 'empty'}`}
                                                onClick={() => setFeedbackRating(star)}
                                            >
                                                ★
                                            </span>
                                        ))}
                                    </div>
                                    <textarea
                                        className="qa-feedback-textarea"
                                        placeholder={t('feedbackCommentPlaceholder')}
                                        value={feedbackComment}
                                        onChange={(e) => setFeedbackComment(e.target.value)}
                                    />
                                    <button
                                        className="qa-feedback-submit-btn"
                                        onClick={handleSubmitFeedback}
                                        disabled={feedbackRating === 0}
                                    >
                                        {t('feedbackSubmit')}
                                    </button>
                                </>
                            ) : (
                                <div className="qa-feedback-success">
                                    {t('feedbackThankYou')}
                                </div>
                            )}
                        </div>

                        {/* 响应时间 */}
                        <div className="response-time">
                            {t('qaResponseTime')}: {answer.responseTimeMs}ms
                        </div>
                    </div>
                </div>
            )}

            {/* 文档反馈原因模态框 */}
            {showReasonModal && (
                <div
                    className="qa-modal-overlay"
                    onClick={() => setShowReasonModal(false)}
                >
                    <div
                        className="qa-modal-content"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <h4 className="qa-modal-title">{t('feedbackDocumentReasonPlaceholder')}</h4>
                        <textarea
                            id="reasonTextarea"
                            className="qa-modal-textarea"
                            placeholder={t('feedbackCommentPlaceholder')}
                            autoFocus
                        />
                        <div className="qa-modal-buttons">
                            <button
                                className="qa-modal-btn qa-modal-btn-secondary"
                                onClick={() => {
                                    setShowReasonModal(false);
                                    setCurrentFeedbackDoc(null);
                                }}
                            >
                                {t('qaImageClose')}
                            </button>
                            <button
                                className="qa-modal-btn qa-modal-btn-primary"
                                onClick={() => {
                                    const textarea = document.getElementById('reasonTextarea');
                                    submitDocumentNotHelpfulReason(textarea.value);
                                }}
                            >
                                {t('feedbackSubmit')}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {/* 空状态 */}
            {!answer && !loading && !error && (
                <div className="empty-state">
                    <div className="empty-state-icon">{t('qaEmptyIcon')}</div>
                    <p>{t('qaEmptyText')}</p>
                    <p style={{fontSize: '14px', marginTop: '10px', color: '#ccc'}}>
                        {t('qaEmptyExample')}
                    </p>
                </div>
            )}
        </div>
    );
}

// 导出到全局
if (typeof window !== 'undefined') {
    window.QATab = QATab;
}

// 如果支持模块导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = QATab;
}

