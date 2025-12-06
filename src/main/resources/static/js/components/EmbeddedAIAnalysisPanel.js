/**
 * 嵌入式AI分析面板 - 用户友好版
 * (Embedded AI Analysis Panel - User Friendly Version)
 *
 * 特性 (Features):
 * 1. 用户友好的分析目标选择（非专业术语）
 * 2. 智能需求检测（自动推荐最佳策略）
 * 3. 多文档联合分析
 * 4. 实时进度反馈
 */
(function() {
    'use strict';

    const { useState, useEffect, useMemo } = React;

    // 分析目标配置 (Analysis goal configurations)
    // 映射用户友好的选项到内部策略
    const ANALYSIS_GOALS = {
        quick: {
            icon: '🚀',
            strategies: ['parallel-summary', 'compress'],
            estimatedTime: '1-2分钟',
            tokenCost: 'low'
        },
        precise: {
            icon: '🔍',
            strategies: ['question-driven', 'hyde'],
            estimatedTime: '30秒',
            tokenCost: 'lowest'
        },
        relation: {
            icon: '🔗',
            strategies: ['entity-relation', 'mind-map'],
            estimatedTime: '3-5分钟',
            tokenCost: 'medium',
            minDocs: 2
        },
        causal: {
            icon: '⛓️',
            strategies: ['sequential-summary', 'entity-relation'],
            estimatedTime: '3-5分钟',
            tokenCost: 'medium',
            minDocs: 2
        },
        comprehensive: {
            icon: '📊',
            strategies: ['hierarchical', 'iterative-refine', 'self-consistency'],
            estimatedTime: '10-15分钟',
            tokenCost: 'high'
        },
        compare: {
            icon: '⚖️',
            strategies: ['parallel-summary', 'structured-compare'],
            estimatedTime: '2-3分钟',
            tokenCost: 'medium',
            minDocs: 2
        }
    };

    window.EmbeddedAIAnalysisPanel = function EmbeddedAIAnalysisPanel({
        selectedDocuments = [],
        onClose,
        onRemoveDocument
    }) {
        const { t } = window.LanguageModule.useTranslation();

        const [customPrompt, setCustomPrompt] = useState('');
        const [analyzing, setAnalyzing] = useState(false);
        const [currentAnalysis, setCurrentAnalysis] = useState(null);

        // 用户选择的分析目标 (User selected analysis goal)
        const [analysisGoal, setAnalysisGoal] = useState('quick');

        // 是否显示高级选项 (Show advanced options)
        const [showAdvanced, setShowAdvanced] = useState(false);

        // 底层分析模式（高级用户可手动切换）
        const [analysisMode, setAnalysisMode] = useState('direct');

        // 智能检测推荐的目标 (Smart detected recommended goal)
        const [recommendedGoal, setRecommendedGoal] = useState(null);
        const [recommendReason, setRecommendReason] = useState('');

        // 智能检测用户意图 (Smart detect user intent)
        useEffect(() => {
            detectUserIntent();
        }, [customPrompt, selectedDocuments]);

        // 根据问题和文档智能推荐分析目标
        const detectUserIntent = () => {
            const question = customPrompt.toLowerCase();
            const docCount = selectedDocuments.length;

            let detected = null;
            let reason = '';

            // 基于问题关键词检测
            if (/什么|哪个|多少|是否|有没有|who|what|which|how many/.test(question)) {
                detected = 'precise';
                reason = t('detectReasonPrecise') || '检测到精确查询类问题';
            } else if (/总结|概括|简述|概述|summarize|summary|overview/.test(question)) {
                detected = 'quick';
                reason = t('detectReasonQuick') || '检测到总结类需求';
            } else if (/对比|比较|区别|相同|不同|差异|compare|difference|versus/.test(question)) {
                detected = 'compare';
                reason = t('detectReasonCompare') || '检测到对比分析需求';
            } else if (/为什么|原因|导致|因为|结果|影响|why|cause|effect|because/.test(question)) {
                detected = 'causal';
                reason = t('detectReasonCausal') || '检测到因果分析需求';
            } else if (/关系|关联|联系|相关|how.*relate|relationship|connection/.test(question)) {
                detected = 'relation';
                reason = t('detectReasonRelation') || '检测到关联分析需求';
            } else if (/全面|详细|深入|完整|comprehensive|detailed|thorough/.test(question)) {
                detected = 'comprehensive';
                reason = t('detectReasonComprehensive') || '检测到深度分析需求';
            }

            // 基于文档数量调整
            if (docCount >= 5 && !detected) {
                detected = 'quick';
                reason = t('detectReasonMultiDocs') || `检测到${docCount}个文档，推荐快速概览`;
            } else if (docCount >= 2 && !detected) {
                detected = 'relation';
                reason = t('detectReasonRelationDocs') || '多个文档，可能需要分析关联';
            }

            setRecommendedGoal(detected);
            setRecommendReason(reason);
        };

        // 处理移除文档
        const handleRemoveDocument = (doc) => {
            if (onRemoveDocument && !analyzing) {
                onRemoveDocument(doc);
            }
        };

        // 根据分析目标获取实际的分析模式
        const getAnalysisModeFromGoal = (goal) => {
            if (['relation', 'causal', 'compare'].includes(goal) && selectedDocuments.length >= 2) {
                return 'multiDoc';
            }
            return 'direct';
        };

        // 分析文档
        const analyzeDocuments = async () => {
            if (!selectedDocuments || selectedDocuments.length === 0) {
                alert(t('pleaseSelectDocuments') || '请选择要分析的文档');
                return;
            }

            const goalConfig = ANALYSIS_GOALS[analysisGoal];
            if (goalConfig.minDocs && selectedDocuments.length < goalConfig.minDocs) {
                alert(t('needMoreDocs')?.replace('{0}', goalConfig.minDocs) || `此分析目标至少需要${goalConfig.minDocs}个文档`);
                return;
            }

            const effectiveMode = showAdvanced ? analysisMode : getAnalysisModeFromGoal(analysisGoal);
            const finalPrompt = customPrompt || getDefaultPromptForGoal(analysisGoal);

            setAnalyzing(true);
            setCurrentAnalysis({
                documents: selectedDocuments,
                prompt: finalPrompt,
                status: 'running',
                progress: 0,
                results: [],
                analysisGoal: analysisGoal,
                analysisMode: effectiveMode
            });

            try {
                let results = [];

                if (effectiveMode === 'multiDoc') {
                    // 多文档联合分析
                    setCurrentAnalysis(prev => ({
                        ...prev,
                        progress: 10,
                        currentDoc: t('multiDocAnalyzing') || '多文档联合分析中...'
                    }));

                    try {
                        const docNames = selectedDocuments.map(d => d.title || d.name);
                        const result = await window.api.analyzeMultiDocuments(docNames, finalPrompt);

                        results.push({
                            document: { title: t('multiDocResult') || '联合分析结果', name: docNames.join(', ') },
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
                            document: { title: t('multiDocResult') || '联合分析结果' },
                            success: false,
                            error: error.message,
                            isMultiDoc: true
                        });
                    }
                } else {
                    // 逐个分析
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
                            if (isPPT) {
                                result = await window.api.analyzePPTDirect(docFileName, finalPrompt);
                            } else {
                                result = await window.api.analyzeDocumentDirect(docFileName, finalPrompt);
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

                // 保存结果到历史
                for (const result of results) {
                    if (result.success && result.data) {
                        try {
                            const docName = result.document.title || result.document.name;
                            await window.api.saveLLMResult({
                                title: `${docName} - ${t('aiAnalysis') || 'AI分析'}`,
                                sourceDocument: docName,
                                question: finalPrompt,
                                analysisType: getGoalLabel(analysisGoal),
                                content: result.data.answer || result.data.summary || result.data.comprehensiveSummary || result.data.finalReport || JSON.stringify(result.data),
                                keyPoints: result.data.keyPoints || []
                            });
                        } catch (saveError) {
                            console.warn('保存分析结果失败:', saveError);
                        }
                    }
                }

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

        // 获取分析目标的默认提示词
        const getDefaultPromptForGoal = (goal) => {
            const prompts = {
                quick: t('goalQuickPrompt') || '请快速总结这些文档的主要内容和关键要点。',
                precise: t('goalPrecisePrompt') || '请根据我的问题，从文档中找出准确的答案。',
                relation: t('goalRelationPrompt') || '请分析这些文档之间的关联关系，找出它们的共同点和差异点。',
                causal: t('goalCausalPrompt') || '请分析这些文档中的因果关系和逻辑链条。',
                comprehensive: t('goalComprehensivePrompt') || '请对这些文档进行全面深入的分析，生成详细的分析报告。',
                compare: t('goalComparePrompt') || '请对比分析这些文档，总结各自的优缺点和差异。'
            };
            return prompts[goal] || prompts.quick;
        };

        // 获取目标的显示标签
        const getGoalLabel = (goal) => {
            const labels = {
                quick: t('goalQuick') || '快速了解',
                precise: t('goalPrecise') || '精确查找',
                relation: t('goalRelation') || '关联分析',
                causal: t('goalCausal') || '因果分析',
                comprehensive: t('goalComprehensive') || '深度分析',
                compare: t('goalCompare') || '对比分析'
            };
            return labels[goal] || goal;
        };

        // 渲染 Markdown
        const renderMarkdown = (text) => {
            if (!text) return null;
            if (window.marked) {
                return React.createElement('div', {
                    dangerouslySetInnerHTML: { __html: window.marked.parse(text) }
                });
            }
            return React.createElement('div', { style: { whiteSpace: 'pre-wrap' } }, text);
        };

        return React.createElement('div', { className: 'ai-analysis-container' },
            // 选中的文档信息
            React.createElement('div', { className: 'ai-selected-info' },
                React.createElement('h3', null,
                    `📁 ${t('selectedDocumentsCount')?.replace('{0}', selectedDocuments.length) || `已选择 ${selectedDocuments.length} 个文档`}`
                ),
                selectedDocuments.length > 0 && React.createElement('div', { className: 'ai-doc-list' },
                    selectedDocuments.map((doc, i) =>
                        React.createElement('div', { key: i, className: 'ai-doc-item-removable' },
                            React.createElement('span', { className: 'ai-doc-name' },
                                `📄 ${doc.title || doc.name}`
                            ),
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

            // 智能推荐提示
            recommendedGoal && recommendedGoal !== analysisGoal && !analyzing &&
                React.createElement('div', { className: 'ai-smart-recommend' },
                    React.createElement('div', { className: 'ai-recommend-content' },
                        React.createElement('span', { className: 'ai-recommend-icon' }, '💡'),
                        React.createElement('span', { className: 'ai-recommend-text' },
                            `${t('smartRecommend') || '智能推荐'}: ${getGoalLabel(recommendedGoal)}`
                        ),
                        React.createElement('span', { className: 'ai-recommend-reason' },
                            `(${recommendReason})`
                        )
                    ),
                    React.createElement('button', {
                        className: 'ai-recommend-apply-btn',
                        onClick: () => setAnalysisGoal(recommendedGoal)
                    }, t('applyRecommend') || '采纳建议')
                ),

            // 分析目标选择（用户友好版）
            React.createElement('div', { className: 'ai-goal-selection' },
                React.createElement('div', { className: 'ai-goal-title' },
                    t('whatDoYouWant') || '📋 您想要什么样的分析？'
                ),
                
                React.createElement('div', { className: 'ai-goal-grid' },
                    // 快速了解
                    React.createElement('label', {
                        className: 'ai-goal-option' + (analysisGoal === 'quick' ? ' active' : '')
                    },
                        React.createElement('input', {
                            type: 'radio',
                            name: 'analysisGoal',
                            checked: analysisGoal === 'quick',
                            onChange: () => setAnalysisGoal('quick'),
                            disabled: analyzing
                        }),
                        React.createElement('div', { className: 'ai-goal-content' },
                            React.createElement('span', { className: 'ai-goal-icon' }, '🚀'),
                            React.createElement('div', { className: 'ai-goal-text' },
                                React.createElement('div', { className: 'ai-goal-label' },
                                    t('goalQuick') || '快速了解大意'
                                ),
                                React.createElement('div', { className: 'ai-goal-desc' },
                                    t('goalQuickDesc') || '几分钟内了解主要内容'
                                )
                            )
                        )
                    ),

                    // 精确查找
                    React.createElement('label', {
                        className: 'ai-goal-option' + (analysisGoal === 'precise' ? ' active' : '')
                    },
                        React.createElement('input', {
                            type: 'radio',
                            name: 'analysisGoal',
                            checked: analysisGoal === 'precise',
                            onChange: () => setAnalysisGoal('precise'),
                            disabled: analyzing
                        }),
                        React.createElement('div', { className: 'ai-goal-content' },
                            React.createElement('span', { className: 'ai-goal-icon' }, '🔍'),
                            React.createElement('div', { className: 'ai-goal-text' },
                                React.createElement('div', { className: 'ai-goal-label' },
                                    t('goalPrecise') || '精确查找答案'
                                ),
                                React.createElement('div', { className: 'ai-goal-desc' },
                                    t('goalPreciseDesc') || '针对问题找出准确答案'
                                )
                            )
                        )
                    ),

                    // 对比分析
                    React.createElement('label', {
                        className: 'ai-goal-option' + (analysisGoal === 'compare' ? ' active' : '') +
                            (selectedDocuments.length < 2 ? ' disabled' : '')
                    },
                        React.createElement('input', {
                            type: 'radio',
                            name: 'analysisGoal',
                            checked: analysisGoal === 'compare',
                            onChange: () => setAnalysisGoal('compare'),
                            disabled: analyzing || selectedDocuments.length < 2
                        }),
                        React.createElement('div', { className: 'ai-goal-content' },
                            React.createElement('span', { className: 'ai-goal-icon' }, '⚖️'),
                            React.createElement('div', { className: 'ai-goal-text' },
                                React.createElement('div', { className: 'ai-goal-label' },
                                    t('goalCompare') || '对比优劣'
                                ),
                                React.createElement('div', { className: 'ai-goal-desc' },
                                    t('goalCompareDesc') || '对比文档的优缺点'
                                ),
                                selectedDocuments.length < 2 && React.createElement('div', { className: 'ai-goal-warning' },
                                    t('needAtLeast2') || '至少需要2个文档'
                                )
                            )
                        )
                    ),

                    // 关联分析
                    React.createElement('label', {
                        className: 'ai-goal-option' + (analysisGoal === 'relation' ? ' active' : '') +
                            (selectedDocuments.length < 2 ? ' disabled' : '')
                    },
                        React.createElement('input', {
                            type: 'radio',
                            name: 'analysisGoal',
                            checked: analysisGoal === 'relation',
                            onChange: () => setAnalysisGoal('relation'),
                            disabled: analyzing || selectedDocuments.length < 2
                        }),
                        React.createElement('div', { className: 'ai-goal-content' },
                            React.createElement('span', { className: 'ai-goal-icon' }, '🔗'),
                            React.createElement('div', { className: 'ai-goal-text' },
                                React.createElement('div', { className: 'ai-goal-label' },
                                    t('goalRelation') || '分析关联关系'
                                ),
                                React.createElement('div', { className: 'ai-goal-desc' },
                                    t('goalRelationDesc') || '找出联系和异同点'
                                ),
                                selectedDocuments.length < 2 && React.createElement('div', { className: 'ai-goal-warning' },
                                    t('needAtLeast2') || '至少需要2个文档'
                                )
                            )
                        )
                    ),

                    // 因果分析
                    React.createElement('label', {
                        className: 'ai-goal-option' + (analysisGoal === 'causal' ? ' active' : '') +
                            (selectedDocuments.length < 2 ? ' disabled' : '')
                    },
                        React.createElement('input', {
                            type: 'radio',
                            name: 'analysisGoal',
                            checked: analysisGoal === 'causal',
                            onChange: () => setAnalysisGoal('causal'),
                            disabled: analyzing || selectedDocuments.length < 2
                        }),
                        React.createElement('div', { className: 'ai-goal-content' },
                            React.createElement('span', { className: 'ai-goal-icon' }, '⛓️'),
                            React.createElement('div', { className: 'ai-goal-text' },
                                React.createElement('div', { className: 'ai-goal-label' },
                                    t('goalCausal') || '追溯因果脉络'
                                ),
                                React.createElement('div', { className: 'ai-goal-desc' },
                                    t('goalCausalDesc') || '分析前因后果'
                                ),
                                selectedDocuments.length < 2 && React.createElement('div', { className: 'ai-goal-warning' },
                                    t('needAtLeast2') || '至少需要2个文档'
                                )
                            )
                        )
                    ),

                    // 全面深度分析
                    React.createElement('label', {
                        className: 'ai-goal-option' + (analysisGoal === 'comprehensive' ? ' active' : '')
                    },
                        React.createElement('input', {
                            type: 'radio',
                            name: 'analysisGoal',
                            checked: analysisGoal === 'comprehensive',
                            onChange: () => setAnalysisGoal('comprehensive'),
                            disabled: analyzing
                        }),
                        React.createElement('div', { className: 'ai-goal-content' },
                            React.createElement('span', { className: 'ai-goal-icon' }, '📊'),
                            React.createElement('div', { className: 'ai-goal-text' },
                                React.createElement('div', { className: 'ai-goal-label' },
                                    t('goalComprehensive') || '全面深度分析'
                                ),
                                React.createElement('div', { className: 'ai-goal-desc' },
                                    t('goalComprehensiveDesc') || '最详细的分析报告'
                                ),
                                React.createElement('div', { className: 'ai-goal-time' },
                                    `⏱️ ${t('estimatedTime') || '预计'}: 10-15${t('minutes') || '分钟'}`
                                )
                            )
                        )
                    )
                )
            ),

            // 自定义问题输入
            React.createElement('div', { className: 'ai-prompt-section' },
                React.createElement('label', { className: 'ai-prompt-label' },
                    t('yourQuestion') || '💬 您的问题（可选）'
                ),
                React.createElement('textarea', {
                    value: customPrompt,
                    onChange: (e) => setCustomPrompt(e.target.value),
                    placeholder: t('questionPlaceholder') || '输入具体问题可以获得更精准的分析结果...',
                    className: 'ai-analysis-textarea',
                    rows: 2,
                    disabled: analyzing
                })
            ),

            // 高级选项折叠区
            React.createElement('div', { className: 'ai-advanced-toggle' },
                React.createElement('button', {
                    className: 'ai-advanced-btn',
                    onClick: () => setShowAdvanced(!showAdvanced),
                    disabled: analyzing
                }, showAdvanced
                    ? `▼ ${t('hideAdvanced') || '收起高级选项'}`
                    : `▶ ${t('showAdvanced') || '高级选项'}`
                )
            ),

            showAdvanced && React.createElement('div', { className: 'ai-advanced-options' },
                React.createElement('div', { className: 'ai-mode-selection' },
                    React.createElement('div', { className: 'ai-mode-title' },
                        t('advancedMode') || '底层分析模式'
                    ),
                    React.createElement('select', {
                        value: analysisMode,
                        onChange: (e) => setAnalysisMode(e.target.value),
                        disabled: analyzing,
                        className: 'ai-mode-select'
                    },
                        React.createElement('option', { value: 'direct' },
                            t('directMode') || '单文档直接分析'
                        ),
                        React.createElement('option', { value: 'knowledgeBase' },
                            t('kbMode') || '知识库增强分析'
                        ),
                        React.createElement('option', { value: 'multiDoc' },
                            t('multiDocMode') || '多文档联合分析'
                        )
                    )
                )
            ),

            // 开始分析按钮
            React.createElement('div', { className: 'ai-action-section' },
                React.createElement('button', {
                    onClick: analyzeDocuments,
                    disabled: analyzing || selectedDocuments.length === 0,
                    className: 'ai-analysis-button' + (analysisGoal === 'comprehensive' ? ' comprehensive' : '')
                }, analyzing
                    ? `🔄 ${t('analyzing') || '分析中...'}`
                    : `${ANALYSIS_GOALS[analysisGoal].icon} ${t('startAnalysis') || '开始分析'}`
                ),
                React.createElement('div', { className: 'ai-estimate-info' },
                    `⏱️ ${t('estimated') || '预计'}: ${ANALYSIS_GOALS[analysisGoal].estimatedTime}`
                )
            ),

            // 分析结果
            currentAnalysis && React.createElement('div', { className: 'ai-results-section' },
                currentAnalysis.status === 'running' && React.createElement('div', { className: 'ai-progress-bar' },
                    React.createElement('div', {
                        className: 'ai-progress-fill',
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
                                            renderMarkdown(JSON.stringify(result.data, null, 2))
                                    ) :
                                    React.createElement('div', { className: 'ai-result-error' },
                                        `${t('analysisFailed') || '分析失败'}: ${result.error}`
                                    )
                            )
                        )
                    ),

                // 分析完成后的反馈
                currentAnalysis.status === 'completed' && React.createElement('div', { className: 'ai-feedback-section' },
                    React.createElement('div', { className: 'ai-feedback-title' },
                        t('wasHelpful') || '这个分析结果对您有帮助吗？'
                    ),
                    React.createElement('div', { className: 'ai-feedback-buttons' },
                        React.createElement('button', {
                            className: 'ai-feedback-btn positive',
                            onClick: () => console.log('Feedback: helpful')
                        }, '😊 ' + (t('helpful') || '很有帮助')),
                        React.createElement('button', {
                            className: 'ai-feedback-btn neutral',
                            onClick: () => console.log('Feedback: neutral')
                        }, '😐 ' + (t('neutral') || '一般')),
                        React.createElement('button', {
                            className: 'ai-feedback-btn negative',
                            onClick: () => console.log('Feedback: not helpful')
                        }, '😕 ' + (t('notHelpful') || '不满意'))
                    )
                )
            )
        );
    };

    console.log('✅ EmbeddedAIAnalysisPanel (User Friendly Version) loaded');
})();
