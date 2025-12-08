/**
 * Comparison Feedback Component / 对比反馈组件
 * Allow users to compare HOPE and LLM answers and provide feedback
 * 允许用户对比 HOPE 和 LLM 答案并提供反馈
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */

function ComparisonFeedback({ sessionId, hopeAnswer, llmAnswer, question }) {
    const { useState } = React;
    const { t } = window.LanguageModule.useTranslation();

    // State Management (状态管理)
    const [selectedChoice, setSelectedChoice] = useState(null);
    const [feedbackComment, setFeedbackComment] = useState('');
    const [submitted, setSubmitted] = useState(false);
    const [showComparison, setShowComparison] = useState(false);
    const [viewMode, setViewMode] = useState('both'); // 'both', 'hope', 'llm'

    /**
     * Handle feedback submission / 处理反馈提交
     */
    const handleSubmit = async () => {
        if (!selectedChoice) {
            alert(t('comparisonWhichBetter'));
            return;
        }

        try {
            console.log('📤 Submitting comparison feedback:', {
                sessionId,
                choice: selectedChoice,
                comment: feedbackComment
            });

            const response = await fetch('/api/qa/stream/feedback', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept-Language': window.LanguageModule.getCurrentLanguage()
                },
                body: JSON.stringify({
                    sessionId: sessionId,
                    hopeAnswerId: hopeAnswer.id,
                    question: question,
                    choice: selectedChoice,
                    comment: feedbackComment,
                    timestamp: new Date().toISOString()
                })
            });

            if (response.ok) {
                console.log('✅ Feedback submitted successfully');
                setSubmitted(true);
                setTimeout(() => setSubmitted(false), 3000);
            } else {
                throw new Error(`HTTP ${response.status}`);
            }
        } catch (err) {
            console.error('❌ Failed to submit feedback:', err);
            alert(t('comparisonSubmitError') + ': ' + err.message);
        }
    };

    /**
     * Choice buttons / 选择按钮
     */
    const renderChoiceButtons = () => {
        const choices = [
            { value: 'hope', label: t('comparisonHopeBetter'), icon: '⚡' },
            { value: 'llm', label: t('comparisonLlmBetter'), icon: '🤖' },
            { value: 'both', label: t('comparisonBothGood'), icon: '👍' },
            { value: 'neither', label: t('comparisonNeitherGood'), icon: '👎' }
        ];

        return React.createElement('div', { className: 'comparison-choices' },
            choices.map(choice =>
                React.createElement('button', {
                    key: choice.value,
                    className: `comparison-choice-button ${selectedChoice === choice.value ? 'selected' : ''}`,
                    onClick: () => setSelectedChoice(choice.value)
                },
                    React.createElement('span', { className: 'choice-icon' }, choice.icon),
                    React.createElement('span', { className: 'choice-label' }, choice.label)
                )
            )
        );
    };

    /**
     * Render comparison view / 渲染对比视图
     */
    const renderComparisonView = () => {
        return React.createElement('div', { className: 'comparison-view' },
            // View mode selector (视图模式选择器)
            React.createElement('div', { className: 'comparison-view-selector' },
                React.createElement('button', {
                    className: `view-mode-button ${viewMode === 'both' ? 'active' : ''}`,
                    onClick: () => setViewMode('both')
                }, t('comparisonViewBoth')),
                React.createElement('button', {
                    className: `view-mode-button ${viewMode === 'hope' ? 'active' : ''}`,
                    onClick: () => setViewMode('hope')
                }, t('comparisonViewHope')),
                React.createElement('button', {
                    className: `view-mode-button ${viewMode === 'llm' ? 'active' : ''}`,
                    onClick: () => setViewMode('llm')
                }, t('comparisonViewLlm'))
            ),

            // Comparison content (对比内容)
            React.createElement('div', { className: `comparison-content view-mode-${viewMode}` },
                (viewMode === 'both' || viewMode === 'hope') && React.createElement('div', { className: 'comparison-answer-panel hope-panel' },
                    React.createElement('h4', null, '⚡ ' + t('streamingHopeAnswer')),
                    React.createElement('div', { className: 'comparison-answer-text' },
                        React.createElement('p', null, hopeAnswer.answer)
                    ),
                    React.createElement('div', { className: 'comparison-answer-meta' },
                        `${t('streamingHopeConfidence')}: ${Math.round(hopeAnswer.confidence * 100)}% | `,
                        `${t('streamingHopeResponseTime')}: ${hopeAnswer.responseTime}ms`
                    )
                ),

                (viewMode === 'both' || viewMode === 'llm') && React.createElement('div', { className: 'comparison-answer-panel llm-panel' },
                    React.createElement('h4', null, '🤖 ' + t('streamingLlmAnswer')),
                    React.createElement('div', {
                        className: 'comparison-answer-text',
                        dangerouslySetInnerHTML: { __html: typeof marked !== 'undefined' ? marked.parse(llmAnswer) : llmAnswer }
                    }),
                    React.createElement('div', { className: 'comparison-answer-meta' },
                        `${t('streamingAnswerLength')}: ${llmAnswer.length} ${t('streamingChars')}`
                    )
                )
            )
        );
    };

    return React.createElement('div', { className: 'comparison-feedback-container' },
        // Header (标题)
        React.createElement('div', { className: 'comparison-header' },
            React.createElement('h3', null, '🔄 ' + t('comparisonTitle')),
            React.createElement('button', {
                className: 'comparison-toggle-button',
                onClick: () => setShowComparison(!showComparison)
            }, showComparison ? t('comparisonHideDiff') : t('comparisonShowDiff'))
        ),

        // Comparison view (对比视图)
        showComparison && renderComparisonView(),

        // Feedback form (反馈表单)
        React.createElement('div', { className: 'comparison-feedback-form' },
            React.createElement('p', { className: 'comparison-question' },
                t('comparisonWhichBetter')
            ),

            // Choice buttons (选择按钮)
            renderChoiceButtons(),

            // Comment textarea (评论文本框)
            React.createElement('textarea', {
                className: 'comparison-feedback-comment',
                placeholder: t('comparisonFeedbackPlaceholder'),
                value: feedbackComment,
                onChange: (e) => setFeedbackComment(e.target.value),
                rows: 3
            }),

            // Submit button (提交按钮)
            React.createElement('button', {
                className: 'comparison-submit-button',
                onClick: handleSubmit,
                disabled: !selectedChoice || submitted
            }, submitted ? t('comparisonSubmitted') : t('comparisonSubmit'))
        ),

        // Success message (成功消息)
        submitted && React.createElement('div', { className: 'comparison-success-message' },
            '✅ ' + t('comparisonSubmitted')
        )
    );
}

// Export (导出)
window.ComparisonFeedback = ComparisonFeedback;

