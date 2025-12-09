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

    // 反馈相关状态 / Feedback related states
    const [feedbackRating, setFeedbackRating] = useState(0);
    const [feedbackComment, setFeedbackComment] = useState('');
    const [feedbackSubmitted, setFeedbackSubmitted] = useState(false);
    const [documentRatings, setDocumentRatings] = useState({}); // 文档表情评价 / Document emoji ratings

    // 相似问题相关状态
    const [expandedSimilarQA, setExpandedSimilarQA] = useState(null); // 展开的相似问题答案

    // 分层反馈和主动学习状态 / Hierarchical feedback and active learning states
    const [showHierarchicalFeedback, setShowHierarchicalFeedback] = useState(false);
    const [selectedDocForFeedback, setSelectedDocForFeedback] = useState(null);
    const [retrievedDocs, setRetrievedDocs] = useState([]); // 用于主动学习推荐
    
    // 追踪已添加到AI分析的文档，用于更新按钮状态
    const [addedDocsVersion, setAddedDocsVersion] = useState(0);

    // HOPE 相关状态
    const [hopeSessionId, setHopeSessionId] = useState(() => {
        // 从 localStorage 恢复或生成新的会话ID
        let sid = localStorage.getItem('hopeSessionId');
        if (!sid) {
            sid = 'hope_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9);
            localStorage.setItem('hopeSessionId', sid);
        }
        return sid;
    });
    const [showHOPEDashboard, setShowHOPEDashboard] = useState(false);

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
        setDocumentRatings({}); // 清除文档评价
        setSessionId(null);
        setSessionInfo(null);

        try {
            // 使用 hopeSessionId 调用 API，支持 HOPE 上下文增强
            const result = await window.api.ask(question, hopeSessionId);
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
            console.error(t('logSessionInfoError'), err);
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
            console.error(t('logLoadMoreDocumentsError'), err);
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
            console.error(t('logLoadPreviousDocumentsError'), err);
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

    // HOPE 层名称转换辅助函数 (HOPE layer name helper function)
    const getHopeLayerName = (hopeSource) => {
        if (!hopeSource) return '';
        const source = hopeSource.toUpperCase();
        if (source.includes('PERMANENT')) {
            return t('hopeLayerPermanent') || '低频层 (技能知识库)';
        } else if (source.includes('ORDINARY')) {
            return t('hopeLayerOrdinary') || '中频层 (近期知识)';
        } else if (source.includes('HIGH') || source.includes('FREQUENCY')) {
            return t('hopeLayerHighFrequency') || '高频层 (实时上下文)';
        }
        return hopeSource;
    };

    // ============================================================================
    // 表情评价功能函数 / Emoji Rating Functions
    // ============================================================================

    /**
     * 处理文档表情评价（直接点击应用，允许更改）
     * Handle document emoji rating (apply directly on click, allow changes)
     */
    const handleDocumentEmojiRate = async (docName, rating) => {
        try {
            // 调用API提交评价
            const result = await window.api.rateDocumentQuality(
                answer.recordId || Date.now().toString(),
                docName,
                rating,
                null // 无评论
            );

            if (result.success) {
                // 更新本地状态
                setDocumentRatings(prev => ({ ...prev, [docName]: rating }));

                // 显示成功提示
                const emojiTexts = {
                    1: t('qaRatingUseless'),
                    2: t('qaRatingNotHelpful'),
                    3: t('qaRatingNeutral'),
                    4: t('qaRatingHelpful'),
                    5: t('qaRatingVeryHelpful')
                };
                const message = `${result.impact || t('feedbackSubmitSuccess')} - ${emojiTexts[rating]}`;
                showToast(message, 'success');
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

    /**
     * 提交整体反馈（保留原有功能）
     * Submit overall feedback (keep original function)
     */
    const handleSubmitFeedback = async () => {
        if (feedbackRating === 0) {
            alert(t('feedbackPleaseRate'));
            return;
        }

        try {
            // 传递 hopeSessionId 以触发 HOPE 学习
            const result = await window.api.submitOverallFeedback(
                answer.recordId || Date.now().toString(),
                feedbackRating,
                feedbackComment,
                hopeSessionId
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

    /**
     * 处理整体回答的表情评价（直接点击应用，允许更改）
     * Handle overall answer emoji rating (apply directly on click, allow changes)
     */
    const handleOverallEmojiRate = async (rating) => {
        try {
            // 调用API提交整体评价
            const result = await window.api.rateOverallQuality(
                answer.recordId || Date.now().toString(),
                rating
            );

            if (result.success) {
                // 更新本地状态
                setFeedbackRating(rating);
                setFeedbackSubmitted(true);

                // 显示成功提示
                const emojiTexts = {
                    1: t('qaRatingUseless'),
                    2: t('qaRatingNotHelpful'),
                    3: t('qaRatingNeutral'),
                    4: t('qaRatingHelpful'),
                    5: t('qaRatingVeryHelpful')
                };
                const message = `${t('feedbackSubmitSuccess')} - ${emojiTexts[rating]}`;
                showToast(message, 'success');
            } else {
                const errorMsg = t('feedbackSubmitError');
                showToast(errorMsg, 'error');
            }
        } catch (err) {
            console.error(t('logFeedbackError'), err);
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
            top: 70px;
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
                            setDocumentRatings({}); // 清除文档评价
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

                        {/* HOPE 标识 (HOPE Badge) */}
                        {answer.hopeSource && (
                            <div className={`hope-badge ${answer.directAnswer ? 'hope-direct' : 'hope-reference'}`}>
                                {answer.directAnswer ? (
                                    <>
                                        <span className="hope-icon">💡</span>
                                        <span className="hope-label">{t('hopeDirectAnswer') || 'HOPE 快速答案'}</span>
                                        <span className="hope-source">{getHopeLayerName(answer.hopeSource)}</span>
                                        <span className="hope-time">{answer.responseTimeMs}ms</span>
                                        {answer.hopeConfidence > 0 && (
                                            <span className="hope-confidence">{t('hopeConfidence') || '置信度'}: {(answer.hopeConfidence * 100).toFixed(0)}%</span>
                                        )}
                                    </>
                                ) : (
                                    <>
                                        <span className="hope-icon">📚</span>
                                        <span className="hope-label">{t('hopeReferenceUsed') || '参考 HOPE 知识'}</span>
                                        <span className="hope-source">{getHopeLayerName(answer.hopeSource)}</span>
                                    </>
                                )}
                            </div>
                        )}

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
                                    <div className="qa-chunks-header-actions">
                                        {answer.sources && answer.sources.length > 0 && (() => {
                                            // 检查所有文档是否已添加
                                            const docs = answer.sources.map((source) => ({
                                                id: source,
                                                name: source,
                                                title: source,
                                                fileName: source
                                            }));
                                            const allAdded = docs.every(doc => 
                                                window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis(doc)
                                            );
                                            const someAdded = docs.some(doc => 
                                                window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis(doc)
                                            );
                                            
                                            return (
                                                <button
                                                    key={`batch-add-sources-${addedDocsVersion}`}
                                                    className="qa-add-to-ai-btn"
                                                    onClick={() => {
                                                        if (allAdded || someAdded) {
                                                            // 全部或部分已添加，执行批量移除
                                                            if (window.removeDocumentsFromAIAnalysis) {
                                                                const removed = window.removeDocumentsFromAIAnalysis(docs);
                                                                if (removed > 0) {
                                                                    showToast(`✖️ ${t('documentRemoved') || '已移除'}: ${removed} 个文档`, 'info');
                                                                    setTimeout(() => setAddedDocsVersion(v => v + 1), 0);
                                                                }
                                                            }
                                                        } else {
                                                            // 未添加，执行批量添加
                                                            if (window.addDocumentsToAIAnalysis) {
                                                                const added = window.addDocumentsToAIAnalysis(docs);
                                                                if (added > 0) {
                                                                    showToast(`✅ ${t('documentAdded')}: ${added} 个文档`, 'success');
                                                                    setTimeout(() => setAddedDocsVersion(v => v + 1), 0);
                                                                }
                                                            }
                                                        }
                                                    }}
                                                    title={allAdded || someAdded ? (t('removeFromAIAnalysis') || '移除') : t('addToAIAnalysis')}
                                                    style={{
                                                        opacity: allAdded ? 0.6 : someAdded ? 0.8 : 1,
                                                        filter: allAdded || someAdded ? 'grayscale(50%)' : 'none'
                                                    }}
                                                >
                                                    {allAdded ? '✔️' : someAdded ? '🔄' : '🤖'} {allAdded || someAdded ? (t('removeFromAIAnalysis') || '移除') : t('addToAIAnalysis')}
                                                </button>
                                            );
                                        })()}
                                        {answer.sources && answer.sources.length > 1 && (
                                            <button
                                                className="qa-chunks-download-all-btn"
                                                onClick={handleBatchDownload}
                                                title={t('qaBatchDownload')}
                                            >
                                                {t('qaBatchDownload')} ({answer.sources.length})
                                            </button>
                                        )}
                                    </div>
                                </div>

                                {/* 参考来源列表 - 带下载和反馈 */}
                                {answer.sources && answer.sources.length > 0 && (
                                    <div className="qa-sources-with-feedback">
                                        {answer.sources.map((source, index) => (
                                            <div key={index} className="qa-source-card">
                                                <div className="qa-source-header">
                                                    <span className="qa-source-number">{index + 1}</span>
                                                    <span className="qa-source-name">{source}</span>
                                                    <div style={{ display: 'flex', gap: '8px' }}>
                                                        <button
                                                            key={`add-btn-${source}-${addedDocsVersion}`}
                                                            className="qa-chunk-add-btn"
                                                            onClick={(e) => {
                                                                e.stopPropagation();
                                                                const doc = {
                                                                    id: source,
                                                                    name: source,
                                                                    title: source,
                                                                    fileName: source
                                                                };
                                                                
                                                                if (window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis(doc)) {
                                                                    // 已添加，执行移除
                                                                    if (window.removeDocumentsFromAIAnalysis) {
                                                                        const removed = window.removeDocumentsFromAIAnalysis(doc);
                                                                        if (removed > 0) {
                                                                            showToast(`✖️ ${t('documentRemoved') || '已移除'}`, 'info');
                                                                            // 使用 setTimeout 确保状态更新立即生效
                                                                            setTimeout(() => setAddedDocsVersion(v => v + 1), 0);
                                                                        }
                                                                    }
                                                                } else {
                                                                    // 未添加，执行添加
                                                                    if (window.addDocumentsToAIAnalysis) {
                                                                        const added = window.addDocumentsToAIAnalysis(doc);
                                                                        if (added > 0) {
                                                                            showToast(`✅ ${t('documentAdded')}`, 'success');
                                                                            // 使用 setTimeout 确保状态更新立即生效
                                                                            setTimeout(() => setAddedDocsVersion(v => v + 1), 0);
                                                                        }
                                                                    }
                                                                }
                                                            }}
                                                            title={window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis({ name: source }) ? (t('removeFromAIAnalysis') || '移除') : t('addToAIAnalysis')}
                                                            style={{ 
                                                                position: 'relative', 
                                                                marginRight: '8px',
                                                                opacity: window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis({ name: source }) ? 0.6 : 1,
                                                                filter: window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis({ name: source }) ? 'grayscale(50%)' : 'none'
                                                            }}
                                                        >
                                                            {window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis({ name: source }) ? '✔️' : '🤖'}
                                                        </button>
                                                        <button
                                                            className="qa-source-download-btn-inline"
                                                            onClick={() => handleDownload(source)}
                                                            title={t('qaDownload')}
                                                        >
                                                             {t('qaDownload')}
                                                        </button>
                                                    </div>
                                                </div>

                                                {/* 表情评价行 / Emoji Rating Row */}
                                                <div className="qa-source-emoji-rating">
                                                    <span className="qa-emoji-rating-label">
                                                        {documentRatings[source]
                                                            ? t('qaEmojiRatingYourChoice') || '您的评价：'
                                                            : t('qaEmojiRatingPrompt') || '这个文档有帮助吗？'}
                                                    </span>
                                                    <div className="qa-emoji-rating-buttons">
                                                        {[
                                                            { rating: 1, emoji: '😞', text: t('qaRatingUseless') },
                                                            { rating: 2, emoji: '🙁', text: t('qaRatingNotHelpful') },
                                                            { rating: 3, emoji: '😐', text: t('qaRatingNeutral') },
                                                            { rating: 4, emoji: '😊', text: t('qaRatingHelpful') },
                                                            { rating: 5, emoji: '🤩', text: t('qaRatingVeryHelpful') }
                                                        ].map(({ rating, emoji, text }) => (
                                                            <button
                                                                key={rating}
                                                                className={`qa-emoji-rating-btn ${documentRatings[source] === rating ? 'selected' : ''}`}
                                                                onClick={() => handleDocumentEmojiRate(source, rating)}
                                                                title={text}
                                                            >
                                                                <span className="qa-emoji">{emoji}</span>
                                                                <span className="qa-emoji-text">{text}</span>
                                                            </button>
                                                        ))}
                                                        {/* 分层反馈按钮 */}
                                                        <button
                                                            className="qa-hierarchical-feedback-btn"
                                                            onClick={() => {
                                                                setSelectedDocForFeedback({
                                                                    name: source,
                                                                    id: source,
                                                                    content: '' // 内容需要从chunks获取
                                                                });
                                                                setShowHierarchicalFeedback(true);
                                                            }}
                                                            title={t('hierarchicalDetailedFeedbackTitle')}
                                                        >
                                                            {t('hierarchicalDetailedFeedback')}
                                                        </button>
                                                    </div>
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
                                            <div className="qa-chunks-header-actions">
                                                <button
                                                    key={`batch-add-chunks-${addedDocsVersion}`}
                                                    className="qa-add-to-ai-btn"
                                                    onClick={() => {
                                                        // 提取唯一的文档并添加到AI分析
                                                        const uniqueDocs = [];
                                                        const docNames = new Set();
                                                        
                                                        answer.chunks.forEach(chunk => {
                                                            const docName = chunk.title || chunk.fileName || `文档_${chunk.documentId}`;
                                                            if (!docNames.has(docName)) {
                                                                docNames.add(docName);
                                                                uniqueDocs.push({
                                                                    id: chunk.documentId,
                                                                    name: docName,
                                                                    title: chunk.title,
                                                                    fileName: chunk.fileName,
                                                                    chunkId: chunk.chunkId
                                                                });
                                                            }
                                                        });

                                                        // 检查所有文档是否已添加
                                                        const allAdded = uniqueDocs.every(doc => 
                                                            window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis(doc)
                                                        );
                                                        const someAdded = uniqueDocs.some(doc => 
                                                            window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis(doc)
                                                        );

                                                        if (allAdded || someAdded) {
                                                            // 全部或部分已添加，执行批量移除
                                                            if (window.removeDocumentsFromAIAnalysis) {
                                                                const removed = window.removeDocumentsFromAIAnalysis(uniqueDocs);
                                                                if (removed > 0) {
                                                                    showToast(`✖️ ${t('documentRemoved') || '已移除'}: ${removed} 个文档`, 'info');
                                                                    setTimeout(() => setAddedDocsVersion(v => v + 1), 0);
                                                                }
                                                            }
                                                        } else {
                                                            // 未添加，执行批量添加
                                                            if (window.addDocumentsToAIAnalysis) {
                                                                const added = window.addDocumentsToAIAnalysis(uniqueDocs);
                                                                if (added > 0) {
                                                                    showToast(`✅ ${t('documentAdded')}: ${added} 个文档`, 'success');
                                                                    setTimeout(() => setAddedDocsVersion(v => v + 1), 0);
                                                                }
                                                            }
                                                        }
                                                    }}
                                                    title={(() => {
                                                        const uniqueDocs = [];
                                                        const docNames = new Set();
                                                        answer.chunks.forEach(chunk => {
                                                            const docName = chunk.title || chunk.fileName || `文档_${chunk.documentId}`;
                                                            if (!docNames.has(docName)) {
                                                                docNames.add(docName);
                                                                uniqueDocs.push({ id: chunk.documentId, name: docName });
                                                            }
                                                        });
                                                        const allAdded = uniqueDocs.every(doc => 
                                                            window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis(doc)
                                                        );
                                                        const someAdded = uniqueDocs.some(doc => 
                                                            window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis(doc)
                                                        );
                                                        return allAdded || someAdded ? (t('removeFromAIAnalysis') || '移除') : t('addToAIAnalysis');
                                                    })()}
                                                    style={(() => {
                                                        const uniqueDocs = [];
                                                        const docNames = new Set();
                                                        answer.chunks.forEach(chunk => {
                                                            const docName = chunk.title || chunk.fileName || `文档_${chunk.documentId}`;
                                                            if (!docNames.has(docName)) {
                                                                docNames.add(docName);
                                                                uniqueDocs.push({ id: chunk.documentId, name: docName });
                                                            }
                                                        });
                                                        const allAdded = uniqueDocs.every(doc => 
                                                            window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis(doc)
                                                        );
                                                        const someAdded = uniqueDocs.some(doc => 
                                                            window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis(doc)
                                                        );
                                                        return {
                                                            opacity: allAdded ? 0.6 : someAdded ? 0.8 : 1,
                                                            filter: allAdded || someAdded ? 'grayscale(50%)' : 'none'
                                                        };
                                                    })()}
                                                >
                                                    {(() => {
                                                        const uniqueDocs = [];
                                                        const docNames = new Set();
                                                        answer.chunks.forEach(chunk => {
                                                            const docName = chunk.title || chunk.fileName || `文档_${chunk.documentId}`;
                                                            if (!docNames.has(docName)) {
                                                                docNames.add(docName);
                                                                uniqueDocs.push({ id: chunk.documentId, name: docName });
                                                            }
                                                        });
                                                        const allAdded = uniqueDocs.every(doc => 
                                                            window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis(doc)
                                                        );
                                                        const someAdded = uniqueDocs.some(doc => 
                                                            window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis(doc)
                                                        );
                                                        const icon = allAdded ? '✔️' : someAdded ? '🔄' : '🤖';
                                                        const text = allAdded || someAdded ? (t('removeFromAIAnalysis') || '移除') : t('addToAIAnalysis');
                                                        return `${icon} ${text}`;
                                                    })()}
                                                </button>
                                                <button
                                                    className="qa-chunks-download-all-btn"
                                                    onClick={handleBatchDownloadChunks}
                                                    title={t('qaChunksDownloadAll')}
                                                >
                                                    {t('qaChunksDownloadAll')}
                                                </button>
                                            </div>
                                        </div>
                                        <div className="qa-chunks-grid">
                                            {answer.chunks.map((chunk) => (
                                                <div key={chunk.chunkId} className="qa-chunk-card-wrapper">
                                                    <button
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
                                                    <button
                                                        key={`add-btn-chunk-${chunk.chunkId}-${addedDocsVersion}`}
                                                        className="qa-chunk-add-btn"
                                                        onClick={(e) => {
                                                            e.stopPropagation();
                                                            const doc = {
                                                                id: chunk.documentId,
                                                                name: chunk.title || chunk.fileName || `文档_${chunk.documentId}`,
                                                                title: chunk.title,
                                                                fileName: chunk.fileName,
                                                                chunkId: chunk.chunkId
                                                            };
                                                            
                                                            if (window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis(doc)) {
                                                                // 已添加，执行移除
                                                                if (window.removeDocumentsFromAIAnalysis) {
                                                                    const removed = window.removeDocumentsFromAIAnalysis(doc);
                                                                    if (removed > 0) {
                                                                        showToast(`✖️ ${t('documentRemoved') || '已移除'}`, 'info');
                                                                        // 使用 setTimeout 确保状态更新立即生效
                                                                        setTimeout(() => setAddedDocsVersion(v => v + 1), 0);
                                                                    }
                                                                }
                                                            } else {
                                                                // 未添加，执行添加
                                                                if (window.addDocumentsToAIAnalysis) {
                                                                    const added = window.addDocumentsToAIAnalysis(doc);
                                                                    if (added > 0) {
                                                                        showToast(`✅ ${t('documentAdded')}`, 'success');
                                                                        // 使用 setTimeout 确保状态更新立即生效
                                                                        setTimeout(() => setAddedDocsVersion(v => v + 1), 0);
                                                                    }
                                                                }
                                                            }
                                                        }}
                                                        title={window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis({ name: chunk.title || chunk.fileName || `文档_${chunk.documentId}` }) ? (t('removeFromAIAnalysis') || '移除') : t('addToAIAnalysis')}
                                                        style={{
                                                            opacity: window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis({ name: chunk.title || chunk.fileName || `文档_${chunk.documentId}` }) ? 0.6 : 1,
                                                            filter: window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis({ name: chunk.title || chunk.fileName || `文档_${chunk.documentId}` }) ? 'grayscale(50%)' : 'none'
                                                        }}
                                                    >
                                                        {window.isDocumentInAIAnalysis && window.isDocumentInAIAnalysis({ name: chunk.title || chunk.fileName || `文档_${chunk.documentId}` }) ? '✔️' : '🤖'}
                                                    </button>
                                                </div>
                                            ))}
                                        </div>
                                    </div>
                                )}
                            </div>
                        )}

                        {/* 用户反馈区域 - 表情评价 / User Feedback Section - Emoji Rating */}
                        <div className="qa-feedback-section">
                            <h4 className="qa-feedback-title">
                                {feedbackRating > 0
                                    ? t('qaEmojiRatingYourChoice') || '您的评价：'
                                    : t('feedbackQuestion')}
                            </h4>
                            <div className="qa-overall-emoji-rating">
                                {[
                                    { rating: 1, emoji: '😞', text: t('qaRatingUseless') },
                                    { rating: 2, emoji: '🙁', text: t('qaRatingNotHelpful') },
                                    { rating: 3, emoji: '😐', text: t('qaRatingNeutral') },
                                    { rating: 4, emoji: '😊', text: t('qaRatingHelpful') },
                                    { rating: 5, emoji: '🤩', text: t('qaRatingVeryHelpful') }
                                ].map(({ rating, emoji, text }) => (
                                    <button
                                        key={rating}
                                        className={`qa-overall-emoji-btn ${feedbackRating === rating ? 'selected' : ''}`}
                                        onClick={() => handleOverallEmojiRate(rating)}
                                        title={text}
                                    >
                                        <span className="qa-emoji">{emoji}</span>
                                        <span className="qa-emoji-text">{text}</span>
                                    </button>
                                ))}
                            </div>
                            {feedbackRating > 0 && (
                                <div className="qa-feedback-success">
                                    ✅ {t('feedbackThankYou')}
                                </div>
                            )}
                        </div>

                        {/* 主动学习推荐面板 / Active Learning Panel */}
                        {window.ActiveLearningPanel && answer.sources && (
                            <ActiveLearningPanel
                                question={question}
                                retrievedDocs={answer.sources.map((s, i) => ({
                                    id: s,
                                    title: s,
                                    rank: i + 1
                                }))}
                                topKUsed={answer.sources?.length || 5}
                                onFeedbackSubmit={(docName, isRelevant) => {
                                    console.log(t('logActiveLearningFeedback'), docName, isRelevant);
                                }}
                                t={t}
                            />
                        )}

                        {/* 响应时间和 HOPE 信息 */}
                        <div className="response-time" style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
                            <span>{t('qaResponseTime')}: {answer.responseTimeMs}ms</span>

                            {/* HOPE 状态标签 */}
                            {answer.strategyUsed && (
                                <span style={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: '4px',
                                    padding: '2px 8px',
                                    borderRadius: '12px',
                                    fontSize: '11px',
                                    fontWeight: '500',
                                    background: answer.directAnswer ? '#dcfce7' : '#dbeafe',
                                    color: answer.directAnswer ? '#166534' : '#1e40af'
                                }}>
                                    {answer.directAnswer ? '⚡' : '🔍'}
                                    {answer.strategyUsed === 'DIRECT_ANSWER' ? '直接回答' :
                                     answer.strategyUsed === 'TEMPLATE_ANSWER' ? '模板增强' :
                                     answer.strategyUsed === 'REFERENCE_ANSWER' ? '参考增强' : 'RAG检索'}
                                </span>
                            )}

                            {/* HOPE 来源层 */}
                            {answer.hopeSource && answer.hopeSource !== 'null' && (
                                <span style={{
                                    display: 'inline-flex',
                                    alignItems: 'center',
                                    gap: '4px',
                                    padding: '2px 8px',
                                    borderRadius: '12px',
                                    fontSize: '11px',
                                    fontWeight: '500',
                                    background: '#fef3c7',
                                    color: '#92400e'
                                }}>
                                    🧠 {answer.hopeSource === 'permanent' ? '低频层' :
                                        answer.hopeSource === 'ordinary' ? '中频层' :
                                        answer.hopeSource === 'high_frequency' ? '高频层' : answer.hopeSource}
                                </span>
                            )}

                            {/* HOPE 置信度 */}
                            {answer.hopeConfidence > 0 && (
                                <span style={{
                                    fontSize: '11px',
                                    color: '#6b7280'
                                }}>
                                    置信度: {(answer.hopeConfidence * 100).toFixed(0)}%
                                </span>
                            )}
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

            {/* 分层反馈弹窗 / Hierarchical Feedback Modal */}
            {showHierarchicalFeedback && selectedDocForFeedback && window.HierarchicalFeedbackPanel && (
                <div
                    className="qa-modal-overlay"
                >
                    <div className="qa-modal-container">
                        <HierarchicalFeedbackPanel
                            qaRecordId={answer?.recordId || 'unknown'}
                            documentName={selectedDocForFeedback.name}
                            documentId={selectedDocForFeedback.id}
                            documentContent={selectedDocForFeedback.content}
                            onClose={() => {
                                setShowHierarchicalFeedback(false);
                                setSelectedDocForFeedback(null);
                            }}
                            t={t}
                        />
                    </div>
                </div>
            )}

            {/* HOPE 仪表盘浮动按钮 */}
            <div
                onClick={() => setShowHOPEDashboard(!showHOPEDashboard)}
                style={{
                    position: 'fixed',
                    bottom: '80px',
                    right: '20px',
                    width: '50px',
                    height: '50px',
                    borderRadius: '50%',
                    background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
                    color: 'white',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    fontSize: '24px',
                    cursor: 'pointer',
                    boxShadow: '0 4px 12px rgba(102, 126, 234, 0.4)',
                    zIndex: 999,
                    transition: 'transform 0.2s ease'
                }}
                title="HOPE 三层记忆架构"
                onMouseEnter={(e) => e.currentTarget.style.transform = 'scale(1.1)'}
                onMouseLeave={(e) => e.currentTarget.style.transform = 'scale(1)'}
            >
                🧠
            </div>

            {/* HOPE 仪表盘面板 */}
            {showHOPEDashboard && window.HOPEDashboardPanel && (
                <div style={{
                    position: 'fixed',
                    bottom: '140px',
                    right: '20px',
                    width: '400px',
                    maxHeight: '70vh',
                    overflowY: 'auto',
                    zIndex: 998,
                    animation: 'slideUp 0.3s ease'
                }}>
                    <HOPEDashboardPanel
                        onToggle={() => setShowHOPEDashboard(false)}
                    />
                </div>
            )}

            {/* 添加动画样式 */}
            <style>{`
                @keyframes slideUp {
                    from {
                        opacity: 0;
                        transform: translateY(20px);
                    }
                    to {
                        opacity: 1;
                        transform: translateY(0);
                    }
                }
            `}</style>
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
