/**
 * API 配置和调用封装
 *
 * @author AI Reviewer Team
 * @since 2025-11-27
 */

// API 配置
const API_BASE_URL = 'http://localhost:8080/api/qa';
const API_DOCS_URL = 'http://localhost:8080/api/documents';
const API_FEEDBACK_URL = 'http://localhost:8080/api/feedback';
const API_DOCUMENT_QA_URL = 'http://localhost:8080/api/document-qa';
const API_LLM_RESULTS_URL = 'http://localhost:8080/api/llm-results';

/**
 * 获取当前语言设置 / Get current language setting
 * @returns {string} 语言代码 (zh/en) / Language code (zh/en)
 */
const getCurrentLang = () => {
    return localStorage.getItem('language') || 'zh';
};

// API 方法
const api = {
    // ========== 智能问答 ==========

    /**
     * 提问
     * @param {string} question - 问题文本
     * @returns {Promise<Object>} 回答结果
     */
    ask: async (question) => {
        const response = await axios.post(`${API_BASE_URL}/ask`, { question });
        return response.data;
    },

    /**
     * 使用指定文档批次进行问答（用于分页引用）
     * @param {string} question - 问题文本
     * @param {string} sessionId - 会话ID
     * @returns {Promise<Object>} 回答结果
     */
    askWithSessionDocuments: async (question, sessionId) => {
        const response = await axios.post(`${API_BASE_URL}/ask-with-session`, {
            question,
            sessionId
        });
        return response.data;
    },

    /**
     * 搜索文档
     * @param {string} query - 搜索关键词
     * @param {number} limit - 结果数量限制
     * @returns {Promise<Object>} 搜索结果
     */
    search: async (query, limit = 10) => {
        const response = await axios.get(`${API_BASE_URL}/search`, {
            params: { query, limit }
        });
        return response.data;
    },

    // ========== 系统状态 ==========

    /**
     * 获取统计信息 / Get statistics
     * @returns {Promise<Object>} 统计数据 / Statistics data
     */
    getStatistics: async () => {
        const response = await axios.get(`${API_BASE_URL}/statistics`, {
            params: { lang: getCurrentLang() } // 添加语言参数 / Add language parameter
        });
        return response.data;
    },

    /**
     * 健康检查 / Health check
     * @returns {Promise<Object>} 健康状态 / Health status
     */
    health: async () => {
        const response = await axios.get(`${API_BASE_URL}/health`, {
            params: { lang: getCurrentLang() } // 添加语言参数 / Add language parameter
        });
        return response.data;
    },

    /**
     * 重建索引 / Rebuild index
     * @returns {Promise<Object>} 重建结果 / Rebuild result
     */
    rebuild: async () => {
        const response = await axios.post(`${API_BASE_URL}/rebuild`, {
            lang: getCurrentLang() // 添加语言参数 / Add language parameter
        });
        return response.data;
    },

    /**
     * 增量索引 / Incremental index
     * @returns {Promise<Object>} 索引结果 / Index result
     */
    incrementalIndex: async () => {
        const response = await axios.post(`${API_BASE_URL}/incremental-index`, {
            lang: getCurrentLang() // 添加语言参数 / Add language parameter
        });
        return response.data;
    },

    // ========== 文档管理 ==========

    /**
     * 上传文档 / Upload document
     * @param {File} file - 文件对象 / File object
     * @returns {Promise<Object>} 上传结果 / Upload result
     */
    uploadDocument: async (file) => {
        const formData = new FormData();
        formData.append('file', file);
        formData.append('lang', getCurrentLang()); // 添加语言参数 / Add language parameter
        const response = await axios.post(`${API_DOCS_URL}/upload`, formData, {
            headers: { 'Content-Type': 'multipart/form-data' }
        });
        return response.data;
    },

    /**
     * 获取文档列表（支持分页、排序、高级搜索）
     * @param {number} page - 页码
     * @param {number} pageSize - 每页数量
     * @param {string} sortBy - 排序字段
     * @param {string} sortOrder - 排序方向
     * @param {Object} filters - 筛选条件
     * @returns {Promise<Object>} 文档列表
     */
    listDocuments: async (page, pageSize, sortBy, sortOrder, filters = {}) => {
        const params = {
            page: page || 1,
            pageSize: pageSize || 20,
            sortBy: sortBy || 'date',
            sortOrder: sortOrder || 'desc',
            search: filters.search || '',
            searchMode: filters.searchMode || 'contains',
            fileTypes: filters.fileTypes || '',
            minSize: filters.minSize || 0,
            // 使用 Number.MAX_SAFE_INTEGER (9007199254740991) 避免精度问题
            // 或者使用一个足够大的值如 1TB = 1099511627776
            maxSize: filters.maxSize || 1099511627776,
            indexed: filters.indexed || 'all',
            startDate: filters.startDate || '',
            endDate: filters.endDate || ''
        };
        const response = await axios.get(`${API_DOCS_URL}/list`, { params });
        return response.data;
    },

    /**
     * 删除文档 / Delete document
     * @param {string} fileName - 文件名 / File name
     * @returns {Promise<Object>} 删除结果 / Delete result
     */
    deleteDocument: async (fileName) => {
        const response = await axios.delete(`${API_DOCS_URL}/${encodeURIComponent(fileName)}`, {
            params: { lang: getCurrentLang() } // 添加语言参数 / Add language parameter
        });
        return response.data;
    },

    /**
     * 下载单个文档
     * @param {string} fileName - 文件名
     * @returns {Promise<Blob>} 文件数据
     */
    downloadDocument: async (fileName) => {
        const response = await axios.get(`${API_DOCS_URL}/download`, {
            params: { fileName: fileName },
            responseType: 'blob'
        });
        return response.data;
    },

    /**
     * 批量下载文档（ZIP）
     * @param {Array<string>} fileNames - 文件名列表
     * @returns {Promise<Blob>} ZIP文件数据
     */
    downloadBatch: async (fileNames) => {
        const response = await axios.post(`${API_DOCS_URL}/download-batch`, fileNames, {
            responseType: 'blob'
        });
        return response.data;
    },

    // ========== 反馈系统 ==========

    /**
     * 提交整体反馈 / Submit overall feedback
     * @param {string} recordId - 记录ID / Record ID
     * @param {number} rating - 评分(1-5) / Rating (1-5)
     * @param {string} feedback - 反馈文本 / Feedback text
     * @returns {Promise<Object>} 提交结果 / Submit result
     */
    submitOverallFeedback: async (recordId, rating, feedback) => {
        const response = await axios.post(`${API_FEEDBACK_URL}/overall`, {
            recordId,
            rating,
            feedback,
            lang: getCurrentLang() // 添加语言参数 / Add language parameter
        });
        return response.data;
    },

    /**
     * 提交文档反馈 / Submit document feedback
     * @param {string} recordId - 记录ID / Record ID
     * @param {string} documentName - 文档名 / Document name
     * @param {string} feedbackType - 反馈类型: HELPFUL/NOT_HELPFUL / Feedback type
     * @param {string} reason - 原因说明 / Reason
     * @returns {Promise<Object>} 提交结果 / Submit result
     */
    submitDocumentFeedback: async (recordId, documentName, feedbackType, reason) => {
        const response = await axios.post(`${API_FEEDBACK_URL}/document`, {
            recordId,
            documentName,
            feedbackType,
            reason,
            lang: getCurrentLang() // 添加语言参数 / Add language parameter
        });
        return response.data;
    },

    /**
     * 星级评价文档质量（用户友好接口）/ Rate document quality (user-friendly)
     * @param {string} recordId - 记录ID / Record ID
     * @param {string} documentName - 文档名 / Document name
     * @param {number} rating - 星级评分 (1-5) / Star rating (1-5)
     * @param {string} comment - 可选评论 / Optional comment
     * @returns {Promise<Object>} 提交结果 / Submit result
     */
    rateDocumentQuality: async (recordId, documentName, rating, comment) => {
        const response = await axios.post(`${API_FEEDBACK_URL}/document/rate`, {
            recordId,
            documentName,
            rating,
            comment: comment || null,
            lang: getCurrentLang() // 添加语言参数 / Add language parameter
        });
        return response.data;
    },

    /**
     * 表情评价整体回答质量（用户友好接口）/ Emoji rating for overall quality
     * @param {string} recordId - 记录ID / Record ID
     * @param {number} rating - 表情评分 (1-5) / Emoji rating (1-5)
     * @returns {Promise<Object>} 提交结果 / Submit result
     */
    rateOverallQuality: async (recordId, rating) => {
        const response = await axios.post(`${API_FEEDBACK_URL}/overall/rate`, {
            recordId,
            rating,
            lang: getCurrentLang() // 添加语言参数 / Add language parameter
        });
        return response.data;
    },

    // ========== 文档AI分析 (Document AI Analysis) ==========

    /**
     * 通用文档AI问答分析（使用知识库）
     * (General document AI Q&A analysis - with knowledge base)
     * @param {string} documentPath - 文档路径 (document path)
     * @param {string} question - 分析问题/提示词 (analysis question/prompt)
     * @returns {Promise<Object>} 分析报告 (analysis report)
     */
    analyzeDocument: async (documentPath, question) => {
        const response = await axios.post(`${API_DOCUMENT_QA_URL}/query`, {
            documentPath,
            question
        });
        return response.data;
    },

    /**
     * 通用文档AI直接分析（不使用知识库）
     * (General document AI direct analysis - without knowledge base)
     * @param {string} documentPath - 文档路径 (document path)
     * @param {string} question - 分析问题/提示词 (analysis question/prompt)
     * @returns {Promise<Object>} 分析报告 (analysis report)
     */
    analyzeDocumentDirect: async (documentPath, question) => {
        const response = await axios.post(`${API_DOCUMENT_QA_URL}/analyze-direct`, {
            documentPath,
            question
        });
        return response.data;
    },

    /**
     * PPT渐进式分析（推荐用于PPT，使用知识库）
     * (PPT progressive analysis - recommended for PPT, with knowledge base)
     * @param {string} documentPath - PPT文件路径 (PPT file path)
     * @param {string} question - 分析问题/提示词 (analysis question/prompt)
     * @returns {Promise<Object>} PPT分析报告 (PPT analysis report)
     */
    analyzePPT: async (documentPath, question) => {
        const response = await axios.post(`${API_DOCUMENT_QA_URL}/analyze-ppt`, {
            documentPath,
            question
        });
        return response.data;
    },

    /**
     * PPT直接分析（不使用知识库）
     * (PPT direct analysis - without knowledge base)
     * @param {string} documentPath - PPT文件路径 (PPT file path)
     * @param {string} question - 分析问题/提示词 (analysis question/prompt)
     * @returns {Promise<Object>} PPT分析报告 (PPT analysis report)
     */
    analyzePPTDirect: async (documentPath, question) => {
        const response = await axios.post(`${API_DOCUMENT_QA_URL}/analyze-ppt-direct`, {
            documentPath,
            question
        });
        return response.data;
    },

    /**
     * 多文档联合分析
     * (Multi-document joint analysis)
     * 分析多个文档之间的关联、逻辑、因果关系
     * (Analyze relationships, logic, and causality between multiple documents)
     *
     * @param {string[]} documentPaths - 文档路径数组 (array of document paths)
     * @param {string} question - 分析问题/提示词 (analysis question/prompt)
     * @returns {Promise<Object>} 多文档分析报告 (multi-document analysis report)
     */
    analyzeMultiDocuments: async (documentPaths, question) => {
        const response = await axios.post(`${API_DOCUMENT_QA_URL}/analyze-multi`, {
            documentPaths,
            question
        });
        return response.data;
    },

    /**
     * 清理分析会话临时文件
     * (Clean up analysis session temporary files)
     * @param {string} sessionId - 会话ID (session ID)
     * @returns {Promise<Object>} 清理结果 (cleanup result)
     */
    cleanupAnalysisSession: async (sessionId) => {
        const response = await axios.delete(`${API_DOCUMENT_QA_URL}/cleanup/${sessionId}`);
        return response.data;
    },

    // ========== LLM 结果文档化 (LLM Result Documentation) ==========

    /**
     * 保存 LLM 分析结果
     * (Save LLM analysis result)
     * @param {Object} result - 分析结果 (analysis result)
     * @returns {Promise<Object>} 保存结果 (save result)
     */
    saveLLMResult: async (result) => {
        const response = await axios.post(`${API_LLM_RESULTS_URL}/save`, result);
        return response.data;
    },

    /**
     * 获取 LLM 结果历史
     * @param {number} limit - 返回数量
     * @returns {Promise<Object>} 历史记录
     */
    getLLMResultHistory: async (limit = 20) => {
        const response = await axios.get(`${API_LLM_RESULTS_URL}/history`, {
            params: { limit }
        });
        return response.data;
    },

    /**
     * 获取 LLM 结果详情
     * @param {string} docId - 文档ID
     * @returns {Promise<Object>} 文档详情
     */
    getLLMResultDetail: async (docId) => {
        const response = await axios.get(`${API_LLM_RESULTS_URL}/${docId}`);
        return response.data;
    },

    /**
     * 预览 LLM 结果（Markdown）
     * @param {string} docId - 文档ID
     * @returns {Promise<string>} Markdown 内容
     */
    previewLLMResult: async (docId) => {
        const response = await axios.get(`${API_LLM_RESULTS_URL}/${docId}/preview`);
        return response.data;
    },

    /**
     * 下载 LLM 结果（Markdown，图片嵌入 Base64）
     * @param {string} docId - 文档ID
     * @param {string} fileName - 文件名
     */
    downloadLLMResultMarkdown: async (docId, fileName) => {
        const response = await axios.get(`${API_LLM_RESULTS_URL}/${docId}/download/markdown`, {
            responseType: 'blob'
        });
        const blob = response.data;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName || `${docId}.md`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
    },

    /**
     * 下载 LLM 结果（PDF）
     * @param {string} docId - 文档ID
     * @param {string} fileName - 文件名
     */
    downloadLLMResultPdf: async (docId, fileName) => {
        const response = await axios.get(`${API_LLM_RESULTS_URL}/${docId}/download/pdf`, {
            responseType: 'blob'
        });
        const blob = response.data;
        const url = window.URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        link.download = fileName || `${docId}.pdf`;
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
        window.URL.revokeObjectURL(url);
    },

    /**
     * 删除 LLM 结果文档
     * @param {string} docId - 文档ID
     * @returns {Promise<Object>} 删除结果
     */
    deleteLLMResult: async (docId) => {
        const response = await axios.delete(`${API_LLM_RESULTS_URL}/${docId}`);
        return response.data;
    }
};

// 导出到全局
if (typeof window !== 'undefined') {
    window.api = api;
}

// 如果支持模块导出
if (typeof module !== 'undefined' && module.exports) {
    module.exports = api;
}
