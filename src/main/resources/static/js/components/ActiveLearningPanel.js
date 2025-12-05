/**
 * 主动学习推荐组件 / Active Learning Recommendation Component
 * 显示系统推荐的可能相关文档，让用户确认/否认
 *
 * @author AI Reviewer Team
 * @since 2025-12-05
 */

(function() {
    'use strict';

    const { useState, useEffect } = React;

    /**
     * 主动学习推荐面板
     */
    function ActiveLearningPanel({
        question,
        retrievedDocs = [],
        topKUsed = 5,
        onFeedbackSubmit,
        t = (key) => key
    }) {
        const [recommendations, setRecommendations] = useState(null);
        const [loading, setLoading] = useState(false);
        const [expanded, setExpanded] = useState(true);
        const [feedbackGiven, setFeedbackGiven] = useState({});

        // 获取推荐
        useEffect(() => {
            if (question && retrievedDocs.length > 0) {
                fetchRecommendations();
            }
        }, [question, retrievedDocs.length]);

        const fetchRecommendations = async () => {
            setLoading(true);
            try {
                const response = await fetch('/api/feedback/hierarchical/active-learning/recommendations', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        question,
                        retrievedDocs: retrievedDocs.slice(0, 20), // 只发送前20个
                        topKUsed
                    })
                });

                if (response.ok) {
                    const data = await response.json();
                    if (data.success) {
                        setRecommendations(data.recommendations);
                    }
                }
            } catch (err) {
                console.error(t('activeLearningLogRecommendationError'), err);
            } finally {
                setLoading(false);
            }
        };

        // 提交反馈
        const submitFeedback = async (documentName, isRelevant) => {
            try {
                const response = await fetch('/api/feedback/hierarchical/active-learning/feedback', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        documentName,
                        relevant: isRelevant,
                        question
                    })
                });

                if (response.ok) {
                    setFeedbackGiven(prev => ({
                        ...prev,
                        [documentName]: isRelevant ? 'relevant' : 'irrelevant'
                    }));

                    if (onFeedbackSubmit) {
                        onFeedbackSubmit(documentName, isRelevant);
                    }
                }
            } catch (err) {
                console.error(t('activeLearningLogFeedbackError'), err);
            }
        };

        // 如果没有推荐或正在加载，显示简单状态
        if (loading) {
            return (
                <div style={styles.panel}>
                    <div style={styles.loading}>{t('activeLearningAnalyzing')}</div>
                </div>
            );
        }

        if (!recommendations || !recommendations.needsConfirmation) {
            return null; // 不需要确认时不显示
        }

        const hasRecommendations =
            (recommendations.uncertainDocuments?.length > 0) ||
            (recommendations.potentiallyRelevantDocuments?.length > 0) ||
            (recommendations.historyBasedRecommendations?.length > 0);

        if (!hasRecommendations) {
            return null;
        }

        return (
            <div style={styles.panel}>
                {/* 标题栏 */}
                <div
                    style={styles.header}
                    onClick={() => setExpanded(!expanded)}
                >
                    <span style={styles.title}>
                        {t('activeLearningTitle')}
                        <span style={styles.badge}>
                            {(recommendations.uncertainDocuments?.length || 0) +
                             (recommendations.potentiallyRelevantDocuments?.length || 0) +
                             (recommendations.historyBasedRecommendations?.length || 0)}
                        </span>
                    </span>
                    <span style={styles.expandIcon}>
                        {expanded ? '▼' : '▶'}
                    </span>
                </div>

                {expanded && (
                    <div style={styles.content}>
                        {/* 置信度指示器 */}
                        <div style={styles.confidenceBar}>
                            <span>{t('activeLearningConfidence')}:</span>
                            <div style={styles.progressBar}>
                                <div style={{
                                    ...styles.progressFill,
                                    width: `${(recommendations.confidenceScore || 0) * 100}%`,
                                    backgroundColor: getConfidenceColor(recommendations.confidenceScore)
                                }} />
                            </div>
                            <span>{Math.round((recommendations.confidenceScore || 0) * 100)}%</span>
                        </div>

                        {/* 推荐原因 */}
                        {recommendations.recommendationReason && (
                            <div style={styles.reason}>
                                💡 {recommendations.recommendationReason}
                            </div>
                        )}

                        {/* 边界文档（不确定性高的文档） */}
                        {recommendations.uncertainDocuments?.length > 0 && (
                            <div style={styles.section}>
                                <h4 style={styles.sectionTitle}>
                                    {t('activeLearningQuestionMark')} {t('activeLearningBoundaryDocs')}
                                </h4>
                                {recommendations.uncertainDocuments.map((doc, idx) => (
                                    <div key={idx} style={styles.docItem}>
                                        <div style={styles.docInfo}>
                                            <span style={styles.docName}>
                                                {t('activeLearningHashMark')}{doc.rank} {doc.document?.title || t('activeLearningUnknownDoc')}
                                            </span>
                                            <span style={styles.docReason}>{doc.reason}</span>
                                        </div>
                                        {renderFeedbackButtons(doc.document?.title)}
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* 历史高分文档 */}
                        {recommendations.potentiallyRelevantDocuments?.length > 0 && (
                            <div style={styles.section}>
                                <h4 style={styles.sectionTitle}>
                                    {t('activeLearningStarMark')} {t('activeLearningHighRatedDocs')}
                                </h4>
                                {recommendations.potentiallyRelevantDocuments.map((doc, idx) => (
                                    <div key={idx} style={styles.docItem}>
                                        <div style={styles.docInfo}>
                                            <span style={styles.docName}>{doc.documentName}</span>
                                            <span style={styles.docReason}>
                                                {t('activeLearningWeight')}: {doc.historicalWeight?.toFixed(2)} - {doc.reason}
                                            </span>
                                        </div>
                                        {renderFeedbackButtons(doc.documentName)}
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* 基于相似问题的推荐 */}
                        {recommendations.historyBasedRecommendations?.length > 0 && (
                            <div style={styles.section}>
                                <h4 style={styles.sectionTitle}>
                                    {t('activeLearningBookMark')} {t('activeLearningHistoryBased')}
                                </h4>
                                {recommendations.historyBasedRecommendations.map((rec, idx) => (
                                    <div key={idx} style={styles.docItem}>
                                        <div style={styles.docInfo}>
                                            <span style={styles.docName}>{rec.documentName}</span>
                                            <span style={styles.docReason}>
                                                {t('activeLearningFromSimilar')} ({t('activeLearningSimilarity')}: {Math.round(rec.similarityScore * 100)}%)
                                            </span>
                                        </div>
                                        {renderFeedbackButtons(rec.documentName)}
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                )}
            </div>
        );

        // 渲染反馈按钮
        function renderFeedbackButtons(documentName) {
            const feedback = feedbackGiven[documentName];

            if (feedback) {
                return (
                    <div style={styles.feedbackGiven}>
                        {feedback === 'relevant' ? t('activeLearningMarkedRelevant') : t('activeLearningMarkedIrrelevant')}
                    </div>
                );
            }

            return (
                <div style={styles.feedbackBtns}>
                    <button
                        style={{...styles.feedbackBtn, ...styles.relevantBtn}}
                        onClick={() => submitFeedback(documentName, true)}
                        title={t('activeLearningRelevantTitle')}
                    >
                        {t('activeLearningRelevant')}
                    </button>
                    <button
                        style={{...styles.feedbackBtn, ...styles.irrelevantBtn}}
                        onClick={() => submitFeedback(documentName, false)}
                        title={t('activeLearningIrrelevantTitle')}
                    >
                        {t('activeLearningIrrelevant')}
                    </button>
                </div>
            );
        }
    }

    // 获取置信度颜色
    function getConfidenceColor(confidence) {
        if (confidence >= 0.7) return '#4caf50';
        if (confidence >= 0.4) return '#ff9800';
        return '#f44336';
    }

    // 样式
    const styles = {
        panel: {
            backgroundColor: '#fff8e1',
            borderRadius: '8px',
            border: '1px solid #ffca28',
            margin: '10px 0',
            overflow: 'hidden'
        },
        header: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '12px 15px',
            backgroundColor: '#fff3e0',
            cursor: 'pointer',
            userSelect: 'none'
        },
        title: {
            fontWeight: 'bold',
            fontSize: '14px',
            display: 'flex',
            alignItems: 'center',
            gap: '8px'
        },
        badge: {
            backgroundColor: '#ff9800',
            color: '#fff',
            padding: '2px 8px',
            borderRadius: '10px',
            fontSize: '12px'
        },
        expandIcon: {
            color: '#666',
            fontSize: '12px'
        },
        content: {
            padding: '15px'
        },
        loading: {
            padding: '15px',
            textAlign: 'center',
            color: '#666'
        },
        confidenceBar: {
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            marginBottom: '15px',
            fontSize: '13px'
        },
        progressBar: {
            flex: 1,
            height: '8px',
            backgroundColor: '#eee',
            borderRadius: '4px',
            overflow: 'hidden'
        },
        progressFill: {
            height: '100%',
            borderRadius: '4px',
            transition: 'width 0.3s'
        },
        reason: {
            backgroundColor: '#e3f2fd',
            padding: '10px',
            borderRadius: '4px',
            fontSize: '13px',
            marginBottom: '15px'
        },
        section: {
            marginBottom: '15px'
        },
        sectionTitle: {
            fontSize: '13px',
            fontWeight: 'bold',
            marginBottom: '10px',
            color: '#666'
        },
        docItem: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '10px',
            backgroundColor: '#fff',
            borderRadius: '4px',
            marginBottom: '8px',
            border: '1px solid #eee'
        },
        docInfo: {
            flex: 1
        },
        docName: {
            display: 'block',
            fontWeight: '500',
            fontSize: '13px',
            marginBottom: '3px'
        },
        docReason: {
            display: 'block',
            fontSize: '11px',
            color: '#888'
        },
        feedbackBtns: {
            display: 'flex',
            gap: '5px'
        },
        feedbackBtn: {
            padding: '5px 10px',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '12px',
            transition: 'transform 0.2s'
        },
        relevantBtn: {
            backgroundColor: '#e8f5e9',
            color: '#2e7d32'
        },
        irrelevantBtn: {
            backgroundColor: '#ffebee',
            color: '#c62828'
        },
        feedbackGiven: {
            fontSize: '12px',
            color: '#666',
            fontStyle: 'italic'
        }
    };

    // 导出组件
    window.ActiveLearningPanel = ActiveLearningPanel;

    // 获取翻译函数并输出加载日志
    const getT = () => window.LanguageModule ? window.LanguageModule.useTranslation().t : (k) => k;
    console.log(getT()('activeLearningLogComponentLoaded'));
})();

