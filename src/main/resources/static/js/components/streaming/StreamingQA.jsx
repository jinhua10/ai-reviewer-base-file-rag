/**
 * Streaming QA Component / 流式问答组件
 * Dual-Track Response: HOPE Quick Answer + LLM Streaming Response
 * 双轨响应：HOPE 快速答案 + LLM 流式响应
 *
 * @author AI Reviewer Team
 * @since 2025-12-09
 */

function StreamingQA() {
    const { useState, useEffect, useRef } = React;
    const { t } = window.LanguageModule.useTranslation();

    // State Management (状态管理)
    const [question, setQuestion] = useState('');
    const [sessionId, setSessionId] = useState(null);
    const [hopeAnswer, setHopeAnswer] = useState(null);
    const [hopeLoading, setHopeLoading] = useState(false);
    const [llmAnswer, setLlmAnswer] = useState('');
    const [llmStreaming, setLlmStreaming] = useState(false);
    const [sessionStatus, setSessionStatus] = useState(null);
    const [sessionInfo, setSessionInfo] = useState(null);
    const [error, setError] = useState(null);

    // SSE Connection (SSE 连接)
    const eventSourceRef = useRef(null);
    const llmStartTimeRef = useRef(null);
    const [llmDuration, setLlmDuration] = useState(0);

    // Cleanup on unmount (卸载时清理)
    useEffect(() => {
        return () => {
            if (eventSourceRef.current) {
                eventSourceRef.current.close();
                console.log('🔌 SSE connection closed');
            }
        };
    }, []);

    /**
     * Handle Ask / 处理提问
     */
    const handleAsk = async () => {
        if (!question.trim()) {
            alert(t('qaInputError'));
            return;
        }

        // Reset state (重置状态)
        setHopeAnswer(null);
        setLlmAnswer('');
        setError(null);
        setSessionStatus(null);
        setSessionInfo(null);
        setHopeLoading(true);
        setLlmStreaming(false);

        // Close previous SSE connection (关闭之前的连接)
        if (eventSourceRef.current) {
            eventSourceRef.current.close();
        }

        try {
            // Step 1: Initiate streaming request (发起流式请求)
            console.log('📤 Initiating streaming request:', question);
            const response = await fetch('/api/qa/stream', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                    'Accept-Language': window.LanguageModule.getCurrentLanguage()
                },
                body: JSON.stringify({
                    question: question,
                    userId: 'web-user-' + Date.now()
                })
            });

            if (!response.ok) {
                throw new Error(`HTTP ${response.status}: ${response.statusText}`);
            }

            const data = await response.json();
            console.log('✅ Streaming session created:', data);

            setSessionId(data.sessionId);
            setHopeAnswer(data.hopeAnswer);
            setHopeLoading(false);

            // Step 2: Connect to SSE for LLM streaming (连接 SSE 获取 LLM 流式输出)
            if (data.sseUrl) {
                connectSSE(data.sseUrl);
            }

        } catch (err) {
            console.error('❌ Streaming request failed:', err);
            setError(t('qaRequestError') + ': ' + err.message);
            setHopeLoading(false);
        }
    };

    /**
     * Connect to SSE / 连接 SSE
     */
    const connectSSE = (sseUrl) => {
        console.log('🔌 Connecting to SSE:', sseUrl);
        setLlmStreaming(true);
        llmStartTimeRef.current = Date.now();

        const eventSource = new EventSource(sseUrl);
        eventSourceRef.current = eventSource;

        // On chunk received (接收文本块)
        eventSource.addEventListener('chunk', (event) => {
            const chunk = event.data;
            setLlmAnswer(prev => prev + chunk);

            // Update duration (更新耗时)
            const duration = Math.floor((Date.now() - llmStartTimeRef.current) / 1000);
            setLlmDuration(duration);
        });

        // On session complete (会话完成)
        eventSource.addEventListener('complete', (event) => {
            console.log('✅ LLM streaming completed');
            setLlmStreaming(false);
            setSessionStatus('completed');
            eventSource.close();
        });

        // On session error (会话错误)
        eventSource.addEventListener('error', (event) => {
            console.error('❌ SSE error:', event);
            setLlmStreaming(false);
            setSessionStatus('error');
            setError(t('streamingLlmError'));
            eventSource.close();
        });

        // On connection error (连接错误)
        eventSource.onerror = (event) => {
            console.error('❌ SSE connection error:', event);
            setLlmStreaming(false);
            eventSource.close();
        };
    };

    /**
     * Fetch session status / 获取会话状态
     */
    const fetchSessionStatus = async () => {
        if (!sessionId) return;

        try {
            const response = await fetch(`/api/qa/stream/${sessionId}/status`);
            if (response.ok) {
                const status = await response.json();
                setSessionInfo(status);
                console.log('📊 Session status:', status);
            }
        } catch (err) {
            console.error('⚠️ Failed to fetch session status:', err);
        }
    };

    /**
     * Retry LLM streaming / 重试 LLM 流式
     */
    const handleRetry = () => {
        if (sessionId) {
            setLlmAnswer('');
            setError(null);
            connectSSE(`/api/qa/stream/${sessionId}`);
        }
    };

    return React.createElement('div', { className: 'streaming-qa-container' },
        // Question Input (问题输入)
        React.createElement('div', { className: 'streaming-question-section' },
            React.createElement('h2', null, t('streamingTitle')),
            React.createElement('div', { className: 'streaming-input-group' },
                React.createElement('textarea', {
                    className: 'streaming-question-input',
                    placeholder: t('qaPlaceholder'),
                    value: question,
                    onChange: (e) => setQuestion(e.target.value),
                    rows: 3
                }),
                React.createElement('button', {
                    className: 'streaming-ask-button',
                    onClick: handleAsk,
                    disabled: hopeLoading || llmStreaming
                }, hopeLoading || llmStreaming ? t('qaThinking') : t('qaButton'))
            )
        ),

        // Dual-Track Response (双轨响应)
        sessionId && React.createElement('div', { className: 'streaming-response-container' },
            // HOPE Quick Answer (HOPE 快速答案)
            React.createElement(HOPEAnswerCard, {
                hopeAnswer: hopeAnswer,
                loading: hopeLoading
            }),

            // LLM Streaming Answer (LLM 流式答案)
            React.createElement(LLMStreamingAnswer, {
                answer: llmAnswer,
                streaming: llmStreaming,
                status: sessionStatus,
                duration: llmDuration,
                error: error,
                onRetry: handleRetry
            })
        ),

        // Comparison Feedback (对比反馈)
        sessionId && sessionStatus === 'completed' && hopeAnswer && React.createElement(ComparisonFeedback, {
            sessionId: sessionId,
            hopeAnswer: hopeAnswer,
            llmAnswer: llmAnswer,
            question: question
        }),

        // Error Display (错误显示)
        error && !llmStreaming && React.createElement('div', { className: 'streaming-error' },
            React.createElement('p', null, `${t('qaErrorPrefix')} ${error}`)
        )
    );
}

// Export (导出)
window.StreamingQA = StreamingQA;

