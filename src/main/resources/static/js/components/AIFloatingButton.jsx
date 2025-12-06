/**
 * AI Floating Button Component / AI悬浮按钮组件
 * 右侧悬浮的快速切换按钮，用于打开/关闭AI分析面板
 * 
 * @author AI Reviewer Team
 * @since 2025-12-06
 */

function AIFloatingButton({ 
    showAIAnalysis, 
    setShowAIAnalysis, 
    selectedDocsCount, 
    splitPosition 
}) {
    const { t } = window.LanguageModule.useTranslation();

    return (
        <div
            className="ai-panel-float-button-container"
            style={{ right: showAIAnalysis ? `calc(${100 - splitPosition}% + 10px)` : '10px' }}
        >
            <button
                onClick={() => setShowAIAnalysis(!showAIAnalysis)}
                title={showAIAnalysis ? t('closeAIPanel') : t('openAIPanel')}
                className={`ai-panel-float-button ${showAIAnalysis ? 'opened' : 'closed'}`}
            >
                {showAIAnalysis ? '✕' : '🤖'}
            </button>
            {selectedDocsCount > 0 && (
                <div className="ai-panel-doc-badge">
                    {selectedDocsCount}
                </div>
            )}
        </div>
    );
}

// 导出到全局
window.AIFloatingButton = AIFloatingButton;
