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
   * 
   * 注意：后端 /qa/ask 接口返回完整答案，前端模拟流式显示效果
   * Note: Backend /qa/ask returns complete answer, frontend simulates streaming effect
   * 
   * @param {Object} params - 问题参数
   * @param {string} params.question - 问题内容
   * @param {Function} onChunk - 数据块回调
   * @returns {Promise} 返回sessionId等信息
   */
  async askStreaming(params, onChunk) {
    try {
      // 调用后端 /ask 接口获取完整答案 (Call backend /ask API to get complete answer)
      const response = await request.post('/qa/ask', {
        question: params.question,
        hopeSessionId: params.hopeSessionId || null
      })

      // axios 拦截器已返回 response.data (Axios interceptor returns response.data)
      const { 
        answer, 
        sessionId, 
        sources, 
        hopeSource, 
        directAnswer,
        strategyUsed,
        hopeConfidence 
      } = response

      if (!onChunk) {
        return { sessionId, answer, sources }
      }

      // 模拟流式输出：逐字显示答案 (Simulate streaming: display answer character by character)
      const text = answer || ''
      const chunkSize = 3 // 每次显示3个字符 (Display 3 characters at a time)
      const delay = 30 // 每次延迟30ms (Delay 30ms each time)

      for (let i = 0; i < text.length; i += chunkSize) {
        const chunk = text.slice(i, i + chunkSize)
        onChunk({
          content: chunk,
          done: false,
          source: hopeSource || strategyUsed,
          confidence: hopeConfidence
        })
        
        // 延迟以产生流式效果 (Delay to create streaming effect)
        if (i + chunkSize < text.length) {
          await new Promise(resolve => setTimeout(resolve, delay))
        }
      }

      // 发送完成信号和来源信息 (Send completion signal and source information)
      onChunk({
        content: '',
        done: true,
        sources: sources || [],
        sessionId,
        hopeSource,
        directAnswer,
        strategyUsed,
        confidence: hopeConfidence
      })

      return { sessionId, answer, sources }

    } catch (error) {
      console.error('❌ Failed to ask question:', error)
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

