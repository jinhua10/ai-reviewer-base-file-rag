/**
 * Main Application Component / 主应用组件
 * 管理整体应用状态、布局和路由
 * 
 * @author AI Reviewer Team
 * @since 2025-12-06
 */

function App() {
    const { t, toggleLanguage, language } = window.LanguageModule.useTranslation();
    const { useState, useEffect } = React;

    // 状态管理 (State management)
    const [activeTab, setActiveTab] = useState('qa');
    const [health, setHealth] = useState(null);

    // 引导页面状态 (Welcome guide state)
    const [showWelcomeGuide, setShowWelcomeGuide] = useState(() => {
        // 检查是否是首次访问 (Check if it's the first visit)
        const completed = localStorage.getItem('welcomeGuideCompleted');
        return completed !== 'true';
    });

    // AI分析面板状态
    const [showAIAnalysis, setShowAIAnalysis] = useState(false);
    const [selectedDocs, setSelectedDocs] = useState(new Set());
    const [selectedDocsData, setSelectedDocsData] = useState([]); // 存储完整的文档对象
    const [splitPosition, setSplitPosition] = useState(() => {
        const saved = localStorage.getItem('aiAnalysisSplitPosition');
        return saved ? parseFloat(saved) : 50;
    });
    const [isDragging, setIsDragging] = useState(false);

    // 添加文档到AI分析（带去重）- 暴露给全局供其他组件调用
    const addDocumentsToAIAnalysis = React.useCallback((docs) => {
        const docsArray = Array.isArray(docs) ? docs : [docs];
        let addedCount = 0;
        let duplicateCount = 0;

        docsArray.forEach(doc => {
            const docName = doc.name || doc.title || doc.fileName;
            
            // 检查是否已存在（根据文档名称去重）
            const exists = selectedDocsData.some(d => 
                (d.name || d.title || d.fileName) === docName
            );

            if (!exists) {
                const docId = doc.id || docName;
                setSelectedDocs(prev => new Set([...prev, docId]));
                setSelectedDocsData(prev => [...prev, doc]);
                addedCount++;
            } else {
                duplicateCount++;
            }
        });

        // 显示提示
        if (addedCount > 0) {
            console.log(`✅ ${t('documentAdded')}: ${addedCount} 个文档`);
        }
        if (duplicateCount > 0) {
            console.log(`ℹ️ ${t('documentAlreadyAdded')}: ${duplicateCount} 个文档`);
        }

        // 如果添加了文档且面板未打开，则打开面板
        if (addedCount > 0 && !showAIAnalysis) {
            setShowAIAnalysis(true);
        }

        return addedCount;
    }, [selectedDocsData, showAIAnalysis, t]);

    // 移除文档从 AI 分析列表
    const removeDocumentsFromAIAnalysis = React.useCallback((docs) => {
        const docsArray = Array.isArray(docs) ? docs : [docs];
        
        // 收集要移除的文档名称
        const docNamesToRemove = new Set(
            docsArray.map(doc => doc.name || doc.title || doc.fileName)
        );

        // 过滤出不在移除列表中的文档
        const remainingDocs = selectedDocsData.filter(d => {
            const docName = d.name || d.title || d.fileName;
            return !docNamesToRemove.has(docName);
        });

        // 计算被移除的数量
        const removedCount = selectedDocsData.length - remainingDocs.length;

        if (removedCount > 0) {
            // 更新状态
            setSelectedDocsData(remainingDocs);
            
            // 更新 selectedDocs Set
            const remainingDocIds = new Set(
                remainingDocs.map(d => d.id || d.title || d.name)
            );
            setSelectedDocs(remainingDocIds);
            
            console.log(t('logDocumentsRemoved').replace('{0}', removedCount));
        }

        return removedCount;
    }, [selectedDocsData]);

    // 检查文档是否已添加
    const isDocumentInAIAnalysis = React.useCallback((doc) => {
        const docName = doc.name || doc.title || doc.fileName;
        return selectedDocsData.some(d => 
            (d.name || d.title || d.fileName) === docName
        );
    }, [selectedDocsData]);

    // 将函数暴露到全局供其他组件使用
    useEffect(() => {
        window.addDocumentsToAIAnalysis = addDocumentsToAIAnalysis;
        window.removeDocumentsFromAIAnalysis = removeDocumentsFromAIAnalysis;
        window.isDocumentInAIAnalysis = isDocumentInAIAnalysis;
    }, [addDocumentsToAIAnalysis, removeDocumentsFromAIAnalysis, isDocumentInAIAnalysis]);

    // 健康检查 (Health check)
    useEffect(() => {
        console.log(t('logAppMounted'));
        checkHealth();

        // 每30秒检查一次服务状态 (Check service status every 30 seconds)
        const intervalId = setInterval(() => {
            console.log(t('logAutoCheckingHealth'));
            checkHealth();
        }, 30000);

        return () => {
            console.log(t('logCleaningInterval'));
            clearInterval(intervalId);
        };
    }, []);

    // 监听引导页面完成事件 (Listen for welcome guide completion)
    useEffect(() => {
        const handleGuideCompleted = () => {
            setShowWelcomeGuide(false);
        };

        window.addEventListener('welcomeGuideCompleted', handleGuideCompleted);

        return () => {
            window.removeEventListener('welcomeGuideCompleted', handleGuideCompleted);
        };
    }, []);

    // 处理分隔线拖拽
    useEffect(() => {
        if (!isDragging) return;

        const handleMouseMove = (e) => {
            const newPosition = (e.clientX / window.innerWidth) * 100;
            if (newPosition > 20 && newPosition < 80) {
                setSplitPosition(newPosition);
                localStorage.setItem('aiAnalysisSplitPosition', newPosition.toString());
            }
        };

        const handleMouseUp = () => {
            setIsDragging(false);
        };

        document.addEventListener('mousemove', handleMouseMove);
        document.addEventListener('mouseup', handleMouseUp);

        return () => {
            document.removeEventListener('mousemove', handleMouseMove);
            document.removeEventListener('mouseup', handleMouseUp);
        };
    }, [isDragging]);

    const checkHealth = async () => {
        try {
            console.log(t('logCallingHealthAPI'));
            const result = await window.api.health();
            console.log(t('logHealthCheckResult'), result);
            setHealth(result);
        } catch (err) {
            console.error(t('logHealthCheckError'), err);
            setHealth({ status: t('statusOffline'), message: 'Service connection failed' });
        }
    };

    // 判断状态并添加图标
    const getStatusWithIcon = (status) => {
        if (!status) return '';
        if (/[\u{1F300}-\u{1F9FF}]|[\u{2600}-\u{26FF}]|[\u{2700}-\u{27BF}]/u.test(status)) {
            return status;
        }
        const isOnline = status === 'UP' || status === '运行中' || status.toLowerCase().includes('online') || status.includes('运行');
        return isOnline ? `✅ ${status}` : `❌ ${status}`;
    };

    return (
        <div className="app-layout-container">
            {/* 左侧主内容区域 */}
            <div 
                className={`main-content-area ${showAIAnalysis ? 'with-ai-panel' : 'without-ai-panel'}`}
                style={{ width: showAIAnalysis ? `${splitPosition}%` : '100%' }}
            >
                <div className={`main-content-wrapper ${showAIAnalysis ? 'with-ai-panel' : 'without-ai-panel'}`}>
                    <div className="app-container">
                        <div className="language-toggle">
                            <button onClick={toggleLanguage}>{t('langToggle')}</button>
                        </div>

                        <header className="header">
                            <h1>{t('title')}</h1>
                            <p className="subtitle">{t('subtitle')}</p>
                            {health && (
                                <div className="health-status">
                                    {t('status')}: {getStatusWithIcon(health.status)}
                                </div>
                            )}
                        </header>

                        <main className="main-content">
                            <div className="tabs">
                                <button
                                    className={`tab ${activeTab === 'qa' ? 'active' : ''}`}
                                    onClick={() => setActiveTab('qa')}
                                >
                                    💬 {t('tabQA')}
                                </button>
                                <button
                                    className={`tab ${activeTab === 'documents' ? 'active' : ''}`}
                                    onClick={() => setActiveTab('documents')}
                                >
                                    🔍📁 {t('tabDocumentsSearch')}
                                </button>
                                <button
                                    className={`tab ${activeTab === 'llm-results' ? 'active' : ''}`}
                                    onClick={() => setActiveTab('llm-results')}
                                >
                                    📚 {t('tabLLMResults') || 'AI分析历史'}
                                </button>
                                <button
                                    className={`tab ${activeTab === 'stats' ? 'active' : ''}`}
                                    onClick={() => setActiveTab('stats')}
                                >
                                    📊 {t('tabStats')}
                                </button>
                                <button
                                    className={`tab ${activeTab === 'hope' ? 'active' : ''}`}
                                    onClick={() => setActiveTab('hope')}
                                    title={t('hopeTabTooltip') || 'HOPE三层记忆架构监控'}
                                >
                                    🧠 {t('tabHope') || 'HOPE监控'}
                                </button>
                            </div>

                            <div className="tab-content">
                                <div className={`tab-content-panel ${activeTab === 'qa' ? '' : 'hidden'}`}>
                                    <QATab />
                                </div>
                                <div className={`tab-content-panel ${activeTab === 'documents' ? '' : 'hidden'}`}>
                                    <DocumentsTab
                                        showAIAnalysis={showAIAnalysis}
                                        setShowAIAnalysis={setShowAIAnalysis}
                                        selectedDocs={selectedDocs}
                                        setSelectedDocs={setSelectedDocs}
                                        selectedDocsData={selectedDocsData}
                                        setSelectedDocsData={setSelectedDocsData}
                                    />
                                </div>
                                <div className={`tab-content-panel ${activeTab === 'llm-results' ? '' : 'hidden'}`}>
                                    <LLMResultsTab />
                                </div>
                                <div className={`tab-content-panel ${activeTab === 'stats' ? '' : 'hidden'}`}>
                                    <StatisticsTab />
                                </div>
                                <div className={`tab-content-panel ${activeTab === 'hope' ? '' : 'hidden'}`}>
                                    {window.HOPEDashboardPanel && React.createElement(window.HOPEDashboardPanel, {
                                        collapsed: false
                                    })}
                                </div>
                            </div>
                        </main>

                        <footer className="footer">
                            <p>{t('footerText')}</p>
                        </footer>
                    </div>
                </div>
            </div>

            {/* 引导页面 (Welcome Guide) */}
            {showWelcomeGuide && window.WelcomeGuide && (
                React.createElement(window.WelcomeGuide)
            )}

            {/* 拖拽分隔线 */}
            {showAIAnalysis && (
                <div
                    className={`drag-divider ${isDragging ? 'dragging' : ''}`}
                    style={{ left: `${splitPosition}%` }}
                    onMouseDown={() => setIsDragging(true)}
                >
                    <div className="drag-divider-handle">
                        ⋮
                    </div>
                </div>
            )}

            {/* AI分析右侧面板 */}
            {showAIAnalysis && (
                <div 
                    className="ai-analysis-panel"
                    style={{ left: `calc(${splitPosition}% + 6px)` }}
                >
                    <div className="ai-analysis-header">
                        <h2 className="ai-analysis-title">
                            🤖 {t('aiAnalysis') || 'AI分析'}
                        </h2>
                        <p className="ai-analysis-subtitle">
                            {selectedDocs.size > 0
                                ? t('selectedDocumentsCount').replace('{0}', selectedDocs.size)
                                : t('pleaseCheckDocumentsOnLeft')}
                        </p>
                    </div>

                    {window.EmbeddedAIAnalysisPanel && React.createElement(window.EmbeddedAIAnalysisPanel, {
                        selectedDocuments: selectedDocsData,
                        onClose: () => setShowAIAnalysis(false),
                        onRemoveDocument: (doc) => {
                            const docId = doc.id || doc.title || doc.name;
                            const newSelectedDocs = new Set(selectedDocs);
                            newSelectedDocs.delete(docId);
                            setSelectedDocs(newSelectedDocs);
                            setSelectedDocsData(prev =>
                                prev.filter(d => (d.id || d.title || d.name) !== docId)
                            );
                        }
                    })}
                </div>
            )}

            {/* 右侧悬浮快速切换按钮 */}
            {window.AIFloatingButton && React.createElement(window.AIFloatingButton, {
                showAIAnalysis: showAIAnalysis,
                setShowAIAnalysis: setShowAIAnalysis,
                selectedDocsCount: selectedDocs.size,
                splitPosition: splitPosition
            })}
        </div>
    );
}

// 导出到全局
window.App = App;
