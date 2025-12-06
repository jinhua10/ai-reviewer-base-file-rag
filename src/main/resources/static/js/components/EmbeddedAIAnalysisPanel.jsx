/**
 * 嵌入式AI分析面板 - 动态配置版
 * (Embedded AI Analysis Panel - Dynamic Configuration Version)
 * JSX 版本 - 使用 Babel 转译
 *
 * 特性 (Features):
 * 1. 动态从后端加载策略配置（无需硬编码）
 * 2. 用户友好的分析目标选择（非专业术语）
 * 3. 智能需求检测（自动推荐最佳策略）
 * 4. 策略参数动态渲染
 * 5. 策略市场集成
 *
 * 设计理念 (Design Philosophy):
 * - 后端添加新策略，前端自动适配
 * - 用户选项完全由后端配置驱动
 * - 支持策略热更新
 */
(function() {
    'use strict';

    const { useState, useEffect, useMemo, useCallback } = React;

    // 默认配置（后端不可用时的降级方案）
    const DEFAULT_GOALS = {
        quick: {
            id: 'quick',
            icon: '🚀',
            label: { zh: '快速了解大意', en: 'Quick Overview' },
            description: { zh: '几分钟内了解主要内容', en: 'Understand main content in minutes' },
            strategies: ['parallel-summary'],
            estimatedTime: { zh: '1-2分钟', en: '1-2 min' },
            tokenCost: 'low',
            minDocs: 1,
            order: 1
        },
        precise: {
            id: 'precise',
            icon: '🔍',
            label: { zh: '精确查找答案', en: 'Find Precise Answers' },
            description: { zh: '针对问题找出准确答案', en: 'Find accurate answers to questions' },
            strategies: ['question-driven'],
            estimatedTime: { zh: '30秒', en: '30 sec' },
            tokenCost: 'lowest',
            minDocs: 1,
            order: 2
        }
    };

    // 默认意图检测规则
    const DEFAULT_INTENT_RULES = [
        { pattern: '什么|哪个|多少|是否|who|what|which|how many', goal: 'precise', reason: { zh: '检测到精确查询', en: 'Detected precise query' } },
        { pattern: '总结|概括|summarize|summary', goal: 'quick', reason: { zh: '检测到总结需求', en: 'Detected summary request' } },
        { pattern: '对比|比较|compare|difference', goal: 'compare', reason: { zh: '检测到对比需求', en: 'Detected comparison request' } },
        { pattern: '为什么|原因|why|cause', goal: 'causal', reason: { zh: '检测到因果分析', en: 'Detected causal analysis' } },
        { pattern: '关系|关联|relationship|connection', goal: 'relation', reason: { zh: '检测到关联分析', en: 'Detected relationship analysis' } },
        { pattern: '全面|详细|comprehensive|detailed', goal: 'comprehensive', reason: { zh: '检测到深度分析', en: 'Detected deep analysis' } }
    ];

    window.EmbeddedAIAnalysisPanel = function EmbeddedAIAnalysisPanel({
        selectedDocuments = [],
        onClose,
        onRemoveDocument
    }) {
        const { t, language } = window.LanguageModule.useTranslation();
        const lang = language || 'zh';

        // ==================== 状态管理 ====================

        // 动态配置（从后端加载）
        const [config, setConfig] = useState({
            goals: DEFAULT_GOALS,
            intentRules: DEFAULT_INTENT_RULES,
            strategies: {},
            loaded: false,
            error: null
        });

        // 用户交互状态
        const [customPrompt, setCustomPrompt] = useState('');
        const [analyzing, setAnalyzing] = useState(false);
        const [currentAnalysis, setCurrentAnalysis] = useState(null);
        const [analysisGoal, setAnalysisGoal] = useState('quick');
        const [showAdvanced, setShowAdvanced] = useState(false);
        const [advancedParams, setAdvancedParams] = useState({});

        // 智能推荐
        const [recommendedGoal, setRecommendedGoal] = useState(null);
        const [recommendReason, setRecommendReason] = useState('');

        // 策略市场
        const [showStrategyMarket, setShowStrategyMarket] = useState(false);
        const [availableStrategies, setAvailableStrategies] = useState([]);

        // 高赞提示词推荐
        const [showPromptRecommendation, setShowPromptRecommendation] = useState(false);
        const [currentStrategy, setCurrentStrategy] = useState('all');

        // ==================== 从后端加载配置 ====================

        useEffect(() => {
            loadConfiguration();
        }, []);

        const loadConfiguration = async () => {
            try {
                // 尝试从后端加载策略配置
                const response = await fetch('/api/strategies/config');
                if (response.ok) {
                    const data = await response.json();
                    setConfig({
                        goals: data.goals || DEFAULT_GOALS,
                        intentRules: data.intentRules || DEFAULT_INTENT_RULES,
                        strategies: data.strategies || {},
                        loaded: true,
                        error: null
                    });

                    // 设置默认选中的目标
                    if (data.defaultGoal) {
                        setAnalysisGoal(data.defaultGoal);
                    }

                    console.log('✅ Strategy configuration loaded from server');
                } else {
                    throw new Error('Failed to load configuration');
                }
            } catch (error) {
                console.warn('⚠️ Using default configuration:', error.message);
                setConfig(prev => ({
                    ...prev,
                    loaded: true,
                    error: 'Using offline configuration'
                }));
            }
        };

        // ==================== 智能意图检测 ====================

        useEffect(() => {
            detectUserIntent();
        }, [customPrompt, selectedDocuments, config.intentRules]);

        const detectUserIntent = useCallback(() => {
            const question = customPrompt.toLowerCase();
            const docCount = selectedDocuments.length;

            let detected = null;
            let reason = '';

            // 基于配置的规则检测
            for (const rule of config.intentRules) {
                const regex = new RegExp(rule.pattern, 'i');
                if (regex.test(question)) {
                    detected = rule.goal;
                    reason = rule.reason[lang] || rule.reason.zh;
                    break;
                }
            }

            // 基于文档数量的默认推荐
            if (!detected && docCount >= 5) {
                detected = 'quick';
                reason = lang === 'en' ? `${docCount} documents, recommend quick overview` : `${docCount}个文档，推荐快速概览`;
            } else if (!detected && docCount >= 2) {
                detected = config.goals.compare ? 'compare' : 'relation';
                reason = lang === 'en' ? 'Multiple documents, may need comparison' : '多个文档，可能需要对比分析';
            }

            setRecommendedGoal(detected);
            setRecommendReason(reason);
        }, [customPrompt, selectedDocuments, config.intentRules, lang]);

        // ==================== 获取本地化文本 ====================

        const getLocalizedText = useCallback((textObj, fallback = '') => {
            if (!textObj) return fallback;
            if (typeof textObj === 'string') return textObj;
            return textObj[lang] || textObj.zh || textObj.en || fallback;
        }, [lang]);

        // ==================== 排序后的目标列表 ====================

        const sortedGoals = useMemo(() => {
            return Object.values(config.goals)
                .sort((a, b) => (a.order || 99) - (b.order || 99));
        }, [config.goals]);

        // ==================== 检查目标是否可用 ====================

        const isGoalAvailable = useCallback((goal) => {
            if (!goal) return false;
            const minDocs = goal.minDocs || 1;
            return selectedDocuments.length >= minDocs;
        }, [selectedDocuments]);

        // ==================== 处理移除文档 ====================

        const handleRemoveDocument = useCallback((doc) => {
            if (onRemoveDocument && !analyzing) {
                onRemoveDocument(doc);
            }
        }, [onRemoveDocument, analyzing]);

        // ==================== 执行分析 ====================

        const analyzeDocuments = async () => {
            if (!selectedDocuments || selectedDocuments.length === 0) {
                alert(t('pleaseSelectDocuments') || '请选择要分析的文档');
                return;
            }

            const goalConfig = config.goals[analysisGoal];
            if (!goalConfig) {
                alert('Invalid analysis goal');
                return;
            }

            if (!isGoalAvailable(goalConfig)) {
                const minDocs = goalConfig.minDocs || 2;
                alert(t('needMoreDocs')?.replace('{0}', minDocs) || `至少需要${minDocs}个文档`);
                return;
            }

            const finalPrompt = customPrompt || getLocalizedText(goalConfig.defaultPrompt, t('defaultAnalysisPrompt'));

            setAnalyzing(true);
            setCurrentAnalysis({
                documents: selectedDocuments,
                prompt: finalPrompt,
                status: 'running',
                progress: 0,
                results: [],
                analysisGoal: analysisGoal
            });

            try {
                const docNames = selectedDocuments.map(d => d.title || d.name);

                // 调用后端分析 API，传递策略配置
                const response = await fetch('/api/document-qa/analyze-smart', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        documentPaths: docNames,
                        question: finalPrompt,
                        goalId: analysisGoal,
                        strategies: goalConfig.strategies,
                        advancedParams: showAdvanced ? advancedParams : {},
                        language: lang
                    })
                });

                if (!response.ok) {
                    throw new Error(`Analysis failed: ${response.status}`);
                }

                const result = await response.json();

                setCurrentAnalysis(prev => ({
                    ...prev,
                    status: 'completed',
                    progress: 100,
                    results: [{
                        document: { title: getLocalizedText(goalConfig.label), name: docNames.join(', ') },
                        success: true,
                        data: result,
                        isMultiDoc: selectedDocuments.length > 1
                    }]
                }));

                // 保存到历史
                try {
                    await window.api.saveLLMResult({
                        title: `${docNames.join(', ')} - ${getLocalizedText(goalConfig.label)}`,
                        sourceDocument: docNames.join(', '),
                        question: finalPrompt,
                        analysisType: getLocalizedText(goalConfig.label),
                        content: result.answer || result.finalReport || result.comprehensiveSummary || JSON.stringify(result),
                        keyPoints: result.keyPoints || []
                    });
                } catch (saveError) {
                    console.warn('Failed to save result:', saveError);
                }

            } catch (error) {
                console.error('Analysis failed:', error);
                setCurrentAnalysis(prev => ({
                    ...prev,
                    status: 'error',
                    error: error.message
                }));
            } finally {
                setAnalyzing(false);
            }
        };

        // ==================== 渲染 Markdown ====================

        const renderMarkdown = useCallback((text) => {
            if (!text) return null;
            if (window.marked) {
                return <div className="markdown-content" dangerouslySetInnerHTML={{ __html: window.marked.parse(text) }} />;
            }
            return <div style={{ whiteSpace: 'pre-wrap' }}>{text}</div>;
        }, []);

        // ==================== 渲染动态参数控件 ====================

        const renderParamControl = useCallback((param) => {
            const value = advancedParams[param.name] ?? param.default;

            switch (param.type) {
                case 'slider':
                    return (
                        <div key={param.name} className="ai-param-control">
                            <label>{getLocalizedText(param.label)}</label>
                            <input
                                type="range"
                                min={param.min || 0}
                                max={param.max || 100}
                                value={value}
                                onChange={(e) => setAdvancedParams(prev => ({
                                    ...prev,
                                    [param.name]: parseInt(e.target.value)
                                }))}
                                disabled={analyzing}
                            />
                            <span className="ai-param-value">{value}</span>
                        </div>
                    );

                case 'checkbox':
                    return (
                        <div key={param.name} className="ai-param-control">
                            <label>
                                <input
                                    type="checkbox"
                                    checked={value}
                                    onChange={(e) => setAdvancedParams(prev => ({
                                        ...prev,
                                        [param.name]: e.target.checked
                                    }))}
                                    disabled={analyzing}
                                />
                                {' ' + getLocalizedText(param.label)}
                            </label>
                        </div>
                    );

                case 'select':
                    return (
                        <div key={param.name} className="ai-param-control">
                            <label>{getLocalizedText(param.label)}</label>
                            <select
                                value={value}
                                onChange={(e) => setAdvancedParams(prev => ({
                                    ...prev,
                                    [param.name]: e.target.value
                                }))}
                                disabled={analyzing}
                            >
                                {param.options.map(opt =>
                                    <option key={opt.value} value={opt.value}>
                                        {getLocalizedText(opt.label)}
                                    </option>
                                )}
                            </select>
                        </div>
                    );

                default:
                    return null;
            }
        }, [advancedParams, analyzing, getLocalizedText]);

        // ==================== 主渲染 ====================

        return (
            <div className="ai-analysis-container">
                {/* 加载指示器 */}
                {!config.loaded && (
                    <div className="ai-loading">
                        ⏳ {t('loadingConfig') || '加载配置中...'}
                    </div>
                )}

                {/* 选中的文档信息 */}
                <div className="ai-selected-info">
                    <h3>
                        📁 {t('selectedDocumentsCount')?.replace('{0}', selectedDocuments.length) || `已选择 ${selectedDocuments.length} 个文档`}
                    </h3>
                    {selectedDocuments.length > 0 && (
                        <div className="ai-doc-list">
                            {selectedDocuments.map((doc, i) =>
                                <div key={i} className="ai-doc-item-removable">
                                    <span className="ai-doc-name">
                                        📄 {doc.title || doc.name}
                                    </span>
                                    <button
                                        className="ai-doc-remove-btn"
                                        onClick={() => handleRemoveDocument(doc)}
                                        disabled={analyzing}
                                        title={t('removeDocument') || '移除'}
                                    >
                                        ×
                                    </button>
                                </div>
                            )}
                        </div>
                    )}
                    {selectedDocuments.length === 0 && (
                        <div className="ai-no-docs-hint">
                            {t('noDocumentsSelected') || '请选择要分析的文档'}
                        </div>
                    )}
                </div>

                {/* 智能推荐提示 */}
                {recommendedGoal && recommendedGoal !== analysisGoal && !analyzing && (
                    <div className="ai-smart-recommend">
                        <div className="ai-recommend-content">
                            <span className="ai-recommend-icon">💡</span>
                            <span className="ai-recommend-text">
                                {t('smartRecommend') || '智能推荐'}: {getLocalizedText(config.goals[recommendedGoal]?.label)}
                            </span>
                            <span className="ai-recommend-reason">
                                ({recommendReason})
                            </span>
                        </div>
                        <button
                            className="ai-recommend-apply-btn"
                            onClick={() => setAnalysisGoal(recommendedGoal)}
                        >
                            {t('applyRecommend') || '采纳'}
                        </button>
                    </div>
                )}

                {/* 动态分析目标选择 */}
                <div className="ai-goal-selection">
                    <div className="ai-goal-header">
                        <div className="ai-goal-title">
                            {t('whatDoYouWant') || '📋 您想要什么样的分析？'}
                        </div>
                        {/* 策略市场入口 */}
                        <button
                            className="ai-strategy-market-btn"
                            onClick={() => setShowStrategyMarket(true)}
                            title={t('strategyMarket') || '策略市场'}
                        >
                            🏪
                        </button>
                    </div>
                    
                    <div className="ai-goal-grid">
                        {sortedGoals.map(goal =>
                            <label
                                key={goal.id}
                                className={
                                    'ai-goal-option' +
                                    (analysisGoal === goal.id ? ' active' : '') +
                                    (!isGoalAvailable(goal) ? ' disabled' : '')
                                }
                            >
                                <input
                                    type="radio"
                                    name="analysisGoal"
                                    checked={analysisGoal === goal.id}
                                    onChange={() => setAnalysisGoal(goal.id)}
                                    disabled={analyzing || !isGoalAvailable(goal)}
                                />
                                <div className="ai-goal-content">
                                    <span className="ai-goal-icon">{goal.icon}</span>
                                    <div className="ai-goal-text">
                                        <div className="ai-goal-label">
                                            {getLocalizedText(goal.label)}
                                        </div>
                                        <div className="ai-goal-desc">
                                            {getLocalizedText(goal.description)}
                                        </div>
                                        {!isGoalAvailable(goal) && (
                                            <div className="ai-goal-warning">
                                                ⚠️ {t('needAtLeast')} {goal.minDocs} {t('documents') || '个文档'}
                                            </div>
                                        )}
                                        {goal.estimatedTime && (
                                            <div className="ai-goal-time">
                                                ⏱️ {getLocalizedText(goal.estimatedTime)}
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </label>
                        )}
                    </div>
                </div>

                {/* 自定义问题输入 */}
                <div className="ai-prompt-section">
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '8px' }}>
                        <label className="ai-prompt-label" style={{ margin: 0 }}>
                            {t('yourQuestion') || '💬 您的问题（可选）'}
                        </label>
                        <button
                            onClick={() => {
                                setCurrentStrategy(getStrategyFromGoal(analysisGoal));
                                setShowPromptRecommendation(true);
                            }}
                            disabled={analyzing}
                            style={{
                                padding: '6px 12px',
                                fontSize: '13px',
                                background: 'linear-gradient(135deg, #FFA726 0%, #FB8C00 100%)',
                                color: '#fff',
                                border: 'none',
                                borderRadius: '6px',
                                cursor: 'pointer',
                                fontWeight: '600',
                                boxShadow: '0 2px 8px rgba(255, 167, 38, 0.4)',
                                transition: 'all 0.3s ease'
                            }}
                            title={t('viewHighRatedPrompts') || '查看该策略下的高赞提示词'}
                        >
                            {t('highRatedPromptsButton') || '💡 高赞提示词'}
                        </button>
                    </div>
                    <textarea
                        value={customPrompt}
                        onChange={(e) => setCustomPrompt(e.target.value)}
                        placeholder={t('questionPlaceholder') || '输入具体问题...'}
                        className="ai-analysis-textarea"
                        rows={2}
                        disabled={analyzing}
                    />
                </div>

                {/* 高级选项（动态渲染策略参数） */}
                {config.goals[analysisGoal]?.userConfigurable && (
                    <div className="ai-advanced-section">
                        <button
                            className="ai-advanced-btn"
                            onClick={() => setShowAdvanced(!showAdvanced)}
                        >
                            {showAdvanced
                                ? `▼ ${t('hideAdvanced') || '收起高级选项'}`
                                : `▶ ${t('showAdvanced') || '高级选项'}`
                            }
                        </button>
                        {showAdvanced && (
                            <div className="ai-advanced-options">
                                {config.goals[analysisGoal].userConfigurable.map(param => renderParamControl(param))}
                            </div>
                        )}
                    </div>
                )}

                {/* 开始分析按钮 */}
                <div className="ai-action-section">
                    <button
                        onClick={analyzeDocuments}
                        disabled={analyzing || selectedDocuments.length === 0}
                        className={
                            'ai-analysis-button' +
                            (config.goals[analysisGoal]?.buttonClass ? ' ' + config.goals[analysisGoal].buttonClass : '')
                        }
                    >
                        {analyzing
                            ? `🔄 ${t('analyzing') || '分析中...'}`
                            : `${config.goals[analysisGoal]?.icon || '🚀'} ${t('startAnalysis') || '开始分析'}`
                        }
                    </button>
                    {config.goals[analysisGoal]?.estimatedTime && (
                        <div className="ai-estimate-info">
                            ⏱️ {t('estimated') || '预计'}: {getLocalizedText(config.goals[analysisGoal].estimatedTime)}
                        </div>
                    )}
                </div>

                {/* 分析结果 */}
                {currentAnalysis && (
                    <div className="ai-results-section">
                        {currentAnalysis.status === 'running' && (
                            <div className="ai-progress-bar">
                                <div
                                    className="ai-progress-fill"
                                    style={{ width: `${currentAnalysis.progress}%` }}
                                />
                                <span className="ai-progress-text">
                                    {currentAnalysis.progress}% - {currentAnalysis.currentDoc || t('analyzing')}
                                </span>
                            </div>
                        )}

                        {currentAnalysis.status === 'completed' && currentAnalysis.results &&
                            currentAnalysis.results.map((result, index) =>
                                <div
                                    key={index}
                                    className={'ai-result-item' + (result.isMultiDoc ? ' multi-doc-result' : '')}
                                >
                                    <div className="ai-result-header">
                                        <span className="ai-result-icon">
                                            {result.success ? '✅' : '❌'}
                                        </span>
                                        <span>{result.document.title || result.document.name}</span>
                                    </div>
                                    <div className="ai-result-body">
                                        {result.success
                                            ? renderMarkdown(
                                                result.data.answer ||
                                                result.data.finalReport ||
                                                result.data.comprehensiveSummary ||
                                                JSON.stringify(result.data, null, 2)
                                            )
                                            : <div className="ai-result-error">
                                                {t('analysisFailed')}: {result.error}
                                            </div>
                                        }
                                    </div>
                                </div>
                            )
                        }

                        {currentAnalysis.status === 'error' && (
                            <div className="ai-error-message">
                                ❌ {currentAnalysis.error}
                            </div>
                        )}

                        {/* 反馈区域 */}
                        {currentAnalysis.status === 'completed' && (
                            <div className="ai-feedback-section">
                                <div className="ai-feedback-title">
                                    {t('wasHelpful') || '有帮助吗？'}
                                </div>
                                <div className="ai-feedback-buttons">
                                    <button
                                        className="ai-feedback-btn positive"
                                        onClick={() => submitFeedback('helpful')}
                                    >
                                        😊
                                    </button>
                                    <button
                                        className="ai-feedback-btn neutral"
                                        onClick={() => submitFeedback('neutral')}
                                    >
                                        😐
                                    </button>
                                    <button
                                        className="ai-feedback-btn negative"
                                        onClick={() => submitFeedback('not_helpful')}
                                    >
                                        😕
                                    </button>
                                </div>
                            </div>
                        )}
                    </div>
                )}

                {/* 策略市场弹窗 */}
                {showStrategyMarket && (
                    <StrategyMarketModal
                        onClose={() => setShowStrategyMarket(false)}
                        onInstall={handleStrategyInstall}
                        language={lang}
                    />
                )}

                {/* 高赞提示词推荐面板 */}
                {window.PromptRecommendationPanel && (
                    <window.PromptRecommendationPanel
                        strategy={currentStrategy}
                        visible={showPromptRecommendation}
                        onSelectPrompt={(prompt) => {
                            setCustomPrompt(prompt);
                            setShowPromptRecommendation(false);
                        }}
                        onClose={() => setShowPromptRecommendation(false)}
                    />
                )}
            </div>
        );

        // 根据目标获取策略类型
        function getStrategyFromGoal(goalId) {
            const goalToStrategyMap = {
                'quick': '快速总结',
                'precise': '精确查找',
                'compare': '对比分析',
                'causal': '深度分析',
                'relation': '深度分析',
                'comprehensive': '深度分析'
            };
            return goalToStrategyMap[goalId] || 'all';
        }

        // 提交反馈
        async function submitFeedback(rating) {
            try {
                await fetch('/api/feedback/analysis', {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        analysisGoal,
                        rating,
                        documentCount: selectedDocuments.length,
                        question: customPrompt
                    })
                });
            } catch (e) {
                console.warn('Feedback submission failed:', e);
            }
        }

        // 处理策略安装
        async function handleStrategyInstall(strategyId) {
            try {
                await fetch(`/api/strategies/${strategyId}/install`, { method: 'POST' });
                await loadConfiguration(); // 重新加载配置
                setShowStrategyMarket(false);
            } catch (e) {
                console.error('Strategy install failed:', e);
            }
        }
    };

    // ==================== 策略市场弹窗组件 ====================

    function StrategyMarketModal({ onClose, onInstall, language }) {
        const { t } = window.LanguageModule.useTranslation();
        const [strategies, setStrategies] = useState([]);
        const [loading, setLoading] = useState(true);
        const [filter, setFilter] = useState('all');
        const [searchQuery, setSearchQuery] = useState('');

        useEffect(() => {
            loadStrategies();
        }, []);

        async function loadStrategies() {
            try {
                const response = await fetch('/api/strategies/marketplace');
                if (response.ok) {
                    const data = await response.json();
                    setStrategies(data.strategies || []);
                }
            } catch (e) {
                console.error('Failed to load strategies:', e);
            } finally {
                setLoading(false);
            }
        }

        const filteredStrategies = strategies.filter(s => {
            if (filter !== 'all' && s.status !== filter) return false;
            if (searchQuery && !s.name.toLowerCase().includes(searchQuery.toLowerCase())) return false;
            return true;
        });

        const getText = (obj) => {
            if (!obj) return '';
            if (typeof obj === 'string') return obj;
            return obj[language] || obj.zh || obj.en || '';
        };

        return (
            <div className="ai-modal-overlay">
                <div
                    className="ai-strategy-market-modal"
                >
                    {/* 头部 */}
                    <div className="ai-market-header">
                        <h2>🏪 {t('strategyMarket') || '策略市场'}</h2>
                        <button
                            className="ai-modal-close"
                            onClick={onClose}
                        >
                            ×
                        </button>
                    </div>

                    {/* 搜索和筛选 */}
                    <div className="ai-market-filters">
                        <input
                            type="text"
                            placeholder={t('searchStrategies') || '搜索策略...'}
                            value={searchQuery}
                            onChange={e => setSearchQuery(e.target.value)}
                            className="ai-market-search"
                        />
                        <select
                            value={filter}
                            onChange={e => setFilter(e.target.value)}
                            className="ai-market-filter-select"
                        >
                            <option value="all">{t('all') || '全部'}</option>
                            <option value="installed">{t('installed') || '已安装'}</option>
                            <option value="available">{t('available') || '可安装'}</option>
                            <option value="update">{t('updates') || '有更新'}</option>
                        </select>
                    </div>

                    {/* 策略列表 */}
                    <div className="ai-market-list">
                        {loading
                            ? <div className="ai-market-loading">⏳ {t('loading') || 'Loading...'}</div>
                            : filteredStrategies.length === 0
                                ? <div className="ai-market-empty">
                                    {t('noStrategiesFound') || '未找到策略'}
                                </div>
                                : filteredStrategies.map(strategy =>
                                    <div
                                        key={strategy.id}
                                        className="ai-strategy-card"
                                    >
                                        <div className="ai-strategy-card-header">
                                            <span className="ai-strategy-icon">{strategy.icon || '📦'}</span>
                                            <div className="ai-strategy-info">
                                                <div className="ai-strategy-name">
                                                    {getText(strategy.name)}
                                                    <span className="ai-strategy-version">
                                                        v{strategy.version}
                                                    </span>
                                                </div>
                                                <div className="ai-strategy-author">
                                                    {strategy.author}
                                                </div>
                                            </div>
                                            {strategy.rating && (
                                                <div className="ai-strategy-rating">
                                                    ⭐ {strategy.rating}
                                                </div>
                                            )}
                                        </div>
                                        <div className="ai-strategy-desc">
                                            {getText(strategy.description)}
                                        </div>
                                        <div className="ai-strategy-tags">
                                            {(strategy.tags || []).map(tag =>
                                                <span key={tag} className="ai-strategy-tag">{tag}</span>
                                            )}
                                        </div>
                                        <div className="ai-strategy-metrics">
                                            <span>📊 {getText(strategy.quality) || 'Good'}</span>
                                            <span>⚡ {getText(strategy.speed) || 'Medium'}</span>
                                            <span>💰 {getText(strategy.cost) || 'Medium'}</span>
                                        </div>
                                        <div className="ai-strategy-actions">
                                            {strategy.status === 'installed'
                                                ? <button
                                                    className="ai-strategy-btn installed"
                                                    disabled
                                                >
                                                    ✓ {t('installed') || '已安装'}
                                                </button>
                                                : strategy.status === 'update'
                                                    ? <button
                                                        className="ai-strategy-btn update"
                                                        onClick={() => onInstall(strategy.id)}
                                                    >
                                                        🔄 {t('update') || '更新'}
                                                    </button>
                                                    : <button
                                                        className="ai-strategy-btn install"
                                                        onClick={() => onInstall(strategy.id)}
                                                    >
                                                        📥 {t('install') || '安装'}
                                                    </button>
                                            }
                                        </div>
                                    </div>
                                )
                        }
                    </div>

                    {/* 底部 */}
                    <div className="ai-market-footer">
                        <span>
                            📦 {strategies.filter(s => s.status === 'installed').length} {t('installed') || '已安装'}
                        </span>
                        <span>
                            🆕 {strategies.filter(s => s.status === 'available').length} {t('available') || '可安装'}
                        </span>
                    </div>
                </div>
            </div>
        );
    }

    console.log('✅ EmbeddedAIAnalysisPanel (Dynamic Version) loaded');
})();
