/**
 * DualTrackAnswer Component (双轨答案组件)
 *
 * 同时展示 HOPE 快速答案和 LLM 流式生成
 * Displays both HOPE quick answer and LLM streaming generation
 *
 * @author AI Reviewer Team
 * @since 2025-12-10
 */

function DualTrackAnswer({ question, sessionId, onComplete }) {
    const { useState, useEffect, useRef } = React;
    const { t } = window.LanguageModule.useTranslation();

    // 状态管理 (State management)
    const [hopeAnswer, setHopeAnswer] = useState(null);
    const [hopeLoading, setHopeLoading] = useState(true);
    const [llmAnswer, setLlmAnswer] = useState('');
    const [llmLoading, setLlmLoading] = useState(true);
    const [error, setError] = useState(null);
    const [llmChunks, setLlmChunks] = useState(0);
    const [totalTime, setTotalTime] = useState({ hope: 0, llm: 0 });

    const eventSourceRef = useRef(null);
    const llmStartTimeRef = useRef(0);

    // 清理函数 (Cleanup function)
    const cleanup = () => {
        if (eventSourceRef.current) {
            eventSourceRef.current.close();
            eventSourceRef.current = null;
        }
    };

    // 主效果：建立 SSE 连接 (Main effect: establish SSE connection)
    useEffect(() => {
        if (!question) return;

        const url = `/api/qa/stream/dual-track?question=${encodeURIComponent(question)}` +
                    (sessionId ? `&sessionId=${sessionId}` : '');

        console.log(t('logDualTrackStart') || '🚀 启动双轨流式响应', question);

        const eventSource = new EventSource(url);
        eventSourceRef.current = eventSource;
        llmStartTimeRef.current = Date.now();

        // 监听 HOPE 答案 (Listen for HOPE answer)
        eventSource.addEventListener('hope', (e) => {
            try {
                const msg = JSON.parse(e.data);
                console.log(t('logHopeReceived') || '💡 收到 HOPE 答案', msg);

                setHopeAnswer({
                    content: msg.content,
                    source: msg.hopeSource,
                    confidence: msg.confidence,
                    responseTime: msg.responseTime,
                    strategy: msg.strategy
                });
                setHopeLoading(false);
                setTotalTime(prev => ({ ...prev, hope: msg.responseTime }));
            } catch (err) {
                console.error(t('logHopeParseFailed') || 'HOPE 消息解析失败', err);
            }
        });

        // 监听 LLM 流式块 (Listen for LLM chunks)
        eventSource.addEventListener('llm', (e) => {
            try {
                const msg = JSON.parse(e.data);

                if (msg.type === 'LLM_CHUNK') {
                    setLlmAnswer(prev => prev + msg.content);
                    setLlmChunks(prev => prev + 1);
                }
            } catch (err) {
                console.error(t('logLlmParseFailed') || 'LLM 消息解析失败', err);
            }
        });

        // 监听完成消息 (Listen for complete message)
        eventSource.addEventListener('complete', (e) => {
            try {
                const msg = JSON.parse(e.data);
                console.log(t('logStreamComplete') || '✅ 流式响应完成', msg);

                setLlmLoading(false);
                const llmTime = Date.now() - llmStartTimeRef.current;
                setTotalTime(prev => ({ ...prev, llm: llmTime }));

                cleanup();

                if (onComplete) {
                    onComplete({
                        hope: hopeAnswer,
                        llm: llmAnswer,
                        totalTime
                    });
                }
            } catch (err) {
                console.error(t('logCompleteParseFailed') || '完成消息解析失败', err);
            }
        });

        // 监听错误 (Listen for errors)
        eventSource.addEventListener('error', (e) => {
            try {
                const msg = JSON.parse(e.data);
                console.error(t('logStreamError') || '❌ 流式响应错误', msg);
                setError(msg.error || t('errorStreamFailed') || '流式响应失败');
            } catch (err) {
                setError(t('errorConnectionFailed') || '连接失败');
            }

            setHopeLoading(false);
            setLlmLoading(false);
            cleanup();
        });

        eventSource.onerror = () => {
            console.error(t('logConnectionError') || '❌ SSE 连接错误');
            setError(t('errorConnectionLost') || '连接中断');
            setHopeLoading(false);
            setLlmLoading(false);
            cleanup();
        };

        // 清理 (Cleanup)
        return cleanup;
    }, [question, sessionId]);

    // 获取 HOPE 层名称 (Get HOPE layer name)
    const getHopeLayerName = (source) => {
        if (!source) return '';
        const s = source.toUpperCase();
        if (s.includes('PERMANENT')) return t('hopeLayerPermanent') || '低频层';
        if (s.includes('ORDINARY')) return t('hopeLayerOrdinary') || '中频层';
        if (s.includes('HIGH')) return t('hopeLayerHighFrequency') || '高频层';
        return source;
    };

    // 停止生成 (Stop generation)
    const handleStop = () => {
        cleanup();
        setLlmLoading(false);
    };

    // 错误显示 (Error display)
    if (error) {
        return (
            <div className="dual-track-error">
                <div className="error-icon">❌</div>
                <div className="error-message">{error}</div>
                <button className="error-retry" onClick={() => window.location.reload()}>
                    {t('retry') || '重试'}
                </button>
            </div>
        );
    }

    // 主渲染 (Main render)
    return (
        <div className="dual-track-container">
            <div className="dual-track-header">
                <h3 className="dual-track-title">
                    {t('dualTrackTitle') || '🎯 双轨响应'}
                </h3>
                <p className="dual-track-subtitle">
                    {t('dualTrackSubtitle') || 'HOPE 快速答案 + LLM 详细分析'}
                </p>
            </div>

            <div className="dual-track-content">
                {/* HOPE 快速答案轨道 (HOPE quick answer track) */}
                <div className="track hope-track">
                    <div className="track-header">
                        <div className="track-title">
                            <span className="track-icon">💡</span>
                            <span>{t('hopeQuickAnswer') || 'HOPE 快速答案'}</span>
                        </div>
                        {hopeAnswer && (
                            <div className="track-meta">
                                <span className="meta-time">⚡ {hopeAnswer.responseTime}ms</span>
                            </div>
                        )}
                    </div>

                    <div className="track-body">
                        {hopeLoading ? (
                            <div className="track-loading">
                                <div className="loading-spinner"></div>
                                <p>{t('hopeQuerying') || 'HOPE 查询中...'}</p>
                            </div>
                        ) : hopeAnswer ? (
                            <div className="track-answer">
                                <div className="answer-content">
                                    {hopeAnswer.content}
                                </div>
                                <div className="answer-meta">
                                    <span className="meta-source">
                                        📚 {getHopeLayerName(hopeAnswer.source)}
                                    </span>
                                    <span className="meta-confidence">
                                        🎯 {t('confidence') || '置信度'}: {(hopeAnswer.confidence * 100).toFixed(0)}%
                                    </span>
                                    <span className="meta-strategy">
                                        {hopeAnswer.strategy === 'DIRECT_ANSWER' ? '⚡ ' + (t('directAnswer') || '直接回答') : '📖 ' + (t('reference') || '参考')}
                                    </span>
                                </div>
                            </div>
                        ) : (
                            <div className="track-empty">
                                <p>{t('hopeNoAnswer') || '暂无 HOPE 答案'}</p>
                            </div>
                        )}
                    </div>
                </div>

                {/* LLM 流式生成轨道 (LLM streaming track) */}
                <div className="track llm-track">
                    <div className="track-header">
                        <div className="track-title">
                            <span className="track-icon">🤖</span>
                            <span>{t('llmDetailedAnalysis') || 'LLM 详细分析'}</span>
                        </div>
                        {llmLoading ? (
                            <button className="track-stop" onClick={handleStop}>
                                {t('stopGeneration') || '停止生成'}
                            </button>
                        ) : (
                            <div className="track-meta">
                                <span className="meta-time">⏱️ {totalTime.llm}ms</span>
                                <span className="meta-chunks">📦 {llmChunks} {t('chunks') || '块'}</span>
                            </div>
                        )}
                    </div>

                    <div className="track-body">
                        {llmAnswer.length === 0 && llmLoading ? (
                            <div className="track-loading">
                                <div className="loading-spinner"></div>
                                <p>{t('llmGenerating') || 'LLM 生成中...'}</p>
                            </div>
                        ) : (
                            <div className="track-answer">
                                <div className={`answer-content ${llmLoading ? 'streaming' : ''}`}>
                                    {llmAnswer || (t('llmWaiting') || '等待 LLM 响应...')}
                                    {llmLoading && <span className="cursor-blink">▋</span>}
                                </div>
                            </div>
                        )}
                    </div>
                </div>
            </div>

            {/* 底部信息栏 (Bottom info bar) */}
            {!llmLoading && hopeAnswer && llmAnswer && (
                <div className="dual-track-footer">
                    <div className="footer-stats">
                        <span className="stat">
                            <span className="stat-label">{t('hopeTime') || 'HOPE 耗时'}:</span>
                            <span className="stat-value">{totalTime.hope}ms</span>
                        </span>
                        <span className="stat">
                            <span className="stat-label">{t('llmTime') || 'LLM 耗时'}:</span>
                            <span className="stat-value">{totalTime.llm}ms</span>
                        </span>
                        <span className="stat">
                            <span className="stat-label">{t('speedup') || '加速比'}:</span>
                            <span className="stat-value highlight">
                                {(totalTime.llm / totalTime.hope).toFixed(1)}x
                            </span>
                        </span>
                    </div>

                    {/* 对比和选择面板 (Comparison and choice panel) */}
                    <div className="dual-track-choice">
                        <div className="choice-title">
                            {t('chooseAnswer') || '请选择您更满意的答案：'}
                        </div>
                        <div className="choice-buttons">
                            <button
                                className="choice-btn choice-btn-hope"
                                onClick={() => {
                                    // 反馈到 HOPE
                                    if (window.api && window.api.submitDualTrackChoice) {
                                        window.api.submitDualTrackChoice(question, 'HOPE', hopeAnswer, llmAnswer, sessionId);
                                    }
                                    showToast(t('choiceHopeSubmitted') || '✅ 已选择 HOPE 答案，感谢反馈！', 'success');
                                }}
                            >
                                💡 {t('chooseHope') || '采用 HOPE 答案'}
                            </button>
                            <button
                                className="choice-btn choice-btn-llm"
                                onClick={() => {
                                    // 反馈到 HOPE
                                    if (window.api && window.api.submitDualTrackChoice) {
                                        window.api.submitDualTrackChoice(question, 'LLM', hopeAnswer, llmAnswer, sessionId);
                                    }
                                    showToast(t('choiceLlmSubmitted') || '✅ 已选择 LLM 答案，感谢反馈！', 'success');
                                }}
                            >
                                🤖 {t('chooseLlm') || '采用 LLM 答案'}
                            </button>
                            <button
                                className="choice-btn choice-btn-both"
                                onClick={() => {
                                    // 反馈到 HOPE
                                    if (window.api && window.api.submitDualTrackChoice) {
                                        window.api.submitDualTrackChoice(question, 'BOTH', hopeAnswer, llmAnswer, sessionId);
                                    }
                                    showToast(t('choiceBothSubmitted') || '✅ 已选择两个都采用，感谢反馈！', 'success');
                                }}
                            >
                                ✨ {t('chooseBoth') || '都采用'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );

    // Toast 提示函数 (Toast notification function)
    function showToast(message, type = 'info') {
        const toast = document.createElement('div');
        toast.className = `dual-track-toast toast-${type}`;
        toast.textContent = message;
        toast.style.cssText = `
            position: fixed;
            top: 80px;
            right: 20px;
            padding: 12px 20px;
            background: ${type === 'success' ? '#10b981' : type === 'error' ? '#ef4444' : '#3b82f6'};
            color: white;
            border-radius: 8px;
            box-shadow: 0 4px 12px rgba(0,0,0,0.2);
            z-index: 10000;
            font-size: 14px;
            animation: slideInRight 0.3s ease-out;
        `;
        document.body.appendChild(toast);

        setTimeout(() => {
            toast.style.animation = 'slideOutRight 0.3s ease-out';
            setTimeout(() => {
                if (document.body.contains(toast)) {
                    document.body.removeChild(toast);
                }
            }, 300);
        }, 3000);
    }
}

// 导出到全局 (Export to global)
if (typeof window !== 'undefined') {
    window.DualTrackAnswer = DualTrackAnswer;
}

// 模块导出 (Module export)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = DualTrackAnswer;
}

