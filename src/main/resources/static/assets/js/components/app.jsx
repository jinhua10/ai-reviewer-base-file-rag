const { useState, useEffect, createContext, useContext } = React;

// 语言翻译字典从 lang.js 加载
// Translation dictionary is loaded from lang.js
// 直接使用 window.translations，无需重新声明

// 创建语言上下文
const LanguageContext = createContext();

// 语言提供者组件
function LanguageProvider({ children }) {
    const [language, setLanguage] = useState(() => {
        return localStorage.getItem('language') || 'zh';
    });

    const toggleLanguage = () => {
        const newLang = language === 'zh' ? 'en' : 'zh';
        setLanguage(newLang);
        localStorage.setItem('language', newLang);
        document.getElementById('html-root').setAttribute('lang', newLang === 'zh' ? 'zh-CN' : 'en');
    };

    const t = (key) => {
        return window.translations[language][key] || key;
    };

    return (
        <LanguageContext.Provider value={{ language, toggleLanguage, t }}>
            {children}
        </LanguageContext.Provider>
    );
}

// 自定义 Hook 使用语言
function useTranslation() {
    return useContext(LanguageContext);
}

// 自定义日期选择器组件
function DatePicker({ value, onChange, placeholder, language }) {
    const [showCalendar, setShowCalendar] = useState(false);
    const [currentYear, setCurrentYear] = useState(new Date().getFullYear());
    const [currentMonth, setCurrentMonth] = useState(new Date().getMonth());
    const calendarRef = React.useRef(null);

    // 如果有值，初始化到该日期
    useEffect(() => {
        if (value) {
            const date = new Date(value);
            setCurrentYear(date.getFullYear());
            setCurrentMonth(date.getMonth());
        }
    }, [value]);

    // 点击外部关闭日历
    useEffect(() => {
        const handleClickOutside = (event) => {
            if (calendarRef.current && !calendarRef.current.contains(event.target)) {
                setShowCalendar(false);
            }
        };
        if (showCalendar) {
            document.addEventListener('mousedown', handleClickOutside);
        }
        return () => document.removeEventListener('mousedown', handleClickOutside);
    }, [showCalendar]);

    const monthNames = language === 'zh'
        ? ['一月', '二月', '三月', '四月', '五月', '六月', '七月', '八月', '九月', '十月', '十一月', '十二月']
        : ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December'];

    const weekDays = language === 'zh'
        ? ['日', '一', '二', '三', '四', '五', '六']
        : ['Sun', 'Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat'];

    const getDaysInMonth = (year, month) => {
        return new Date(year, month + 1, 0).getDate();
    };

    const getFirstDayOfMonth = (year, month) => {
        return new Date(year, month, 1).getDay();
    };

    const formatDate = (date) => {
        if (!date) return '';
        const d = new Date(date);
        const year = d.getFullYear();
        const month = String(d.getMonth() + 1).padStart(2, '0');
        const day = String(d.getDate()).padStart(2, '0');
        return `${year}-${month}-${day}`;
    };

    const displayDate = (date) => {
        if (!date) return '';
        const d = new Date(date);
        if (language === 'zh') {
            return `${d.getFullYear()}年${d.getMonth() + 1}月${d.getDate()}日`;
        } else {
            return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
        }
    };

    const handleDateSelect = (day) => {
        const selected = new Date(currentYear, currentMonth, day);
        onChange(formatDate(selected));
        setShowCalendar(false);
    };

    const handlePrevMonth = () => {
        if (currentMonth === 0) {
            setCurrentMonth(11);
            setCurrentYear(currentYear - 1);
        } else {
            setCurrentMonth(currentMonth - 1);
        }
    };

    const handleNextMonth = () => {
        if (currentMonth === 11) {
            setCurrentMonth(0);
            setCurrentYear(currentYear + 1);
        } else {
            setCurrentMonth(currentMonth + 1);
        }
    };

    const renderCalendar = () => {
        const daysInMonth = getDaysInMonth(currentYear, currentMonth);
        const firstDay = getFirstDayOfMonth(currentYear, currentMonth);
        const days = [];
        const today = new Date();
        const selectedDate = value ? new Date(value) : null;

        // 填充空白
        for (let i = 0; i < firstDay; i++) {
            days.push(<div key={`empty-${i}`} style={{padding: '12px'}}></div>);
        }

        // 填充日期
        for (let day = 1; day <= daysInMonth; day++) {
            const date = new Date(currentYear, currentMonth, day);
            const isToday = date.toDateString() === today.toDateString();
            const isSelected = selectedDate && date.toDateString() === selectedDate.toDateString();

            days.push(
                <div
                    key={day}
                    onClick={() => handleDateSelect(day)}
                    style={{
                        padding: '12px',
                        textAlign: 'center',
                        cursor: 'pointer',
                        borderRadius: '6px',
                        fontSize: '15px',
                        fontWeight: isSelected ? '600' : '400',
                        background: isSelected ? '#667eea' : isToday ? '#e3f2fd' : 'transparent',
                        color: isSelected ? 'white' : isToday ? '#1976d2' : '#333',
                        transition: 'all 0.2s',
                        border: isToday && !isSelected ? '2px solid #2196f3' : '2px solid transparent'
                    }}
                    onMouseEnter={(e) => {
                        if (!isSelected) {
                            e.currentTarget.style.background = '#f5f5f5';
                        }
                    }}
                    onMouseLeave={(e) => {
                        if (!isSelected) {
                            e.currentTarget.style.background = isToday ? '#e3f2fd' : 'transparent';
                        }
                    }}
                >
                    {day}
                </div>
            );
        }

        return days;
    };

    return (
        <div ref={calendarRef} style={{position: 'relative', display: 'inline-block'}}>
            <input
                type="text"
                className="input-field"
                style={{width: '180px', cursor: 'pointer'}}
                placeholder={placeholder}
                value={displayDate(value)}
                onClick={() => setShowCalendar(!showCalendar)}
                readOnly
            />
            {showCalendar && (
                <div style={{
                    position: 'absolute',
                    top: '100%',
                    left: 0,
                    marginTop: '5px',
                    background: 'white',
                    border: '2px solid #667eea',
                    borderRadius: '12px',
                    boxShadow: '0 8px 24px rgba(0,0,0,0.15)',
                    zIndex: 1000,
                    padding: '20px',
                    minWidth: '350px'
                }}>
                    {/* 月份年份导航 */}
                    <div style={{
                        display: 'flex',
                        justifyContent: 'space-between',
                        alignItems: 'center',
                        marginBottom: '20px',
                        paddingBottom: '15px',
                        borderBottom: '2px solid #f0f0f0'
                    }}>
                        <button
                            onClick={handlePrevMonth}
                            style={{
                                background: '#f5f5f5',
                                border: 'none',
                                borderRadius: '6px',
                                padding: '8px 12px',
                                cursor: 'pointer',
                                fontSize: '18px',
                                fontWeight: 'bold',
                                transition: 'all 0.2s'
                            }}
                            onMouseEnter={(e) => e.currentTarget.style.background = '#e0e0e0'}
                            onMouseLeave={(e) => e.currentTarget.style.background = '#f5f5f5'}
                        >
                            ←
                        </button>
                        <div style={{
                            fontSize: '18px',
                            fontWeight: '600',
                            color: '#333'
                        }}>
                            {language === 'zh'
                                ? `${currentYear}年 ${monthNames[currentMonth]}`
                                : `${monthNames[currentMonth]} ${currentYear}`
                            }
                        </div>
                        <button
                            onClick={handleNextMonth}
                            style={{
                                background: '#f5f5f5',
                                border: 'none',
                                borderRadius: '6px',
                                padding: '8px 12px',
                                cursor: 'pointer',
                                fontSize: '18px',
                                fontWeight: 'bold',
                                transition: 'all 0.2s'
                            }}
                            onMouseEnter={(e) => e.currentTarget.style.background = '#e0e0e0'}
                            onMouseLeave={(e) => e.currentTarget.style.background = '#f5f5f5'}
                        >
                            →
                        </button>
                    </div>

                    {/* 星期标题 */}
                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(7, 1fr)',
                        gap: '5px',
                        marginBottom: '10px'
                    }}>
                        {weekDays.map(day => (
                            <div key={day} style={{
                                padding: '10px',
                                textAlign: 'center',
                                fontWeight: '600',
                                color: '#666',
                                fontSize: '14px'
                            }}>
                                {day}
                            </div>
                        ))}
                    </div>

                    {/* 日期网格 */}
                    <div style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(7, 1fr)',
                        gap: '5px'
                    }}>
                        {renderCalendar()}
                    </div>

                    {/* 快捷按钮 */}
                    <div style={{
                        marginTop: '15px',
                        paddingTop: '15px',
                        borderTop: '2px solid #f0f0f0',
                        display: 'flex',
                        gap: '10px',
                        justifyContent: 'space-between'
                    }}>
                        <button
                            onClick={() => {
                                onChange(formatDate(new Date()));
                                setShowCalendar(false);
                            }}
                            style={{
                                flex: 1,
                                padding: '8px 16px',
                                background: '#667eea',
                                color: 'white',
                                border: 'none',
                                borderRadius: '6px',
                                cursor: 'pointer',
                                fontSize: '14px',
                                fontWeight: '600',
                                transition: 'all 0.2s'
                            }}
                            onMouseEnter={(e) => e.currentTarget.style.background = '#5568d3'}
                            onMouseLeave={(e) => e.currentTarget.style.background = '#667eea'}
                        >
                            {language === 'zh' ? '今天' : 'Today'}
                        </button>
                        <button
                            onClick={() => {
                                onChange('');
                                setShowCalendar(false);
                            }}
                            style={{
                                flex: 1,
                                padding: '8px 16px',
                                background: '#f5f5f5',
                                color: '#666',
                                border: 'none',
                                borderRadius: '6px',
                                cursor: 'pointer',
                                fontSize: '14px',
                                fontWeight: '600',
                                transition: 'all 0.2s'
                            }}
                            onMouseEnter={(e) => e.currentTarget.style.background = '#e0e0e0'}
                            onMouseLeave={(e) => e.currentTarget.style.background = '#f5f5f5'}
                        >
                            {language === 'zh' ? '清除' : 'Clear'}
                        </button>
                    </div>
                </div>
            )}
        </div>
    );
}

// API 已在 api.js 中定义并通过 window.api 暴露
// 这里直接使用 window.api 即可

// 问答组件
function QATab() {
    const { t } = useTranslation();
    const [question, setQuestion] = useState('');
    const [answer, setAnswer] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);

    // 反馈相关状态
    const [feedbackRating, setFeedbackRating] = useState(0);
    const [feedbackComment, setFeedbackComment] = useState('');
    const [feedbackSubmitted, setFeedbackSubmitted] = useState(false);
    const [documentFeedbacks, setDocumentFeedbacks] = useState({});
    const [showReasonModal, setShowReasonModal] = useState(false);
    const [currentFeedbackDoc, setCurrentFeedbackDoc] = useState(null);

    // 配置 marked
    useEffect(() => {
        if (typeof marked !== 'undefined') {
            marked.setOptions({
                breaks: true,
                gfm: true,
                headerIds: true,
                mangle: false
            });
        }
    }, []);

    // 当答案更新时，高亮代码块
    useEffect(() => {
        if (answer && typeof hljs !== 'undefined') {
            document.querySelectorAll('.answer-text pre code').forEach((block) => {
                hljs.highlightElement(block);
            });
        }
    }, [answer]);

    const handleAsk = async () => {
        if (!question.trim()) {
            alert(t('qaInputError'));
            return;
        }

        setLoading(true);
        setError(null);
        setAnswer(null);

        try {
            const result = await api.ask(question);
            setAnswer(result);
        } catch (err) {
            setError(err.message || t('qaRequestError'));
        } finally {
            setLoading(false);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleAsk();
        }
    };

    // 下载单个文件
    const handleDownload = async (fileName) => {
        try {
            const blob = await api.downloadDocument(fileName);
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = fileName;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (err) {
            alert(t('qaDownloadError') + ': ' + err.message);
        }
    };

    // 批量下载所有参考文件
    const handleBatchDownload = async () => {
        if (!answer || !answer.sources || answer.sources.length === 0) {
            return;
        }

        try {
            const blob = await api.downloadBatch(answer.sources);
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `references_${new Date().getTime()}.zip`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);
        } catch (err) {
            alert(t('qaBatchDownloadError') + ': ' + err.message);
        }
    };

    // 下载单个文档块
    const handleChunkDownload = async (documentId, chunkId, buttonElement) => {
        try {
            // 添加下载动画
            buttonElement.classList.add('downloading');

            const response = await fetch(`/api/chunks/download/${encodeURIComponent(documentId)}/${encodeURIComponent(chunkId)}`);

            if (!response.ok) {
                throw new Error(t('qaChunkDownloadError'));
            }

            const blob = await response.blob();
            const url = window.URL.createObjectURL(blob);
            const link = document.createElement('a');
            link.href = url;
            link.download = `${chunkId}.md`;
            document.body.appendChild(link);
            link.click();
            document.body.removeChild(link);
            window.URL.revokeObjectURL(url);

            // 移除动画
            setTimeout(() => {
                buttonElement.classList.remove('downloading');
            }, 600);
        } catch (err) {
            buttonElement.classList.remove('downloading');
            alert(t('qaChunkDownloadError') + ': ' + err.message);
        }
    };

    // 批量下载所有文档块
    const handleBatchDownloadChunks = async () => {
        if (!answer || !answer.chunks || answer.chunks.length === 0) {
            alert(t('qaChunkDownloadError'));
            return;
        }

        try {
            let successCount = 0;
            let failCount = 0;

            for (let i = 0; i < answer.chunks.length; i++) {
                const chunk = answer.chunks[i];
                try {
                    const response = await fetch(`/api/chunks/download/${encodeURIComponent(chunk.documentId)}/${encodeURIComponent(chunk.chunkId)}`);

                    if (!response.ok) {
                        throw new Error('Download failed');
                    }

                    const blob = await response.blob();
                    const url = window.URL.createObjectURL(blob);
                    const link = document.createElement('a');
                    link.href = url;
                    link.download = `${chunk.title || 'chunk_' + (chunk.chunkIndex + 1)}.md`;
                    document.body.appendChild(link);
                    link.click();
                    document.body.removeChild(link);
                    window.URL.revokeObjectURL(url);

                    successCount++;

                    // 延迟一下避免浏览器阻止多个下载
                    if (i < answer.chunks.length - 1) {
                        await new Promise(resolve => setTimeout(resolve, 300));
                    }
                } catch (err) {
                    console.error(`Failed to download chunk ${chunk.chunkId}:`, err);
                    failCount++;
                }
            }

            if (failCount > 0) {
                alert(`${t('qaChunksDownloadAll')}: ${successCount}/${answer.chunks.length} ${t('docsUploadSuccess')}, ${failCount} ${t('docsUploadError')}`);
            } else {
                alert(`${t('qaChunksDownloadAll')}: ${successCount}/${answer.chunks.length} ${t('docsUploadSuccess')}`);
            }
        } catch (err) {
            alert(t('qaChunkDownloadError') + ': ' + err.message);
        }
    };

    // 图片点击放大
    useEffect(() => {
        const handleImageClick = (e) => {
            if (e.target.tagName === 'IMG' && e.target.closest('.answer-text')) {
                showImageModal(e.target.src, e.target.alt);
            }
        };

        document.addEventListener('click', handleImageClick);
        return () => document.removeEventListener('click', handleImageClick);
    }, []);

    // 显示图片模态框
    const showImageModal = (src, alt) => {
        const modal = document.createElement('div');
        modal.className = 'image-modal active';
        modal.innerHTML = `
                    <div class="image-modal-content">
                        <button class="image-modal-close" aria-label="${t('qaImageClose')}">&times;</button>
                        <img src="${src}" alt="${alt || t('qaImageAlt')}" />
                        ${alt ? `<div class="image-caption">${alt}</div>` : ''}
                    </div>
                `;

        modal.onclick = (e) => {
            if (e.target === modal || e.target.classList.contains('image-modal-close')) {
                modal.classList.remove('active');
                setTimeout(() => modal.remove(), 300);
            }
        };

        // ESC 键关闭
        const handleEsc = (e) => {
            if (e.key === 'Escape') {
                modal.classList.remove('active');
                setTimeout(() => modal.remove(), 300);
                document.removeEventListener('keydown', handleEsc);
            }
        };
        document.addEventListener('keydown', handleEsc);

        document.body.appendChild(modal);
    };

    // 提交整体反馈
    const handleSubmitFeedback = async () => {
        if (feedbackRating === 0) {
            alert(t('feedbackPleaseRate'));
            return;
        }

        try {
            const result = await api.submitOverallFeedback(
                answer.recordId || Date.now().toString(),
                feedbackRating,
                feedbackComment
            );

            if (result.success) {
                setFeedbackSubmitted(true);
            } else {
                alert(t('feedbackError'));
            }
        } catch (err) {
            console.error('提交反馈失败:', err);
            alert(t('feedbackError'));
        }
    };

    // 提交文档反馈（有帮助）
    const handleDocumentHelpful = async (docName) => {
        if (documentFeedbacks[docName]) {
            return; // 已经提交过反馈
        }

        try {
            const result = await api.submitDocumentFeedback(
                answer.recordId || Date.now().toString(),
                docName,
                'HELPFUL',
                null
            );

            if (result.success) {
                setDocumentFeedbacks(prev => ({
                    ...prev,
                    [docName]: 'HELPFUL'
                }));
            }
        } catch (err) {
            console.error('提交文档反馈失败:', err);
        }
    };

    // 提交文档反馈（无关）
    const handleDocumentNotHelpful = (docName) => {
        setCurrentFeedbackDoc(docName);
        setShowReasonModal(true);
    };

    // 提交文档无关反馈的原因
    const submitDocumentNotHelpfulReason = async (reason) => {
        if (!currentFeedbackDoc) return;

        try {
            const result = await api.submitDocumentFeedback(
                answer.recordId || Date.now().toString(),
                currentFeedbackDoc,
                'NOT_HELPFUL',
                reason
            );

            if (result.success) {
                setDocumentFeedbacks(prev => ({
                    ...prev,
                    [currentFeedbackDoc]: 'NOT_HELPFUL'
                }));
            }
        } catch (err) {
            console.error('提交文档反馈失败:', err);
        } finally {
            setShowReasonModal(false);
            setCurrentFeedbackDoc(null);
        }
    };

    return (
        <div className="qa-section">
            <div className="input-group">
                <input
                    type="text"
                    className="input-field"
                    placeholder={t('qaPlaceholder')}
                    value={question}
                    onChange={(e) => setQuestion(e.target.value)}
                    onKeyPress={handleKeyPress}
                    disabled={loading}
                />
                <button
                    className="btn btn-primary"
                    onClick={handleAsk}
                    disabled={loading}
                >
                    {loading ? t('qaThinking') : t('qaButton')}
                </button>
            </div>

            {loading && (
                <div className="loading">
                    <div className="spinner"></div>
                    <p>{t('qaAIThinking')}</p>
                </div>
            )}

            {error && (
                <div className="error">
                    {t('qaErrorPrefix')} {error}
                </div>
            )}

            {answer && !loading && (
                <div className="answer-card">
                    <h3>{t('qaAnswer')}</h3>
                    <div
                        className="answer-text"
                        dangerouslySetInnerHTML={{
                            __html: typeof marked !== 'undefined'
                                ? marked.parse(answer.answer)
                                : answer.answer
                        }}
                    ></div>

                    {answer.sources && answer.sources.length > 0 && (
                        <div className="sources">
                            <h4>{t('qaSources')}</h4>
                            {answer.sources.map((source, index) => (
                                <div key={index} className="source-item">
                                    <span className="source-text">
                                        {index + 1}. {source}
                                    </span>
                                    <div className="source-actions">
                                        <button
                                            className="btn-icon"
                                            onClick={() => handleDownload(source)}
                                            title={t('qaDownload')}
                                        >
                                            {t('qaDownload')}
                                        </button>
                                    </div>
                                </div>
                            ))}
                            {answer.sources.length > 1 && (
                                <button
                                    className="btn-batch-download"
                                    onClick={handleBatchDownload}
                                >
                                    {t('qaBatchDownload')} ({answer.sources.length} {t('docsFiles')})
                                </button>
                            )}
                        </div>
                    )}

                    {/* 文档切分块下载区域与文档反馈区域（合并） */}
                    {(answer.chunks && answer.chunks.length > 0) || (answer.sources && answer.sources.length > 0) ? (
                        <div className="chunks-section">
                            <h4>
                                📦 {t('qaChunksAndFeedback')}
                            </h4>

                            {/* 文档切分块 */}
                            {answer.chunks && answer.chunks.length > 0 && (
                                <>
                                    <div style={{ marginBottom: '10px', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                        <span style={{ fontSize: '14px', color: '#666' }}>
                                            {answer.chunks.length} {t('qaChunksAvailable')}
                                        </span>
                                        <button
                                            className="btn-batch-download-chunks"
                                            onClick={handleBatchDownloadChunks}
                                            title={t('qaChunksDownloadAll')}
                                        >
                                            {t('qaChunksDownloadAll')}
                                        </button>
                                    </div>
                                    <div className="chunks-grid">
                                        {answer.chunks.map((chunk, index) => (
                                            <button
                                                key={chunk.chunkId}
                                                className="chunk-button"
                                                onClick={(e) => handleChunkDownload(chunk.documentId, chunk.chunkId, e.currentTarget)}
                                                title={`${t('qaChunkDownload')}: ${chunk.title || t('qaChunkTitle') + ' ' + (chunk.chunkIndex + 1)}`}
                                            >
                                                <div className="chunk-title">
                                                    📄 {chunk.title || `${t('qaChunkTitle')} ${chunk.chunkIndex + 1}`}
                                                </div>
                                                <div className="chunk-info">
                                                    <span className="chunk-index">
                                                        {chunk.chunkIndex + 1}/{chunk.totalChunks || answer.chunks.length}
                                                    </span>
                                                    <span className="chunk-size">
                                                        {(chunk.contentLength / 1024).toFixed(1)} KB
                                                    </span>
                                                </div>
                                            </button>
                                        ))}
                                    </div>
                                </>
                            )}

                            {/* 文档反馈区域 */}
                            {answer.sources && answer.sources.length > 0 && (
                                <div className="document-feedback-area" style={{ marginTop: answer.chunks && answer.chunks.length > 0 ? '20px' : '0' }}>
                                    <h5 style={{ marginBottom: '15px', color: '#1565c0', fontSize: '15px' }}>
                                        📚 {t('feedbackDocumentQuestion')}
                                    </h5>
                                    {answer.sources.map((source, index) => (
                                        <div key={index} className="document-feedback-item">
                                            <div className="document-feedback-name">
                                                {index + 1}. {source}
                                            </div>
                                            <div className="document-feedback-buttons">
                                                <button
                                                    className={`document-feedback-btn ${documentFeedbacks[source] === 'HELPFUL' ? 'liked' : ''}`}
                                                    onClick={() => handleDocumentHelpful(source)}
                                                    disabled={documentFeedbacks[source] !== undefined}
                                                >
                                                    {documentFeedbacks[source] === 'HELPFUL'
                                                        ? t('feedbackDocumentSubmitted')
                                                        : t('feedbackDocumentHelpful')}
                                                </button>
                                                <button
                                                    className={`document-feedback-btn ${documentFeedbacks[source] === 'NOT_HELPFUL' ? 'disliked' : ''}`}
                                                    onClick={() => handleDocumentNotHelpful(source)}
                                                    disabled={documentFeedbacks[source] !== undefined}
                                                >
                                                    {documentFeedbacks[source] === 'NOT_HELPFUL'
                                                        ? t('feedbackDocumentSubmitted')
                                                        : t('feedbackDocumentNotHelpful')}
                                                </button>
                                            </div>
                                        </div>
                                    ))}
                                </div>
                            )}
                        </div>
                    ) : null}

                    {/* 用户反馈区域 */}
                    {!feedbackSubmitted ? (
                        <div className="feedback-section">
                            <h4>💬 {t('feedbackQuestion')}</h4>

                            <div className="feedback-rating-buttons">
                                <button
                                    className={`feedback-rating-button ${feedbackRating === 5 ? 'selected' : ''}`}
                                    onClick={() => setFeedbackRating(5)}
                                >
                                    {t('feedbackRating5')}
                                </button>
                                <button
                                    className={`feedback-rating-button ${feedbackRating === 4 ? 'selected' : ''}`}
                                    onClick={() => setFeedbackRating(4)}
                                >
                                    {t('feedbackRating4')}
                                </button>
                                <button
                                    className={`feedback-rating-button ${feedbackRating === 3 ? 'selected' : ''}`}
                                    onClick={() => setFeedbackRating(3)}
                                >
                                    {t('feedbackRating3')}
                                </button>
                                <button
                                    className={`feedback-rating-button ${feedbackRating === 2 ? 'selected' : ''}`}
                                    onClick={() => setFeedbackRating(2)}
                                >
                                    {t('feedbackRating2')}
                                </button>
                                <button
                                    className={`feedback-rating-button ${feedbackRating === 1 ? 'selected' : ''}`}
                                    onClick={() => setFeedbackRating(1)}
                                >
                                    {t('feedbackRating1')}
                                </button>
                            </div>

                            <textarea
                                className="feedback-comment"
                                placeholder={t('feedbackCommentPlaceholder')}
                                value={feedbackComment}
                                onChange={(e) => setFeedbackComment(e.target.value)}
                            />

                            <button
                                className="feedback-submit-btn"
                                onClick={handleSubmitFeedback}
                                disabled={feedbackRating === 0}
                            >
                                {t('feedbackSubmit')}
                            </button>
                        </div>
                    ) : (
                        <div className="feedback-section">
                            <div className="feedback-success">
                                {t('feedbackThankYou')}
                            </div>
                        </div>
                    )}


                    <div className="response-time">
                        {t('qaResponseTime')}: {answer.responseTimeMs}ms
                    </div>
                </div>
            )}

            {/* 文档反馈原因模态框 */}
            {showReasonModal && (
                <div className="feedback-reason-modal" onClick={() => setShowReasonModal(false)}>
                    <div className="feedback-reason-content" onClick={(e) => e.stopPropagation()}>
                        <h4>{t('feedbackDocumentReasonPlaceholder')}</h4>
                        <textarea
                            id="reasonTextarea"
                            placeholder={t('feedbackCommentPlaceholder')}
                            autoFocus
                        />
                        <div className="feedback-reason-actions">
                            <button
                                className="btn btn-secondary"
                                onClick={() => {
                                    setShowReasonModal(false);
                                    setCurrentFeedbackDoc(null);
                                }}
                            >
                                {t('qaImageClose')}
                            </button>
                            <button
                                className="btn btn-primary"
                                onClick={() => {
                                    const textarea = document.getElementById('reasonTextarea');
                                    submitDocumentNotHelpfulReason(textarea.value);
                                }}
                            >
                                {t('feedbackSubmit')}
                            </button>
                        </div>
                    </div>
                </div>
            )}

            {!answer && !loading && !error && (
                <div className="empty-state">
                    <div className="empty-state-icon">{t('qaEmptyIcon')}</div>
                    <p>{t('qaEmptyText')}</p>
                    <p style={{ fontSize: '14px', marginTop: '10px', color: '#ccc' }}>
                        {t('qaEmptyExample')}
                    </p>
                </div>
            )}
        </div>
    );
}

// 搜索组件
function SearchTab() {
    const { t } = useTranslation();
    const [query, setQuery] = useState('');
    const [results, setResults] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState(null);
    const [limit, setLimit] = useState(10);

    const handleSearch = async () => {
        if (!query.trim()) {
            alert(t('searchInputError'));
            return;
        }

        setLoading(true);
        setError(null);
        setResults(null);

        try {
            const result = await api.search(query, limit);
            setResults(result);
        } catch (err) {
            setError(err.message || t('searchError'));
        } finally {
            setLoading(false);
        }
    };

    const handleKeyPress = (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            handleSearch();
        }
    };

    return (
        <div className="search-section">
            <div className="input-group">
                <input
                    type="text"
                    className="input-field"
                    placeholder={t('searchPlaceholder')}
                    value={query}
                    onChange={(e) => setQuery(e.target.value)}
                    onKeyPress={handleKeyPress}
                    disabled={loading}
                />
                <select
                    className="input-field"
                    style={{ flex: '0 0 120px' }}
                    value={limit}
                    onChange={(e) => setLimit(Number(e.target.value))}
                >
                    <option value={5}>5</option>
                    <option value={10}>10</option>
                    <option value={20}>20</option>
                    <option value={50}>50</option>
                </select>
                <button
                    className="btn btn-primary"
                    onClick={handleSearch}
                    disabled={loading}
                >
                    {loading ? t('searchSearching') : t('searchButton')}
                </button>
            </div>

            {loading && (
                <div className="loading">
                    <div className="spinner"></div>
                    <p>{t('searchSearching')}</p>
                </div>
            )}

            {error && (
                <div className="error">
                    {t('qaErrorPrefix')} {error}
                </div>
            )}

            {results && !loading && (
                <div>
                    <div style={{ marginBottom: '15px', color: '#666' }}>
                        <strong>{results.total}</strong> {t('searchResultsCount')}
                    </div>

                    <div className="search-results">
                        {results.documents.map((doc, index) => (
                            <div key={doc.id} className="document-card">
                                <div className="document-title">
                                    📄 {doc.title || `Document ${index + 1}`}
                                </div>
                                <div className="document-excerpt">
                                    {doc.excerpt}
                                </div>
                            </div>
                        ))}
                    </div>
                </div>
            )}

            {!results && !loading && !error && (
                <div className="empty-state">
                    <div className="empty-state-icon">{t('searchEmptyIcon')}</div>
                    <p>{t('searchEmptyText')}</p>
                </div>
            )}
        </div>
    );
}

// 文档管理组件
function DocumentsTab() {
    const { t, language } = useTranslation();
    const [documents, setDocuments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [uploading, setUploading] = useState(false);
    const [uploadProgress, setUploadProgress] = useState(null);
    const [filterText, setFilterText] = useState('');

    // 分页状态
    const [currentPage, setCurrentPage] = useState(1);
    const [pageSize, setPageSize] = useState(20);
    const [totalPages, setTotalPages] = useState(0);
    const [totalCount, setTotalCount] = useState(0);

    // 排序状态
    const [sortBy, setSortBy] = useState('date'); // name, size, date, type
    const [sortOrder, setSortOrder] = useState('desc'); // asc, desc

    // 高级搜索状态
    const [showAdvancedSearch, setShowAdvancedSearch] = useState(false);
    const [advancedFilters, setAdvancedFilters] = useState({
        search: '',
        searchMode: 'contains',
        fileTypes: [],
        minSize: '',
        maxSize: '',
        indexed: 'all',
        startDate: '',
        endDate: ''
    });

    // 支持的文件类型列表（可扩展配置）
    const [supportedFileTypes, setSupportedFileTypes] = useState([
        'pdf', 'docx', 'doc', 'xlsx', 'xls', 'pptx', 'ppt', 'txt', 'md', 'html', 'xml'
    ]);

    // 从后端加载支持的文件类型（如果API提供）
    useEffect(() => {
        const loadSupportedFileTypes = async () => {
            try {
                // 尝试从API获取支持的文件类型
                const response = await fetch('/api/documents/supported-types');
                if (response.ok) {
                    const types = await response.json();
                    if (Array.isArray(types) && types.length > 0) {
                        setSupportedFileTypes(types);
                    }
                }
            } catch (err) {
                // 如果API不存在或失败，使用默认值
                console.log('使用默认文件类型列表');
            }
        };
        loadSupportedFileTypes();
    }, []);

    // 首次加载
    useEffect(() => {
        loadDocuments();
    }, []);

    // 当分页、排序、搜索参数改变时重新加载
    useEffect(() => {
        if (!loading) {  // 避免首次加载时重复调用
            loadDocuments();
        }
    }, [currentPage, pageSize, sortBy, sortOrder, filterText, showAdvancedSearch]);

    const loadDocuments = async () => {
        setLoading(true);
        setError(null);

        try {
            console.log(t('logLoadingDocs'), `- 页码: ${currentPage}, 每页: ${pageSize}, 排序: ${sortBy} ${sortOrder}, 搜索: '${filterText}'`);

            // 构建过滤参数
            const filters = showAdvancedSearch ? {
                search: advancedFilters.search,
                searchMode: advancedFilters.searchMode,
                fileTypes: advancedFilters.fileTypes.join(','),
                minSize: advancedFilters.minSize ? parseInt(advancedFilters.minSize) * 1024 * 1024 : 0,
                maxSize: advancedFilters.maxSize ? parseInt(advancedFilters.maxSize) * 1024 * 1024 : 1099511627776,
                indexed: advancedFilters.indexed,
                startDate: advancedFilters.startDate,
                endDate: advancedFilters.endDate
            } : {
                search: filterText || '',
                searchMode: 'contains'
            };

            const result = await api.listDocuments(currentPage, pageSize, sortBy, sortOrder, filters);
            console.log(t('logDocsResponse'), result);

            if (result.success) {
                setDocuments(result.documents || []);
                setTotalCount(result.total || 0);
                setTotalPages(result.totalPages || 0);
                console.log(t('logDocsLoaded'), result.documents?.length || 0, t('logDocsCount'), `共 ${result.total} 个`);
            } else {
                const errorMsg = result.message || t('docsGetListError');
                console.error(t('logLoadFailed'), errorMsg);
                setError(errorMsg);
            }
        } catch (err) {
            console.error(t('logLoadException'), err);
            const errorMsg = err.response?.data?.message || err.message || t('docsLoadError');
            setError(errorMsg);
        } finally {
            setLoading(false);
        }
    };

    const handleFileSelect = async (event) => {
        const files = event.target.files;
        if (!files || files.length === 0) return;

        setUploading(true);
        setUploadProgress({ total: files.length, current: 0, success: 0, failed: 0 });

        for (let i = 0; i < files.length; i++) {
            const file = files[i];
            try {
                await api.uploadDocument(file);
                setUploadProgress(prev => ({ ...prev, current: i + 1, success: prev.success + 1 }));
            } catch (err) {
                setUploadProgress(prev => ({ ...prev, current: i + 1, failed: prev.failed + 1 }));
            }
        }

        setUploading(false);
        setTimeout(() => setUploadProgress(null), 3000);

        // 上传后回到第一页重新加载
        setCurrentPage(1);
        loadDocuments();

        // 清空文件输入
        event.target.value = '';
    };

    const handleDelete = async (fileName) => {
        if (!confirm(t('docsDeleteConfirm'))) {
            return;
        }

        try {
            const result = await api.deleteDocument(fileName);
            if (result.success) {
                alert(t('docsDeleteSuccess'));
                // 删除后重新加载当前页
                loadDocuments();
            } else {
                alert(t('docsDeleteError') + ': ' + result.message);
            }
        } catch (err) {
            alert(t('docsDeleteError') + ': ' + err.message);
        }
    };

    const formatFileSize = (bytes) => {
        if (bytes < 1024) return bytes + ' B';
        if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(2) + ' KB';
        return (bytes / 1024 / 1024).toFixed(2) + ' MB';
    };

    // 跳转页面
    const goToPage = (page) => {
        if (page < 1) page = 1;
        if (page > totalPages) page = totalPages;
        setCurrentPage(page);
    };

    // 搜索框改变时重置到第一页
    const handleSearchChange = (value) => {
        setFilterText(value);
        setCurrentPage(1);
    };

    // 排序改变时重置到第一页
    const handleSortChange = (field, order) => {
        if (field) setSortBy(field);
        if (order) setSortOrder(order);
        setCurrentPage(1);
    };

    // 每页数量改变时重置到第一页
    const handlePageSizeChange = (size) => {
        setPageSize(size);
        setCurrentPage(1);
    };

    // 高级搜索辅助函数
    const updateFilter = (key, value) => {
        setAdvancedFilters(prev => ({
            ...prev,
            [key]: value
        }));
    };

    const toggleFileType = (type, checked) => {
        setAdvancedFilters(prev => ({
            ...prev,
            fileTypes: checked
                ? [...prev.fileTypes, type]
                : prev.fileTypes.filter(t => t !== type)
        }));
    };

    const applyFilters = () => {
        setCurrentPage(1);
        loadDocuments();
    };

    const resetFilters = () => {
        setAdvancedFilters({
            search: '',
            searchMode: 'contains',
            fileTypes: [],
            minSize: '',
            maxSize: '',
            indexed: 'all',
            startDate: '',
            endDate: ''
        });
        setCurrentPage(1);
    };

    const hasActiveFilters = () => {
        return advancedFilters.search !== '' ||
               advancedFilters.fileTypes.length > 0 ||
               advancedFilters.minSize !== '' ||
               advancedFilters.maxSize !== '' ||
               advancedFilters.indexed !== 'all' ||
               advancedFilters.startDate !== '' ||
               advancedFilters.endDate !== '';
    };

    const getActiveFilterCount = () => {
        let count = 0;
        if (advancedFilters.search) count++;
        if (advancedFilters.fileTypes.length > 0) count++;
        if (advancedFilters.minSize || advancedFilters.maxSize) count++;
        if (advancedFilters.indexed !== 'all') count++;
        if (advancedFilters.startDate || advancedFilters.endDate) count++;
        return count;
    };

    return (
        <div>
            {/* 上传区域 */}
            <div style={{ marginBottom: '30px', padding: '20px', background: '#f8f9ff', borderRadius: '8px' }}>
                <h3 style={{ marginBottom: '15px' }}>{t('docsUploadArea')}</h3>
                <input
                    type="file"
                    id="fileInput"
                    multiple
                    accept=".xlsx,.xls,.docx,.doc,.pptx,.ppt,.pdf,.txt,.md,.html,.xml"
                    onChange={handleFileSelect}
                    disabled={uploading}
                    style={{ display: 'none' }}
                />
                <label htmlFor="fileInput" className="btn btn-primary" style={{ display: 'inline-block', cursor: uploading ? 'not-allowed' : 'pointer', opacity: uploading ? 0.5 : 1 }}>
                    {uploading ? t('docsUploading') : t('docsUploadButton')}
                </label>
                <p style={{ marginTop: '10px', fontSize: '14px', color: '#666' }}>
                    {t('docsUploadFormats')}
                </p>

                {uploadProgress && (
                    <div style={{ marginTop: '15px', padding: '10px', background: 'white', borderRadius: '4px' }}>
                        <div>{t('docsUploadProgress')} {uploadProgress.current}/{uploadProgress.total}</div>
                        <div style={{ fontSize: '14px', color: '#666', marginTop: '5px' }}>
                            {t('docsUploadSuccessCount')} {uploadProgress.success} | {t('docsUploadFailedCount')} {uploadProgress.failed}
                        </div>
                    </div>
                )}
            </div>

            {/* 文档列表区域 */}
            <div>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '15px' }}>
                    <h3>{t('docsListTitle')} {!loading && totalCount > 0 && `(${documents.length}/${totalCount})`}</h3>
                    <button
                        className="btn btn-secondary"
                        onClick={loadDocuments}
                        disabled={loading}
                    >
                        {loading ? t('docsListRefreshing') : t('docsListRefresh')}
                    </button>
                </div>

                {/* 文件名过滤输入框 */}
                {!loading && totalCount > 0 && (
                    <div style={{ marginBottom: '15px' }}>
                        {/* 搜索模式切换按钮 */}
                        <div style={{marginBottom: '10px'}}>
                            <button
                                className="btn btn-secondary"
                                onClick={() => setShowAdvancedSearch(!showAdvancedSearch)}
                                style={{marginRight: '10px'}}
                            >
                                {showAdvancedSearch ? t('docsSimpleSearch') : t('docsAdvancedSearch')}
                            </button>
                        </div>

                        {/* 简单搜索 */}
                        {!showAdvancedSearch && (
                            <input
                                type="text"
                                className="input-field"
                                placeholder={t('docsFilterPlaceholder')}
                                value={filterText}
                                onChange={(e) => handleSearchChange(e.target.value)}
                                style={{ width: '100%', marginBottom: '10px' }}
                            />
                        )}

                        {/* 高级搜索面板 */}
                        {showAdvancedSearch && (
                            <div className="advanced-search-panel" style={{
                                background: '#f8f9fa',
                                padding: '20px',
                                borderRadius: '8px',
                                marginBottom: '15px',
                                border: '2px solid #667eea'
                            }}>
                                {/* 文件名搜索 + 搜索模式 */}
                                <div style={{marginBottom: '15px', display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center'}}>
                                    <label style={{minWidth: '80px', fontWeight: '600'}}>{t('docsFilterPlaceholder')}</label>
                                    <input
                                        type="text"
                                        className="input-field"
                                        style={{flex: '1', minWidth: '200px'}}
                                        placeholder={t('docsFilterPlaceholder')}
                                        value={advancedFilters.search}
                                        onChange={(e) => updateFilter('search', e.target.value)}
                                    />
                                    <select
                                        className="input-field"
                                        style={{width: 'auto'}}
                                        value={advancedFilters.searchMode}
                                        onChange={(e) => updateFilter('searchMode', e.target.value)}
                                    >
                                        <option value="contains">{t('docsSearchModeContains')}</option>
                                        <option value="exact">{t('docsSearchModeExact')}</option>
                                        <option value="regex">{t('docsSearchModeRegex')}</option>
                                    </select>
                                </div>

                                {/* 文件类型多选 - 紧凑型下拉框 */}
                                <div style={{marginBottom: '15px'}}>
                                    <label style={{fontWeight: '600', display: 'block', marginBottom: '8px'}}>
                                        {t('docsFileTypeFilter')}
                                        {advancedFilters.fileTypes.length > 0 && (
                                            <span style={{marginLeft: '8px', color: '#667eea', fontSize: '14px'}}>
                                                ({advancedFilters.fileTypes.length} {t('docsSelected')})
                                            </span>
                                        )}
                                    </label>
                                    <div style={{display: 'flex', gap: '10px', alignItems: 'center', flexWrap: 'wrap'}}>
                                        {/* 下拉多选框 */}
                                        <select
                                            className="input-field"
                                            style={{minWidth: '200px', flex: '1'}}
                                            multiple
                                            size="1"
                                            value={advancedFilters.fileTypes}
                                            onChange={(e) => {
                                                const selected = Array.from(e.target.selectedOptions, option => option.value);
                                                updateFilter('fileTypes', selected);
                                            }}
                                            onFocus={(e) => e.target.size = Math.min(supportedFileTypes.length, 8)}
                                            onBlur={(e) => e.target.size = 1}
                                        >
                                            {supportedFileTypes.map(type => (
                                                <option key={type} value={type}>
                                                    {type.toUpperCase()}
                                                </option>
                                            ))}
                                        </select>

                                        {/* 快捷操作按钮 */}
                                        <button
                                            type="button"
                                            className="btn-secondary"
                                            style={{padding: '6px 12px', fontSize: '13px'}}
                                            onClick={() => updateFilter('fileTypes', [...supportedFileTypes])}
                                        >
                                            {t('docsSelectAll')}
                                        </button>
                                        <button
                                            type="button"
                                            className="btn-secondary"
                                            style={{padding: '6px 12px', fontSize: '13px'}}
                                            onClick={() => updateFilter('fileTypes', [])}
                                        >
                                            {t('docsClearAll')}
                                        </button>
                                    </div>

                                    {/* 已选择的文件类型标签显示 */}
                                    {advancedFilters.fileTypes.length > 0 && (
                                        <div style={{marginTop: '8px', display: 'flex', flexWrap: 'wrap', gap: '6px'}}>
                                            {advancedFilters.fileTypes.map(type => (
                                                <span
                                                    key={type}
                                                    style={{
                                                        display: 'inline-flex',
                                                        alignItems: 'center',
                                                        gap: '4px',
                                                        padding: '4px 8px',
                                                        background: '#667eea',
                                                        color: 'white',
                                                        borderRadius: '4px',
                                                        fontSize: '12px',
                                                        fontWeight: '500'
                                                    }}
                                                >
                                                    {type.toUpperCase()}
                                                    <button
                                                        type="button"
                                                        onClick={() => toggleFileType(type, false)}
                                                        style={{
                                                            background: 'none',
                                                            border: 'none',
                                                            color: 'white',
                                                            cursor: 'pointer',
                                                            padding: '0',
                                                            marginLeft: '2px',
                                                            fontSize: '14px',
                                                            lineHeight: '1'
                                                        }}
                                                    >
                                                        ×
                                                    </button>
                                                </span>
                                            ))}
                                        </div>
                                    )}
                                </div>

                                {/* 文件大小范围 + 索引状态 (同一行，更紧凑) */}
                                <div style={{marginBottom: '15px', display: 'flex', gap: '20px', flexWrap: 'wrap', alignItems: 'center'}}>
                                    {/* 文件大小 */}
                                    <div style={{display: 'flex', gap: '10px', alignItems: 'center'}}>
                                        <label style={{fontWeight: '600', whiteSpace: 'nowrap'}}>{t('docsFileSizeFilter')}</label>
                                        <input
                                            type="number"
                                            className="input-field"
                                            style={{width: '100px'}}
                                            placeholder={t('docsFileSizeMin')}
                                            value={advancedFilters.minSize}
                                            onChange={(e) => updateFilter('minSize', e.target.value)}
                                            min="0"
                                        />
                                        <span>-</span>
                                        <input
                                            type="number"
                                            className="input-field"
                                            style={{width: '100px'}}
                                            placeholder={t('docsFileSizeMax')}
                                            value={advancedFilters.maxSize}
                                            onChange={(e) => updateFilter('maxSize', e.target.value)}
                                            min="0"
                                        />
                                        <span>{t('docsFileSizeUnit')}</span>
                                    </div>

                                    {/* 索引状态 (缩小宽度) */}
                                    <div style={{display: 'flex', gap: '10px', alignItems: 'center'}}>
                                        <label style={{fontWeight: '600', whiteSpace: 'nowrap'}}>{t('docsIndexedFilter')}</label>
                                        <select
                                            className="input-field"
                                            style={{width: '120px'}}
                                            value={advancedFilters.indexed}
                                            onChange={(e) => updateFilter('indexed', e.target.value)}
                                        >
                                            <option value="all">{t('docsIndexedAll')}</option>
                                            <option value="true">{t('docsIndexedYes')}</option>
                                            <option value="false">{t('docsIndexedNo')}</option>
                                        </select>
                                    </div>
                                </div>

                                {/* 日期范围 (调小一点) */}
                                <div style={{marginBottom: '15px', display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center'}}>
                                    <label style={{fontWeight: '600', whiteSpace: 'nowrap'}}>{t('docsDateFilter')}</label>
                                    <div style={{transform: 'scale(0.95)', transformOrigin: 'left center'}}>
                                        <DatePicker
                                            value={advancedFilters.startDate}
                                            onChange={(date) => updateFilter('startDate', date)}
                                            placeholder={t('docsDateStart')}
                                            language={language}
                                        />
                                    </div>
                                    <span>-</span>
                                    <div style={{transform: 'scale(0.95)', transformOrigin: 'left center'}}>
                                        <DatePicker
                                            value={advancedFilters.endDate}
                                            onChange={(date) => updateFilter('endDate', date)}
                                            placeholder={t('docsDateEnd')}
                                            language={language}
                                        />
                                    </div>
                                </div>

                                {/* 操作按钮 */}
                                <div style={{display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '20px'}}>
                                    <button onClick={applyFilters} className="btn btn-primary">
                                        {t('docsApplyFilter')}
                                    </button>
                                    <button onClick={resetFilters} className="btn btn-secondary">
                                        {t('docsResetFilter')}
                                    </button>
                                </div>
                            </div>
                        )}

                        {/* 当前激活的筛选条件显示 */}
                        {showAdvancedSearch && hasActiveFilters() && (
                            <div style={{
                                background: '#e3f2fd',
                                padding: '10px 15px',
                                borderRadius: '6px',
                                marginBottom: '15px',
                                borderLeft: '4px solid #2196f3',
                                fontSize: '14px'
                            }}>
                                <span style={{fontWeight: '600'}}>{t('docsActiveFilters')}: {getActiveFilterCount()} {t('docsFilterCount')}</span>
                            </div>
                        )}

                        {/* 排序和分页控制栏 */}
                        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', alignItems: 'center', background: '#f8f9fa', padding: '10px', borderRadius: '6px' }}>
                            {/* 排序方式 */}
                            <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                                <label style={{ fontSize: '13px', color: '#666' }}>{t('docsSortBy')}:</label>
                                <select
                                    className="input-field"
                                    style={{ width: 'auto', padding: '5px 8px', fontSize: '13px' }}
                                    value={sortBy}
                                    onChange={(e) => handleSortChange(e.target.value, null)}
                                >
                                    <option value="date">{t('docsSortByDate')}</option>
                                    <option value="name">{t('docsSortByName')}</option>
                                    <option value="size">{t('docsSortBySize')}</option>
                                    <option value="type">{t('docsSortByType')}</option>
                                </select>
                                <select
                                    className="input-field"
                                    style={{ width: 'auto', padding: '5px 8px', fontSize: '13px' }}
                                    value={sortOrder}
                                    onChange={(e) => handleSortChange(null, e.target.value)}
                                >
                                    <option value="desc">{t('docsSortDesc')}</option>
                                    <option value="asc">{t('docsSortAsc')}</option>
                                </select>
                            </div>

                            {/* 每页显示数量 */}
                            <div style={{ display: 'flex', alignItems: 'center', gap: '5px' }}>
                                <label style={{ fontSize: '13px', color: '#666' }}>{t('docsPageSize')}:</label>
                                <select
                                    className="input-field"
                                    style={{ width: 'auto', padding: '5px 8px', fontSize: '13px' }}
                                    value={pageSize}
                                    onChange={(e) => handlePageSizeChange(Number(e.target.value))}
                                >
                                    <option value={10}>10 {t('docsPageSizeItems')}</option>
                                    <option value={20}>20 {t('docsPageSizeItems')}</option>
                                    <option value={50}>50 {t('docsPageSizeItems')}</option>
                                    <option value={100}>100 {t('docsPageSizeItems')}</option>
                                    <option value={-1}>{t('docsShowAll')}</option>
                                </select>
                            </div>

                            {/* 统计信息 */}
                            <div style={{ marginLeft: 'auto', fontSize: '13px', color: '#666' }}>
                                {filterText ? (
                                    <>
                                        {t('docsFilterResult')} {totalCount} {t('logDocsCount')}
                                        <button
                                            onClick={() => handleSearchChange('')}
                                            style={{
                                                marginLeft: '10px',
                                                background: 'none',
                                                border: 'none',
                                                color: '#667eea',
                                                cursor: 'pointer',
                                                textDecoration: 'underline',
                                                fontSize: '13px'
                                            }}
                                        >
                                            {t('docsFilterClear')}
                                        </button>
                                    </>
                                ) : (
                                    <>
                                        {t('docsPaginationTotal')} {totalCount} {t('logDocsCount')}
                                    </>
                                )}
                            </div>
                        </div>
                    </div>
                )}

                {/* 加载状态 */}
                {loading && (
                    <div className="loading">
                        <div className="spinner"></div>
                        <p>{t('docsLoadingList')}</p>
                    </div>
                )}

                {/* 错误状态 */}
                {error && !loading && (
                    <div className="error">
                        {t('qaErrorPrefix')} {error}
                        <button
                            className="btn btn-secondary"
                            onClick={loadDocuments}
                            style={{ marginTop: '10px', display: 'block' }}
                        >
                            {t('docsRetry')}
                        </button>
                    </div>
                )}

                {/* 空状态 */}
                {!error && !loading && documents.length === 0 && (
                    <div className="empty-state">
                        <div className="empty-state-icon">📁</div>
                        <p>{t('docsListEmpty')}</p>
                        <p style={{ fontSize: '14px', marginTop: '10px', color: '#ccc' }}>
                            {t('docsEmptyHint')}
                        </p>
                    </div>
                )}

                {/* 文档列表 */}
                {!error && !loading && documents.length > 0 && (
                    <>
                        <div style={{ display: 'grid', gap: '10px' }}>
                            {documents.map((doc, index) => (
                                <div key={index} className="document-card" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                                    <div style={{ flex: 1 }}>
                                        <div className="document-title">
                                            📄 {doc.fileName}
                                        </div>
                                        <div style={{ fontSize: '13px', color: '#999', marginTop: '5px' }}>
                                            📦 {formatFileSize(doc.fileSize)} |
                                            📅 {doc.uploadTime} |
                                            🏷️ {doc.fileType.toUpperCase()}
                                            {doc.indexed && <span style={{ marginLeft: '5px' }}>| ✅ {t('docsIndexed')}</span>}
                                        </div>
                                    </div>
                                    <button
                                        className="btn btn-secondary"
                                        onClick={() => handleDelete(doc.fileName)}
                                        style={{
                                            padding: '8px 16px',
                                            background: '#ff4d4f',
                                            color: 'white',
                                            marginLeft: '10px'
                                        }}
                                    >
                                        {t('docsListDelete')}
                                    </button>
                                </div>
                            ))}
                        </div>

                        {/* 分页控制 */}
                        {pageSize !== -1 && totalPages > 1 && (
                            <div className="pagination-container">
                                <button
                                    className="pagination-btn"
                                    onClick={() => goToPage(currentPage - 1)}
                                    disabled={currentPage === 1}
                                >
                                    {t('docsPaginationPrev')}
                                </button>

                                <div className="pagination-info">
                                    <span>
                                        {t('docsPagination')} {currentPage} {t('docsPaginationPage')} / {t('docsPaginationTotal')} {totalPages} {t('docsPaginationPage')}
                                    </span>
                                    <span style={{ margin: '0 10px', color: '#ccc' }}>|</span>
                                    <input
                                        type="number"
                                        className="pagination-jump-input"
                                        min="1"
                                        max={totalPages}
                                        placeholder={currentPage.toString()}
                                        onKeyPress={(e) => {
                                            if (e.key === 'Enter') {
                                                const page = parseInt(e.target.value);
                                                if (page && page >= 1 && page <= totalPages) {
                                                    goToPage(page);
                                                    e.target.value = '';
                                                }
                                            }
                                        }}
                                    />
                                    <button
                                        className="pagination-jump-btn"
                                        onClick={(e) => {
                                            const input = e.target.previousElementSibling;
                                            const page = parseInt(input.value);
                                            if (page && page >= 1 && page <= totalPages) {
                                                goToPage(page);
                                                input.value = '';
                                            }
                                        }}
                                    >
                                        {t('docsPaginationJump')}
                                    </button>
                                </div>

                                <button
                                    className="pagination-btn"
                                    onClick={() => goToPage(currentPage + 1)}
                                    disabled={currentPage === totalPages}
                                >
                                    {t('docsPaginationNext')}
                                </button>
                            </div>
                        )}
                    </>
                )}

                {/* 提示信息 */}
                {!error && !loading && totalCount > 0 && (
                    <div style={{ marginTop: '20px', padding: '15px', background: '#fff3cd', borderRadius: '8px', fontSize: '14px' }}>
                        {t('docsUploadTip')}
                    </div>
                )}
            </div>
        </div>
    );
}

// 统计信息组件
function StatisticsTab() {
    const { t } = useTranslation();
    const [stats, setStats] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [rebuilding, setRebuilding] = useState(false);
    const [incrementalIndexing, setIncrementalIndexing] = useState(false);
    const [rebuildResult, setRebuildResult] = useState(null);

    useEffect(() => {
        loadStatistics();
    }, []);

    const loadStatistics = async () => {
        setLoading(true);
        setError(null);

        try {
            const result = await api.getStatistics();
            setStats(result);
        } catch (err) {
            setError(err.message || t('statsLoadError'));
        } finally {
            setLoading(false);
        }
    };

    const handleRebuild = async () => {
        if (!confirm(t('statsRebuildConfirm'))) {
            return;
        }

        setRebuilding(true);
        setError(null);
        setRebuildResult(null);

        try {
            const result = await api.rebuild();
            setRebuildResult(result);

            if (result.success) {
                // 重建成功后刷新统计信息
                setTimeout(() => {
                    loadStatistics();
                }, 1000);
            }
        } catch (err) {
            setError(err.message || t('statsRebuildError'));
        } finally {
            setRebuilding(false);
        }
    };

    const handleIncrementalIndex = async () => {
        if (!confirm(t('statsIncrementalConfirm'))) {
            return;
        }

        setIncrementalIndexing(true);
        setError(null);
        setRebuildResult(null);

        try {
            const result = await api.incrementalIndex();
            setRebuildResult(result);

            if (result.success) {
                // 增量索引成功后刷新统计信息
                setTimeout(() => {
                    loadStatistics();
                }, 1000);
            }
        } catch (err) {
            setError(err.message || t('statsIncrementalError'));
        } finally {
            setIncrementalIndexing(false);
        }
    };

    if (loading) {
        return (
            <div className="loading">
                <div className="spinner"></div>
                <p>{t('statsLoadingStats')}</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="error">
                {t('qaErrorPrefix')} {error}
                <button
                    className="btn btn-secondary"
                    onClick={loadStatistics}
                    style={{ marginTop: '10px' }}
                >
                    {t('statsRetry')}
                </button>
            </div>
        );
    }

    return (
        <div>
            <div className="statistics-grid">
                <div className="stat-card">
                    <div className="stat-value">{stats.documentCount}</div>
                    <div className="stat-label">{t('statsDocCount')}</div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">{stats.indexedDocumentCount}</div>
                    <div className="stat-label">{t('statsIndexedCount')}</div>
                </div>
                <div className="stat-card">
                    <div className="stat-value">
                        {stats.documentCount > 0
                            ? Math.round((stats.indexedDocumentCount / stats.documentCount) * 100)
                            : 0}%
                    </div>
                    <div className="stat-label">{t('statsIndexProgress')}</div>
                </div>
            </div>

            {(rebuilding || incrementalIndexing) && (
                <div className="loading" style={{ marginTop: '30px' }}>
                    <div className="spinner"></div>
                    <p>{rebuilding ? t('statsRebuilding') : t('statsIncrementalIndexing')}</p>
                    <p style={{ fontSize: '14px', color: '#999', marginTop: '10px' }}>
                        {rebuilding ? t('statsRebuildWait') : t('statsIncrementalWait')}
                    </p>
                </div>
            )}

            {rebuildResult && !rebuilding && !incrementalIndexing && (
                <div className={rebuildResult.success ? 'answer-card' : 'error'} style={{ marginTop: '30px' }}>
                    <h3>{rebuildResult.success ? t('statsSuccess') : t('statsFailed')}</h3>
                    <div style={{ marginTop: '10px' }}>
                        <p>{rebuildResult.message}</p>
                        {rebuildResult.success && rebuildResult.processedFiles > 0 && (
                            <div style={{ marginTop: '15px', fontSize: '14px' }}>
                                <p>{t('statsProcessedFiles')}: {rebuildResult.processedFiles} {t('statsCount')}</p>
                                <p>{t('statsTotalDocs')}: {rebuildResult.totalDocuments} {t('statsCount')}</p>
                                <p>{t('statsDuration')}: {(rebuildResult.durationMs / 1000).toFixed(2)} {t('statsSeconds')}</p>
                            </div>
                        )}
                        {rebuildResult.suggestion && (
                            <p style={{ marginTop: '10px', fontSize: '14px', color: '#666' }}>
                                {t('statsSuggestion')} {rebuildResult.suggestion}
                            </p>
                        )}
                    </div>
                </div>
            )}

            <div style={{ marginTop: '30px' }}>
                <div style={{ padding: '20px', background: '#f8f9ff', borderRadius: '8px', marginBottom: '20px' }}>
                    <h4 style={{ marginBottom: '10px', color: '#667eea' }}>{t('statsIndexGuideTitle')}</h4>
                    <div style={{ fontSize: '14px', lineHeight: '1.8', color: '#666' }}>
                        <p style={{ marginBottom: '8px' }}>
                            {t('statsIncrementalDesc')}
                        </p>
                        <p>
                            {t('statsRebuildDesc')}
                        </p>
                    </div>
                </div>

                <div style={{ textAlign: 'center', display: 'flex', gap: '10px', justifyContent: 'center', flexWrap: 'wrap' }}>
                    <button
                        className="btn btn-secondary"
                        onClick={loadStatistics}
                        disabled={rebuilding || incrementalIndexing}
                    >
                        {t('statsRefresh')}
                    </button>
                    <button
                        className="btn btn-primary"
                        onClick={handleIncrementalIndex}
                        disabled={rebuilding || incrementalIndexing}
                        style={{
                            background: incrementalIndexing ? '#ccc' : 'linear-gradient(135deg, #52c41a 0%, #73d13d 100%)'
                        }}
                    >
                        {incrementalIndexing ? t('statsIndexing') : t('statsIncrementalIndex')}
                    </button>
                    <button
                        className="btn btn-primary"
                        onClick={handleRebuild}
                        disabled={rebuilding || incrementalIndexing}
                    >
                        {rebuilding ? t('statsIndexingProgress') : t('statsRebuildIndex')}
                    </button>
                </div>
            </div>
        </div>
    );
}

// 主应用组件
function App() {
    const { t, toggleLanguage, language } = useTranslation();
    const [activeTab, setActiveTab] = useState('qa');
    const [health, setHealth] = useState(null);

    useEffect(() => {
        checkHealth();
    }, []);

    const checkHealth = async () => {
        try {
            const result = await api.health();
            setHealth(result);
        } catch (err) {
            setHealth({ status: 'DOWN', message: 'Service connection failed' });
        }
    };

    return (
        <div className="app-container">
            {/* 语言切换按钮 */}
            <div className="language-toggle">
                <button onClick={toggleLanguage}>
                    {t('langToggle')}
                </button>
            </div>

            <header className="header">
                <h1>{t('title')}</h1>
                <p>{t('subtitle')}</p>
                {health && (
                    <div style={{ marginTop: '10px', fontSize: '14px' }}>
                        {t('status')}: {health.status === 'UP' ? t('statusOnline') : t('statusOffline')}
                    </div>
                )}
            </header>

            <main className="main-content">
                <div className="tabs">
                    <button
                        className={`tab ${activeTab === 'qa' ? 'active' : ''}`}
                        onClick={() => setActiveTab('qa')}
                    >
                        {t('tabQA')}
                    </button>
                    <button
                        className={`tab ${activeTab === 'search' ? 'active' : ''}`}
                        onClick={() => setActiveTab('search')}
                    >
                        {t('tabSearch')}
                    </button>
                    <button
                        className={`tab ${activeTab === 'documents' ? 'active' : ''}`}
                        onClick={() => setActiveTab('documents')}
                    >
                        {t('tabDocuments')}
                    </button>
                    <button
                        className={`tab ${activeTab === 'stats' ? 'active' : ''}`}
                        onClick={() => setActiveTab('stats')}
                    >
                        {t('tabStats')}
                    </button>
                </div>

                <div className="tab-content">
                    {activeTab === 'qa' && <QATab />}
                    {activeTab === 'search' && <SearchTab />}
                    {activeTab === 'documents' && <DocumentsTab />}
                    {activeTab === 'stats' && <StatisticsTab />}
                </div>
            </main>

            <footer className="footer">
                <p>{t('footerText')}</p>
            </footer>
        </div>
    );
}

// 渲染应用
ReactDOM.render(
    <LanguageProvider>
        <App />
    </LanguageProvider>,
    document.getElementById('root')
);
