/**
 * 分层反馈组件 / Hierarchical Feedback Component
 * JSX 版本 - 使用 Babel 转译
 * 支持文档级、段落级、句子级反馈
 * (Supports document-level, paragraph-level, and sentence-level feedback)
 *
 * @author AI Reviewer Team
 * @since 2025-12-05
 */

(function() {
    'use strict';

    const { useState, useEffect, useCallback } = React;

    /**
     * 分层反馈面板组件
     * (Hierarchical Feedback Panel Component)
     */
    function HierarchicalFeedbackPanel({
        qaRecordId,
        documentName,
        documentId,
        documentContent: initialContent,
        onClose,
        t = (key) => key
    }) {
        // 状态 (State)
        const [activeTab, setActiveTab] = useState('document'); // document, paragraph, sentence
        const [feedback, setFeedback] = useState(null);
        const [loading, setLoading] = useState(false);
        const [paragraphs, setParagraphs] = useState([]);
        const [selectedText, setSelectedText] = useState(null);
        const [highlights, setHighlights] = useState([]);
        const [documentContent, setDocumentContent] = useState(initialContent || '');
        const [contentLoading, setContentLoading] = useState(false);
        const [paragraphFeedbackStatus, setParagraphFeedbackStatus] = useState({}); // 段落反馈状态

        // 文档级反馈状态 (Document-level feedback state)
        const [docRating, setDocRating] = useState(0);
        const [docRelevance, setDocRelevance] = useState('');
        const [docComment, setDocComment] = useState('');
        const [docTags, setDocTags] = useState([]);

        // 加载文档内容（如果没有提供）
        // (Load document content if not provided)
        useEffect(() => {
            if (!documentContent && documentName) {
                loadDocumentContent();
            }
        }, [documentName]);

        const loadDocumentContent = async () => {
            setContentLoading(true);
            try {
                // 尝试从搜索 API 获取文档内容
                // (Try to get document content from search API)
                const response = await fetch(`/api/search?query=${encodeURIComponent(documentName)}&limit=1`);
                if (response.ok) {
                    const data = await response.json();
                    if (data.results && data.results.length > 0) {
                        const content = data.results[0].content || data.results[0].text || '';
                        setDocumentContent(content);
                    }
                }
            } catch (err) {
                console.error(t('logHierarchicalLoadContentError'), err);
            } finally {
                setContentLoading(false);
            }
        };

        // 加载已有反馈 (Load existing feedback)
        useEffect(() => {
            if (qaRecordId && documentName) {
                loadExistingFeedback();
            }
        }, [qaRecordId, documentName]);

        // 分析段落 (Analyze paragraphs)
        useEffect(() => {
            if (documentContent && activeTab === 'paragraph') {
                analyzeParagraphs();
            }
        }, [documentContent, activeTab]);

        const loadExistingFeedback = async () => {
            try {
                const response = await fetch(
                    `/api/feedback/hierarchical/${encodeURIComponent(qaRecordId)}/${encodeURIComponent(documentName)}`
                );
                if (response.ok) {
                    const data = await response.json();
                    if (data.success && data.feedback) {
                        setFeedback(data.feedback);
                        // 恢复文档级反馈 (Restore document-level feedback)
                        if (data.feedback.documentFeedback) {
                            setDocRating(data.feedback.documentFeedback.rating || 0);
                            setDocRelevance(data.feedback.documentFeedback.relevance || '');
                            setDocComment(data.feedback.documentFeedback.comment || '');
                            setDocTags(data.feedback.documentFeedback.tags || []);
                        }
                        // 恢复高亮 (Restore highlights)
                        if (data.feedback.sentenceFeedbacks) {
                            setHighlights(data.feedback.sentenceFeedbacks);
                        }
                    }
                }
            } catch (err) {
                console.error(t('hierarchicalLogLoadFeedbackError') || '加载反馈失败:', err);
            }
        };

        const analyzeParagraphs = async () => {
            if (!documentContent) {
                // 如果没有内容，尝试简单分段 (If no content, try simple paragraph splitting)
                setParagraphs([{
                    preview: t('hierarchicalNoContent') || '暂无文档内容，请在问答后使用此功能',
                    startOffset: 0,
                    endOffset: 0
                }]);
                return;
            }
            try {
                // 先尝试调用 API，如果失败则本地分段
                // (Try API first, fallback to local splitting if failed)
                const response = await fetch('/api/feedback/hierarchical/analyze-paragraphs', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({ content: documentContent })
                });
                if (response.ok) {
                    const data = await response.json();
                    if (data.success && data.paragraphs && data.paragraphs.length > 0) {
                        setParagraphs(data.paragraphs);
                        return;
                    }
                }
                // 本地分段备选方案 (Local splitting fallback)
                const localParagraphs = documentContent
                    .split(/\n\n+/)
                    .filter(p => p.trim().length > 0)
                    .map((p, i) => ({
                        preview: p.substring(0, 100) + (p.length > 100 ? '...' : ''),
                        fullContent: p,
                        startOffset: i * 100,
                        endOffset: (i + 1) * 100
                    }));
                setParagraphs(localParagraphs.length > 0 ? localParagraphs : [{
                    preview: documentContent.substring(0, 200) + '...',
                    fullContent: documentContent,
                    startOffset: 0,
                    endOffset: documentContent.length
                }]);
            } catch (err) {
                console.error(t('hierarchicalLogAnalyzeParagraphsError') || '分析段落失败:', err);
                // 本地分段 (Local splitting)
                const localParagraphs = documentContent
                    .split(/\n\n+/)
                    .filter(p => p.trim().length > 0)
                    .map((p, i) => ({
                        preview: p.substring(0, 100) + (p.length > 100 ? '...' : ''),
                        fullContent: p,
                        startOffset: i * 100,
                        endOffset: (i + 1) * 100
                    }));
                setParagraphs(localParagraphs);
            }
        };

        // 提交文档级反馈 (Submit document-level feedback)
        const submitDocumentFeedback = async () => {
            setLoading(true);
            try {
                const response = await fetch('/api/feedback/hierarchical/document', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        qaRecordId,
                        documentName,
                        documentId,
                        rating: docRating,
                        relevance: docRelevance,
                        comment: docComment,
                        tags: docTags
                    })
                });
                if (response.ok) {
                    alert(t('hierarchicalSubmitSuccess') || '✅ 文档级反馈已保存');
                    loadExistingFeedback();
                } else {
                    alert(t('hierarchicalSubmitError') || '❌ 提交失败');
                }
            } catch (err) {
                alert((t('hierarchicalSubmitError') || '❌ 提交失败: ') + err.message);
            } finally {
                setLoading(false);
            }
        };

        // 提交段落反馈 (Submit paragraph feedback)
        const submitParagraphFeedback = async (paragraphIndex, helpful, feedbackType) => {
            // 更新本地状态显示反馈中 (Update local state to show feedback in progress)
            setParagraphFeedbackStatus(prev => ({
                ...prev,
                [paragraphIndex]: { loading: true }
            }));

            try {
                const para = paragraphs[paragraphIndex];
                const response = await fetch('/api/feedback/hierarchical/paragraph', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        qaRecordId,
                        documentName,
                        documentId,
                        paragraphIndex,
                        contentPreview: para?.preview || '',
                        startOffset: para?.startOffset || 0,
                        endOffset: para?.endOffset || 0,
                        helpful,
                        feedbackType
                    })
                });
                if (response.ok) {
                    // 更新状态显示成功 (Update state to show success)
                    setParagraphFeedbackStatus(prev => ({
                        ...prev,
                        [paragraphIndex]: {
                            submitted: true,
                            helpful,
                            feedbackType,
                            loading: false
                        }
                    }));
                } else {
                    setParagraphFeedbackStatus(prev => ({
                        ...prev,
                        [paragraphIndex]: { loading: false, error: true }
                    }));
                }
            } catch (err) {
                console.error(t('hierarchicalLogParagraphFeedbackError') || '段落反馈失败:', err);
                setParagraphFeedbackStatus(prev => ({
                    ...prev,
                    [paragraphIndex]: { loading: false, error: true }
                }));
            }
        };

        // 添加高亮
        const addHighlight = async (highlightType, isKeyInfo = false) => {
            if (!selectedText) return;

            const newHighlight = {
                sentenceIndex: highlights.length,
                content: selectedText.text,
                startOffset: selectedText.start,
                endOffset: selectedText.end,
                highlightType,
                keyInformation: isKeyInfo,
                annotation: ''
            };

            const newHighlights = [...highlights, newHighlight];
            setHighlights(newHighlights);
            setSelectedText(null);

            // 提交到后端
            try {
                await fetch('/api/feedback/hierarchical/sentence', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        qaRecordId,
                        documentName,
                        documentId,
                        ...newHighlight
                    })
                });
            } catch (err) {
                console.error(t('hierarchicalLogHighlightSaveError'), err);
            }
        };

        // 处理文本选择
        const handleTextSelection = () => {
            const selection = window.getSelection();
            if (selection && selection.toString().trim()) {
                setSelectedText({
                    text: selection.toString(),
                    start: 0, // 简化处理
                    end: selection.toString().length
                });
            }
        };

        // 渲染标签页 (Render Tabs)
        const renderTabs = () => (
            <div className="hierarchical-tabs">
                <button
                    className={`hierarchical-tab ${activeTab === 'document' ? 'hierarchical-tab-active' : ''}`}
                    onClick={() => setActiveTab('document')}
                >
                    {t('hierarchicalDocLevel')}
                </button>
                <button
                    className={`hierarchical-tab ${activeTab === 'paragraph' ? 'hierarchical-tab-active' : ''}`}
                    onClick={() => setActiveTab('paragraph')}
                >
                    {t('hierarchicalParagraphLevel')}
                </button>
                <button
                    className={`hierarchical-tab ${activeTab === 'sentence' ? 'hierarchical-tab-active' : ''}`}
                    onClick={() => setActiveTab('sentence')}
                >
                    {t('hierarchicalSentenceLevel')}
                </button>
            </div>
        );

        // 渲染文档级反馈 (Render Document Feedback)
        const renderDocumentFeedback = () => (
            <div className="hierarchical-feedback-section">
                <h4>{t('hierarchicalRatingIcon')} {t('hierarchicalRating')}</h4>
                <div className="hierarchical-rating-row">
                    {[1, 2, 3, 4, 5].map(star => (
                        <span
                            key={star}
                            className="hierarchical-star"
                            style={{color: star <= docRating ? '#ffc107' : '#ddd'}}
                            onClick={() => setDocRating(star)}
                        >
                            ★
                        </span>
                    ))}
                </div>

                <h4>{t('hierarchicalRelevanceIcon')} {t('hierarchicalRelevance')}</h4>
                <select
                    className="hierarchical-select"
                    value={docRelevance}
                    onChange={(e) => setDocRelevance(e.target.value)}
                >
                    <option value="">{t('hierarchicalSelectPlaceholder')}</option>
                    <option value="HIGHLY_RELEVANT">{t('hierarchicalRelevanceHighly')}</option>
                    <option value="RELEVANT">{t('hierarchicalRelevanceRelevant')}</option>
                    <option value="PARTIALLY_RELEVANT">{t('hierarchicalRelevancePartially')}</option>
                    <option value="NOT_RELEVANT">{t('hierarchicalRelevanceNot')}</option>
                    <option value="MISLEADING">{t('hierarchicalRelevanceMisleading')}</option>
                </select>

                <h4>{t('hierarchicalCommentIcon')} {t('hierarchicalComment')}</h4>
                <textarea
                    className="hierarchical-textarea"
                    value={docComment}
                    onChange={(e) => setDocComment(e.target.value)}
                    placeholder={t('hierarchicalCommentPlaceholder')}
                />

                <h4>{t('hierarchicalTagsIcon')} {t('hierarchicalTags')}</h4>
                <div className="hierarchical-tags-container">
                    {[t('hierarchicalTagAccurate'), t('hierarchicalTagUseful'), t('hierarchicalTagDetailed'), t('hierarchicalTagNeedsMore'), t('hierarchicalTagOutdated'), t('hierarchicalTagWrong')].map(tag => (
                        <span
                            key={tag}
                            className={`hierarchical-tag ${docTags.includes(tag) ? 'hierarchical-tag-selected' : ''}`}
                            onClick={() => {
                                setDocTags(prev =>
                                    prev.includes(tag)
                                        ? prev.filter(t => t !== tag)
                                        : [...prev, tag]
                                );
                            }}
                        >
                            {tag}
                        </span>
                    ))}
                </div>

                <button
                    className="hierarchical-submit-btn"
                    onClick={submitDocumentFeedback}
                    disabled={loading || docRating === 0}
                >
                    {loading ? t('hierarchicalSubmitting') : t('hierarchicalSubmit')}
                </button>
            </div>
        );

        // 渲染段落级反馈 (Render Paragraph Feedback)
        const renderParagraphFeedback = () => (
            <div className="hierarchical-feedback-section">
                <p className="hierarchical-hint">{t('hierarchicalParagraphHint') || '点击段落旁的按钮标记是否有帮助'}</p>

                {contentLoading ? (
                    <p className="hierarchical-loading-text">🔄 {t('hierarchicalLoadingContent') || '正在加载文档内容...'}</p>
                ) : paragraphs.length === 0 ? (
                    <p className="hierarchical-loading-text">🔄 {t('hierarchicalAnalyzingParagraphs') || '正在分析段落...'}</p>
                ) : (
                    <div className="hierarchical-paragraph-list">
                        {paragraphs.map((para, idx) => {
                            const feedbackStatus = paragraphFeedbackStatus[idx];
                            const isSubmitted = feedbackStatus?.submitted;
                            const isLoading = feedbackStatus?.loading;

                            return (
                                <div
                                    key={idx}
                                    className="hierarchical-paragraph-item"
                                    style={{
                                        backgroundColor: isSubmitted
                                            ? (feedbackStatus.helpful ? '#e8f5e9' : '#ffebee')
                                            : '#fff',
                                        borderLeft: isSubmitted
                                            ? `4px solid ${feedbackStatus.helpful ? '#4caf50' : '#f44336'}`
                                            : '4px solid transparent'
                                    }}
                                >
                                    <div className="hierarchical-paragraph-content">
                                        <span className="hierarchical-paragraph-index">#{idx + 1}</span>
                                        <span>{para.preview}</span>
                                        {isSubmitted && (
                                            <span style={{
                                                marginLeft: '10px',
                                                fontSize: '12px',
                                                color: feedbackStatus.helpful ? '#4caf50' : '#f44336'
                                            }}>
                                                {feedbackStatus.helpful ? '✅ ' + (t('hierarchicalMarkedHelpful') || '已标记有帮助') : '❌ ' + (t('hierarchicalMarkedNotHelpful') || '已标记无帮助')}
                                            </span>
                                        )}
                                    </div>
                                    <div className="hierarchical-paragraph-actions">
                                        {isLoading ? (
                                            <span style={{fontSize: '14px'}}>⏳</span>
                                        ) : (
                                            <>
                                                <button
                                                    className="hierarchical-helpful-btn"
                                                    style={{opacity: isSubmitted && feedbackStatus.feedbackType !== 'KEY_POINT' ? 0.5 : 1}}
                                                    onClick={() => submitParagraphFeedback(idx, true, 'KEY_POINT')}
                                                    title={t('hierarchicalKeyPoint') || '关键要点'}
                                                    disabled={isLoading}
                                                >
                                                    🔑
                                                </button>
                                                <button
                                                    className="hierarchical-helpful-btn"
                                                    style={{opacity: isSubmitted && feedbackStatus.feedbackType !== 'SUPPORTING_DETAIL' ? 0.5 : 1}}
                                                    onClick={() => submitParagraphFeedback(idx, true, 'SUPPORTING_DETAIL')}
                                                    title={t('hierarchicalSupportingDetail') || '支撑细节'}
                                                    disabled={isLoading}
                                                >
                                                    👍
                                                </button>
                                                <button
                                                    className="hierarchical-not-helpful-btn"
                                                    style={{opacity: isSubmitted && feedbackStatus.feedbackType !== 'IRRELEVANT' ? 0.5 : 1}}
                                                    onClick={() => submitParagraphFeedback(idx, false, 'IRRELEVANT')}
                                                    title={t('hierarchicalIrrelevant') || '不相关'}
                                                    disabled={isLoading}
                                                >
                                                    👎
                                                </button>
                                                <button
                                                    className="hierarchical-not-helpful-btn"
                                                    style={{opacity: isSubmitted && feedbackStatus.feedbackType !== 'WRONG_INFO' ? 0.5 : 1}}
                                                    onClick={() => submitParagraphFeedback(idx, false, 'WRONG_INFO')}
                                                    title={t('hierarchicalWrongInfo') || '错误信息'}
                                                    disabled={isLoading}
                                                >
                                                    ❌
                                                </button>
                                            </>
                                        )}
                                    </div>
                                </div>
                            );
                        })}
                    </div>
                )}
            </div>
        );

        // 渲染句子级反馈（高亮标记）(Render Sentence Feedback - Highlight Marking)
        const renderSentenceFeedback = () => (
            <div className="hierarchical-feedback-section">
                <p className="hierarchical-hint">{t('hierarchicalSentenceHint')}</p>

                {selectedText && (
                    <div className="hierarchical-selection-toolbar">
                        <span className="hierarchical-selected-text">"{selectedText.text.substring(0, 50)}..."</span>
                        <div className="hierarchical-highlight-btns">
                            <button
                                className="hierarchical-highlight-btn"
                                style={{backgroundColor: '#4caf50'}}
                                onClick={() => addHighlight('ANSWER', true)}
                                title={t('hierarchicalMarkAsAnswer')}
                            >
                                {t('hierarchicalHighlightAnswer')}
                            </button>
                            <button
                                className="hierarchical-highlight-btn"
                                style={{backgroundColor: '#2196f3'}}
                                onClick={() => addHighlight('KEY_FACT', true)}
                                title={t('hierarchicalMarkAsKeyFact')}
                            >
                                {t('hierarchicalHighlightKeyFact')}
                            </button>
                            <button
                                className="hierarchical-highlight-btn"
                                style={{backgroundColor: '#ff9800'}}
                                onClick={() => addHighlight('IMPORTANT')}
                                title={t('hierarchicalMarkAsImportant')}
                            >
                                {t('hierarchicalHighlightImportant')}
                            </button>
                            <button
                                className="hierarchical-highlight-btn"
                                style={{backgroundColor: '#f44336'}}
                                onClick={() => addHighlight('WRONG')}
                                title={t('hierarchicalMarkAsWrong')}
                            >
                                {t('hierarchicalHighlightWrong')}
                            </button>
                        </div>
                    </div>
                )}

                <div
                    className="hierarchical-content-area"
                    onMouseUp={handleTextSelection}
                >
                    {documentContent || t('hierarchicalNoContent')}
                </div>

                {highlights.length > 0 && (
                    <div className="hierarchical-highlights-list">
                        <h4>{t('hierarchicalHighlightsList')} ({highlights.length})</h4>
                        {highlights.map((h, idx) => (
                            <div key={idx} className="hierarchical-highlight-item">
                                <span 
                                    className="hierarchical-highlight-type"
                                    style={{backgroundColor: getHighlightColor(h.highlightType)}}
                                >
                                    {h.highlightType}
                                </span>
                                <span>{h.content.substring(0, 30)}...</span>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        );

        return (
            <div className="hierarchical-panel">
                <div className="hierarchical-header">
                    <h3 className="hierarchical-header-title">
                        {t('hierarchicalFeedbackTitle')} - {documentName}
                    </h3>
                    {onClose && (
                        <button 
                            className="hierarchical-close-btn"
                            onClick={onClose}
                            title={t('hierarchicalFeedbackModalClose')}
                        >
                            ×
                        </button>
                    )}
                </div>

                {renderTabs()}

                <div className="hierarchical-content">
                    {activeTab === 'document' && renderDocumentFeedback()}
                    {activeTab === 'paragraph' && renderParagraphFeedback()}
                    {activeTab === 'sentence' && renderSentenceFeedback()}
                </div>
            </div>
        );
    }

    // 获取高亮颜色 (Get Highlight Color)
    function getHighlightColor(type) {
        const colors = {
            'ANSWER': '#4caf50',
            'KEY_FACT': '#2196f3',
            'IMPORTANT': '#ff9800',
            'EXAMPLE': '#9c27b0',
            'DEFINITION': '#00bcd4',
            'WRONG': '#f44336',
            'UNCERTAIN': '#9e9e9e'
        };
        return colors[type] || '#9e9e9e';
    }

    // 样式已提取到 hierarchical-feedback.css (Styles extracted to hierarchical-feedback.css)

    // 导出组件 (Export Component)
    window.HierarchicalFeedbackPanel = HierarchicalFeedbackPanel;

    console.log(window.LanguageModule ? window.LanguageModule.useTranslation().t('hierarchicalLogComponentLoaded') : '✅ HierarchicalFeedbackPanel loaded');
})();
