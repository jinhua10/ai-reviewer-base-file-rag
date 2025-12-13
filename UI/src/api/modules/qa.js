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
   * 流式问答 (Streaming Q&A)
   *
   * 使用新的统一流式接口 /qa/ask-stream
   * (Uses new unified streaming interface /qa/ask-stream)
   *
   * @param {Object} params - 问题参数
   * @param {string} params.question - 问题内容
   * @param {string} params.knowledgeMode - 知识库模式: 'none' | 'rag' | 'role'
   * @param {string} params.roleName - 角色名称（当 knowledgeMode='role' 时）
   * @param {boolean} params.useKnowledgeBase - 是否使用知识库（兼容参数）
   * @param {string} params.hopeSessionId - HOPE 会话 ID（可选）
   * @param {Function} onChunk - 数据块回调
   * @returns {Promise<{reader, stop}>}
   */
  async askStreaming(params, onChunk) {
    try {
      console.log('🚀 Starting streaming Q&A:', params.question)
      console.log('📝 Knowledge Mode:', params.knowledgeMode)
      console.log('👤 Role Name:', params.roleName)

      // 使用 fetch 发起流式请求
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

      console.log('📥 Response received, starting to read stream...')

      // 读取流式响应
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      // 异步读取流
      const readStream = async () => {
        try {
          while (true) {
            const { done, value } = await reader.read()

            if (done) {
              console.log('✅ Stream completed')
              if (onChunk) {
                onChunk({
                  content: '',
                  done: true,
                  type: 'complete'
                })
              }
              break
            }

            // 解码数据块
            const chunk = decoder.decode(value, { stream: true })
            buffer += chunk

            // 处理 SSE 格式的数据：data: xxx\n\n
            const lines = buffer.split('\n')
            buffer = lines.pop() || '' // 保留不完整的行

            for (const line of lines) {
              if (line.startsWith('data: ')) {
                const data = line.substring(6).trim()
                if (data && onChunk) {
                  console.log(`📦 Received chunk:`, data.substring(0, 50))
                  onChunk({
                    content: data,
                    done: false,
                    type: 'llm'
                  })
                }
              }
            }
          }
        } catch (error) {
          console.error('❌ Error reading stream:', error)
          if (onChunk) {
            onChunk({
              type: 'error',
              error: error.message
            })
          }
        }
      }

      // 开始读取流
      readStream()

      return {
        reader,
        stop: () => {
          reader.cancel()
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

