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

    // 分页引用相关状态
    const [sessionId, setSessionId] = useState(null);
    const [sessionInfo, setSessionInfo] = useState(null);
    const [loadingMore, setLoadingMore] = useState(false);

    // 反馈相关状态
    const [feedbackRating, setFeedbackRating] = useState(0);
    const [feedbackComment, setFeedbackComment] = useState('');
    const [feedbackSubmitted, setFeedbackSubmitted] = useState(false);
    const [documentFeedbacks, setDocumentFeedbacks] = useState({});
    const [documentRatings, setDocumentRatings] = useState({}); // 新增：文档星级评价
    const [showReasonModal, setShowReasonModal] = useState(false);
    const [currentFeedbackDoc, setCurrentFeedbackDoc] = useState(null);
    const [showRatingModal, setShowRatingModal] = useState(false); // 新增：星级评价模态框
    const [currentRatingDoc, setCurrentRatingDoc] = useState(null); // 新增：当前评价的文档

    // 相似问题相关状态
    const [expandedSimilarQA, setExpandedSimilarQA] = useState(null); // 展开的相似问题答案

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
        setDocumentRatings({}); // 清除文档星级评价
        setSessionId(null);
        setSessionInfo(null);

        try {
            const result = await window.api.ask(question);
            setAnswer(result);

            // 保存会话ID并获取会话信息
            if (result.sessionId) {
                setSessionId(result.sessionId);
                await fetchSessionInfo(result.sessionId);
            }
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

    // ============================================================================
    // 会话管理和分页引用函数
    // ============================================================================

    const fetchSessionInfo = async (sid) => {
        try {
            const response = await fetch(`/api/search/session/${sid}/info`);
            if (response.ok) {
                const info = await response.json();
                setSessionInfo(info);
            }
        } catch (err) {
            console.error('Failed to fetch session info:', err);
        }
    };

    const handleLoadMore = async () => {
        if (!sessionId || loadingMore) return;

        setLoadingMore(true);
        try {
            // 切换到下一批文档
            const response = await fetch(`/api/search/session/${sessionId}/next`, {
                method: 'POST'
            });

            if (!response.ok) {
                throw new Error('Failed to load more documents');
            }

            const sessionDocs = await response.json();

            // 使用新文档重新生成回答
            const result = await window.api.askWithSessionDocuments(question, sessionId);

            // 更新答案
            setAnswer(result);

            // 更新会话信息
            await fetchSessionInfo(sessionId);

            showToast(t('qaLoadMoreSuccess') || `已加载第 ${sessionDocs.currentPage} 批文档`, 'success');
        } catch (err) {
            console.error('Failed to load more documents:', err);
            showToast(t('qaLoadMoreError') || '加载更多文档失败', 'error');
        } finally {
            setLoadingMore(false);
        }
    };

    const handleLoadPrevious = async () => {
        if (!sessionId || loadingMore) return;

        setLoadingMore(true);
        try {
            // 切换到上一批文档
            const response = await fetch(`/api/search/session/${sessionId}/previous`, {
                method: 'POST'
            });

            if (!response.ok) {
                throw new Error('Failed to load previous documents');
            }

            const sessionDocs = await response.json();

            // 使用新文档重新生成回答
            const result = await window.api.askWithSessionDocuments(question, sessionId);

            // 更新答案
            setAnswer(result);

            // 更新会话信息
            await fetchSessionInfo(sessionId);

            showToast(t('qaLoadPreviousSuccess') || `已加载第 ${sessionDocs.currentPage} 批文档`, 'success');
        } catch (err) {
            console.error('Failed to load previous documents:', err);
            showToast(t('qaLoadPreviousError') || '加载上一批文档失败', 'error');
        } finally {
            setLoadingMore(false);
        }
    };

    // ============================================================================
    // 相似问题处理函数
    // ============================================================================

    const handleToggleSimilarAnswer = (recordId) => {
        setExpandedSimilarQA(prev => prev === recordId ? null : recordId);
    };

    const handleUseSimilarAnswer = (similarQA) => {
        // 提示用户这是历史答案
        showToast(t('qaUsingSimilarAnswer') || '📚 已加载历史答案供您参考', 'info');

        // 滚动到答案区域
        setTimeout(() => {
            const answerElement = document.querySelector('.qa-answer-text');
            if (answerElement) {
                answerElement.scrollIntoView({ behavior: 'smooth', block: 'start' });
            }
        }, 100);
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
                    console.error(`${t('logChunkDownloadFailed')} ${chunk.chunkId}:`, err);
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
            console.error(t('logFeedbackError'), err);
            alert(t('feedbackError'));
        }
    };

    const handleDocumentHelpful = async (docName) => {
        if (documentFeedbacks[docName]) return;

        try {
            const result = await window.api.submitDocumentFeedback(
                answer.recordId || Date.now().toString(),
                docName,
                'LIKE',  // 修改为 LIKE
                null
            );

            if (result.success) {
                setDocumentFeedbacks(prev => ({ ...prev, [docName]: 'LIKE' }));
                // 显示成功提示
                const message = t('feedbackSubmitSuccess');

                // 使用非阻塞的提示
                showToast(message, 'success');
            } else {
                const errorMsg = t('feedbackSubmitError');
                showToast(errorMsg, 'error');
            }
        } catch (err) {
            console.error(t('logDocumentFeedbackError'), err);
            const errorMsg = t('feedbackSubmitError') + ': ' + (err.message || t('networkError'));
            showToast(errorMsg, 'error');
        }
    };

    const handleDocumentNotHelpful = (docName) => {
        if (documentFeedbacks[docName]) return;
        setCurrentFeedbackDoc(docName);
        setShowReasonModal(true);
    };

    const submitDocumentNotHelpfulReason = async (reason) => {
        if (!currentFeedbackDoc) return;

        try {
            const result = await window.api.submitDocumentFeedback(
                answer.recordId || Date.now().toString(),
                currentFeedbackDoc,
                'DISLIKE',  // 修改为 DISLIKE
                reason
            );

            if (result.success) {
                setDocumentFeedbacks(prev => ({ ...prev, [currentFeedbackDoc]: 'DISLIKE' }));
                // 显示成功提示
                const message = t('feedbackSubmitSuccess');
                showToast(message, 'success');
            } else {
                const errorMsg = t('feedbackSubmitError');
                showToast(errorMsg, 'error');
            }
        } catch (err) {
            console.error(t('logDocumentFeedbackError'), err);
            const errorMsg = t('feedbackSubmitError') + ': ' + (err.message || t('networkError'));
            showToast(errorMsg, 'error');
        } finally {
            setShowReasonModal(false);
            setCurrentFeedbackDoc(null);
        }
    };

    // ============================================================================
    // 星级评价功能函数
    // ============================================================================

    const handleDocumentRate = (docName) => {
        if (documentRatings[docName]) return; // 已经评价过
        setCurrentRatingDoc(docName);
        setFeedbackComment(''); // 清空评论
        setShowRatingModal(true);
    };

    const [tempRating, setTempRating] = useState(0); // 临时评分

    const submitDocumentRating = async (rating, comment) => {
        if (!currentRatingDoc || rating === 0) {
            showToast(t('qaRatingSelectStar'), 'error');
            return;
        }

        try {
            const result = await window.api.rateDocumentQuality(
                answer.recordId || Date.now().toString(),
                currentRatingDoc,
                rating,
                comment
            );

            if (result.success) {
                setDocumentRatings(prev => ({ ...prev, [currentRatingDoc]: rating }));

                // 显示成功提示（包含影响说明）
                const message = result.impact || t('feedbackSubmitSuccess');
                showToast(message, 'success');

                setShowRatingModal(false);
                setCurrentRatingDoc(null);
                setTempRating(0);
                setFeedbackComment('');
            } else {
                const errorMsg = t('feedbackSubmitError');
                showToast(errorMsg, 'error');
            }
        } catch (err) {
            console.error(t('logDocumentRatingError'), err);
            const errorMsg = t('feedbackSubmitError') + ': ' + (err.message || t('networkError'));
            showToast(errorMsg, 'error');
        }
    };

    // Toast 提示函数
    const showToast = (message, type = 'info') => {
        const toast = document.createElement('div');
        toast.className = `toast-notification toast-${type}`;

        // 使用innerHTML避免CSS ::before图标被覆盖
        const icon = type === 'success' ? '✅' : type === 'error' ? '❌' : 'ℹ️';
        toast.innerHTML = `<span class="toast-icon">${icon}</span><span class="toast-message">${message}</span>`;

        toast.style.cssText = `
            position: fixed;
            top: 20px;
            right: 20px;
            padding: 12px 20px;
            background: ${type === 'success' ? '#4caf50' : type === 'error' ? '#f44336' : '#2196f3'};
            color: white;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.3);
            z-index: 10001;
            font-size: 14px;
            font-weight: 500;
            animation: slideInRight 0.3s ease-out;
            max-width: 400px;
            display: flex;
            align-items: center;
            gap: 8px;
        `;

        document.body.appendChild(toast);

        // 3秒后自动消失
        setTimeout(() => {
            toast.style.animation = 'slideOutRight 0.3s ease-out';
            setTimeout(() => {
                if (document.body.contains(toast)) {
                    document.body.removeChild(toast);
                }
            }, 300);
        }, 3000);
    };

    // ============================================================================
    // 主渲染
    // ============================================================================

    return (
        <div className="qa-section qa-input-section">
            {/* 问题输入区域 */}
            <div className="qa-input-container">
                <textarea
                    className="qa-question-input"
                    placeholder={t('qaPlaceholder')}
                    value={question}
                    onChange={(e) => setQuestion(e.target.value)}
                    onKeyPress={handleKeyPress}
                    disabled={loading}
                    rows={4}
                />
                <div className="qa-action-buttons">
                    <button
                        className="qa-ask-button btn btn-primary"
                        onClick={handleAsk}
                        disabled={loading}
                    >
                        {loading ? t('qaThinking') : t('qaButton')}
                    </button>
                    <button
                        className="qa-clear-button btn btn-secondary"
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
                        {t('docsClearButton')}
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
                    {/* 相似问题推荐（如果有） */}
                    {answer.similarQuestions && answer.similarQuestions.length > 0 && (
                        <div className="qa-similar-questions-panel">
                            <div className="qa-similar-header">
                                <h4 className="qa-similar-title">
                                    💡 {t('qaSimilarQuestions') || '您可能想问'}
                                </h4>
                                <span className="qa-similar-hint">
                                    {t('qaSimilarHint') || '以下是相似的历史高质量问答'}
                                </span>
                            </div>
                            <div className="qa-similar-list">
                                {answer.similarQuestions.map((sq, index) => (
                                    <div key={sq.recordId} className="qa-similar-item">
                                        <div className="qa-similar-item-header">
                                            <div className="qa-similar-badges">
                                                <span className="qa-similar-badge qa-similar-badge-similarity">
                                                    {t('qaSimilarity') || '相似度'} {(sq.similarity * 100).toFixed(0)}%
                                                </span>
                                                <span className="qa-similar-badge qa-similar-badge-rating">
                                                    {'⭐'.repeat(sq.rating)}
                                                </span>
                                            </div>
                                            <span className="qa-similar-index">#{index + 1}</span>
                                        </div>

                                        <div className="qa-similar-question">
                                            <strong>{t('qaQuestion') || '问题'}：</strong>
                                            {sq.question}
                                        </div>

                                        {expandedSimilarQA === sq.recordId && (
                                            <div className="qa-similar-answer">
                                                <strong>{t('qaAnswer') || '回答'}：</strong>
                                                <div
                                                    className="qa-similar-answer-content"
                                                    dangerouslySetInnerHTML={{
                                                        __html: typeof marked !== 'undefined'
                                                            ? marked.parse(sq.answer)
                                                            : sq.answer
                                                    }}
                                                />
                                            </div>
                                        )}

                                        <div className="qa-similar-actions">
                                            <button
                                                className="qa-similar-btn qa-similar-btn-toggle"
                                                onClick={() => handleToggleSimilarAnswer(sq.recordId)}
                                            >
                                                {expandedSimilarQA === sq.recordId
                                                    ? (t('qaCollapseAnswer') || '收起答案 ▲')
                                                    : (t('qaExpandAnswer') || '查看答案 ▼')
                                                }
                                            </button>
                                            {expandedSimilarQA === sq.recordId && (
                                                <button
                                                    className="qa-similar-btn qa-similar-btn-use"
                                                    onClick={() => handleUseSimilarAnswer(sq)}
                                                >
                                                    ✓ {t('qaUseThisAnswer') || '采用此答案'}
                                                </button>
                                            )}
                                        </div>
                                    </div>
                                ))}
                            </div>
                            <div className="qa-similar-footer">
                                <p className="qa-similar-note">
                                    ℹ️ {t('qaSimilarNote') || '这些是之前其他用户高评分的问答，供您参考。您也可以继续查看下方AI的新回答。'}
                                </p>
                            </div>
                        </div>
                    )}

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

                        {/* 会话信息和分页控制 */}
                        {sessionInfo && (
                            <div className="qa-session-info">
                                <div className="qa-session-stats">
                                    <span className="qa-session-stat">
                                        {t('qaSessionTotalDocs')} <strong>{sessionInfo.totalDocuments}</strong> {t('qaSessionDocsCount')}
                                    </span>
                                    <span className="qa-session-stat">
                                        {t('qaSessionCurrentUsed')} <strong>{answer.usedDocuments?.length || sessionInfo.documentsPerQuery}</strong> {t('qaSessionUsedCount')}
                                    </span>
                                    {sessionInfo.remainingDocuments > 0 && (
                                        <span className="qa-session-stat">
                                            {t('qaSessionRemaining')} <strong>{sessionInfo.remainingDocuments}</strong> {t('qaSessionRemainingCount')}
                                        </span>
                                    )}
                                    <span className="qa-session-stat">
                                        {t('qaSessionPageInfo')} <strong>{sessionInfo.currentPage}</strong> {t('qaSessionPageOf')} <strong>{sessionInfo.totalPages}</strong> {t('qaSessionPageSuffix')}
                                    </span>
                                </div>

                                {/* 分页控制按钮 */}
                                {(sessionInfo.hasPrevious || sessionInfo.hasNext) && (
                                    <div className="qa-pagination-controls">
                                        <button
                                            className="qa-pagination-btn"
                                            onClick={handleLoadPrevious}
                                            disabled={!sessionInfo.hasPrevious || loadingMore}
                                        >
                                            {t('qaPreviousBatch')}
                                        </button>

                                        <span className="qa-pagination-info">
                                            {sessionInfo.currentPage} {t('qaSessionPageOf')} {sessionInfo.totalPages}
                                        </span>

                                        <button
                                            className="qa-pagination-btn qa-pagination-btn-primary"
                                            onClick={handleLoadMore}
                                            disabled={!sessionInfo.hasNext || loadingMore}
                                        >
                                            {loadingMore ? t('qaLoadingMore') : t('qaNextBatch')}
                                        </button>
                                    </div>
                                )}

                                {sessionInfo.remainingDocuments === 0 && !sessionInfo.hasNext && (
                                    <div className="qa-all-docs-used">
                                        {t('qaSessionAllDocsUsed')}
                                    </div>
                                )}
                            </div>
                        )}

                        {/* 参考来源、文档切分块和反馈 - 统一区域 */}
                        {((answer.sources && answer.sources.length > 0) || (answer.chunks && answer.chunks.length > 0)) && (
                            <div className="qa-unified-section">
                                <div className="qa-unified-header">
                                    <h4 className="qa-unified-title">
                                        {t('qaChunksAndFeedback')}
                                    </h4>
                                    {answer.sources && answer.sources.length > 1 && (
                                        <button
                                            className="qa-batch-download-btn"
                                            onClick={handleBatchDownload}
                                            title={t('qaBatchDownload')}
                                        >
                                            {t('qaBatchDownload')} ({answer.sources.length})
                                        </button>
                                    )}
                                </div>

                                {/* 参考来源列表 - 带下载和反馈 */}
                                {answer.sources && answer.sources.length > 0 && (
                                    <div className="qa-sources-with-feedback">
                                        {answer.sources.map((source, index) => (
                                            <div key={index} className="qa-source-card">
                                                <div className="qa-source-header">
                                                    <span className="qa-source-number">{index + 1}</span>
                                                    <span className="qa-source-name">{source}</span>
                                                    <button
                                                        className="qa-source-download-btn-inline"
                                                        onClick={() => handleDownload(source)}
                                                        title={t('qaDownload')}
                                                    >
                                                         {t('qaDownload')}
                                                    </button>
                                                </div>

                                                {/* 反馈按钮行 */}
                                                <div className="qa-source-feedback-row">
                                                    {/* 简单反馈按钮 */}
                                                    <button
                                                        className={`qa-source-feedback-btn helpful ${documentFeedbacks[source] === 'LIKE' ? 'active submitted' : ''} ${documentFeedbacks[source] ? 'disabled' : ''}`}
                                                        onClick={() => handleDocumentHelpful(source)}
                                                        disabled={documentFeedbacks[source] !== undefined}
                                                        title={documentFeedbacks[source] === 'LIKE' ? t('feedbackDocumentSubmitted') : t('feedbackDocumentHelpful')}
                                                    >
                                                        {documentFeedbacks[source] === 'LIKE'
                                                            ? t('feedbackDocumentSubmitted')
                                                            : t('feedbackDocumentHelpful')}
                                                    </button>
                                                    <button
                                                        className={`qa-source-feedback-btn not-helpful ${documentFeedbacks[source] === 'DISLIKE' ? 'active submitted' : ''} ${documentFeedbacks[source] ? 'disabled' : ''}`}
                                                        onClick={() => handleDocumentNotHelpful(source)}
                                                        disabled={documentFeedbacks[source] !== undefined}
                                                        title={documentFeedbacks[source] === 'DISLIKE' ? t('feedbackDocumentSubmitted') : t('feedbackDocumentNotHelpful')}
                                                    >
                                                        {documentFeedbacks[source] === 'DISLIKE'
                                                            ? t('feedbackDocumentSubmitted')
                                                            : t('feedbackDocumentNotHelpful')}
                                                    </button>

                                                    {/* 星级评价按钮 */}
                                                    <button
                                                        className={`qa-source-feedback-btn rate-quality ${documentRatings[source] ? 'rated' : ''}`}
                                                        onClick={() => handleDocumentRate(source)}
                                                        disabled={documentRatings[source] !== undefined}
                                                        title={documentRatings[source] ? t('qaRatedStars').replace('{rating}', documentRatings[source]) : t('qaRateQuality')}
                                                    >
                                                        {documentRatings[source] ? (
                                                            t('qaRatedStars').replace('{rating}', documentRatings[source])
                                                        ) : (
                                                            t('qaRateQuality')
                                                        )}
                                                    </button>
                                                </div>
                                            </div>
                                        ))}
                                    </div>
                                )}

                                {/* 文档切分块 */}
                                {answer.chunks && answer.chunks.length > 0 && (
                                    <div className="qa-chunks-subsection">
                                        <div className="qa-chunks-header">
                                            <h5 className="qa-chunks-subtitle">
                                                {t('qaChunksTitle')} ({answer.chunks.length})
                                            </h5>
                                            <button
                                                className="qa-chunks-download-all-btn"
                                                onClick={handleBatchDownloadChunks}
                                                title={t('qaChunksDownloadAll')}
                                            >
                                                {t('qaChunksDownloadAll')}
                                            </button>
                                        </div>
                                        <div className="qa-chunks-grid">
                                            {answer.chunks.map((chunk) => (
                                                <button
                                                    key={chunk.chunkId}
                                                    className="qa-chunk-card"
                                                    onClick={(e) => handleChunkDownload(chunk.documentId, chunk.chunkId, e.currentTarget)}
                                                    title={`${t('qaChunkDownload')}: ${chunk.title || t('qaChunkTitle') + ' ' + (chunk.chunkIndex + 1)}`}
                                                >
                                                    <div className="qa-chunk-title">
                                                        {chunk.title || `${t('qaChunkTitle')} ${chunk.chunkIndex + 1}`}
                                                    </div>
                                                    <div className="qa-chunk-info">
                                                        <span className="qa-chunk-index">
                                                            {chunk.chunkIndex + 1}/{chunk.totalChunks || answer.chunks.length}
                                                        </span>
                                                        <span className="qa-chunk-size">
                                                            {(chunk.contentLength / 1024).toFixed(1)} KB
                                                        </span>
                                                    </div>
                                                </button>
                                            ))}
                                        </div>
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

            {/* 星级评价模态框 */}
            {showRatingModal && (
                <div
                    className="qa-modal-overlay"
                    onClick={() => {
                        setShowRatingModal(false);
                        setCurrentRatingDoc(null);
                        setTempRating(0);
                    }}
                >
                    <div
                        className="qa-modal-content qa-rating-modal"
                        onClick={(e) => e.stopPropagation()}
                    >
                        <h4 className="qa-modal-title">{t('qaRatingModalTitle')}</h4>
                        <p className="qa-modal-subtitle">{t('qaRatingModalSubtitle')}</p>

                        <div className="qa-modal-rating-container">
                            <div className="qa-rating-stars qa-modal-rating-stars">
                                {[1, 2, 3, 4, 5].map(star => (
                                    <span
                                        key={star}
                                        className={`qa-star ${star <= tempRating ? 'filled' : 'empty'}`}
                                        onClick={() => setTempRating(star)}
                                    >
                                        ★
                                    </span>
                                ))}
                            </div>
                            <p className="qa-rating-description">
                                {tempRating === 0 && t('qaRatingSelectStar')}
                                {tempRating === 1 && t('qaRatingUseless')}
                                {tempRating === 2 && t('qaRatingNotHelpful')}
                                {tempRating === 3 && t('qaRatingNeutral')}
                                {tempRating === 4 && t('qaRatingHelpful')}
                                {tempRating === 5 && t('qaRatingVeryHelpful')}
                            </p>
                        </div>

                        <textarea
                            className="qa-modal-textarea"
                            placeholder={t('qaRatingOptionalComment')}
                            value={feedbackComment}
                            onChange={(e) => setFeedbackComment(e.target.value)}
                            rows={3}
                        />

                        <div className="qa-modal-buttons">
                            <button
                                className="qa-modal-btn qa-modal-btn-secondary"
                                onClick={() => {
                                    setShowRatingModal(false);
                                    setCurrentRatingDoc(null);
                                    setTempRating(0);
                                    setFeedbackComment('');
                                }}
                            >
                                {t('qaRatingCancel')}
                            </button>
                            <button
                                className="qa-modal-btn qa-modal-btn-primary"
                                onClick={() => submitDocumentRating(tempRating, feedbackComment)}
                                disabled={tempRating === 0}
                            >
                                {t('qaRatingSubmit')}
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

