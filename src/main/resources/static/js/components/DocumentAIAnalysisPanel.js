/**
 * Document AI Analysis Component
 * 文档AI分析组件
 *
 * 功能：
 * 1. 显示已上传/已搜索到的文档列表
 * 2. 支持单个文档AI总结
 * 3. 支持批量选择文档进行AI分析
 * 4. 自定义提示词输入
 *
 * @author AI Reviewer Team
 * @since 2025-12-03
 */

(function() {
    'use strict';

    const { useState, useEffect } = React;

    /**
     * 文档AI分析面板组件
     */
    window.DocumentAIAnalysisPanel = function DocumentAIAnalysisPanel({
        documents = [],           // 文档列表
        onClose,                  // 关闭回调
        uploadedFiles = []        // 新上传的文件
    }) {
        const { t } = window.LanguageModule.useTranslation();

        // 状态管理
        const [selectedDocs, setSelectedDocs] = useState(new Set());
        const [customPrompt, setCustomPrompt] = useState('');
        const [analyzing, setAnalyzing] = useState(false);
        const [currentAnalysis, setCurrentAnalysis] = useState(null);
        const [analysisHistory, setAnalysisHistory] = useState([]);
        const [showHistory, setShowHistory] = useState(false);

        // 合并文档列表（已索引 + 新上传）
        const allDocuments = React.useMemo(() => {
            const indexed = documents.map(doc => ({
                ...doc,
                source: 'indexed',
                displayName: doc.title || doc.name,
                path: doc.path || doc.title
            }));

            const uploaded = uploadedFiles.map(file => ({
                name: file.name,
                size: file.size,
                type: file.type,
                lastModified: file.lastModified,
                source: 'uploaded',
                displayName: file.name,
                path: file.name,
                file: file
            }));

            return [...indexed, ...uploaded];
        }, [documents, uploadedFiles]);

        // 切换文档选择
        const toggleDocSelection = (docPath) => {
            setSelectedDocs(prev => {
                const newSet = new Set(prev);
                if (newSet.has(docPath)) {
                    newSet.delete(docPath);
                } else {
                    newSet.add(docPath);
                }
                return newSet;
            });
        };

        // 全选/取消全选
        const toggleSelectAll = () => {
            if (selectedDocs.size === allDocuments.length) {
                setSelectedDocs(new Set());
            } else {
                setSelectedDocs(new Set(allDocuments.map(d => d.path)));
            }
        };

        // 快速总结（单个文档）
        const quickSummary = async (doc) => {
            const defaultPrompt = doc.displayName.endsWith('.pptx') || doc.displayName.endsWith('.ppt')
                ? '请详细分析这份PPT的主要内容，包括核心观点、结构框架和关键信息。'
                : '请总结这份文档的核心内容和主要观点。';

            await analyzeDocuments([doc], defaultPrompt);
        };

        // 批量分析文档
        const analyzeDocuments = async (docs, prompt) => {
            if (!docs || docs.length === 0) {
                alert(t('pleaseSelectDocuments') || '请选择要分析的文档');
                return;
            }

            const finalPrompt = prompt || customPrompt || (
                docs.length === 1
                    ? '请总结这份文档的核心内容。'
                    : '请分别总结这些文档的核心内容，并找出它们之间的关联。'
            );

            setAnalyzing(true);
            setCurrentAnalysis({
                documents: docs,
                prompt: finalPrompt,
                status: 'running',
                progress: 0,
                results: []
            });

            try {
                const results = [];

                for (let i = 0; i < docs.length; i++) {
                    const doc = docs[i];

                    // 更新进度
                    setCurrentAnalysis(prev => ({
                        ...prev,
                        progress: Math.round((i / docs.length) * 100),
                        currentDoc: doc.displayName
                    }));

                    try {
                        let result;

                        // 判断文档类型选择API
                        const isPPT = doc.displayName.endsWith('.pptx') || doc.displayName.endsWith('.ppt');

                        if (isPPT) {
                            // PPT专用分析
                            result = await window.api.analyzePPT(doc.path, finalPrompt);
                        } else {
                            // 通用文档分析
                            result = await window.api.analyzeDocument(doc.path, finalPrompt);
                        }

                        results.push({
                            document: doc,
                            success: true,
                            data: result
                        });

                    } catch (error) {
                        console.error(`分析文档失败: ${doc.displayName}`, error);
                        results.push({
                            document: doc,
                            success: false,
                            error: error.message
                        });
                    }
                }

                // 分析完成
                const finalAnalysis = {
                    documents: docs,
                    prompt: finalPrompt,
                    status: 'completed',
                    progress: 100,
                    results: results,
                    timestamp: Date.now()
                };

                setCurrentAnalysis(finalAnalysis);
                setAnalysisHistory(prev => [finalAnalysis, ...prev].slice(0, 10)); // 保留最近10次

            } catch (error) {
                console.error('分析过程出错:', error);
                setCurrentAnalysis(prev => ({
                    ...prev,
                    status: 'error',
                    error: error.message
                }));
            } finally {
                setAnalyzing(false);
            }
        };

        // 开始批量分析
        const startBatchAnalysis = () => {
            const selectedDocsList = allDocuments.filter(d => selectedDocs.has(d.path));
            analyzeDocuments(selectedDocsList, customPrompt);
        };

        // 渲染文档列表
        const renderDocumentList = () => {
            if (allDocuments.length === 0) {
                return (
                    <div style={styles.emptyState}>
                        <div style={styles.emptyIcon}>📄</div>
                        <p>{t('noDocumentsToAnalyze') || '暂无可分析的文档'}</p>
                        <p style={styles.emptyHint}>
                            {t('uploadOrSearchDocs') || '请上传文档或通过搜索找到文档'}
                        </p>
                    </div>
                );
            }

            return (
                <div style={styles.documentList}>
                    <div style={styles.listHeader}>
                        <label style={styles.selectAllLabel}>
                            <input
                                type="checkbox"
                                checked={selectedDocs.size === allDocuments.length && allDocuments.length > 0}
                                onChange={toggleSelectAll}
                                style={styles.checkbox}
                            />
                            <span>{t('selectAll') || '全选'} ({selectedDocs.size}/{allDocuments.length})</span>
                        </label>
                    </div>

                    {allDocuments.map((doc, index) => (
                        <div key={doc.path + index} style={styles.documentItem}>
                            <div style={styles.docItemLeft}>
                                <input
                                    type="checkbox"
                                    checked={selectedDocs.has(doc.path)}
                                    onChange={() => toggleDocSelection(doc.path)}
                                    style={styles.checkbox}
                                />
                                <div style={styles.docIcon}>
                                    {getFileIcon(doc.displayName)}
                                </div>
                                <div style={styles.docInfo}>
                                    <div style={styles.docName}>{doc.displayName}</div>
                                    <div style={styles.docMeta}>
                                        {doc.source === 'uploaded' && (
                                            <span style={styles.badge}>{t('uploaded') || '已上传'}</span>
                                        )}
                                        {doc.size && (
                                            <span>{formatFileSize(doc.size)}</span>
                                        )}
                                    </div>
                                </div>
                            </div>
                            <div style={styles.docItemRight}>
                                <button
                                    onClick={() => quickSummary(doc)}
                                    disabled={analyzing}
                                    style={styles.quickButton}
                                    title={t('quickSummary') || '快速总结'}
                                >
                                    ✨ {t('summary') || '总结'}
                                </button>
                            </div>
                        </div>
                    ))}
                </div>
            );
        };

        // 渲染分析区域
        const renderAnalysisArea = () => {
            return (
                <div style={styles.analysisArea}>
                    <div style={styles.promptSection}>
                        <label style={styles.promptLabel}>
                            {t('customPrompt') || '自定义提示词'}
                            <span style={styles.optional}>({t('optional') || '可选'})</span>
                        </label>
                        <textarea
                            value={customPrompt}
                            onChange={(e) => setCustomPrompt(e.target.value)}
                            placeholder={t('promptPlaceholder') || '输入你的问题或分析要求，例如：请总结文档的核心观点...'}
                            style={styles.promptTextarea}
                            rows={3}
                            disabled={analyzing}
                        />
                        <div style={styles.promptHints}>
                            <div style={styles.hintTitle}>{t('suggestedPrompts') || '建议提示词'}：</div>
                            <button
                                onClick={() => setCustomPrompt('请详细总结这份文档的核心内容和关键观点。')}
                                style={styles.hintButton}
                                disabled={analyzing}
                            >
                                📋 {t('summary') || '总结'}
                            </button>
                            <button
                                onClick={() => setCustomPrompt('请分析这份文档的逻辑结构和论证方式。')}
                                style={styles.hintButton}
                                disabled={analyzing}
                            >
                                🔍 {t('analyze') || '分析'}
                            </button>
                            <button
                                onClick={() => setCustomPrompt('请提取文档中的关键数据和重要结论。')}
                                style={styles.hintButton}
                                disabled={analyzing}
                            >
                                💡 {t('extract') || '提取'}
                            </button>
                        </div>
                    </div>

                    <div style={styles.actionButtons}>
                        <button
                            onClick={startBatchAnalysis}
                            disabled={analyzing || selectedDocs.size === 0}
                            style={{
                                ...styles.analyzeButton,
                                ...(analyzing || selectedDocs.size === 0 ? styles.buttonDisabled : {})
                            }}
                        >
                            {analyzing ? '🔄 分析中...' : `🚀 开始分析 (${selectedDocs.size})`}
                        </button>
                        {analysisHistory.length > 0 && (
                            <button
                                onClick={() => setShowHistory(!showHistory)}
                                style={styles.historyButton}
                            >
                                📜 {showHistory ? t('hideHistory') : t('showHistory')} ({analysisHistory.length})
                            </button>
                        )}
                    </div>
                </div>
            );
        };

        // 渲染分析结果
        const renderAnalysisResults = () => {
            if (!currentAnalysis) return null;

            return (
                <div style={styles.resultsSection}>
                    <div style={styles.resultsHeader}>
                        <h3>{t('analysisResults') || '分析结果'}</h3>
                        {currentAnalysis.status === 'running' && (
                            <div style={styles.progressBar}>
                                <div style={{ ...styles.progressFill, width: `${currentAnalysis.progress}%` }} />
                                <span style={styles.progressText}>
                                    {currentAnalysis.progress}% - {currentAnalysis.currentDoc}
                                </span>
                            </div>
                        )}
                    </div>

                    <div style={styles.resultsContent}>
                        {currentAnalysis.results.map((result, index) => (
                            <div key={index} style={styles.resultItem}>
                                <div style={styles.resultHeader}>
                                    <span style={styles.resultIcon}>
                                        {result.success ? '✅' : '❌'}
                                    </span>
                                    <span style={styles.resultDocName}>{result.document.displayName}</span>
                                </div>
                                <div style={styles.resultBody}>
                                    {result.success ? (
                                        <div style={styles.resultSuccess}>
                                            {renderAnalysisData(result.data)}
                                        </div>
                                    ) : (
                                        <div style={styles.resultError}>
                                            {t('analysisFailed') || '分析失败'}: {result.error}
                                        </div>
                                    )}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            );
        };

        // 渲染分析数据
        const renderAnalysisData = (data) => {
            if (!data) return null;

            // PPT分析结果
            if (data.slideResults) {
                return (
                    <div style={styles.pptAnalysis}>
                        <div style={styles.summarySection}>
                            <h4>{t('comprehensiveSummary') || '综合总结'}</h4>
                            <div style={styles.markdown}>
                                {renderMarkdown(data.comprehensiveSummary)}
                            </div>
                        </div>
                        <div style={styles.slideDetails}>
                            <h4>{t('slideDetails') || '幻灯片详情'} ({data.slideResults.length})</h4>
                            {data.slideResults.slice(0, 5).map(slide => (
                                <div key={slide.slideNumber} style={styles.slideItem}>
                                    <div style={styles.slideTitle}>
                                        第 {slide.slideNumber} 张: {slide.title}
                                    </div>
                                    <div style={styles.slideKeyPoints}>
                                        {slide.keyPoints}
                                    </div>
                                </div>
                            ))}
                            {data.slideResults.length > 5 && (
                                <div style={styles.moreSlides}>
                                    ... 还有 {data.slideResults.length - 5} 张幻灯片
                                </div>
                            )}
                        </div>
                    </div>
                );
            }

            // 通用文档分析结果
            if (data.finalReport) {
                return (
                    <div style={styles.docAnalysis}>
                        <div style={styles.markdown}>
                            {renderMarkdown(data.finalReport)}
                        </div>
                    </div>
                );
            }

            return <pre>{JSON.stringify(data, null, 2)}</pre>;
        };

        // 渲染Markdown
        const renderMarkdown = (text) => {
            if (!text) return null;
            if (window.marked) {
                return <div dangerouslySetInnerHTML={{ __html: window.marked.parse(text) }} />;
            }
            return <div style={{ whiteSpace: 'pre-wrap' }}>{text}</div>;
        };

        return (
            <div style={styles.container}>
                <div style={styles.header}>
                    <h2>{t('documentAIAnalysis') || '文档AI分析'}</h2>
                    <button onClick={onClose} style={styles.closeButton}>✕</button>
                </div>

                <div style={styles.content}>
                    <div style={styles.leftPanel}>
                        <h3 style={styles.sectionTitle}>
                            {t('documentList') || '文档列表'}
                        </h3>
                        {renderDocumentList()}
                        {renderAnalysisArea()}
                    </div>

                    <div style={styles.rightPanel}>
                        {renderAnalysisResults()}
                    </div>
                </div>
            </div>
        );
    };

    // 辅助函数
    function getFileIcon(fileName) {
        const ext = fileName.split('.').pop().toLowerCase();
        const iconMap = {
            'pdf': '📕',
            'doc': '📘',
            'docx': '📘',
            'xls': '📗',
            'xlsx': '📗',
            'ppt': '📙',
            'pptx': '📙',
            'txt': '📄',
            'md': '📝'
        };
        return iconMap[ext] || '📄';
    }

    function formatFileSize(bytes) {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB';
        if (bytes < 1024 * 1024 * 1024) return (bytes / 1024 / 1024).toFixed(1) + ' MB';
        return (bytes / 1024 / 1024 / 1024).toFixed(1) + ' GB';
    }

    // 样式
    const styles = {
        container: {
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: '#fff',
            zIndex: 1000,
            display: 'flex',
            flexDirection: 'column'
        },
        header: {
            padding: '20px',
            borderBottom: '1px solid #e0e0e0',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
        },
        closeButton: {
            background: 'none',
            border: 'none',
            fontSize: '24px',
            cursor: 'pointer',
            padding: '5px 10px'
        },
        content: {
            display: 'flex',
            flex: 1,
            overflow: 'hidden'
        },
        leftPanel: {
            width: '40%',
            borderRight: '1px solid #e0e0e0',
            display: 'flex',
            flexDirection: 'column',
            overflow: 'hidden'
        },
        rightPanel: {
            flex: 1,
            overflow: 'auto',
            padding: '20px'
        },
        sectionTitle: {
            padding: '20px',
            margin: 0,
            borderBottom: '1px solid #e0e0e0'
        },
        documentList: {
            flex: 1,
            overflow: 'auto',
            padding: '10px'
        },
        listHeader: {
            padding: '10px',
            borderBottom: '1px solid #e0e0e0',
            marginBottom: '10px'
        },
        selectAllLabel: {
            display: 'flex',
            alignItems: 'center',
            cursor: 'pointer',
            userSelect: 'none'
        },
        checkbox: {
            marginRight: '10px',
            cursor: 'pointer',
            width: '18px',
            height: '18px'
        },
        documentItem: {
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center',
            padding: '12px',
            borderRadius: '8px',
            marginBottom: '8px',
            border: '1px solid #e0e0e0',
            transition: 'all 0.2s'
        },
        docItemLeft: {
            display: 'flex',
            alignItems: 'center',
            flex: 1,
            minWidth: 0
        },
        docIcon: {
            fontSize: '24px',
            marginRight: '12px'
        },
        docInfo: {
            flex: 1,
            minWidth: 0
        },
        docName: {
            fontWeight: '500',
            marginBottom: '4px',
            overflow: 'hidden',
            textOverflow: 'ellipsis',
            whiteSpace: 'nowrap'
        },
        docMeta: {
            fontSize: '12px',
            color: '#666',
            display: 'flex',
            gap: '10px'
        },
        badge: {
            backgroundColor: '#e3f2fd',
            color: '#1976d2',
            padding: '2px 8px',
            borderRadius: '12px',
            fontSize: '11px'
        },
        docItemRight: {
            marginLeft: '12px'
        },
        quickButton: {
            padding: '6px 12px',
            backgroundColor: '#4CAF50',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '13px',
            whiteSpace: 'nowrap'
        },
        analysisArea: {
            padding: '20px',
            borderTop: '1px solid #e0e0e0',
            backgroundColor: '#f9f9f9'
        },
        promptSection: {
            marginBottom: '15px'
        },
        promptLabel: {
            display: 'block',
            marginBottom: '8px',
            fontWeight: '500'
        },
        optional: {
            fontSize: '12px',
            color: '#999',
            fontWeight: 'normal'
        },
        promptTextarea: {
            width: '100%',
            padding: '10px',
            borderRadius: '4px',
            border: '1px solid #ddd',
            fontSize: '14px',
            resize: 'vertical',
            boxSizing: 'border-box'
        },
        promptHints: {
            marginTop: '10px',
            display: 'flex',
            flexWrap: 'wrap',
            gap: '8px',
            alignItems: 'center'
        },
        hintTitle: {
            fontSize: '12px',
            color: '#666'
        },
        hintButton: {
            padding: '4px 10px',
            fontSize: '12px',
            backgroundColor: '#fff',
            border: '1px solid #ddd',
            borderRadius: '4px',
            cursor: 'pointer'
        },
        actionButtons: {
            display: 'flex',
            gap: '10px'
        },
        analyzeButton: {
            flex: 1,
            padding: '12px',
            backgroundColor: '#2196F3',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '15px',
            fontWeight: '500'
        },
        historyButton: {
            padding: '12px 20px',
            backgroundColor: '#fff',
            color: '#333',
            border: '1px solid #ddd',
            borderRadius: '4px',
            cursor: 'pointer',
            fontSize: '14px'
        },
        buttonDisabled: {
            backgroundColor: '#ccc',
            cursor: 'not-allowed'
        },
        emptyState: {
            textAlign: 'center',
            padding: '60px 20px',
            color: '#999'
        },
        emptyIcon: {
            fontSize: '64px',
            marginBottom: '20px'
        },
        emptyHint: {
            fontSize: '14px',
            marginTop: '10px'
        },
        resultsSection: {
            padding: '20px'
        },
        resultsHeader: {
            marginBottom: '20px'
        },
        progressBar: {
            width: '100%',
            height: '30px',
            backgroundColor: '#f0f0f0',
            borderRadius: '4px',
            position: 'relative',
            marginTop: '10px',
            overflow: 'hidden'
        },
        progressFill: {
            height: '100%',
            backgroundColor: '#4CAF50',
            transition: 'width 0.3s'
        },
        progressText: {
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            fontSize: '12px',
            fontWeight: '500'
        },
        resultsContent: {
            display: 'flex',
            flexDirection: 'column',
            gap: '20px'
        },
        resultItem: {
            border: '1px solid #e0e0e0',
            borderRadius: '8px',
            overflow: 'hidden'
        },
        resultHeader: {
            padding: '12px 16px',
            backgroundColor: '#f5f5f5',
            display: 'flex',
            alignItems: 'center',
            gap: '10px'
        },
        resultIcon: {
            fontSize: '20px'
        },
        resultDocName: {
            fontWeight: '500'
        },
        resultBody: {
            padding: '16px'
        },
        resultSuccess: {
            color: '#333'
        },
        resultError: {
            color: '#f44336'
        },
        pptAnalysis: {
            display: 'flex',
            flexDirection: 'column',
            gap: '20px'
        },
        summarySection: {
            padding: '15px',
            backgroundColor: '#f9f9f9',
            borderRadius: '8px'
        },
        slideDetails: {
            padding: '15px',
            backgroundColor: '#fff',
            border: '1px solid #e0e0e0',
            borderRadius: '8px'
        },
        slideItem: {
            padding: '10px',
            marginBottom: '10px',
            borderLeft: '3px solid #2196F3'
        },
        slideTitle: {
            fontWeight: '500',
            marginBottom: '5px'
        },
        slideKeyPoints: {
            fontSize: '14px',
            color: '#666',
            whiteSpace: 'pre-wrap'
        },
        moreSlides: {
            textAlign: 'center',
            color: '#999',
            padding: '10px',
            fontStyle: 'italic'
        },
        markdown: {
            lineHeight: '1.6'
        }
    };

    console.log('✅ DocumentAIAnalysisPanel component loaded');
})();

