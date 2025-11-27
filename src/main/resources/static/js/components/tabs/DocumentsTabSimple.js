/**
 * DocumentsTab 简化测试版本
 * 用于快速验证组件是否能正常渲染
 */

function DocumentsTabSimple() {
    const { useState, useEffect } = React;
    const { t, language } = window.LanguageModule.useTranslation();

    const [documents, setDocuments] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);

    useEffect(() => {
        console.log('📄 DocumentsTabSimple mounted');
        loadDocuments();
    }, []);

    const loadDocuments = async () => {
        console.log('🔄 Loading documents...');
        setLoading(true);
        setError(null);

        try {
            const result = await window.api.listDocuments(1, 20, 'date', 'desc', {});
            console.log('✅ Documents loaded:', result);

            if (result.success) {
                setDocuments(result.documents || []);
            } else {
                setError(result.message || '加载失败');
            }
        } catch (err) {
            console.error('❌ Load error:', err);
            setError(err.message || '加载失败');
        } finally {
            setLoading(false);
        }
    };

    console.log('🎨 Rendering, state:', { loading, error, docCount: documents.length });

    if (loading) {
        return React.createElement('div', {
            style: { padding: '40px', textAlign: 'center' }
        },
            React.createElement('div', {
                className: 'spinner',
                style: {
                    border: '4px solid #f3f3f3',
                    borderTop: '4px solid #667eea',
                    borderRadius: '50%',
                    width: '40px',
                    height: '40px',
                    animation: 'spin 1s linear infinite',
                    margin: '0 auto 20px'
                }
            }),
            React.createElement('p', null, '加载中...')
        );
    }

    if (error) {
        return React.createElement('div', {
            style: {
                padding: '20px',
                background: '#fee',
                border: '2px solid #f00',
                borderRadius: '8px',
                color: '#c00'
            }
        }, '❌ 错误: ' + error);
    }

    if (documents.length === 0) {
        return React.createElement('div', {
            style: {
                padding: '40px',
                textAlign: 'center',
                background: 'white',
                borderRadius: '8px'
            }
        },
            React.createElement('div', {
                style: { fontSize: '48px', marginBottom: '20px' }
            }, '📄'),
            React.createElement('p', null, '暂无文档')
        );
    }

    return React.createElement('div', {
        style: { padding: '20px' }
    },
        React.createElement('h2', null, `📁 文档列表 (${documents.length} 个)`),
        React.createElement('div', {
            style: {
                display: 'grid',
                gap: '15px',
                marginTop: '20px'
            }
        },
            ...documents.map((doc, index) =>
                React.createElement('div', {
                    key: doc.id || index,
                    style: {
                        background: 'white',
                        padding: '15px',
                        borderRadius: '8px',
                        border: '1px solid #e0e0e0',
                        boxShadow: '0 2px 4px rgba(0,0,0,0.1)'
                    }
                },
                    React.createElement('div', {
                        style: {
                            display: 'flex',
                            justifyContent: 'space-between',
                            alignItems: 'center',
                            marginBottom: '10px'
                        }
                    },
                        React.createElement('h3', {
                            style: { margin: 0, fontSize: '16px' }
                        }, `📄 ${doc.name || 'Unknown'}`),
                        React.createElement('span', {
                            style: {
                                padding: '4px 8px',
                                borderRadius: '4px',
                                fontSize: '12px',
                                background: doc.indexed ? '#4caf50' : '#ff9800',
                                color: 'white'
                            }
                        }, doc.indexed ? '✓ 已索引' : '○ 未索引')
                    ),
                    React.createElement('div', {
                        style: {
                            fontSize: '13px',
                            color: '#666',
                            display: 'grid',
                            gap: '5px'
                        }
                    },
                        React.createElement('div', null, `大小: ${formatSize(doc.size)}`),
                        React.createElement('div', null, `类型: ${doc.type || 'unknown'}`),
                        React.createElement('div', null, `时间: ${formatDate(doc.uploadTime)}`)
                    )
                )
            )
        )
    );
}

function formatSize(bytes) {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
}

function formatDate(dateStr) {
    if (!dateStr) return '-';
    try {
        const date = new Date(dateStr);
        return date.toLocaleDateString() + ' ' + date.toLocaleTimeString();
    } catch {
        return dateStr;
    }
}

// 导出
if (typeof window !== 'undefined') {
    window.DocumentsTabSimple = DocumentsTabSimple;
}

