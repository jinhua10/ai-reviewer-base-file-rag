/**
 * 语言翻译字典 / Language Translation Dictionary
 *
 * 支持中文(zh)和英文(en)两种语言
 * Supports Chinese (zh) and English (en)
 *
 * @author AI Reviewer Team
 * @since 2025-11-26
 */

const translations = {
    zh: {
        // Header
        title: '🤖 知识库问答系统',
        subtitle: '基于 LocalFileRAG 的智能问答平台',
        statusOnline: '✅ 运行中',
        statusOffline: '❌ 离线',
        status: '状态',

        // Tabs
        tabQA: '💬 智能问答',
        tabSearch: '🔍 文档搜索',
        tabDocuments: '📁 文档管理',
        tabStats: '📊 统计信息',

        // QA Tab
        qaPlaceholder: '请输入您的问题...',
        qaButton: '🤔 提问',
        qaThinking: '思考中...',
        qaAIThinking: 'AI 正在思考...',
        qaAnswer: '💡 回答',
        qaSources: '📚 参考来源',
        qaDownload: '💾 下载',
        qaBatchDownload: '📦 批量下载全部',
        qaResponseTime: '⏱️ 响应时间',
        qaEmptyIcon: '💭',
        qaEmptyText: '在上方输入框提出您的问题',
        qaEmptyExample: '示例: "文档的主要内容是什么？"',
        qaErrorPrefix: '❌',
        qaInputError: '请输入问题',
        qaDownloadError: '下载失败',
        qaBatchDownloadError: '批量下载失败',
        qaRequestError: '请求失败，请检查服务是否正常运行',

        // Search Tab
        searchPlaceholder: '请输入搜索关键词',
        searchButton: '🔍 搜索',
        searchSearching: '搜索中...',
        searchResults: '搜索结果',
        searchNoResults: '未找到相关文档',
        searchEmptyIcon: '🔍',
        searchEmptyText: '输入关键词搜索文档',
        searchEmptyExample: '示例: "技术文档"',
        searchResultsCount: '个结果',
        searchScore: '相关度',
        searchResultsLimit: '最多显示',
        searchInputError: '请输入搜索关键词',
        searchError: '搜索失败',

        // Documents Tab
        docsTitle: '📁 文档管理',
        docsUploadArea: '📤 上传文档',
        docsUploadHint: '点击或拖拽文件到此区域上传',
        docsUploadSupport: '支持 PDF、Word、TXT、Markdown、Excel、PowerPoint、图片等格式',
        docsUploadButton: '选择文件',
        docsUploading: '上传中...',
        docsUploadSuccess: '✅ 上传成功',
        docsUploadError: '上传失败',
        docsListTitle: '📚 已上传文档',
        docsListEmpty: '暂无文档',
        docsListRefresh: '🔄 刷新列表',
        docsListDelete: '🗑️ 删除',
        docsListDownload: '💾 下载',
        docsDeleteConfirm: '确定要删除这个文件吗？',
        docsDeleteSuccess: '删除成功',
        docsDeleteError: '删除失败',
        docsSelectFile: '请先选择文件',
        docsFiles: '个文件',

        // Stats Tab
        statsTitle: '📊 系统统计',
        statsDocCount: '📄 文档总数',
        statsIndexedCount: '✅ 已索引文档',
        statsCacheHitRate: '🎯 缓存命中率',
        statsIndexProgress: '✅ 索引完成度',
        statsRefresh: '🔄 刷新统计',
        statsIncrementalIndex: '⚡ 增量索引',
        statsRebuildIndex: '🔨 重建索引',
        statsRebuilding: '正在重建知识库索引...',
        statsIncrementalIndexing: '正在执行增量索引...',
        statsRebuildWait: '这可能需要几分钟时间，请耐心等待',
        statsIncrementalWait: '只处理新增和修改的文档，速度较快',
        statsIndexing: '⏳ 索引中...',
        statsIndexingProgress: '⏳ 重建中...',
        statsSuccess: '✅ 操作成功',
        statsFailed: '❌ 操作失败',
        statsProcessedFiles: '📄 处理文件',
        statsTotalDocs: '📚 总文档数',
        statsDuration: '⏱️ 耗时',
        statsSuggestion: '💡',
        statsIndexGuideTitle: '📖 索引说明',
        statsIncrementalDesc: '🔄 增量索引：只处理新增和修改的文档，性能更优，推荐日常使用',
        statsRebuildDesc: '🔨 重建索引：完全重建所有文档索引，耗时较长，适用于首次构建或大规模变更',
        statsSeconds: '秒',
        statsCount: '个',

        // Footer
        footerText: '© 2025 LocalFileRAG Knowledge QA System | Powered by Java & React',

        // Language Toggle
        langToggle: '🌐 English',
    },
    en: {
        // Header
        title: '🤖 Knowledge Base QA System',
        subtitle: 'Intelligent Q&A Platform Based on LocalFileRAG',
        statusOnline: '✅ Online',
        statusOffline: '❌ Offline',
        status: 'Status',

        // Tabs
        tabQA: '💬 Q&A',
        tabSearch: '🔍 Search',
        tabDocuments: '📁 Documents',
        tabStats: '📊 Statistics',

        // QA Tab
        qaPlaceholder: 'Enter your question...',
        qaButton: '🤔 Ask',
        qaThinking: 'Thinking...',
        qaAIThinking: 'AI is thinking...',
        qaAnswer: '💡 Answer',
        qaSources: '📚 References',
        qaDownload: '💾 Download',
        qaBatchDownload: '📦 Download All',
        qaResponseTime: '⏱️ Response Time',
        qaEmptyIcon: '💭',
        qaEmptyText: 'Enter your question in the input box above',
        qaEmptyExample: 'Example: "What is the main content of the document?"',
        qaErrorPrefix: '❌',
        qaInputError: 'Please enter a question',
        qaDownloadError: 'Download failed',
        qaBatchDownloadError: 'Batch download failed',
        qaRequestError: 'Request failed, please check if the service is running',

        // Search Tab
        searchPlaceholder: 'Enter search keywords',
        searchButton: '🔍 Search',
        searchSearching: 'Searching...',
        searchResults: 'Search Results',
        searchNoResults: 'No documents found',
        searchEmptyIcon: '🔍',
        searchEmptyText: 'Enter keywords to search documents',
        searchEmptyExample: 'Example: "technical documentation"',
        searchResultsCount: 'results',
        searchScore: 'Score',
        searchResultsLimit: 'Limit',
        searchInputError: 'Please enter search keywords',
        searchError: 'Search failed',

        // Documents Tab
        docsTitle: '📁 Document Management',
        docsUploadArea: '📤 Upload Documents',
        docsUploadHint: 'Click or drag files to this area to upload',
        docsUploadSupport: 'Supports PDF, Word, TXT, Markdown, Excel, PowerPoint, Images, etc.',
        docsUploadButton: 'Select File',
        docsUploading: 'Uploading...',
        docsUploadSuccess: '✅ Upload Successful',
        docsUploadError: 'Upload failed',
        docsListTitle: '📚 Uploaded Documents',
        docsListEmpty: 'No documents',
        docsListRefresh: '🔄 Refresh',
        docsListDelete: '🗑️ Delete',
        docsListDownload: '💾 Download',
        docsDeleteConfirm: 'Are you sure you want to delete this file?',
        docsDeleteSuccess: 'Deleted successfully',
        docsDeleteError: 'Delete failed',
        docsSelectFile: 'Please select a file first',
        docsFiles: 'files',

        // Stats Tab
        statsTitle: '📊 System Statistics',
        statsDocCount: '📄 Total Documents',
        statsIndexedCount: '✅ Indexed Documents',
        statsCacheHitRate: '🎯 Cache Hit Rate',
        statsIndexProgress: '✅ Index Progress',
        statsRefresh: '🔄 Refresh',
        statsIncrementalIndex: '⚡ Incremental Index',
        statsRebuildIndex: '🔨 Rebuild Index',
        statsRebuilding: 'Rebuilding knowledge base index...',
        statsIncrementalIndexing: 'Performing incremental indexing...',
        statsRebuildWait: 'This may take a few minutes, please be patient',
        statsIncrementalWait: 'Only processes new and modified documents, faster',
        statsIndexing: '⏳ Indexing...',
        statsIndexingProgress: '⏳ Rebuilding...',
        statsSuccess: '✅ Operation Successful',
        statsFailed: '❌ Operation Failed',
        statsProcessedFiles: '📄 Processed Files',
        statsTotalDocs: '📚 Total Documents',
        statsDuration: '⏱️ Duration',
        statsSuggestion: '💡',
        statsIndexGuideTitle: '📖 Index Guide',
        statsIncrementalDesc: '🔄 Incremental Index: Only processes new and modified documents, better performance, recommended for daily use',
        statsRebuildDesc: '🔨 Rebuild Index: Completely rebuilds all document indexes, takes longer, suitable for initial setup or major changes',
        statsSeconds: 's',
        statsCount: '',

        // Footer
        footerText: '© 2025 LocalFileRAG Knowledge QA System | Powered by Java & React',

        // Language Toggle
        langToggle: '🌐 中文',
    }
};

// 如果在浏览器环境中，将 translations 暴露到全局
if (typeof window !== 'undefined') {
    window.translations = translations;
}

// 如果支持模块导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = translations;
}

