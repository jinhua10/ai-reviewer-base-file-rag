/**
 * 引导页面组件 (Welcome Guide Component)
 * JSX 版本 - 使用 Babel 转译
 *
 * 功能：
 * - 首次访问时自动显示
 * - 介绍系统核心概念和特性
 * - 支持跳过和逐步浏览
 * - 使用 localStorage 记录完成状态
 *
 * @author AI Reviewer Team
 * @since 2025-12-10
 */

function WelcomeGuide() {
    const { useState, useEffect } = React;
    const { t } = window.LanguageModule.useTranslation();

    // 状态管理 (State management)
    const [currentStep, setCurrentStep] = useState(1);
    const [isAnimating, setIsAnimating] = useState(false);
    const totalSteps = 5;

    // 处理下一步 (Handle next step)
    const handleNext = () => {
        if (currentStep < totalSteps) {
            setIsAnimating(true);
            setTimeout(() => {
                setCurrentStep(currentStep + 1);
                setIsAnimating(false);
            }, 300);
        }
    };

    // 处理上一步 (Handle previous step)
    const handlePrevious = () => {
        if (currentStep > 1) {
            setIsAnimating(true);
            setTimeout(() => {
                setCurrentStep(currentStep - 1);
                setIsAnimating(false);
            }, 300);
        }
    };

    // 跳过引导 (Skip guide)
    const handleSkip = () => {
        if (confirm(t('welcomeSkip') + '?')) {
            completeGuide();
        }
    };

    // 开始使用 (Start using)
    const handleStart = () => {
        completeGuide();
    };

    // 完成引导 (Complete guide)
    const completeGuide = () => {
        localStorage.setItem('welcomeGuideCompleted', 'true');
        // 触发自定义事件通知主应用 (Trigger custom event to notify main app)
        window.dispatchEvent(new CustomEvent('welcomeGuideCompleted'));
    };

    // 渲染步骤内容 (Render step content)
    const renderStepContent = () => {
        switch (currentStep) {
            case 1:
                return renderStep1();
            case 2:
                return renderStep2();
            case 3:
                return renderStep3();
            case 4:
                return renderStep4();
            case 5:
                return renderStep5();
            default:
                return null;
        }
    };

    // 步骤 1: 问题分析 (Step 1: Problem analysis)
    const renderStep1 = () => (
        <div className="welcome-step-content">
            <h2 className="welcome-step-title">{t('welcomeStep1Title')}</h2>

            <div className="welcome-problem-grid">
                <div className="welcome-problem-card">
                    <div className="problem-icon">📦</div>
                    <h3>{t('welcomeProblem1Title')}</h3>
                    <p>{t('welcomeProblem1Desc')}</p>
                </div>

                <div className="welcome-problem-card">
                    <div className="problem-icon">🔒</div>
                    <h3>{t('welcomeProblem2Title')}</h3>
                    <p>{t('welcomeProblem2Desc')}</p>
                </div>

                <div className="welcome-problem-card">
                    <div className="problem-icon">💾</div>
                    <h3>{t('welcomeProblem3Title')}</h3>
                    <p>{t('welcomeProblem3Desc')}</p>
                </div>
            </div>

            <div className="welcome-summary">
                <p>{t('welcomeProblemSummary')}</p>
            </div>
        </div>
    );

    // 步骤 2: 解决方案 (Step 2: Solution)
    const renderStep2 = () => (
        <div className="welcome-step-content">
            <h2 className="welcome-step-title">{t('welcomeStep2Title')}</h2>

            <div className="welcome-vision-box">
                <h3>{t('welcomeVisionTitle')}</h3>
                <p>{t('welcomeVisionDesc')}</p>
            </div>

            <div className="welcome-approach-list">
                <div className="welcome-approach-item">
                    <div className="approach-header">
                        <span className="approach-icon">📚</span>
                        <h4>{t('welcomeApproach1Title')}</h4>
                    </div>
                    <p>{t('welcomeApproach1Desc')}</p>
                </div>

                <div className="welcome-approach-item">
                    <div className="approach-header">
                        <span className="approach-icon">🎭</span>
                        <h4>{t('welcomeApproach2Title')}</h4>
                    </div>
                    <p>{t('welcomeApproach2Desc')}</p>
                </div>

                <div className="welcome-approach-item">
                    <div className="approach-header">
                        <span className="approach-icon">♻️</span>
                        <h4>{t('welcomeApproach3Title')}</h4>
                    </div>
                    <p>{t('welcomeApproach3Desc')}</p>
                </div>
            </div>
        </div>
    );

    // 步骤 3: 核心特性 (Step 3: Core features)
    const renderStep3 = () => (
        <div className="welcome-step-content">
            <h2 className="welcome-step-title">{t('welcomeStep3Title')}</h2>

            <div className="welcome-features-grid">
                <div className="welcome-feature-card">
                    <div className="feature-icon">🧠</div>
                    <h3>{t('welcomeFeature1Title')}</h3>
                    <p>{t('welcomeFeature1Desc')}</p>
                </div>

                <div className="welcome-feature-card">
                    <div className="feature-icon">⚡</div>
                    <h3>{t('welcomeFeature2Title')}</h3>
                    <p>{t('welcomeFeature2Desc')}</p>
                </div>

                <div className="welcome-feature-card">
                    <div className="feature-icon">🎯</div>
                    <h3>{t('welcomeFeature3Title')}</h3>
                    <p>{t('welcomeFeature3Desc')}</p>
                </div>

                <div className="welcome-feature-card">
                    <div className="feature-icon">🔍</div>
                    <h3>{t('welcomeFeature4Title')}</h3>
                    <p>{t('welcomeFeature4Desc')}</p>
                </div>
            </div>
        </div>
    );

    // 步骤 4: 知识演化 (Step 4: Knowledge evolution)
    const renderStep4 = () => (
        <div className="welcome-step-content">
            <h2 className="welcome-step-title">{t('welcomeStep4Title')}</h2>

            <div className="welcome-evolution-intro">
                <p>{t('welcomeEvolutionIntro')}</p>
            </div>

            <div className="welcome-cycle-flow">
                <div className="welcome-cycle-item">
                    <div className="cycle-number">1</div>
                    <h4>{t('welcomeCycle1Title')}</h4>
                    <p>{t('welcomeCycle1Desc')}</p>
                    <div className="cycle-arrow">↓</div>
                </div>

                <div className="welcome-cycle-item">
                    <div className="cycle-number">2</div>
                    <h4>{t('welcomeCycle2Title')}</h4>
                    <p>{t('welcomeCycle2Desc')}</p>
                    <div className="cycle-arrow">↓</div>
                </div>

                <div className="welcome-cycle-item">
                    <div className="cycle-number">3</div>
                    <h4>{t('welcomeCycle3Title')}</h4>
                    <p>{t('welcomeCycle3Desc')}</p>
                    <div className="cycle-arrow">↓</div>
                </div>

                <div className="welcome-cycle-item">
                    <div className="cycle-number">4</div>
                    <h4>{t('welcomeCycle4Title')}</h4>
                    <p>{t('welcomeCycle4Desc')}</p>
                </div>
            </div>

            <div className="welcome-note">
                <p>{t('welcomeEvolutionNote')}</p>
            </div>
        </div>
    );

    // 步骤 5: 开始使用 (Step 5: Get started)
    const renderStep5 = () => (
        <div className="welcome-step-content welcome-final-step">
            <h2 className="welcome-step-title">{t('welcomeStep5Title')}</h2>

            <div className="welcome-ready-box">
                <h3>{t('welcomeReadyTitle')}</h3>
                <p>{t('welcomeReadyDesc')}</p>
            </div>

            <div className="welcome-features-list">
                <h4>{t('welcomeFeatureListTitle')}</h4>
                <ul>
                    <li>{t('welcomeFeatureList1')}</li>
                    <li>{t('welcomeFeatureList2')}</li>
                    <li>{t('welcomeFeatureList3')}</li>
                    <li>{t('welcomeFeatureList4')}</li>
                </ul>
            </div>

            <div className="welcome-reopen-hint">
                <p>{t('welcomeGuideReopen')}</p>
            </div>

            <button className="welcome-start-button" onClick={handleStart}>
                {t('welcomeStartButton')} 🚀
            </button>
        </div>
    );

    return (
        <div className="welcome-guide-overlay">
            <div className="welcome-guide-container">
                {/* 头部 (Header) */}
                <div className="welcome-guide-header">
                    <h1 className="welcome-title">{t('welcomeTitle')}</h1>
                    <p className="welcome-subtitle">{t('welcomeSubtitle')}</p>
                    <button className="welcome-skip-button" onClick={handleSkip}>
                        {t('welcomeSkip')}
                    </button>
                </div>

                {/* 进度指示器 (Progress indicator) */}
                <div className="welcome-progress">
                    {Array.from({ length: totalSteps }, (_, i) => (
                        <div
                            key={i}
                            className={`welcome-progress-dot ${i + 1 === currentStep ? 'active' : ''} ${i + 1 < currentStep ? 'completed' : ''}`}
                            onClick={() => setCurrentStep(i + 1)}
                        />
                    ))}
                </div>

                {/* 内容区域 (Content area) */}
                <div className={`welcome-guide-body ${isAnimating ? 'animating' : ''}`}>
                    {renderStepContent()}
                </div>

                {/* 底部导航 (Bottom navigation) */}
                <div className="welcome-guide-footer">
                    <button
                        className="welcome-nav-button"
                        onClick={handlePrevious}
                        disabled={currentStep === 1}
                    >
                        ← {t('welcomePrevious')}
                    </button>

                    <div className="welcome-step-indicator">
                        {currentStep} / {totalSteps}
                    </div>

                    {currentStep < totalSteps ? (
                        <button
                            className="welcome-nav-button welcome-nav-primary"
                            onClick={handleNext}
                        >
                            {t('welcomeNext')} →
                        </button>
                    ) : (
                        <button
                            className="welcome-nav-button welcome-nav-primary"
                            onClick={handleStart}
                        >
                            {t('welcomeStart')} 🚀
                        </button>
                    )}
                </div>
            </div>
        </div>
    );
}

// 导出到全局 (Export to global)
if (typeof window !== 'undefined') {
    window.WelcomeGuide = WelcomeGuide;
}

// 如果支持模块导出 (Module export support)
if (typeof module !== 'undefined' && module.exports) {
    module.exports = WelcomeGuide;
}

