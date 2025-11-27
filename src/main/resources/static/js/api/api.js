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
     * 获取统计信息
     * @returns {Promise<Object>} 统计数据
     */
    getStatistics: async () => {
        const response = await axios.get(`${API_BASE_URL}/statistics`);
        return response.data;
    },

    /**
     * 健康检查
     * @returns {Promise<Object>} 健康状态
     */
    health: async () => {
        const response = await axios.get(`${API_BASE_URL}/health`);
        return response.data;
    },

    /**
     * 重建索引
     * @returns {Promise<Object>} 重建结果
     */
    rebuild: async () => {
        const response = await axios.post(`${API_BASE_URL}/rebuild`);
        return response.data;
    },

    /**
     * 增量索引
     * @returns {Promise<Object>} 索引结果
     */
    incrementalIndex: async () => {
        const response = await axios.post(`${API_BASE_URL}/incremental-index`);
        return response.data;
    },

    // ========== 文档管理 ==========

    /**
     * 上传文档
     * @param {File} file - 文件对象
     * @returns {Promise<Object>} 上传结果
     */
    uploadDocument: async (file) => {
        const formData = new FormData();
        formData.append('file', file);
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
     * 删除文档
     * @param {string} fileName - 文件名
     * @returns {Promise<Object>} 删除结果
     */
    deleteDocument: async (fileName) => {
        const response = await axios.delete(`${API_DOCS_URL}/${encodeURIComponent(fileName)}`);
        return response.data;
    },

    /**
     * 下载单个文档
     * @param {string} fileName - 文件名
     * @returns {Promise<Blob>} 文件数据
     */
    downloadDocument: async (fileName) => {
        const response = await axios.get(`${API_DOCS_URL}/download/${encodeURIComponent(fileName)}`, {
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
     * 提交整体反馈
     * @param {string} recordId - 记录ID
     * @param {number} rating - 评分(1-5)
     * @param {string} feedback - 反馈文本
     * @returns {Promise<Object>} 提交结果
     */
    submitOverallFeedback: async (recordId, rating, feedback) => {
        const response = await axios.post(`${API_FEEDBACK_URL}/overall`, {
            recordId,
            rating,
            feedback
        });
        return response.data;
    },

    /**
     * 提交文档反馈
     * @param {string} recordId - 记录ID
     * @param {string} documentName - 文档名
     * @param {string} feedbackType - 反馈类型: HELPFUL/NOT_HELPFUL
     * @param {string} reason - 原因说明
     * @returns {Promise<Object>} 提交结果
     */
    submitDocumentFeedback: async (recordId, documentName, feedbackType, reason) => {
        const response = await axios.post(`${API_FEEDBACK_URL}/document`, {
            recordId,
            documentName,
            feedbackType,
            reason
        });
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

