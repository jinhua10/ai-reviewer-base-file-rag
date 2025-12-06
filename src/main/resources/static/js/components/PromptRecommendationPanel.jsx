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
                console.error('Failed to load prompt recommendations:', err);
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
            <div style={styles.overlay} onClick={onClose}>
                <div style={styles.panel} onClick={(e) => e.stopPropagation()}>
                    {/* 标题栏 */}
                    <div style={styles.header}>
                        <h3 style={styles.title}>
                            {t('promptRecommendationsTitle') || '💡 高赞提示词推荐'}
                        </h3>
                        <button style={styles.closeButton} onClick={onClose}>
                            ✕
                        </button>
                    </div>

                    {/* 策略标签 */}
                    <div style={styles.strategyTag}>
                        <span style={{
                            ...styles.strategyBadge,
                            backgroundColor: getStrategyColor(strategy)
                        }}>
                            {getStrategyDisplayName(strategy)}
                        </span>
                    </div>

                    {/* 内容区域 */}
                    <div style={styles.content}>
                        {loading && (
                            <div style={styles.loading}>
                                <div style={styles.spinner} />
                                <p>{t('loading') || '加载中...'}</p>
                            </div>
                        )}

                        {error && (
                            <div style={styles.error}>
                                ❌ {error}
                            </div>
                        )}

                        {!loading && !error && prompts.length === 0 && (
                            <div style={styles.empty}>
                                <div style={styles.emptyIcon}>📝</div>
                                <p>{t('noPrompts') || '暂无高赞提示词'}</p>
                                <p style={styles.emptyHint}>
                                    {t('noPromptsHint') || '使用AI分析后，给予高评分的提示词会出现在这里'}
                                </p>
                            </div>
                        )}

                        {!loading && !error && prompts.length > 0 && (
                            <div style={styles.promptList}>
                                {prompts.map((prompt, index) => (
                                    <div
                                        key={index}
                                        style={styles.promptItem}
                                        onClick={() => handleSelectPrompt(prompt)}
                                    >
                                        <div style={styles.promptHeader}>
                                            <span style={styles.rating}>
                                                {getRatingStars(prompt.rating)}
                                            </span>
                                            <span style={{
                                                ...styles.strategyLabel,
                                                color: getStrategyColor(prompt.strategy)
                                            }}>
                                                {getStrategyDisplayName(prompt.strategy)}
                                            </span>
                                        </div>
                                        <div style={styles.promptText}>
                                            {prompt.prompt}
                                        </div>
                                        <div style={styles.promptFooter}>
                                            <span style={styles.usageCount}>
                                                🔥 {(t('usageTimes') || '使用 {0} 次').replace('{0}', prompt.usageCount)}
                                            </span>
                                            <span style={styles.clickHint}>
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

    const styles = {
        overlay: {
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.5)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 10000,
            backdropFilter: 'blur(4px)'
        },
        panel: {
            backgroundColor: '#ffffff',
            borderRadius: '16px',
            boxShadow: '0 12px 40px rgba(0, 0, 0, 0.3)',
            width: '90%',
            maxWidth: '600px',
            maxHeight: '80vh',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden'
        },
        header: {
            padding: '20px 24px',
            borderBottom: '2px solid #E8EAF6',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)'
        },
        title: {
            margin: 0,
            fontSize: '20px',
            fontWeight: '700',
            color: '#ffffff',
            textShadow: '0 2px 4px rgba(0,0,0,0.2)'
        },
        closeButton: {
            background: 'rgba(255, 255, 255, 0.2)',
            border: 'none',
            borderRadius: '50%',
            width: '32px',
            height: '32px',
            fontSize: '18px',
            color: '#ffffff',
            cursor: 'pointer',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            transition: 'all 0.2s',
            fontWeight: 'bold'
        },
        strategyTag: {
            padding: '12px 24px',
            backgroundColor: '#F5F5F5',
            borderBottom: '1px solid #E0E0E0'
        },
        strategyBadge: {
            display: 'inline-block',
            padding: '6px 16px',
            borderRadius: '20px',
            color: '#ffffff',
            fontSize: '14px',
            fontWeight: '600',
            boxShadow: '0 2px 8px rgba(0,0,0,0.2)'
        },
        content: {
            flex: 1,
            overflowY: 'auto',
            padding: '16px'
        },
        loading: {
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '60px 20px',
            color: '#666'
        },
        spinner: {
            width: '40px',
            height: '40px',
            border: '4px solid #E0E0E0',
            borderTop: '4px solid #667eea',
            borderRadius: '50%',
            animation: 'spin 1s linear infinite',
            marginBottom: '16px'
        },
        error: {
            padding: '20px',
            backgroundColor: '#FFEBEE',
            color: '#C62828',
            borderRadius: '8px',
            textAlign: 'center',
            margin: '20px'
        },
        empty: {
            textAlign: 'center',
            padding: '60px 20px',
            color: '#999'
        },
        emptyIcon: {
            fontSize: '64px',
            marginBottom: '16px'
        },
        emptyHint: {
            fontSize: '13px',
            color: '#BBB',
            marginTop: '8px'
        },
        promptList: {
            display: 'flex',
            flexDirection: 'column',
            gap: '12px'
        },
        promptItem: {
            padding: '16px',
            backgroundColor: '#FAFAFA',
            borderRadius: '12px',
            border: '2px solid transparent',
            cursor: 'pointer',
            transition: 'all 0.3s ease',
            ':hover': {
                borderColor: '#667eea',
                backgroundColor: '#F3F4FF',
                transform: 'translateY(-2px)',
                boxShadow: '0 4px 12px rgba(102, 126, 234, 0.2)'
            }
        },
        promptHeader: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            marginBottom: '10px'
        },
        rating: {
            fontSize: '16px'
        },
        strategyLabel: {
            fontSize: '12px',
            fontWeight: '600',
            padding: '4px 10px',
            backgroundColor: 'rgba(255, 255, 255, 0.9)',
            borderRadius: '12px',
            boxShadow: '0 1px 3px rgba(0,0,0,0.1)'
        },
        promptText: {
            fontSize: '15px',
            color: '#333',
            lineHeight: '1.6',
            marginBottom: '10px',
            fontWeight: '500'
        },
        promptFooter: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            fontSize: '12px',
            color: '#999'
        },
        usageCount: {
            fontSize: '12px'
        },
        clickHint: {
            color: '#667eea',
            fontWeight: '600'
        }
    };

    // 添加CSS动画
    const styleSheet = document.createElement('style');
    styleSheet.textContent = `
        @keyframes spin {
            0% { transform: rotate(0deg); }
            100% { transform: rotate(360deg); }
        }
        
        .prompt-item-hover:hover {
            border-color: #667eea !important;
            background-color: #F3F4FF !important;
            transform: translateY(-2px) !important;
            box-shadow: 0 4px 12px rgba(102, 126, 234, 0.2) !important;
        }
    `;
    document.head.appendChild(styleSheet);

    console.log('✅ PromptRecommendationPanel component loaded (JSX)');
})();
