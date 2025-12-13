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
   * @param {boolean} params.useKnowledgeBase - 是否使用知识库 RAG（可选，默认 true）
   * @returns {Promise} 回答结果
   */
  ask(params) {
    return request.post('/qa/ask', {
      ...params,
      useKnowledgeBase: params.useKnowledgeBase !== undefined ? params.useKnowledgeBase : true
    })
  },

  /**
   * 使用指定文档批次进行问答 (Ask with specific documents)
   * @param {Object} params - 问题参数
   * @param {string} params.question - 问题内容
   * @param {string} params.sessionId - 会话 ID
   * @param {boolean} params.useKnowledgeBase - 是否使用知识库 RAG（可选，默认 true）
   * @returns {Promise} 回答结果
   */
  askWithSession(params) {
    return request.post('/qa/ask-with-session', {
      ...params,
      useKnowledgeBase: params.useKnowledgeBase !== undefined ? params.useKnowledgeBase : true
    })
  },

  /**
   * 流式问答 - 双轨架构 (Streaming Q&A - Dual-track Architecture)
   *
   * 第一轨：立即返回 HOPE 快速答案（<300ms）
   * 第二轨：通过 SSE 订阅 LLM 详细答案（流式）
   *
   * @param {Object} params - 问题参数
   * @param {string} params.question - 问题内容
   * @param {string} params.knowledgeMode - 知识库模式: 'none' | 'rag' | 'role'
   * @param {string} params.roleName - 角色名称（当 knowledgeMode='role' 时）
   * @param {boolean} params.useKnowledgeBase - 是否使用知识库（兼容参数）
   * @param {string} params.hopeSessionId - HOPE 会话 ID（可选）
   * @param {Function} onChunk - 数据块回调
   * @returns {Promise<{sessionId, eventSource, stop}>}
   */
  async askStreaming(params, onChunk) {
    try {
      console.log('🚀 Starting dual-track streaming Q&A:', params.question)
      console.log('📝 Knowledge Mode:', params.knowledgeMode)
      console.log('👤 Role Name:', params.roleName)

      // Step 1: 发起双轨流式请求，获取 sessionId 和 HOPE 快速答案
      const response = await fetch('/api/qa/ask-stream', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          question: params.question,
          knowledgeMode: params.knowledgeMode || 'rag',
          roleName: params.roleName || 'general',
          useKnowledgeBase: params.useKnowledgeBase !== undefined ? params.useKnowledgeBase : true,
          hopeSessionId: params.hopeSessionId
        })
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      const result = await response.json()
      const { sessionId, hopeAnswer, sseUrl, question } = result

      console.log('📥 Received session info:', {
        sessionId,
        hasHopeAnswer: !!hopeAnswer,
        sseUrl
      })

      // Step 2: 如果有 HOPE 快速答案，立即发送
      if (hopeAnswer && hopeAnswer.answer && onChunk) {
        console.log('💡 HOPE fast answer received:', {
          source: hopeAnswer.source,
          confidence: hopeAnswer.confidence,
          responseTime: hopeAnswer.responseTime
        })

        onChunk({
          content: hopeAnswer.answer,
          done: false,
          type: 'hope',
          source: hopeAnswer.source,
          confidence: hopeAnswer.confidence,
          canDirectAnswer: hopeAnswer.canDirectAnswer,
          responseTime: hopeAnswer.responseTime
        })
      }

      // Step 3: 订阅 LLM 流式输出（SSE）
      const eventSourceUrl = `${window.location.origin}${sseUrl}`
      console.log('📡 Subscribing to LLM stream:', eventSourceUrl)

      const eventSource = new EventSource(eventSourceUrl)

      // 监听 LLM 流式输出
      eventSource.addEventListener('llm', (event) => {
        try {
          const data = event.data
          console.log('📦 LLM chunk received:', data.substring(0, 50))

          if (onChunk) {
            onChunk({
              content: data,
              done: false,
              type: 'llm'
            })
          }
        } catch (error) {
          console.error('❌ Failed to parse LLM chunk:', error)
        }
      })

      // 监听完成事件
      eventSource.addEventListener('complete', (event) => {
        console.log('✅ LLM streaming completed')

        try {
          const stats = JSON.parse(event.data)
          console.log('📊 Streaming stats:', stats)

          if (onChunk) {
            onChunk({
              content: '',
              done: true,
              type: 'complete',
              sessionId,
              totalChunks: stats.totalChunks,
              totalTime: stats.totalTime
            })
          }
        } catch (e) {
          // 如果解析失败，仍然发送完成信号
          if (onChunk) {
            onChunk({
              content: '',
              done: true,
              type: 'complete',
              sessionId
            })
          }
        }

        eventSource.close()
      })

      // 监听错误事件
      eventSource.addEventListener('error', (event) => {
        console.error('❌ SSE connection error:', event)

        if (eventSource.readyState === EventSource.CLOSED) {
          console.log('🔌 EventSource closed')
        } else {
          eventSource.close()

          if (onChunk) {
            onChunk({
              type: 'error',
              error: 'SSE connection failed'
            })
          }
        }
      })

      // 返回控制对象
      return {
        sessionId,
        eventSource,
        stop: () => {
          eventSource.close()
          console.log('🛑 Stream stopped')
        }
      }

    } catch (error) {
      console.error('❌ Failed to ask streaming question:', error)
      if (onChunk) {
        onChunk({
          type: 'error',
          error: error.message
        })
      }
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

