/**
 * QA 标签页组件 / QA Tab Component
 * 负责问答功能、文档下载、反馈系统
 */

function QATab() {
    // 获取React hooks（避免重复声明）
    const { useState, useEffect } = React;

    // 使用语言Hook
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

    const handleAsk = async () => {
        if (!question.trim()) {
            alert(t('qaInputError'));
            return;
        }

        setLoading(true);
        setError(null);
        setAnswer(null);

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

    // 下载单个文件
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

    // 批量下载所有参考文件
    const handleBatchDownload = async () => {
        if (!answer || !answer.sources || answer.sources.length === 0) {
            return;
        }

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

    // 下载单个文档块
    const handleChunkDownload = async (documentId, chunkId, buttonElement) => {
        try {
            // 添加下载动画
            buttonElement.classList.add('downloading');

            const response = await fetch(`/api/chunks/download/${encodeURIComponent(documentId)}/${encodeURIComponent(chunkId)}`);

            if (!response.ok) {
                throw new Error(t('qaChunkDownloadError'));
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `${chunkId}.md`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            // 移除动画
            setTimeout(() => {
                buttonElement.classList.remove('downloading');
            }, 600);
        } catch (err) {
            buttonElement.classList.remove('downloading');
            alert(t('qaChunkDownloadError') + ': ' + err.message);
        }
    };

    // 批量下载所有文档块
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

                    if (!response.ok) {
                        throw new Error('Download failed');
                    }

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

                    // 延迟一下避免浏览器阻止多个下载
                    if (i < answer.chunks.length - 1) {
                        await new Promise(resolve => setTimeout(resolve, 300));
                    }
                } catch (err) {
                    console.error(`Failed to download chunk ${chunk.chunkId}:`, err);
                    failCount++;
                }
            }

            if (failCount > 0) {
                alert(`${t('qaChunksDownloadAll')}: ${successCount}/${answer.chunks.length} ${t('docsUploadSuccess')}, ${failCount} ${t('docsUploadError')}`);
            } else {
                alert(`${t('qaChunksDownloadAll')}: ${successCount}/${answer.chunks.length} ${t('docsUploadSuccess')}`);
            }
        } catch (err) {
            alert(t('qaChunkDownloadError') + ': ' + err.message);
        }
    };

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

    // 显示图片模态框
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

        // ESC 键关闭
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

    // 提交整体反馈
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

    // 提交文档反馈（有帮助）
    const handleDocumentHelpful = async (docName) => {
        if (documentFeedbacks[docName]) {
            return; // 已经提交过反馈
        }

        try {
            const result = await window.api.submitDocumentFeedback(
                answer.recordId || Date.now().toString(),
                docName,
                'HELPFUL',
                null
            );

            if (result.success) {
                setDocumentFeedbacks(prev => ({
                    ...prev,
                    [docName]: 'HELPFUL'
                }));
            }
        } catch (err) {
            console.error('提交文档反馈失败:', err);
        }
    };

    // 提交文档反馈（无关）
    const handleDocumentNotHelpful = (docName) => {
        setCurrentFeedbackDoc(docName);
        setShowReasonModal(true);
    };

    // 提交文档无关反馈的原因
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
                setDocumentFeedbacks(prev => ({
                    ...prev,
                    [currentFeedbackDoc]: 'NOT_HELPFUL'
                }));
            }
        } catch (err) {
            console.error('提交文档反馈失败:', err);
        } finally {
            setShowReasonModal(false);
            setCurrentFeedbackDoc(null);
        }
    };

    // 渲染组件
    return React.createElement('div', { className: 'qa-section' },
        // 输入组
        React.createElement('div', { className: 'input-group' },
            React.createElement('input', {
                type: 'text',
                className: 'input-field',
                placeholder: t('qaPlaceholder'),
                value: question,
                onChange: (e) => setQuestion(e.target.value),
                onKeyPress: handleKeyPress,
                disabled: loading
            }),
            React.createElement('button', {
                className: 'btn btn-primary',
                onClick: handleAsk,
                disabled: loading
            }, loading ? t('qaThinking') : t('qaButton'))
        ),

        // 加载状态
        loading && React.createElement('div', { className: 'loading' },
            React.createElement('div', { className: 'spinner' }),
            React.createElement('p', null, t('qaAIThinking'))
        ),

        // 错误状态
        error && React.createElement('div', { className: 'error' },
            t('qaErrorPrefix') + ' ' + error
        ),

        // 答案卡片
        answer && !loading && React.createElement('div', { className: 'answer-card' },
            React.createElement('h3', null, t('qaAnswer')),
            React.createElement('div', {
                className: 'answer-text',
                dangerouslySetInnerHTML: {
                    __html: typeof marked !== 'undefined'
                        ? marked.parse(answer.answer)
                        : answer.answer
                }
            }),

            // 参考来源
            answer.sources && answer.sources.length > 0 && React.createElement('div', { className: 'sources' },
                React.createElement('h4', null, t('qaSources')),
                ...answer.sources.map((source, index) =>
                    React.createElement('div', { key: index, className: 'source-item' },
                        React.createElement('span', { className: 'source-text' },
                            `${index + 1}. ${source}`
                        ),
                        React.createElement('div', { className: 'source-actions' },
                            React.createElement('button', {
                                className: 'btn-icon',
                                onClick: () => handleDownload(source),
                                title: t('qaDownload')
                            }, t('qaDownload'))
                        )
                    )
                ),
                answer.sources.length > 1 && React.createElement('button', {
                    className: 'btn-batch-download',
                    onClick: handleBatchDownload
                }, `${t('qaBatchDownload')} (${answer.sources.length} ${t('docsFiles')})`)
            ),

            // 文档切分块和反馈区域
            ((answer.chunks && answer.chunks.length > 0) || (answer.sources && answer.sources.length > 0)) &&
            React.createElement('div', { className: 'chunks-section' },
                React.createElement('h4', null, `📦 ${t('qaChunksAndFeedback')}`),

                // 文档切分块
                answer.chunks && answer.chunks.length > 0 && React.createElement(React.Fragment, null,
                    React.createElement('div', {
                        style: {
                            marginBottom: '10px',
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center'
                        }
                    },
                        React.createElement('span', { style: { fontSize: '14px', color: '#666' } },
                            `${answer.chunks.length} ${t('qaChunksAvailable')}`
                        ),
                        React.createElement('button', {
                            className: 'btn-batch-download-chunks',
                            onClick: handleBatchDownloadChunks,
                            title: t('qaChunksDownloadAll')
                        }, t('qaChunksDownloadAll'))
                    ),
                    React.createElement('div', { className: 'chunks-grid' },
                        ...answer.chunks.map((chunk, index) =>
                            React.createElement('button', {
                                key: chunk.chunkId,
                                className: 'chunk-button',
                                onClick: (e) => handleChunkDownload(chunk.documentId, chunk.chunkId, e.currentTarget),
                                title: `${t('qaChunkDownload')}: ${chunk.title || t('qaChunkTitle') + ' ' + (chunk.chunkIndex + 1)}`
                            },
                                React.createElement('div', { className: 'chunk-title' },
                                    `📄 ${chunk.title || `${t('qaChunkTitle')} ${chunk.chunkIndex + 1}`}`
                                ),
                                React.createElement('div', { className: 'chunk-info' },
                                    React.createElement('span', { className: 'chunk-index' },
                                        `${chunk.chunkIndex + 1}/${chunk.totalChunks || answer.chunks.length}`
                                    ),
                                    React.createElement('span', { className: 'chunk-size' },
                                        `${(chunk.contentLength / 1024).toFixed(1)} KB`
                                    )
                                )
                            )
                        )
                    )
                ),

                // 文档反馈区域
                answer.sources && answer.sources.length > 0 &&
                React.createElement('div', {
                    className: 'document-feedback-area',
                    style: { marginTop: answer.chunks && answer.chunks.length > 0 ? '20px' : '0' }
                },
                    React.createElement('h5', {
                        style: { marginBottom: '15px', color: '#1565c0', fontSize: '15px' }
                    }, `📚 ${t('feedbackDocumentQuestion')}`),
                    ...answer.sources.map((source, index) =>
                        React.createElement('div', { key: index, className: 'document-feedback-item' },
                            React.createElement('div', { className: 'document-feedback-name' },
                                `${index + 1}. ${source}`
                            ),
                            React.createElement('div', { className: 'document-feedback-buttons' },
                                React.createElement('button', {
                                    className: `document-feedback-btn ${documentFeedbacks[source] === 'HELPFUL' ? 'liked' : ''}`,
                                    onClick: () => handleDocumentHelpful(source),
                                    disabled: documentFeedbacks[source] !== undefined
                                }, documentFeedbacks[source] === 'HELPFUL'
                                    ? t('feedbackDocumentSubmitted')
                                    : t('feedbackDocumentHelpful')),
                                React.createElement('button', {
                                    className: `document-feedback-btn ${documentFeedbacks[source] === 'NOT_HELPFUL' ? 'disliked' : ''}`,
                                    onClick: () => handleDocumentNotHelpful(source),
                                    disabled: documentFeedbacks[source] !== undefined
                                }, documentFeedbacks[source] === 'NOT_HELPFUL'
                                    ? t('feedbackDocumentSubmitted')
                                    : t('feedbackDocumentNotHelpful'))
                            )
                        )
                    )
                )
            ),

            // 用户反馈区域
            !feedbackSubmitted ? React.createElement('div', { className: 'feedback-section' },
                React.createElement('h4', null, `💬 ${t('feedbackQuestion')}`),
                React.createElement('div', { className: 'feedback-rating-buttons' },
                    ...[5, 4, 3, 2, 1].map(rating =>
                        React.createElement('button', {
                            key: rating,
                            className: `feedback-rating-button ${feedbackRating === rating ? 'selected' : ''}`,
                            onClick: () => setFeedbackRating(rating)
                        }, t(`feedbackRating${rating}`))
                    )
                ),
                React.createElement('textarea', {
                    className: 'feedback-comment',
                    placeholder: t('feedbackCommentPlaceholder'),
                    value: feedbackComment,
                    onChange: (e) => setFeedbackComment(e.target.value)
                }),
                React.createElement('button', {
                    className: 'feedback-submit-btn',
                    onClick: handleSubmitFeedback,
                    disabled: feedbackRating === 0
                }, t('feedbackSubmit'))
            ) : React.createElement('div', { className: 'feedback-section' },
                React.createElement('div', { className: 'feedback-success' },
                    t('feedbackThankYou')
                )
            ),

            // 响应时间
            React.createElement('div', { className: 'response-time' },
                `${t('qaResponseTime')}: ${answer.responseTimeMs}ms`
            )
        ),

        // 文档反馈原因模态框
        showReasonModal && React.createElement('div', {
            className: 'feedback-reason-modal',
            onClick: () => setShowReasonModal(false)
        },
            React.createElement('div', {
                className: 'feedback-reason-content',
                onClick: (e) => e.stopPropagation()
            },
                React.createElement('h4', null, t('feedbackDocumentReasonPlaceholder')),
                React.createElement('textarea', {
                    id: 'reasonTextarea',
                    placeholder: t('feedbackCommentPlaceholder'),
                    autoFocus: true
                }),
                React.createElement('div', { className: 'feedback-reason-actions' },
                    React.createElement('button', {
                        className: 'btn btn-secondary',
                        onClick: () => {
                            setShowReasonModal(false);
                            setCurrentFeedbackDoc(null);
                        }
                    }, t('qaImageClose')),
                    React.createElement('button', {
                        className: 'btn btn-primary',
                        onClick: () => {
                            const textarea = document.getElementById('reasonTextarea');
                            submitDocumentNotHelpfulReason(textarea.value);
                        }
                    }, t('feedbackSubmit'))
                )
            )
        ),

        // 空状态
        !answer && !loading && !error && React.createElement('div', { className: 'empty-state' },
            React.createElement('div', { className: 'empty-state-icon' }, t('qaEmptyIcon')),
            React.createElement('p', null, t('qaEmptyText')),
            React.createElement('p', {
                style: { fontSize: '14px', marginTop: '10px', color: '#ccc' }
            }, t('qaEmptyExample'))
        )
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

