/**
 * 高赞提示词推荐浮动面板
 * 当用户选择策略时，在右侧显示该策略下的高评分历史提示词
 * JSX 版本 - 使用 Babel 转译
 */
(function() {
    'use strict';

    const { useState, useEffect } = React;

    window.PromptRecommendationPanel = function PromptRecommendationPanel({
        strategy = 'all',
        visible = false,
        onSelectPrompt,
        onClose
    }) {
        const { t, language } = window.LanguageModule.useTranslation();

        const [prompts, setPrompts] = useState([]);
        const [loading, setLoading] = useState(false);
        const [error, setError] = useState(null);

        // 当策略变化时加载推荐提示词
        useEffect(() => {
            if (visible && strategy) {
                loadPrompts();
            }
        }, [strategy, visible]);

        const loadPrompts = async () => {
            setLoading(true);
            setError(null);

            try {
                const response = await window.api.getPromptRecommendations(strategy, 10);
                if (response.success) {
                    setPrompts(response.prompts || []);
                } else {
                    setError(t('loadFailed') || '加载失败');
                }
            } catch (err) {
                console.error(t('logPromptRecommendationError'), err);
                setError((t('loadFailed') || '加载失败') + ': ' + err.message);
            } finally {
                setLoading(false);
            }
        };

        const handleSelectPrompt = (prompt) => {
            if (onSelectPrompt) {
                onSelectPrompt(prompt.prompt);
            }
        };

        const getRatingStars = (rating) => {
            return '⭐'.repeat(rating);
        };

        // 策略标识符映射（统一处理中英文）
        const normalizeStrategy = (strategy) => {
            const strategyNormalizeMap = {
                '快速总结': 'quickSummary',
                'Quick Summary': 'quickSummary',
                '深度分析': 'deepAnalysis',
                'Deep Analysis': 'deepAnalysis',
                '对比分析': 'compareAnalysis',
                'Comparison': 'compareAnalysis',
                '信息提取': 'infoExtraction',
                'Info Extraction': 'infoExtraction',
                '精确查找': 'preciseSearch',
                'Precise Search': 'preciseSearch',
                '通用': 'general',
                'General': 'general',
                'all': 'all'
            };
            return strategyNormalizeMap[strategy] || strategy;
        };

        const getStrategyColor = (strategy) => {
            const normalized = normalizeStrategy(strategy);
            const colors = {
                'quickSummary': '#42A5F5',
                'deepAnalysis': '#FF9800',
                'compareAnalysis': '#66BB6A',
                'infoExtraction': '#AB47BC',
                'preciseSearch': '#26C6DA',
                'general': '#78909C'
            };
            return colors[normalized] || '#78909C';
        };

        const getStrategyDisplayName = (strategy) => {
            const normalized = normalizeStrategy(strategy);
            
            if (normalized === 'all') {
                return t('allStrategies') || '全部策略';
            }
            
            const strategyKeyMap = {
                'quickSummary': 'quickSummaryStrategy',
                'deepAnalysis': 'deepAnalysisStrategy',
                'compareAnalysis': 'compareAnalysisStrategy',
                'infoExtraction': 'infoExtractionStrategy',
                'preciseSearch': 'preciseSearchStrategy',
                'general': 'generalStrategy'
            };
            
            const translationKey = strategyKeyMap[normalized];
            return translationKey ? (t(translationKey) || strategy) : strategy;
        };

        if (!visible) return null;

        return (
            <div className="prompt-panel-overlay" onClick={onClose}>
                <div className="prompt-panel" onClick={(e) => e.stopPropagation()}>
                    {/* 头部 (Header) */}
                    <div className="prompt-panel-header">
                        <h3 className="prompt-panel-title">
                            {t('promptRecommendationsTitle') || '💡 高赞提示词推荐'}
                        </h3>
                        <button className="prompt-panel-close-btn" onClick={onClose}>
                            ✕
                        </button>
                    </div>

                    {/* 策略标签 (Strategy tag) */}
                    <div className="prompt-strategy-tag-area">
                        <span
                            className="prompt-strategy-badge"
                            style={{ backgroundColor: getStrategyColor(strategy) }}
                        >
                            {getStrategyDisplayName(strategy)}
                        </span>
                    </div>

                    {/* 内容区域 (Content area) */}
                    <div className="prompt-panel-content">
                        {loading && (
                            <div className="prompt-panel-loading">
                                <div className="prompt-panel-spinner" />
                                <p>{t('loading') || '加载中...'}</p>
                            </div>
                        )}

                        {error && (
                            <div className="prompt-panel-error">
                                ❌ {error}
                            </div>
                        )}

                        {!loading && !error && prompts.length === 0 && (
                            <div className="prompt-panel-empty">
                                <div className="prompt-panel-empty-icon">📝</div>
                                <p>{t('noPrompts') || '暂无高赞提示词'}</p>
                                <p className="prompt-panel-empty-hint">
                                    {t('noPromptsHint') || '使用AI分析后，给予高评分的提示词会出现在这里'}
                                </p>
                            </div>
                        )}

                        {!loading && !error && prompts.length > 0 && (
                            <div className="prompt-list">
                                {prompts.map((prompt, index) => (
                                    <div
                                        key={index}
                                        className="prompt-item"
                                        onClick={() => handleSelectPrompt(prompt)}
                                    >
                                        <div className="prompt-item-header">
                                            <span className="prompt-item-rating">
                                                {getRatingStars(prompt.rating)}
                                            </span>
                                            <span
                                                className="prompt-item-strategy-label"
                                                style={{ color: getStrategyColor(prompt.strategy) }}
                                            >
                                                {getStrategyDisplayName(prompt.strategy)}
                                            </span>
                                        </div>
                                        <div className="prompt-item-text">
                                            {prompt.prompt}
                                        </div>
                                        <div className="prompt-item-footer">
                                            <span className="prompt-item-usage-count">
                                                🔥 {(t('usageTimes') || '使用 {0} 次').replace('{0}', prompt.usageCount)}
                                            </span>
                                            <span className="prompt-item-click-hint">
                                                {t('clickToUse') || '点击使用 →'}
                                            </span>
                                        </div>
                                    </div>
                                ))}
                            </div>
                        )}
                    </div>
                </div>
            </div>
        );
    };

    // 样式已提取到 CSS 文件 (Styles extracted to CSS file)
    // See: assets/css/prompt-recommendation.css

    console.log(window.LanguageModule ? window.LanguageModule.useTranslation().t('logPromptRecommendationLoaded') : '✅ PromptRecommendationPanel component loaded (JSX)');
})();
