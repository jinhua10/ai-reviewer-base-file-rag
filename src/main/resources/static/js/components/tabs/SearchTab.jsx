/**
 * Search Tab Component / 搜索标签页组件
 * JSX 版本 - 使用 Babel 转译
 * 负责文档搜索功能
 *
 * @author AI Reviewer Team
 * @since 2025-11-28
 */

function SearchTab() {
    const { useState } = React;
    const { t } = window.LanguageModule.useTranslation();

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
                            <div key={doc.id} className="document-card search-result-card">
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

if (typeof window !== 'undefined') {
    window.SearchTab = SearchTab;
}

if (typeof module !== 'undefined' && module.exports) {
    module.exports = SearchTab;
}

