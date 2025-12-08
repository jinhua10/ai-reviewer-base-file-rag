/**
 * HOPE Answer Card Component / HOPE 答案卡片组件
 * Display HOPE quick answer with confidence and source info
 * 显示 HOPE 快速答案及置信度和来源信息
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */

function HOPEAnswerCard({ hopeAnswer, loading }) {
    const { t } = window.LanguageModule.useTranslation();

    /**
     * Get source layer display text / 获取来源层显示文本
     */
    const getSourceText = (source) => {
        switch (source) {
            case 'HOPE_PERMANENT':
                return t('streamingHopeSourcePermanent');
            case 'HOPE_ORDINARY':
                return t('streamingHopeSourceOrdinary');
            case 'HOPE_TRANSIENT':
                return t('streamingHopeSourceTransient');
            default:
                return source;
        }
    };

    /**
     * Get confidence level badge / 获取置信度徽章
     */
    const getConfidenceBadge = (confidence) => {
        if (confidence >= 0.9) return 'confidence-badge-high';
        if (confidence >= 0.7) return 'confidence-badge-medium';
        return 'confidence-badge-low';
    };

    return React.createElement('div', { className: 'hope-answer-card' },
        // Header (标题)
        React.createElement('div', { className: 'hope-answer-header' },
            React.createElement('h3', null, '⚡ ' + t('streamingHopeAnswer')),
            loading && React.createElement('div', { className: 'hope-loading-spinner' })
        ),

        // Content (内容)
        React.createElement('div', { className: 'hope-answer-content' },
            loading && React.createElement('div', { className: 'hope-loading-text' },
                React.createElement('p', null, t('streamingHopeLoading'))
            ),

            !loading && !hopeAnswer && React.createElement('div', { className: 'hope-no-answer' },
                React.createElement('p', null, '🤔 ' + t('streamingHopeNoAnswer'))
            ),

            !loading && hopeAnswer && React.createElement('div', { className: 'hope-answer-body' },
                // Answer Text (答案文本)
                React.createElement('div', { className: 'hope-answer-text' },
                    React.createElement('p', null, hopeAnswer.answer)
                ),

                // Metadata (元数据)
                React.createElement('div', { className: 'hope-answer-metadata' },
                    // Confidence (置信度)
                    React.createElement('div', { className: 'hope-metadata-item' },
                        React.createElement('span', { className: 'hope-metadata-label' },
                            t('streamingHopeConfidence') + ':'
                        ),
                        React.createElement('span', {
                            className: `hope-confidence-badge ${getConfidenceBadge(hopeAnswer.confidence)}`
                        },
                            `${Math.round(hopeAnswer.confidence * 100)}%`
                        )
                    ),

                    // Source Layer (来源层)
                    React.createElement('div', { className: 'hope-metadata-item' },
                        React.createElement('span', { className: 'hope-metadata-label' },
                            t('streamingHopeSource') + ':'
                        ),
                        React.createElement('span', { className: 'hope-source-badge' },
                            getSourceText(hopeAnswer.source)
                        )
                    ),

                    // Response Time (响应时间)
                    React.createElement('div', { className: 'hope-metadata-item' },
                        React.createElement('span', { className: 'hope-metadata-label' },
                            t('streamingHopeResponseTime') + ':'
                        ),
                        React.createElement('span', { className: 'hope-response-time' },
                            `${hopeAnswer.responseTime}ms`
                        )
                    ),

                    // Can Direct Answer (是否可直接回答)
                    hopeAnswer.canDirectAnswer && React.createElement('div', {
                        className: 'hope-can-answer-badge'
                    },
                        '✅ ' + t('streamingHopeCanAnswer')
                    )
                )
            )
        )
    );
}

// Export (导出)
window.HOPEAnswerCard = HOPEAnswerCard;

