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
   * 流式问答 - 双轨输出 (Streaming Q&A - Dual Track)
   * 
   * 架构：
   * 1. POST /api/qa/stream → 获取 sessionId 和 HOPE 快速答案
   * 2. EventSource 订阅 /api/qa/stream/{sessionId} → 接收 LLM 流式输出
   * 
   * Architecture:
   * 1. POST /api/qa/stream → Get sessionId and HOPE fast answer
   * 2. EventSource subscribe /api/qa/stream/{sessionId} → Receive LLM streaming output
   * 
   * @param {Object} params - 问题参数 (Question parameters)
   * @param {string} params.question - 问题内容 (Question content)
   * @param {string} [params.userId] - 用户 ID (User ID, optional)
   * @param {Function} onChunk - 数据块回调 (Chunk callback)
   * @returns {Promise<{sessionId: string, eventSource: EventSource}>}
   */
  async askStreaming(params, onChunk) {
    try {
      // Step 1: 发起流式问答，获取 sessionId 和 HOPE 快速答案
      // (Step 1: Initiate streaming Q&A, get sessionId and HOPE fast answer)
      console.log('🚀 Starting streaming Q&A:', params.question)
      
      const response = await request.post('/qa/stream', {
        question: params.question,
        userId: params.userId || 'anonymous',
        useKnowledgeBase: params.useKnowledgeBase !== undefined ? params.useKnowledgeBase : true,
        knowledgeMode: params.knowledgeMode, // 'none' | 'rag' | 'role'
        roleName: params.roleName // 角色名称（当 knowledgeMode='role' 时）
      })

      console.log('📥 Received initial response:', response)

      const { sessionId, question, hopeAnswer, sseUrl } = response
      
      if (!sessionId || !sseUrl) {
        throw new Error('Invalid response: missing sessionId or sseUrl')
      }

      // Step 2: 如果 HOPE 有快速答案，立即发送
      // (Step 2: If HOPE has fast answer, send immediately)
      if (hopeAnswer && hopeAnswer.answer && onChunk) {
        onChunk({
          content: hopeAnswer.answer,
          done: false,
          type: 'hope',
          source: hopeAnswer.source,
          confidence: hopeAnswer.confidence,
          canDirectAnswer: hopeAnswer.canDirectAnswer,
          responseTime: hopeAnswer.responseTime
        })

        // 如果 HOPE 能直接回答，可能不需要 LLM
        // (If HOPE can directly answer, may not need LLM)
        if (hopeAnswer.canDirectAnswer) {
          // 发送完成信号
          // (Send completion signal)
          if (onChunk) {
            onChunk({
              content: '',
              done: true,
              sessionId,
              hopeAnswer: hopeAnswer.answer,
              source: hopeAnswer.source,
              type: 'complete'
            })
          }
          return { sessionId, eventSource: null }
        }
      }

      // Step 3: 订阅 LLM 流式输出（SSE）
      // (Step 3: Subscribe to LLM streaming output via SSE)
      // EventSource 需要完整 URL（包括协议和域名）
      // sseUrl 已包含 /api 前缀（如 /api/qa/stream/xxx）
      const eventSourceUrl = `${window.location.origin}${sseUrl}`
      const eventSource = new EventSource(eventSourceUrl)

      let fullLLMAnswer = ''
      let chunkCount = 0

      // 监听 chunk 事件（HybridStreamingService 使用 'chunk' 事件名）
      // (Listen to chunk event from HybridStreamingService)
      eventSource.addEventListener('chunk', (event) => {
        try {
          const chunk = event.data // 纯文本块

          fullLLMAnswer += chunk
          chunkCount++

          console.log(`📦 Received chunk #${chunkCount}:`, chunk.substring(0, 50))

          if (onChunk) {
            onChunk({
              content: chunk,
              done: false,
              type: 'llm',
              chunkIndex: chunkCount
            })
          }
        } catch (error) {
          console.error('❌ Failed to process chunk:', error)
        }
      })

      // 监听 LLM 文本块事件（StreamingQAController.dualTrackStreaming 使用 'llm' 事件名）
      // (Listen to LLM chunk events from dualTrackStreaming)
      eventSource.addEventListener('llm', (event) => {
        try {
          const message = JSON.parse(event.data)
          const chunk = message.content

          fullLLMAnswer += chunk
          chunkCount++

          console.log(`📦 Received LLM chunk #${chunkCount}:`, chunk.substring(0, 50))

          if (onChunk) {
            onChunk({
              content: chunk,
              done: false,
              type: 'llm',
              chunkIndex: message.chunkIndex
            })
          }
        } catch (error) {
          console.error('❌ Failed to parse LLM chunk:', error, 'Data:', event.data)
        }
      })

      // 监听完成事件
      // (Listen to complete event)
      eventSource.addEventListener('complete', (event) => {
        console.log('📢 Received complete event:', event.data)

        eventSource.close()

        // 兼容两种格式：纯文本 "done" 或 JSON
        let totalChunks = chunkCount
        let totalTime = 0

        if (event.data !== 'done') {
          try {
            const message = JSON.parse(event.data)
            totalChunks = message.totalChunks || chunkCount
            totalTime = message.totalTime || 0
          } catch (error) {
            console.warn('⚠️ Complete message is not JSON, using fallback values')
          }
        }

        console.log(`✅ LLM generation completed: ${totalChunks} chunks, ${totalTime}ms`)

        if (onChunk) {
          onChunk({
            content: '',
            done: true,
            type: 'complete',
            sessionId,
            hopeAnswer: hopeAnswer?.answer || null,
            llmAnswer: fullLLMAnswer,
            totalChunks,
            totalTime
          })
        }
      })

      // 监听 HOPE 事件（dualTrackStreaming 使用）
      // (Listen to HOPE event from dualTrackStreaming)
      eventSource.addEventListener('hope', (event) => {
        try {
          const message = JSON.parse(event.data)
          console.log('💡 Received HOPE answer:', message)

          if (onChunk) {
            onChunk({
              content: message.content,
              done: false,
              type: 'hope',
              source: message.hopeSource,
              confidence: message.confidence
            })
          }
        } catch (error) {
          console.error('❌ Failed to parse HOPE message:', error)
        }
      })

      // 监听错误事件
      // (Listen to error event)
      eventSource.addEventListener('error', (event) => {
        console.error('❌ SSE error:', event)
        
        // 只在非正常关闭时报错
        if (eventSource.readyState === EventSource.CLOSED) {
          console.log('🔌 EventSource closed')
        } else if (eventSource.readyState === EventSource.CONNECTING) {
          console.log('🔄 EventSource reconnecting...')
        } else {
          eventSource.close()
          
          if (onChunk) {
            onChunk({
              content: '',
              done: true,
              type: 'error',
              error: 'SSE connection failed'
            })
          }
        }
      })

      // 监听连接打开
      eventSource.addEventListener('open', () => {
        console.log('✅ SSE connection opened:', eventSourceUrl)
      })

      // 返回 sessionId 和 eventSource（允许手动关闭）
      // (Return sessionId and eventSource for manual close)
      return { sessionId, eventSource }

    } catch (error) {
      console.error('❌ Failed to ask streaming question:', error)
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

