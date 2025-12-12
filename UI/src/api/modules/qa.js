/**
 * 问答 API 模块 (Q&A API Module)
 *
 * 提供智能问答相关的 API 接口
 * (Provides Q&A-related API interfaces)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import { request } from '../index'

const qaApi = {
  /**
   * 提问 (Ask question)
   * @param {Object} params - 问题参数
   * @param {string} params.question - 问题内容
   * @param {string} params.hopeSessionId - HOPE 会话 ID（可选）
   * @returns {Promise} 回答结果
   */
  ask(params) {
    return request.post('/qa/ask', params)
  },

  /**
   * 使用指定文档批次进行问答 (Ask with specific documents)
   * @param {Object} params - 问题参数
   * @param {string} params.question - 问题内容
   * @param {string} params.sessionId - 会话 ID
   * @returns {Promise} 回答结果
   */
  askWithSession(params) {
    return request.post('/qa/ask-with-session', params)
  },

  /**
   * 流式问答 (Streaming Q&A)
   * @param {Object} params - 问题参数
   * @param {Function} onChunk - 数据块回调
   * @returns {Promise} EventSource 实例
   */
  askStreaming(params, onChunk) {
    // 使用 EventSource 进行流式传输
    const url = `/qa/ask-streaming?question=${encodeURIComponent(params.question)}`
    const eventSource = new EventSource(url)

    eventSource.onmessage = (event) => {
      const data = JSON.parse(event.data)
      if (onChunk) {
        onChunk(data)
      }
    }

    eventSource.onerror = (error) => {
      console.error('❌ Streaming error:', error)
      eventSource.close()
    }

    return eventSource
  },

  /**
   * 获取问答历史 (Get Q&A history)
   * @param {Object} params - 查询参数
   * @param {number} params.page - 页码
   * @param {number} params.pageSize - 每页条数
   * @returns {Promise} 历史记录
   */
  getHistory(params) {
    return request.get('/qa/history', params)
  },

  /**
   * 获取相似问题 (Get similar questions)
   * @param {string} question - 问题内容
   * @returns {Promise} 相似问题列表
   */
  getSimilarQuestions(question) {
    return request.get('/qa/similar', { question })
  },

  /**
   * 反馈回答质量 (Feedback answer quality)
   * @param {Object} params - 反馈参数
   * @param {string} params.answerId - 回答 ID
   * @param {number} params.rating - 评分（1-5）
   * @param {string} params.comment - 评论（可选）
   * @returns {Promise} 反馈结果
   */
  feedback(params) {
    return request.post('/qa/feedback', params)
  },

  /**
   * 获取推荐提示词 (Get recommended prompts)
   * @returns {Promise} 推荐提示词列表
   */
  getRecommendedPrompts() {
    return request.get('/qa/prompts/recommended')
  },
}

export default qaApi

