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
                        const fileName = (doc.title || doc.name || '').toLowerCase();
                        const isPPT = fileName.endsWith('.pptx') || fileName.endsWith('.ppt');

                        let result;
                        if (isPPT) {
                            result = await window.api.analyzePPT(doc.path || doc.title, finalPrompt);
                        } else {
                            result = await window.api.analyzeDocument(doc.path || doc.title, finalPrompt);
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

            } catch (error) {
                console.error('分析失败:', error);
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
                        style: styles.hintButton,
                        className: 'ai-analysis-hint-button',
                        disabled: analyzing
                    }, '📋 ' + (t('summary') || '总结')),
                    React.createElement('button', {
                        onClick: () => setCustomPrompt('请分析这份文档的逻辑结构和论证方式。'),
                        style: styles.hintButton,
                        className: 'ai-analysis-hint-button',
                        disabled: analyzing
                    }, '🔍 ' + (t('analyze') || '分析')),
                    React.createElement('button', {
                        onClick: () => setCustomPrompt('请提取文档中的关键数据和重要结论。'),
                        style: styles.hintButton,
                        className: 'ai-analysis-hint-button',
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
            background: 'rgba(255, 255, 255, 0.95)',
            padding: '18px',
            borderRadius: '8px',
            marginBottom: '20px',
            border: '1px solid rgba(255, 255, 255, 0.3)',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)'
        },
        docList: {
            marginTop: '12px',
            fontSize: '14px',
            color: '#1565c0'
        },
        docItem: {
            padding: '5px 0',
            color: '#1976d2'
        },
        promptSection: {
            marginBottom: '20px'
        },
        label: {
            display: 'block',
            marginBottom: '8px',
            fontWeight: '500',
            color: '#ffffff',
            fontSize: '14px',
            textShadow: '0 1px 3px rgba(0,0,0,0.3)'
        },
        textarea: {
            width: '100%',
            padding: '12px',
            border: '2px solid rgba(255, 255, 255, 0.3)',
            borderRadius: '6px',
            fontSize: '14px',
            boxSizing: 'border-box',
            resize: 'vertical',
            fontFamily: 'inherit',
            transition: 'all 0.2s',
            backgroundColor: 'rgba(255, 255, 255, 0.95)',
            color: '#333'
        },
        promptHints: {
            marginTop: '12px',
            display: 'flex',
            gap: '10px',
            flexWrap: 'wrap'
        },
        hintButton: {
            padding: '8px 14px',
            fontSize: '13px',
            background: 'linear-gradient(135deg, #64B5F6 0%, #42A5F5 100%)',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            transition: 'all 0.2s',
            fontWeight: '500',
            color: '#ffffff',
            boxShadow: '0 2px 6px rgba(66, 165, 245, 0.4)',
            textShadow: '0 1px 2px rgba(0,0,0,0.2)'
        },
        analyzeButton: {
            width: '100%',
            padding: '14px',
            background: 'linear-gradient(135deg, #42a5f5 0%, #2196F3 100%)',
            color: 'white',
            border: 'none',
            borderRadius: '6px',
            cursor: 'pointer',
            fontSize: '15px',
            fontWeight: '600',
            marginBottom: '20px',
            transition: 'all 0.2s',
            boxShadow: '0 4px 12px rgba(33, 150, 243, 0.4)',
            textShadow: '0 1px 2px rgba(0,0,0,0.2)'
        },
        buttonDisabled: {
            background: 'rgba(176, 190, 197, 0.8)',
            cursor: 'not-allowed',
            boxShadow: 'none'
        },
        resultsSection: {
            marginTop: '24px'
        },
        progressBar: {
            width: '100%',
            height: '32px',
            backgroundColor: 'rgba(255, 255, 255, 0.3)',
            borderRadius: '6px',
            position: 'relative',
            marginBottom: '20px',
            overflow: 'hidden',
            border: '1px solid rgba(255, 255, 255, 0.2)'
        },
        progressFill: {
            height: '100%',
            background: 'linear-gradient(90deg, #42a5f5 0%, #2196F3 100%)',
            transition: 'width 0.3s ease',
            boxShadow: 'inset 0 2px 4px rgba(0,0,0,0.1)'
        },
        progressText: {
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            fontSize: '12px',
            fontWeight: '600',
            color: '#ffffff',
            textShadow: '0 1px 3px rgba(0,0,0,0.5)'
        },
        resultItem: {
            border: '1px solid rgba(255, 255, 255, 0.3)',
            borderRadius: '8px',
            marginBottom: '16px',
            overflow: 'hidden',
            boxShadow: '0 4px 12px rgba(0,0,0,0.15)',
            transition: 'box-shadow 0.2s',
            backgroundColor: 'rgba(255, 255, 255, 0.95)'
        },
        resultHeader: {
            padding: '14px 16px',
            background: 'linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%)',
            display: 'flex',
            alignItems: 'center',
            gap: '10px',
            fontWeight: '600',
            borderBottom: '1px solid rgba(144, 202, 249, 0.5)',
            color: '#1976d2'
        },
        resultIcon: {
            fontSize: '20px'
        },
        resultBody: {
            padding: '16px',
            lineHeight: '1.7',
            backgroundColor: '#ffffff',
            color: '#333'
        }
    };

    console.log('✅ EmbeddedAIAnalysisPanel component loaded');
})();

