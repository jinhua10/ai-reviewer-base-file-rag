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

        // 渲染标签页
        const renderTabs = () => (
            <div style={styles.tabs}>
                <button
                    style={{...styles.tab, ...(activeTab === 'document' ? styles.tabActive : {})}}
                    onClick={() => setActiveTab('document')}
                >
                    {t('hierarchicalDocLevel')}
                </button>
                <button
                    style={{...styles.tab, ...(activeTab === 'paragraph' ? styles.tabActive : {})}}
                    onClick={() => setActiveTab('paragraph')}
                >
                    {t('hierarchicalParagraphLevel')}
                </button>
                <button
                    style={{...styles.tab, ...(activeTab === 'sentence' ? styles.tabActive : {})}}
                    onClick={() => setActiveTab('sentence')}
                >
                    {t('hierarchicalSentenceLevel')}
                </button>
            </div>
        );

        // 渲染文档级反馈
        const renderDocumentFeedback = () => (
            <div style={styles.feedbackSection}>
                <h4>{t('hierarchicalRatingIcon')} {t('hierarchicalRating')}</h4>
                <div style={styles.ratingRow}>
                    {[1, 2, 3, 4, 5].map(star => (
                        <span
                            key={star}
                            style={{...styles.star, color: star <= docRating ? '#ffc107' : '#ddd'}}
                            onClick={() => setDocRating(star)}
                        >
                            ★
                        </span>
                    ))}
                </div>

                <h4>{t('hierarchicalRelevanceIcon')} {t('hierarchicalRelevance')}</h4>
                <select
                    style={styles.select}
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
                    style={styles.textarea}
                    value={docComment}
                    onChange={(e) => setDocComment(e.target.value)}
                    placeholder={t('hierarchicalCommentPlaceholder')}
                />

                <h4>{t('hierarchicalTagsIcon')} {t('hierarchicalTags')}</h4>
                <div style={styles.tagsContainer}>
                    {[t('hierarchicalTagAccurate'), t('hierarchicalTagUseful'), t('hierarchicalTagDetailed'), t('hierarchicalTagNeedsMore'), t('hierarchicalTagOutdated'), t('hierarchicalTagWrong')].map(tag => (
                        <span
                            key={tag}
                            style={{
                                ...styles.tag,
                                ...(docTags.includes(tag) ? styles.tagSelected : {})
                            }}
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
                    style={styles.submitBtn}
                    onClick={submitDocumentFeedback}
                    disabled={loading || docRating === 0}
                >
                    {loading ? t('hierarchicalSubmitting') : t('hierarchicalSubmit')}
                </button>
            </div>
        );

        // 渲染段落级反馈 (Render paragraph-level feedback)
        const renderParagraphFeedback = () => (
            <div style={styles.feedbackSection}>
                <p style={styles.hint}>{t('hierarchicalParagraphHint') || '点击段落旁的按钮标记是否有帮助'}</p>

                {contentLoading ? (
                    <p style={styles.loadingText}>🔄 {t('hierarchicalLoadingContent') || '正在加载文档内容...'}</p>
                ) : paragraphs.length === 0 ? (
                    <p style={styles.loadingText}>🔄 {t('hierarchicalAnalyzingParagraphs') || '正在分析段落...'}</p>
                ) : (
                    <div style={styles.paragraphList}>
                        {paragraphs.map((para, idx) => {
                            const feedbackStatus = paragraphFeedbackStatus[idx];
                            const isSubmitted = feedbackStatus?.submitted;
                            const isLoading = feedbackStatus?.loading;

                            return (
                                <div
                                    key={idx}
                                    style={{
                                        ...styles.paragraphItem,
                                        backgroundColor: isSubmitted
                                            ? (feedbackStatus.helpful ? '#e8f5e9' : '#ffebee')
                                            : '#fff',
                                        borderLeft: isSubmitted
                                            ? `4px solid ${feedbackStatus.helpful ? '#4caf50' : '#f44336'}`
                                            : '4px solid transparent'
                                    }}
                                >
                                    <div style={styles.paragraphContent}>
                                        <span style={styles.paragraphIndex}>#{idx + 1}</span>
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
                                    <div style={styles.paragraphActions}>
                                        {isLoading ? (
                                            <span style={{fontSize: '14px'}}>⏳</span>
                                        ) : (
                                            <>
                                                <button
                                                    style={{
                                                        ...styles.helpfulBtn,
                                                        opacity: isSubmitted && feedbackStatus.feedbackType !== 'KEY_POINT' ? 0.5 : 1
                                                    }}
                                                    onClick={() => submitParagraphFeedback(idx, true, 'KEY_POINT')}
                                                    title={t('hierarchicalKeyPoint') || '关键要点'}
                                                    disabled={isLoading}
                                                >
                                                    🔑
                                                </button>
                                                <button
                                                    style={{
                                                        ...styles.helpfulBtn,
                                                        opacity: isSubmitted && feedbackStatus.feedbackType !== 'SUPPORTING_DETAIL' ? 0.5 : 1
                                                    }}
                                                    onClick={() => submitParagraphFeedback(idx, true, 'SUPPORTING_DETAIL')}
                                                    title={t('hierarchicalSupportingDetail') || '支撑细节'}
                                                    disabled={isLoading}
                                                >
                                                    👍
                                                </button>
                                                <button
                                                    style={{
                                                        ...styles.notHelpfulBtn,
                                                        opacity: isSubmitted && feedbackStatus.feedbackType !== 'IRRELEVANT' ? 0.5 : 1
                                                    }}
                                                    onClick={() => submitParagraphFeedback(idx, false, 'IRRELEVANT')}
                                                    title={t('hierarchicalIrrelevant') || '不相关'}
                                                    disabled={isLoading}
                                                >
                                                    👎
                                                </button>
                                                <button
                                                    style={{
                                                        ...styles.notHelpfulBtn,
                                                        opacity: isSubmitted && feedbackStatus.feedbackType !== 'WRONG_INFO' ? 0.5 : 1
                                                    }}
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

        // 渲染句子级反馈（高亮标记）
        const renderSentenceFeedback = () => (
            <div style={styles.feedbackSection}>
                <p style={styles.hint}>{t('hierarchicalSentenceHint')}</p>

                {selectedText && (
                    <div style={styles.selectionToolbar}>
                        <span style={styles.selectedText}>"{selectedText.text.substring(0, 50)}..."</span>
                        <div style={styles.highlightBtns}>
                            <button
                                style={{...styles.highlightBtn, backgroundColor: '#4caf50'}}
                                onClick={() => addHighlight('ANSWER', true)}
                                title={t('hierarchicalMarkAsAnswer')}
                            >
                                {t('hierarchicalHighlightAnswer')}
                            </button>
                            <button
                                style={{...styles.highlightBtn, backgroundColor: '#2196f3'}}
                                onClick={() => addHighlight('KEY_FACT', true)}
                                title={t('hierarchicalMarkAsKeyFact')}
                            >
                                {t('hierarchicalHighlightKeyFact')}
                            </button>
                            <button
                                style={{...styles.highlightBtn, backgroundColor: '#ff9800'}}
                                onClick={() => addHighlight('IMPORTANT')}
                                title={t('hierarchicalMarkAsImportant')}
                            >
                                {t('hierarchicalHighlightImportant')}
                            </button>
                            <button
                                style={{...styles.highlightBtn, backgroundColor: '#f44336'}}
                                onClick={() => addHighlight('WRONG')}
                                title={t('hierarchicalMarkAsWrong')}
                            >
                                {t('hierarchicalHighlightWrong')}
                            </button>
                        </div>
                    </div>
                )}

                <div
                    style={styles.contentArea}
                    onMouseUp={handleTextSelection}
                >
                    {documentContent || t('hierarchicalNoContent')}
                </div>

                {highlights.length > 0 && (
                    <div style={styles.highlightsList}>
                        <h4>{t('hierarchicalHighlightsList')} ({highlights.length})</h4>
                        {highlights.map((h, idx) => (
                            <div key={idx} style={styles.highlightItem}>
                                <span style={{
                                    ...styles.highlightType,
                                    backgroundColor: getHighlightColor(h.highlightType)
                                }}>
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
            <div style={styles.panel}>
                <div style={styles.header}>
                    <h3 style={{ margin: 0, color: '#ffffff', fontSize: '20px', fontWeight: '600' }}>
                        {t('hierarchicalFeedbackTitle')} - {documentName}
                    </h3>
                    {onClose && (
                        <button 
                            style={styles.closeBtn} 
                            onClick={onClose}
                            onMouseEnter={(e) => {
                                e.currentTarget.style.transform = 'scale(1.1) rotate(90deg)';
                                e.currentTarget.style.boxShadow = '0 4px 12px rgba(238, 90, 111, 0.5)';
                            }}
                            onMouseLeave={(e) => {
                                e.currentTarget.style.transform = 'scale(1) rotate(0deg)';
                                e.currentTarget.style.boxShadow = '0 2px 8px rgba(238, 90, 111, 0.3)';
                            }}
                            title={t('hierarchicalFeedbackModalClose')}
                        >
                            ×
                        </button>
                    )}
                </div>

                {renderTabs()}

                <div style={styles.content}>
                    {activeTab === 'document' && renderDocumentFeedback()}
                    {activeTab === 'paragraph' && renderParagraphFeedback()}
                    {activeTab === 'sentence' && renderSentenceFeedback()}
                </div>
            </div>
        );
    }

    // 获取高亮颜色
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

    // 样式
    const styles = {
        panel: {
            backgroundColor: '#fff',
            borderRadius: '12px',
            boxShadow: '0 4px 20px rgba(0,0,0,0.15)',
            width: '100%',
            margin: '0 auto'
        },
        header: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '20px 24px',
            borderBottom: '2px solid #e8eaf6',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            borderRadius: '12px 12px 0 0'
        },
        closeBtn: {
            width: '36px',
            height: '36px',
            borderRadius: '50%',
            background: 'linear-gradient(135deg, #ff6b6b 0%, #ee5a6f 100%)',
            border: 'none',
            fontSize: '22px',
            fontWeight: 'bold',
            cursor: 'pointer',
            color: '#fff',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'all 0.3s ease',
            boxShadow: '0 2px 8px rgba(238, 90, 111, 0.3)',
            lineHeight: '1'
        },
        tabs: {
            display: 'flex',
            borderBottom: '1px solid #eee'
        },
        tab: {
            flex: 1,
            padding: '12px',
            border: 'none',
            backgroundColor: '#fff',
            cursor: 'pointer',
            fontSize: '14px',
            transition: 'all 0.2s'
        },
        tabActive: {
            backgroundColor: '#e3f2fd',
            borderBottom: '2px solid #2196f3',
            fontWeight: 'bold'
        },
        content: {
            padding: '20px'
        },
        feedbackSection: {

        },
        ratingRow: {
            display: 'flex',
            gap: '5px',
            marginBottom: '15px'
        },
        star: {
            fontSize: '28px',
            cursor: 'pointer',
            transition: 'transform 0.2s'
        },
        select: {
            width: '100%',
            padding: '10px',
            borderRadius: '4px',
            border: '1px solid #ddd',
            marginBottom: '15px'
        },
        textarea: {
            width: '100%',
            minHeight: '80px',
            padding: '10px',
            borderRadius: '4px',
            border: '1px solid #ddd',
            marginBottom: '15px',
            resize: 'vertical'
        },
        tagsContainer: {
            display: 'flex',
            flexWrap: 'wrap',
            gap: '8px',
            marginBottom: '15px'
        },
        tag: {
            padding: '5px 12px',
            borderRadius: '15px',
            border: '1px solid #ddd',
            cursor: 'pointer',
            fontSize: '13px',
            transition: 'all 0.2s'
        },
        tagSelected: {
            backgroundColor: '#2196f3',
            color: '#fff',
            borderColor: '#2196f3'
        },
        submitBtn: {
            width: '100%',
            padding: '12px',
            backgroundColor: '#4caf50',
            color: '#fff',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '15px'
        },
        hint: {
            color: '#666',
            fontSize: '13px',
            marginBottom: '15px'
        },
        paragraphList: {
            maxHeight: '400px',
            overflowY: 'auto'
        },
        paragraphItem: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '10px',
            borderBottom: '1px solid #eee',
            gap: '10px'
        },
        paragraphContent: {
            flex: 1,
            fontSize: '13px'
        },
        paragraphIndex: {
            color: '#999',
            marginRight: '8px'
        },
        paragraphActions: {
            display: 'flex',
            gap: '5px'
        },
        helpfulBtn: {
            padding: '5px 8px',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            backgroundColor: '#e8f5e9'
        },
        notHelpfulBtn: {
            padding: '5px 8px',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            backgroundColor: '#ffebee'
        },
        selectionToolbar: {
            backgroundColor: '#f5f5f5',
            padding: '10px',
            borderRadius: '4px',
            marginBottom: '15px'
        },
        selectedText: {
            display: 'block',
            marginBottom: '10px',
            fontStyle: 'italic',
            color: '#666'
        },
        highlightBtns: {
            display: 'flex',
            gap: '8px',
            flexWrap: 'wrap'
        },
        highlightBtn: {
            padding: '6px 12px',
            color: '#fff',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '12px'
        },
        contentArea: {
            padding: '15px',
            backgroundColor: '#fafafa',
            borderRadius: '4px',
            maxHeight: '300px',
            overflowY: 'auto',
            fontSize: '14px',
            lineHeight: '1.6',
            userSelect: 'text'
        },
        highlightsList: {
            marginTop: '15px',
            padding: '10px',
            backgroundColor: '#f5f5f5',
            borderRadius: '4px'
        },
        highlightItem: {
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            padding: '5px 0',
            borderBottom: '1px solid #eee'
        },
        highlightType: {
            padding: '2px 8px',
            borderRadius: '3px',
            color: '#fff',
            fontSize: '11px'
        },
        loadingText: {
            textAlign: 'center',
            color: '#666',
            padding: '20px'
        }
    };

    // 导出组件
    window.HierarchicalFeedbackPanel = HierarchicalFeedbackPanel;

    console.log(window.LanguageModule ? window.LanguageModule.useTranslation().t('hierarchicalLogComponentLoaded') : '✅ HierarchicalFeedbackPanel loaded');
})();
