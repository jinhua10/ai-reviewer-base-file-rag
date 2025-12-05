/**
 * 嵌入式AI分析面板 - 简化版
 * 用于分屏显示，不包含弹窗和文档列表
 */
(function() {
    'use strict';

    const { useState } = React;

    window.EmbeddedAIAnalysisPanel = function EmbeddedAIAnalysisPanel({
        selectedDocuments = [],
        onClose
    }) {
        const { t } = window.LanguageModule.useTranslation();

        const [customPrompt, setCustomPrompt] = useState('');
        const [analyzing, setAnalyzing] = useState(false);
        const [currentAnalysis, setCurrentAnalysis] = useState(null);

        // 批量分析文档
        const analyzeDocuments = async () => {
            if (!selectedDocuments || selectedDocuments.length === 0) {
                alert(t('pleaseSelectDocuments') || '请选择要分析的文档');
                return;
            }

            const finalPrompt = customPrompt || '请总结这些文档的核心内容。';

            setAnalyzing(true);
            setCurrentAnalysis({
                documents: selectedDocuments,
                prompt: finalPrompt,
                status: 'running',
                progress: 0,
                results: []
            });

            try {
                const results = [];

                for (let i = 0; i < selectedDocuments.length; i++) {
                    const doc = selectedDocuments[i];

                    setCurrentAnalysis(prev => ({
                        ...prev,
                        progress: Math.round(((i + 1) / selectedDocuments.length) * 100),
                        currentDoc: doc.title || doc.name
                    }));

                    try {
                        const docFileName = doc.title || doc.name || '';
                        const isPPT = docFileName.toLowerCase().endsWith('.pptx') || docFileName.toLowerCase().endsWith('.ppt');

                        // 直接传递文件名，后端会根据配置的 document.source-path 自动解析完整路径
                        let result;
                        if (isPPT) {
                            result = await window.api.analyzePPT(docFileName, finalPrompt);
                        } else {
                            result = await window.api.analyzeDocument(docFileName, finalPrompt);
                        }

                        results.push({
                            document: doc,
                            success: true,
                            data: result
                        });
                    } catch (error) {
                        results.push({
                            document: doc,
                            success: false,
                            error: error.message
                        });
                    }
                }

                setCurrentAnalysis(prev => ({
                    ...prev,
                    status: 'completed',
                    results: results
                }));

                // 保存成功的分析结果到历史记录
                // (Save successful analysis results to history)
                for (const result of results) {
                    if (result.success && result.data) {
                        try {
                            const docName = result.document.title || result.document.name;
                            await window.api.saveLLMResult({
                                title: `${docName} - ${t('aiAnalysis') || 'AI分析'}`,
                                sourceDocument: docName,
                                question: finalPrompt,
                                analysisType: t('documentAnalysis') || '文档分析',
                                content: result.data.answer || result.data.summary || result.data.comprehensiveSummary || result.data.finalReport || JSON.stringify(result.data),
                                keyPoints: result.data.keyPoints || []
                            });
                            console.log(t('logAnalysisResultSaved') || '✅ 分析结果已保存到历史记录');
                        } catch (saveError) {
                            console.warn(t('logAnalysisResultSaveFailed') || '⚠️ 保存分析结果失败:', saveError);
                        }
                    }
                }

            } catch (error) {
                console.error(t('embeddedAILogAnalysisError'), error);
                setCurrentAnalysis(prev => ({
                    ...prev,
                    status: 'error',
                    error: error.message
                }));
            } finally {
                setAnalyzing(false);
            }
        };

        const renderMarkdown = (text) => {
            if (!text) return null;
            if (window.marked) {
                return React.createElement('div', {
                    dangerouslySetInnerHTML: { __html: window.marked.parse(text) }
                });
            }
            return React.createElement('div', {
                style: { whiteSpace: 'pre-wrap' }
            }, text);
        };

        return React.createElement('div', { style: styles.container },
            // 选中的文档信息
            React.createElement('div', {
                style: styles.selectedInfo,
                className: 'ai-selected-info'
            },
                React.createElement('h3', { style: { margin: 0, fontSize: '16px', color: '#1976d2' } },
                    `📁 ${t('selectedDocumentsCount') ? t('selectedDocumentsCount').replace('{0}', selectedDocuments.length) : `已选择 ${selectedDocuments.length} 个文档`}`
                ),
                selectedDocuments.length > 0 && React.createElement('div', { style: styles.docList },
                    selectedDocuments.map((doc, i) =>
                        React.createElement('div', { key: i, style: styles.docItem },
                            `• ${doc.title || doc.name}`
                        )
                    )
                )
            ),

            // 提示词输入
            React.createElement('div', { style: styles.promptSection },
                React.createElement('label', { style: styles.label },
                    t('customPrompt') || '自定义提示词'
                ),
                React.createElement('textarea', {
                    value: customPrompt,
                    onChange: (e) => setCustomPrompt(e.target.value),
                    placeholder: t('promptPlaceholder') || '输入你的问题或分析要求...',
                    style: styles.textarea,
                    className: 'ai-analysis-textarea',
                    rows: 3,
                    disabled: analyzing
                }),
                React.createElement('div', { style: styles.promptHints },
                    React.createElement('button', {
                        onClick: () => setCustomPrompt('请详细总结这份文档的核心内容和关键观点。'),
                        style: styles.hintButtonSummary,
                        className: 'ai-analysis-hint-button ai-hint-summary',
                        disabled: analyzing
                    }, '📋 ' + (t('summary') || '总结')),
                    React.createElement('button', {
                        onClick: () => setCustomPrompt('请分析这份文档的逻辑结构和论证方式。'),
                        style: styles.hintButtonAnalyze,
                        className: 'ai-analysis-hint-button ai-hint-analyze',
                        disabled: analyzing
                    }, '🔍 ' + (t('analyze') || '分析')),
                    React.createElement('button', {
                        onClick: () => setCustomPrompt('请提取文档中的关键数据和重要结论。'),
                        style: styles.hintButtonExtract,
                        className: 'ai-analysis-hint-button ai-hint-extract',
                        disabled: analyzing
                    }, '💡 ' + (t('extract') || '提取'))
                )
            ),

            // 开始分析按钮
            React.createElement('button', {
                onClick: analyzeDocuments,
                disabled: analyzing || selectedDocuments.length === 0,
                className: 'ai-analysis-button',
                style: {
                    ...styles.analyzeButton,
                    ...(analyzing || selectedDocuments.length === 0 ? styles.buttonDisabled : {})
                }
            }, analyzing ? `🔄 ${t('analyzeInProgress') || '分析中...'}` : `🚀 ${t('startAnalyze') || '开始分析'} (${selectedDocuments.length})`),

            // 分析结果
            currentAnalysis && React.createElement('div', { style: styles.resultsSection },
                currentAnalysis.status === 'running' && React.createElement('div', { style: styles.progressBar },
                    React.createElement('div', {
                        style: { ...styles.progressFill, width: `${currentAnalysis.progress}%` },
                        className: 'ai-progress-fill'
                    }),
                    React.createElement('span', { style: styles.progressText },
                        `${currentAnalysis.progress}% - ${currentAnalysis.currentDoc || ''}`
                    )
                ),

                currentAnalysis.status === 'completed' && currentAnalysis.results &&
                    currentAnalysis.results.map((result, index) =>
                        React.createElement('div', {
                            key: index,
                            style: styles.resultItem,
                            className: 'ai-result-item'
                        },
                            React.createElement('div', { style: styles.resultHeader },
                                React.createElement('span', { style: styles.resultIcon },
                                    result.success ? '✅' : '❌'
                                ),
                                React.createElement('span', null, result.document.title || result.document.name)
                            ),
                            React.createElement('div', {
                                style: styles.resultBody,
                                className: 'ai-result-body'
                            },
                                result.success ?
                                    React.createElement('div', null,
                                        result.data.comprehensiveSummary ?
                                            renderMarkdown(result.data.comprehensiveSummary) :
                                        result.data.finalReport ?
                                            renderMarkdown(result.data.finalReport) :
                                            JSON.stringify(result.data, null, 2)
                                    ) :
                                    React.createElement('div', { style: { color: '#f44336' } },
                                        `${t('analysisFailed') || '分析失败'}: ${result.error}`
                                    )
                            )
                        )
                    )
            )
        );
    };

    const styles = {
        container: {
            padding: '0'
        },
        selectedInfo: {
            background: 'linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(250, 250, 255, 0.95) 100%)',
            padding: '20px',
            borderRadius: '12px',
            marginBottom: '20px',
            border: '2px solid rgba(100, 181, 246, 0.3)',
            boxShadow: '0 6px 20px rgba(66, 165, 245, 0.15)',
            transition: 'all 0.3s ease'
        },
        docList: {
            marginTop: '14px',
            fontSize: '14px',
            color: '#1976d2',
            maxHeight: '200px',
            overflowY: 'auto',
            paddingRight: '8px'
        },
        docItem: {
            padding: '6px 0',
            color: '#1565c0',
            transition: 'all 0.2s',
            borderLeft: '3px solid transparent',
            paddingLeft: '8px'
        },
        promptSection: {
            marginBottom: '20px'
        },
        label: {
            display: 'block',
            marginBottom: '10px',
            fontWeight: '600',
            color: '#ffffff',
            fontSize: '15px',
            textShadow: '0 2px 4px rgba(0,0,0,0.3)',
            letterSpacing: '0.3px'
        },
        textarea: {
            width: '100%',
            padding: '14px',
            border: '2px solid rgba(255, 255, 255, 0.4)',
            borderRadius: '8px',
            fontSize: '14px',
            boxSizing: 'border-box',
            resize: 'vertical',
            fontFamily: 'inherit',
            transition: 'all 0.3s ease',
            backgroundColor: 'rgba(255, 255, 255, 0.98)',
            color: '#333',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            lineHeight: '1.6'
        },
        promptHints: {
            marginTop: '14px',
            display: 'flex',
            gap: '12px',
            flexWrap: 'wrap'
        },
        // 总结按钮 - 蓝色
        hintButtonSummary: {
            padding: '10px 18px',
            fontSize: '13px',
            background: 'linear-gradient(135deg, #42A5F5 0%, #1E88E5 100%)',
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer',
            transition: 'all 0.3s ease',
            fontWeight: '600',
            color: '#ffffff',
            boxShadow: '0 4px 12px rgba(66, 165, 245, 0.5)',
            textShadow: '0 1px 2px rgba(0,0,0,0.2)',
            position: 'relative',
            overflow: 'hidden'
        },
        // 分析按钮 - 橙色
        hintButtonAnalyze: {
            padding: '10px 18px',
            fontSize: '13px',
            background: 'linear-gradient(135deg, #FF9800 0%, #F57C00 100%)',
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer',
            transition: 'all 0.3s ease',
            fontWeight: '600',
            color: '#ffffff',
            boxShadow: '0 4px 12px rgba(255, 152, 0, 0.5)',
            textShadow: '0 1px 2px rgba(0,0,0,0.2)',
            position: 'relative',
            overflow: 'hidden'
        },
        // 提取按钮 - 绿色
        hintButtonExtract: {
            padding: '10px 18px',
            fontSize: '13px',
            background: 'linear-gradient(135deg, #66BB6A 0%, #43A047 100%)',
            border: 'none',
            borderRadius: '8px',
            cursor: 'pointer',
            transition: 'all 0.3s ease',
            fontWeight: '600',
            color: '#ffffff',
            boxShadow: '0 4px 12px rgba(102, 187, 106, 0.5)',
            textShadow: '0 1px 2px rgba(0,0,0,0.2)',
            position: 'relative',
            overflow: 'hidden'
        },
        analyzeButton: {
            width: '100%',
            padding: '16px',
            background: 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
            color: 'white',
            border: 'none',
            borderRadius: '10px',
            cursor: 'pointer',
            fontSize: '16px',
            fontWeight: '700',
            marginBottom: '20px',
            transition: 'all 0.3s ease',
            boxShadow: '0 6px 20px rgba(102, 126, 234, 0.4)',
            textShadow: '0 2px 4px rgba(0,0,0,0.2)',
            letterSpacing: '0.5px',
            position: 'relative',
            overflow: 'hidden'
        },
        buttonDisabled: {
            background: 'linear-gradient(135deg, rgba(189, 189, 189, 0.5) 0%, rgba(158, 158, 158, 0.5) 100%)',
            cursor: 'not-allowed',
            boxShadow: '0 2px 8px rgba(0,0,0,0.1)',
            color: 'rgba(255, 255, 255, 0.7)',
            border: '2px solid rgba(255, 255, 255, 0.2)'
        },
        resultsSection: {
            marginTop: '24px'
        },
        progressBar: {
            width: '100%',
            height: '36px',
            backgroundColor: 'rgba(255, 255, 255, 0.25)',
            borderRadius: '8px',
            position: 'relative',
            marginBottom: '20px',
            overflow: 'hidden',
            border: '2px solid rgba(255, 255, 255, 0.3)',
            boxShadow: 'inset 0 2px 8px rgba(0,0,0,0.1)'
        },
        progressFill: {
            height: '100%',
            background: 'linear-gradient(90deg, #00E676 0%, #00C853 50%, #00BFA5 100%)',
            transition: 'width 0.3s ease',
            boxShadow: '0 0 20px rgba(0, 230, 118, 0.5)',
            position: 'relative'
        },
        progressText: {
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            fontSize: '13px',
            fontWeight: '700',
            color: '#ffffff',
            textShadow: '0 2px 4px rgba(0,0,0,0.5)',
            letterSpacing: '0.5px'
        },
        resultItem: {
            border: '2px solid rgba(100, 181, 246, 0.3)',
            borderRadius: '12px',
            marginBottom: '18px',
            overflow: 'hidden',
            boxShadow: '0 6px 20px rgba(0,0,0,0.12)',
            transition: 'all 0.3s ease',
            backgroundColor: 'rgba(255, 255, 255, 0.98)'
        },
        resultHeader: {
            padding: '16px 20px',
            background: 'linear-gradient(135deg, #E8EAF6 0%, #C5CAE9 100%)',
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            fontWeight: '700',
            borderBottom: '2px solid rgba(63, 81, 181, 0.2)',
            color: '#3F51B5',
            fontSize: '15px'
        },
        resultIcon: {
            fontSize: '22px'
        },
        resultBody: {
            padding: '20px',
            lineHeight: '1.8',
            backgroundColor: '#ffffff',
            color: '#333',
            fontSize: '14px'
        }
    };

    // 获取翻译函数并输出加载日志
    const getT = () => window.LanguageModule ? window.LanguageModule.useTranslation().t : (k) => k;
    console.log(getT()('embeddedAILogComponentLoaded'));
})();

