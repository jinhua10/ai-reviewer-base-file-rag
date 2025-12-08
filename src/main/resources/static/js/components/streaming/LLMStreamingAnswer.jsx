/**
 * LLM Streaming Answer Component / LLM 流式答案组件
 * Display real-time LLM streaming response
 * 显示实时 LLM 流式响应
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */

function LLMStreamingAnswer({ answer, streaming, status, duration, error, onRetry }) {
    const { t } = window.LanguageModule.useTranslation();
    const { useEffect } = React;

    /**
     * Highlight code blocks / 高亮代码块
     */
    useEffect(() => {
        if (answer && typeof hljs !== 'undefined') {
            document.querySelectorAll('.llm-answer-text pre code').forEach((block) => {
                hljs.highlightElement(block);
            });
        }
    }, [answer]);

    /**
     * Render markdown / 渲染 Markdown
     */
    const renderMarkdown = (text) => {
        if (!text) return '';
        if (typeof marked !== 'undefined') {
            return marked.parse(text);
        }
        return text;
    };

    /**
     * Get status text / 获取状态文本
     */
    const getStatusText = () => {
        if (streaming) return t('streamingLlmGenerating');
        if (status === 'completed') return t('streamingLlmComplete');
        if (status === 'error') return t('streamingLlmError');
        if (status === 'interrupted') return t('streamingSessionInterrupted');
        return t('streamingLlmConnecting');
    };

    /**
     * Get status icon / 获取状态图标
     */
    const getStatusIcon = () => {
        if (streaming) return '⏳';
        if (status === 'completed') return '✅';
        if (status === 'error') return '❌';
        if (status === 'interrupted') return '⚠️';
        return '🔄';
    };

    return React.createElement('div', { className: 'llm-streaming-answer' },
        // Header (标题)
        React.createElement('div', { className: 'llm-answer-header' },
            React.createElement('h3', null, '🤖 ' + t('streamingLlmAnswer')),
            React.createElement('div', { className: `llm-status-badge llm-status-${status || 'connecting'}` },
                getStatusIcon() + ' ' + getStatusText()
            )
        ),

        // Content (内容)
        React.createElement('div', { className: 'llm-answer-content' },
            // Streaming indicator (流式指示器)
            streaming && React.createElement('div', { className: 'llm-streaming-indicator' },
                React.createElement('div', { className: 'typing-dots' },
                    React.createElement('span'),
                    React.createElement('span'),
                    React.createElement('span')
                )
            ),

            // Answer text (答案文本)
            answer && React.createElement('div', {
                className: 'llm-answer-text',
                dangerouslySetInnerHTML: { __html: renderMarkdown(answer) }
            }),

            // Empty state (空状态)
            !answer && !streaming && !error && React.createElement('div', { className: 'llm-empty-state' },
                React.createElement('p', null, '🔄 ' + t('streamingLlmConnecting'))
            ),

            // Error state (错误状态)
            error && React.createElement('div', { className: 'llm-error-state' },
                React.createElement('p', null, '❌ ' + error),
                React.createElement('button', {
                    className: 'llm-retry-button',
                    onClick: onRetry
                }, t('streamingLlmRetry'))
            )
        ),

        // Footer metadata (底部元数据)
        (answer || streaming) && React.createElement('div', { className: 'llm-answer-footer' },
            // Answer length (答案长度)
            React.createElement('div', { className: 'llm-metadata-item' },
                React.createElement('span', { className: 'llm-metadata-icon' }, '📝'),
                React.createElement('span', null,
                    `${t('streamingAnswerLength')}: ${answer.length} ${t('streamingChars')}`
                )
            ),

            // Duration (耗时)
            duration > 0 && React.createElement('div', { className: 'llm-metadata-item' },
                React.createElement('span', { className: 'llm-metadata-icon' }, '⏱️'),
                React.createElement('span', null,
                    `${t('streamingDuration')}: ${duration} ${t('streamingSeconds')}`
                )
            ),

            // Streaming progress indicator (流式进度指示器)
            streaming && React.createElement('div', { className: 'llm-streaming-progress' },
                React.createElement('div', { className: 'progress-bar' },
                    React.createElement('div', { className: 'progress-bar-fill' })
                )
            )
        )
    );
}

// Export (导出)
window.LLMStreamingAnswer = LLMStreamingAnswer;

