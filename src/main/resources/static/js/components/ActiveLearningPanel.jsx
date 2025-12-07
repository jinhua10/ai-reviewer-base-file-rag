/**
 * 主动学习推荐组件 / Active Learning Recommendation Component
 * JSX 版本 - 使用 Babel 转译
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

        // 渲染反馈按钮
        function renderFeedbackButtons(documentName) {
            const feedback = feedbackGiven[documentName];

            if (feedback) {
                return (
                    <div className="active-learning-feedback-given">
                        {feedback === 'relevant' ? t('activeLearningMarkedRelevant') : t('activeLearningMarkedIrrelevant')}
                    </div>
                );
            }

            return (
                <div className="active-learning-feedback-btns">
                    <button
                        className="active-learning-feedback-btn active-learning-relevant-btn"
                        onClick={() => submitFeedback(documentName, true)}
                        title={t('activeLearningRelevantTitle')}
                    >
                        {t('activeLearningRelevant')}
                    </button>
                    <button
                        className="active-learning-feedback-btn active-learning-irrelevant-btn"
                        onClick={() => submitFeedback(documentName, false)}
                        title={t('activeLearningIrrelevantTitle')}
                    >
                        {t('activeLearningIrrelevant')}
                    </button>
                </div>
            );
        }

        // 如果没有推荐或正在加载，显示简单状态 (If no recommendations or loading, show simple state)
        if (loading) {
            return (
                <div className="active-learning-panel">
                    <div className="active-learning-loading">{t('activeLearningAnalyzing')}</div>
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
            <div className="active-learning-panel">
                {/* 标题栏 (Title Bar) */}
                <div
                    className="active-learning-header"
                    onClick={() => setExpanded(!expanded)}
                >
                    <span className="active-learning-title">
                        {t('activeLearningTitle')}
                        <span className="active-learning-badge">
                            {(recommendations.uncertainDocuments?.length || 0) +
                             (recommendations.potentiallyRelevantDocuments?.length || 0) +
                             (recommendations.historyBasedRecommendations?.length || 0)}
                        </span>
                    </span>
                    <span className={`active-learning-expand-icon ${expanded ? 'expanded' : ''}`}>
                        {expanded ? '▼' : '▶'}
                    </span>
                </div>

                {expanded && (
                    <div className="active-learning-content">
                        {/* 置信度指示器 (Confidence Indicator) */}
                        <div className="active-learning-confidence-bar">
                            <span>{t('activeLearningConfidence')}:</span>
                            <div className="active-learning-progress-bar">
                                <div 
                                    className="active-learning-progress-fill"
                                    style={{
                                        width: `${(recommendations.confidenceScore || 0) * 100}%`,
                                        backgroundColor: getConfidenceColor(recommendations.confidenceScore)
                                    }} 
                                />
                            </div>
                            <span>{Math.round((recommendations.confidenceScore || 0) * 100)}%</span>
                        </div>

                        {/* 推荐原因 (Recommendation Reason) */}
                        {recommendations.recommendationReason && (
                            <div className="active-learning-reason">
                                💡 {recommendations.recommendationReason}
                            </div>
                        )}

                        {/* 边界文档（不确定性高的文档）(Boundary Documents - High Uncertainty) */}
                        {recommendations.uncertainDocuments?.length > 0 && (
                            <div className="active-learning-section">
                                <h4 className="active-learning-section-title">
                                    {t('activeLearningQuestionMark')} {t('activeLearningBoundaryDocs')}
                                </h4>
                                {recommendations.uncertainDocuments.map((doc, idx) => (
                                    <div key={idx} className="active-learning-doc-item">
                                        <div className="active-learning-doc-info">
                                            <span className="active-learning-doc-name">
                                                {t('activeLearningHashMark')}{doc.rank} {doc.document?.title || t('activeLearningUnknownDoc')}
                                            </span>
                                            <span className="active-learning-doc-reason">{doc.reason}</span>
                                        </div>
                                        {renderFeedbackButtons(doc.document?.title)}
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* 历史高分文档 (Historical High-Rated Documents) */}
                        {recommendations.potentiallyRelevantDocuments?.length > 0 && (
                            <div className="active-learning-section">
                                <h4 className="active-learning-section-title">
                                    {t('activeLearningStarMark')} {t('activeLearningHighRatedDocs')}
                                </h4>
                                {recommendations.potentiallyRelevantDocuments.map((doc, idx) => (
                                    <div key={idx} className="active-learning-doc-item">
                                        <div className="active-learning-doc-info">
                                            <span className="active-learning-doc-name">{doc.documentName}</span>
                                            <span className="active-learning-doc-reason">
                                                {t('activeLearningWeight')}: {doc.historicalWeight?.toFixed(2)} - {doc.reason}
                                            </span>
                                        </div>
                                        {renderFeedbackButtons(doc.documentName)}
                                    </div>
                                ))}
                            </div>
                        )}

                        {/* 基于相似问题的推荐 (History-Based Recommendations) */}
                        {recommendations.historyBasedRecommendations?.length > 0 && (
                            <div className="active-learning-section">
                                <h4 className="active-learning-section-title">
                                    {t('activeLearningBookMark')} {t('activeLearningHistoryBased')}
                                </h4>
                                {recommendations.historyBasedRecommendations.map((rec, idx) => (
                                    <div key={idx} className="active-learning-doc-item">
                                        <div className="active-learning-doc-info">
                                            <span className="active-learning-doc-name">{rec.documentName}</span>
                                            <span className="active-learning-doc-reason">
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
    }

    // 获取置信度颜色 (Get Confidence Color)
    function getConfidenceColor(confidence) {
        if (confidence >= 0.7) return '#4caf50';
        if (confidence >= 0.4) return '#ff9800';
        return '#f44336';
    }

    // 样式已提取到 active-learning.css (Styles extracted to active-learning.css)

    // 导出组件 (Export Component)
    window.ActiveLearningPanel = ActiveLearningPanel;

    // 获取翻译函数并输出加载日志 (Get translation function and output loading log)
    const getT = () => window.LanguageModule ? window.LanguageModule.useTranslation().t : (k) => k;
    console.log(getT()('activeLearningLogComponentLoaded'));
})();
