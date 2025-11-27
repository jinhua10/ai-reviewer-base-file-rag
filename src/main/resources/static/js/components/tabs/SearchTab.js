/**
 * 搜索标签页组件 / Search Tab Component
 * 负责文档搜索功能
 */

function SearchTab() {
    // 获取React hooks（避免重复声明）
    const { useState } = React;

    // 使用语言Hook
    const { t } = window.LanguageModule.useTranslation();

    // 状态管理
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
            const result = await window.api.search(query, limit);
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

    // 渲染组件
    return React.createElement('div', { className: 'search-section' },
        // 输入组
        React.createElement('div', { className: 'input-group' },
            React.createElement('input', {
                type: 'text',
                className: 'input-field',
                placeholder: t('searchPlaceholder'),
                value: query,
                onChange: (e) => setQuery(e.target.value),
                onKeyPress: handleKeyPress,
                disabled: loading
            }),
            React.createElement('select', {
                className: 'input-field',
                style: { flex: '0 0 120px' },
                value: limit,
                onChange: (e) => setLimit(Number(e.target.value))
            },
                React.createElement('option', { value: 5 }, '5'),
                React.createElement('option', { value: 10 }, '10'),
                React.createElement('option', { value: 20 }, '20'),
                React.createElement('option', { value: 50 }, '50')
            ),
            React.createElement('button', {
                className: 'btn btn-primary',
                onClick: handleSearch,
                disabled: loading
            }, loading ? t('searchSearching') : t('searchButton'))
        ),

        // 加载状态
        loading && React.createElement('div', { className: 'loading' },
            React.createElement('div', { className: 'spinner' }),
            React.createElement('p', null, t('searchSearching'))
        ),

        // 错误状态
        error && React.createElement('div', { className: 'error' },
            t('qaErrorPrefix') + ' ' + error
        ),

        // 搜索结果
        results && !loading && React.createElement('div', null,
            React.createElement('div', {
                style: { marginBottom: '15px', color: '#666' }
            },
                React.createElement('strong', null, results.total),
                ` ${t('searchResultsCount')}`
            ),

            React.createElement('div', { className: 'search-results' },
                ...results.documents.map((doc, index) =>
                    React.createElement('div', {
                        key: doc.id,
                        className: 'document-card'
                    },
                        React.createElement('div', { className: 'document-title' },
                            `📄 ${doc.title || `Document ${index + 1}`}`
                        ),
                        React.createElement('div', { className: 'document-excerpt' },
                            doc.excerpt
                        )
                    )
                )
            )
        ),

        // 空状态
        !results && !loading && !error && React.createElement('div', { className: 'empty-state' },
            React.createElement('div', { className: 'empty-state-icon' }, t('searchEmptyIcon')),
            React.createElement('p', null, t('searchEmptyText'))
        )
    );
}

// 导出到全局
if (typeof window !== 'undefined') {
    window.SearchTab = SearchTab;
}

// 如果支持模块导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = SearchTab;
}

