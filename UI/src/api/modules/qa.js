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
   * @param {string} params.question - 问题内容
   * @param {Function} onChunk - 数据块回调
   * @returns {Promise} 返回一个包含sessionId和sseUrl的对象
   */
  async askStreaming(params, onChunk) {
    try {
      // 步骤1: 发起流式请求，获取sessionId和sseUrl
      // Step 1: Initiate streaming request, get sessionId and sseUrl
      const response = await request.post('/qa/stream', {
        question: params.question,
        userId: 'web-user-' + Date.now()
      })

      const { sessionId, sseUrl, hopeAnswer } = response.data || response

      // 如果HOPE能直接回答，立即返回
      // If HOPE can answer directly, return immediately
      if (hopeAnswer && hopeAnswer.canDirectAnswer) {
        if (onChunk) {
          onChunk({
            content: hopeAnswer.answer,
            done: true,
            source: 'HOPE',
            confidence: hopeAnswer.confidence
          })
        }
        return { sessionId, closed: true }
      }

      // 步骤2: 使用SSE接收流式数据
      // Step 2: Use SSE to receive streaming data
      const fullUrl = sseUrl.startsWith('http') ? sseUrl : window.location.origin + sseUrl
      const eventSource = new EventSource(fullUrl)

      eventSource.onmessage = (event) => {
        try {
          const data = JSON.parse(event.data)
          if (onChunk) {
            onChunk(data)
          }
          // 如果收到完成标记，关闭连接
          // If done signal received, close connection
          if (data.done || data.type === 'done') {
            eventSource.close()
          }
        } catch (error) {
          console.error('❌ Failed to parse SSE data:', error)
        }
      }

      eventSource.onerror = (error) => {
        console.error('❌ SSE connection error:', error)
        eventSource.close()
        if (onChunk) {
          onChunk({ error: true, message: 'Connection lost' })
        }
      }

      return { sessionId, eventSource }
    } catch (error) {
      console.error('❌ Failed to start streaming:', error)
      throw error
    }
  },

  /**
   * 获取问答历史 / Get Q&A history
   * @param {Object} params - 查询参数 / Query parameters
   * @param {number} params.page - 页码 / Page number
   * @param {number} params.pageSize - 每页条数 / Items per page
   * @returns {Promise} 历史记录 / History records
   */
  getHistory(params) {
    return request.get('/qa/history', params)
  },

  /**
   * 获取相似问题 / Get similar questions
   * @param {string} question - 问题内容 / Question content
   * @returns {Promise} 相似问题列表 / Similar questions list
   */
  getSimilarQuestions(question) {
    return request.get('/qa/similar', { question })
  },

  /**
   * 反馈回答质量 / Feedback answer quality
   * @param {Object} params - 反馈参数 / Feedback parameters
   * @param {string} params.answerId - 回答 ID / Answer ID
   * @param {number} params.rating - 评分（1-5）/ Rating (1-5)
   * @param {string} params.comment - 评论（可选）/ Comment (optional)
   * @returns {Promise} 反馈结果 / Feedback result
   */
  feedback(params) {
    return request.post('/qa/feedback', params)
  },

  /**
   * 获取推荐提示词 / Get recommended prompts
   * @returns {Promise} 推荐提示词列表 / Recommended prompts list
   */
  getRecommendedPrompts() {
    return request.get('/qa/prompts/recommended')
  },
}

export default qaApi

