/**
 * 嵌入式AI分析面板 - 简化版
 * (Embedded AI Analysis Panel - Simplified Version)
 * 用于分屏显示，不包含弹窗和文档列表
 * (For split-screen display, without modal and document list)
 *
 * 支持三种分析模式 (Supports three analysis modes):
 * 1. 单文档分析 - 逐个分析每个文档 (Single document analysis)
 * 2. 知识库分析 - 结合知识库进行分析 (Knowledge base analysis)
 * 3. 多文档联合分析 - 分析文档间的关联、逻辑、因果关系 (Multi-document joint analysis)
 */
(function() {
    'use strict';

    const { useState } = React;

    window.EmbeddedAIAnalysisPanel = function EmbeddedAIAnalysisPanel({
        selectedDocuments = [],
        onClose,
        onRemoveDocument  // 新增：移除文档回调 (New: remove document callback)
    }) {
        const { t } = window.LanguageModule.useTranslation();

        const [customPrompt, setCustomPrompt] = useState('');
        const [analyzing, setAnalyzing] = useState(false);
        const [currentAnalysis, setCurrentAnalysis] = useState(null);

        // 分析模式: 'direct' | 'knowledgeBase' | 'multiDoc'
        // (Analysis mode: 'direct' | 'knowledgeBase' | 'multiDoc')
        const [analysisMode, setAnalysisMode] = useState('direct');

        // 处理移除文档 (Handle document removal)
        const handleRemoveDocument = (doc) => {
            if (onRemoveDocument && !analyzing) {
                onRemoveDocument(doc);
            }
        };

        // 批量分析文档 (Batch analyze documents)
        const analyzeDocuments = async () => {
            if (!selectedDocuments || selectedDocuments.length === 0) {
                alert(t('pleaseSelectDocuments') || '请选择要分析的文档');
                return;
            }

            // 多文档联合分析需要至少2个文档
            // (Multi-document analysis requires at least 2 documents)
            if (analysisMode === 'multiDoc' && selectedDocuments.length < 2) {
                alert(t('multiDocNeedAtLeast2') || '多文档联合分析至少需要选择2个文档');
                return;
            }

            const finalPrompt = customPrompt || (t('defaultAnalysisPrompt') || '请总结这些文档的核心内容。');

            setAnalyzing(true);
            setCurrentAnalysis({
                documents: selectedDocuments,
                prompt: finalPrompt,
                status: 'running',
                progress: 0,
                results: [],
                analysisMode: analysisMode
            });

            try {
                let results = [];

                if (analysisMode === 'multiDoc') {
                    // 多文档联合分析模式 (Multi-document joint analysis mode)
                    setCurrentAnalysis(prev => ({
                        ...prev,
                        progress: 10,
                        currentDoc: t('multiDocAnalyzing') || '多文档联合分析中...'
                    }));

                    try {
                        const docNames = selectedDocuments.map(d => d.title || d.name);
                        const result = await window.api.analyzeMultiDocuments(docNames, finalPrompt);

                        results.push({
                            document: { title: t('multiDocResult') || '多文档联合分析结果', name: docNames.join(', ') },
                            success: true,
                            data: result,
                            isMultiDoc: true
                        });

                        setCurrentAnalysis(prev => ({
                            ...prev,
                            progress: 100
                        }));
                    } catch (error) {
                        results.push({
                            document: { title: t('multiDocResult') || '多文档联合分析结果' },
                            success: false,
                            error: error.message,
                            isMultiDoc: true
                        });
                    }
                } else {
                    // 单文档或知识库分析模式 (Single document or knowledge base analysis mode)
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

                            let result;

                            if (analysisMode === 'knowledgeBase') {
                                // 使用知识库分析
                                if (isPPT) {
                                    result = await window.api.analyzePPT(docFileName, finalPrompt);
                                } else {
                                    result = await window.api.analyzeDocument(docFileName, finalPrompt);
                                }
                            } else {
                                // 直接分析单个文档
                                if (isPPT) {
                                    result = await window.api.analyzePPTDirect(docFileName, finalPrompt);
                                } else {
                                    result = await window.api.analyzeDocumentDirect(docFileName, finalPrompt);
                                }
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
                            const analysisTypeKey = result.isMultiDoc ? 'multiDocAnalysis' : 'documentAnalysis';
                            await window.api.saveLLMResult({
                                title: `${docName} - ${t('aiAnalysis') || 'AI分析'}`,
                                sourceDocument: docName,
                                question: finalPrompt,
                                analysisType: t(analysisTypeKey) || (result.isMultiDoc ? '多文档联合分析' : '文档分析'),
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

        return React.createElement('div', { className: 'ai-analysis-container' },
            // 选中的文档信息（带快速取消按钮）(Selected document info with quick remove button)
            React.createElement('div', { className: 'ai-selected-info' },
                React.createElement('h3', null,
                    `📁 ${t('selectedDocumentsCount') ? t('selectedDocumentsCount').replace('{0}', selectedDocuments.length) : `已选择 ${selectedDocuments.length} 个文档`}`
                ),
                selectedDocuments.length > 0 && React.createElement('div', { className: 'ai-doc-list' },
                    selectedDocuments.map((doc, i) =>
                        React.createElement('div', { key: i, className: 'ai-doc-item-removable' },
                            React.createElement('span', { className: 'ai-doc-name' },
                                `📄 ${doc.title || doc.name}`
                            ),
                            // 快速取消选择按钮 (Quick remove button)
                            React.createElement('button', {
                                className: 'ai-doc-remove-btn',
                                onClick: () => handleRemoveDocument(doc),
                                disabled: analyzing,
                                title: t('removeDocument') || '移除此文档'
                            }, '×')
                        )
                    )
                ),
                selectedDocuments.length === 0 && React.createElement('div', { className: 'ai-no-docs-hint' },
                    t('noDocumentsSelected') || '请在左侧列表中勾选要分析的文档'
                )
            ),

            // 分析模式选择 (Analysis mode selection)
            React.createElement('div', { className: 'ai-mode-selection' },
                React.createElement('div', { className: 'ai-mode-title' },
                    t('analysisMode') || '分析模式'
                ),
                
                // 单文档分析选项 (Single document analysis option)
                React.createElement('label', {
                    className: 'ai-mode-option' + (analysisMode === 'direct' ? ' active direct-mode' : '')
                },
                    React.createElement('input', {
                        type: 'radio',
                        name: 'analysisMode',
                        checked: analysisMode === 'direct',
                        onChange: () => setAnalysisMode('direct'),
                        disabled: analyzing
                    }),
                    React.createElement('div', { className: 'ai-mode-option-content' },
                        React.createElement('div', { className: 'ai-mode-option-title direct' },
                            '📄 ' + (t('directAnalysisMode') || '单文档分析')
                        ),
                        React.createElement('div', { className: 'ai-mode-option-desc' },
                            t('directModeDesc') || '逐个分析每个文档，不使用知识库'
                        )
                    )
                ),
                
                // 知识库分析选项 (Knowledge base analysis option)
                React.createElement('label', {
                    className: 'ai-mode-option' + (analysisMode === 'knowledgeBase' ? ' active kb-mode' : '')
                },
                    React.createElement('input', {
                        type: 'radio',
                        name: 'analysisMode',
                        checked: analysisMode === 'knowledgeBase',
                        onChange: () => setAnalysisMode('knowledgeBase'),
                        disabled: analyzing
                    }),
                    React.createElement('div', { className: 'ai-mode-option-content' },
                        React.createElement('div', { className: 'ai-mode-option-title kb' },
                            '📚 ' + (t('knowledgeBaseMode') || '知识库分析')
                        ),
                        React.createElement('div', { className: 'ai-mode-option-desc' },
                            t('kbModeDesc') || '结合知识库中的相关内容进行分析'
                        )
                    )
                ),

                // 多文档联合分析选项 (Multi-document joint analysis option)
                React.createElement('label', {
                    className: 'ai-mode-option' + (analysisMode === 'multiDoc' ? ' active multi-doc-mode' : '')
                },
                    React.createElement('input', {
                        type: 'radio',
                        name: 'analysisMode',
                        checked: analysisMode === 'multiDoc',
                        onChange: () => setAnalysisMode('multiDoc'),
                        disabled: analyzing
                    }),
                    React.createElement('div', { className: 'ai-mode-option-content' },
                        React.createElement('div', { className: 'ai-mode-option-title multi-doc' },
                            '🔗 ' + (t('multiDocMode') || '多文档联合分析')
                        ),
                        React.createElement('div', { className: 'ai-mode-option-desc' },
                            t('multiDocModeDesc') || '分析文档间的关联、逻辑和因果关系'
                        ),
                        selectedDocuments.length < 2 && analysisMode === 'multiDoc' &&
                            React.createElement('div', { className: 'ai-mode-warning' },
                                '⚠️ ' + (t('multiDocNeedAtLeast2') || '至少需要选择2个文档')
                            )
                    )
                )
            ),

            // 多文档分析提示词快捷按钮（仅在多文档模式显示）
            // (Multi-doc prompt shortcuts - only show in multi-doc mode)
            analysisMode === 'multiDoc' && React.createElement('div', { className: 'ai-multi-doc-prompts' },
                React.createElement('div', { className: 'ai-multi-doc-prompts-title' },
                    '🔗 ' + (t('multiDocPromptShortcuts') || '联合分析快捷提示')
                ),
                React.createElement('div', { className: 'ai-multi-doc-prompts-grid' },
                    React.createElement('button', {
                        onClick: () => setCustomPrompt(t('multiDocRelationPrompt') || '请分析这些文档之间的关联关系，找出它们的共同点和差异点。'),
                        className: 'ai-multi-doc-prompt-btn',
                        disabled: analyzing
                    }, '🔍 ' + (t('relationAnalysis') || '关联分析')),
                    React.createElement('button', {
                        onClick: () => setCustomPrompt(t('multiDocCausalPrompt') || '请分析这些文档之间的因果关系和逻辑链条。'),
                        className: 'ai-multi-doc-prompt-btn',
                        disabled: analyzing
                    }, '⛓️ ' + (t('causalAnalysis') || '因果分析')),
                    React.createElement('button', {
                        onClick: () => setCustomPrompt(t('multiDocComparePrompt') || '请对比分析这些文档，生成对比表格，总结各自的优缺点。'),
                        className: 'ai-multi-doc-prompt-btn',
                        disabled: analyzing
                    }, '📊 ' + (t('compareAnalysis') || '对比分析')),
                    React.createElement('button', {
                        onClick: () => setCustomPrompt(t('multiDocSynthesisPrompt') || '请综合这些文档的内容，生成一份整合报告，包含思维导图结构。'),
                        className: 'ai-multi-doc-prompt-btn',
                        disabled: analyzing
                    }, '🗺️ ' + (t('synthesisReport') || '综合报告'))
                )
            ),

            // 提示词输入 (Prompt input)
            React.createElement('div', { className: 'ai-prompt-section' },
                React.createElement('label', { className: 'ai-prompt-label' },
                    t('customPrompt') || '自定义提示词'
                ),
                React.createElement('textarea', {
                    value: customPrompt,
                    onChange: (e) => setCustomPrompt(e.target.value),
                    placeholder: analysisMode === 'multiDoc'
                        ? (t('multiDocPromptPlaceholder') || '输入多文档联合分析的问题，如：分析文档间的关联、对比差异等...')
                        : (t('promptPlaceholder') || '输入你的问题或分析要求...'),
                    className: 'ai-analysis-textarea',
                    rows: 3,
                    disabled: analyzing
                }),
                // 单文档模式的快捷提示词 (Single doc mode prompt shortcuts)
                analysisMode !== 'multiDoc' && React.createElement('div', { className: 'ai-prompt-hints' },
                    React.createElement('button', {
                        onClick: () => setCustomPrompt(t('summaryPrompt') || '请详细总结这份文档的核心内容和关键观点。'),
                        className: 'ai-analysis-hint-button ai-hint-summary',
                        disabled: analyzing
                    }, '📋 ' + (t('summary') || '总结')),
                    React.createElement('button', {
                        onClick: () => setCustomPrompt(t('analyzePrompt') || '请分析这份文档的逻辑结构和论证方式。'),
                        className: 'ai-analysis-hint-button ai-hint-analyze',
                        disabled: analyzing
                    }, '🔍 ' + (t('analyze') || '分析')),
                    React.createElement('button', {
                        onClick: () => setCustomPrompt(t('extractPrompt') || '请提取文档中的关键数据和重要结论。'),
                        className: 'ai-analysis-hint-button ai-hint-extract',
                        disabled: analyzing
                    }, '💡 ' + (t('extract') || '提取'))
                )
            ),

            // 开始分析按钮 (Start analysis button)
            React.createElement('button', {
                onClick: analyzeDocuments,
                disabled: analyzing || selectedDocuments.length === 0 || (analysisMode === 'multiDoc' && selectedDocuments.length < 2),
                className: 'ai-analysis-button' + (analysisMode === 'multiDoc' ? ' multi-doc' : '')
            }, analyzing
                ? `🔄 ${t('analyzeInProgress') || '分析中...'}`
                : analysisMode === 'multiDoc'
                    ? `🔗 ${t('startMultiDocAnalyze') || '开始联合分析'} (${selectedDocuments.length})`
                    : `🚀 ${t('startAnalyze') || '开始分析'} (${selectedDocuments.length})`
            ),

            // 分析结果 (Analysis results)
            currentAnalysis && React.createElement('div', { className: 'ai-results-section' },
                currentAnalysis.status === 'running' && React.createElement('div', { className: 'ai-progress-bar' },
                    React.createElement('div', {
                        className: 'ai-progress-fill' + (currentAnalysis.analysisMode === 'multiDoc' ? ' multi-doc' : ''),
                        style: { width: `${currentAnalysis.progress}%` }
                    }),
                    React.createElement('span', { className: 'ai-progress-text' },
                        `${currentAnalysis.progress}% - ${currentAnalysis.currentDoc || ''}`
                    )
                ),

                currentAnalysis.status === 'completed' && currentAnalysis.results &&
                    currentAnalysis.results.map((result, index) =>
                        React.createElement('div', {
                            key: index,
                            className: 'ai-result-item' + (result.isMultiDoc ? ' multi-doc-result' : '')
                        },
                            React.createElement('div', { className: 'ai-result-header' },
                                React.createElement('span', { className: 'ai-result-icon' },
                                    result.success ? (result.isMultiDoc ? '🔗' : '✅') : '❌'
                                ),
                                React.createElement('span', null, result.document.title || result.document.name)
                            ),
                            React.createElement('div', { className: 'ai-result-body' },
                                result.success ?
                                    React.createElement('div', null,
                                        result.data.comprehensiveSummary ?
                                            renderMarkdown(result.data.comprehensiveSummary) :
                                        result.data.finalReport ?
                                            renderMarkdown(result.data.finalReport) :
                                        result.data.multiDocAnalysis ?
                                            renderMarkdown(result.data.multiDocAnalysis) :
                                            JSON.stringify(result.data, null, 2)
                                    ) :
                                    React.createElement('div', { className: 'ai-result-error' },
                                        `${t('analysisFailed') || '分析失败'}: ${result.error}`
                                    )
                            )
                        )
                    )
            )
        );
    };

    // 获取翻译函数并输出加载日志
    const getT = () => window.LanguageModule ? window.LanguageModule.useTranslation().t : (k) => k;
    console.log(getT()('embeddedAILogComponentLoaded'));
})();
