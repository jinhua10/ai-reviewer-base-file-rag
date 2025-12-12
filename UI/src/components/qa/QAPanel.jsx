/**
 * 问答主面板 (Q&A Main Panel)
 *
 * 智能问答系统的主界面容器
 * (Main interface container for intelligent Q&A system)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useState, useRef } from 'react'
import { Layout } from 'antd'
import ChatBox from './ChatBox'
import QuestionInput from './QuestionInput'
import SimilarQuestions from './SimilarQuestions'
import ConversationHistory from './ConversationHistory'
import { useLanguage } from '../../contexts/LanguageContext'
import { useQA } from '../../contexts/QAContext'
import qaApi from '../../api/modules/qa'
import '../../assets/css/qa/qa-panel.css'

const { Content, Sider } = Layout

/**
 * 问答主面板组件
 * @returns {JSX.Element}
 */
function QAPanel() {
  const { t } = useLanguage()
  const {
    messages,
    setMessages,
    similarQuestions,
    setSimilarQuestions,
    currentQuestion,
    setCurrentQuestion,
  } = useQA()

  // 本地状态（不需要跨Tab保持）
  const [loading, setLoading] = useState(false) // 加载状态
  const [historyVisible, setHistoryVisible] = useState(false) // 历史记录可见性
  const [currentEventSource, setCurrentEventSource] = useState(null) // 当前 EventSource 连接
  
  // 使用ref追踪当前流式消息的内容，避免React批量更新导致重复累加
  const streamingContentRef = useRef('')
  const streamingLLMAnswerRef = useRef('')
  
  // 从 localStorage 读取流式模式偏好（默认为 true）
  const [isStreamingMode, setIsStreamingMode] = useState(() => {
    const saved = localStorage.getItem('qa_streaming_mode')
    return saved !== null ? saved === 'true' : true
  })
  
  // 从 localStorage 读取知识库开关（默认为 true）
  const [useKnowledgeBase, setUseKnowledgeBase] = useState(() => {
    const saved = localStorage.getItem('qa_use_knowledge_base')
    return saved !== null ? saved === 'true' : true
  })

  /**
   * 切换流式/非流式模式
   */
  const toggleStreamingMode = () => {
    const newMode = !isStreamingMode
    setIsStreamingMode(newMode)
    localStorage.setItem('qa_streaming_mode', newMode.toString())
    console.log(`🔄 Switched to ${newMode ? 'streaming' : 'non-streaming'} mode`)
  }
  
  /**
   * 切换知识库使用
   */
  const toggleKnowledgeBase = () => {
    const newValue = !useKnowledgeBase
    setUseKnowledgeBase(newValue)
    localStorage.setItem('qa_use_knowledge_base', newValue.toString())
    console.log(`🔄 ${newValue ? 'Enabled' : 'Disabled'} knowledge base`)
  }

  /**
   * 处理问题提交
   * 根据用户选择使用流式或非流式模式
   * @param {string} question - 问题内容
   */
  const handleSubmitQuestion = async (question) => {
    // 根据用户设置决定使用哪种模式
    if (!isStreamingMode) {
      return handleSubmitQuestionNonStreaming(question)
    }
    
    // 默认使用流式模式
    if (!question.trim()) return

    // 添加用户问题到消息列表
    const userMessage = {
      id: Date.now(),
      type: 'question',
      content: question,
      timestamp: new Date().toISOString(),
    }
    setMessages(prev => [...prev, userMessage])
    setCurrentQuestion(question)
    setLoading(true)

    try {
      // 创建答案消息占位符 / Create answer message placeholder
      const answerMessage = {
        id: Date.now() + 1,
        type: 'answer',
        content: '',
        streaming: true,
        timestamp: new Date().toISOString(),
        sessionId: null,
        sources: [],
      }
      setMessages(prev => [...prev, answerMessage])
      
      // 重置ref内容
      streamingContentRef.current = ''
      streamingLLMAnswerRef.current = ''

      // 调用流式 API（双轨输出）/ Call streaming API (Dual Track)
      const result = await qaApi.askStreaming(
        { 
          question,
          useKnowledgeBase  // 是否使用知识库
        },
        (data) => {
          // 先累加到ref（不触发渲染，避免React批量更新导致的重复累加）
          // Accumulate to ref first (avoid re-render and duplicate accumulation from React batching)
          if (data.type === 'hope') {
            streamingContentRef.current = data.content
          } else if (data.type === 'llm') {
            streamingLLMAnswerRef.current += data.content
          }
          
          // 然后从ref读取更新UI（只触发一次渲染）
          // Then read from ref to update UI (trigger render only once)
          setMessages(prev => {
            const newMessages = [...prev]
            const lastMessage = newMessages[newMessages.length - 1]
            
            if (lastMessage && lastMessage.streaming) {
              // 处理不同类型的数据 / Handle different types of data
              switch (data.type) {
                case 'hope':
                  // HOPE 快速答案（立即显示）/ HOPE fast answer (display immediately)
                  lastMessage.content = streamingContentRef.current
                  lastMessage.source = `HOPE (${data.source})`
                  lastMessage.confidence = data.confidence
                  lastMessage.hopeAnswer = streamingContentRef.current
                  lastMessage.canDirectAnswer = data.canDirectAnswer
                  break

                case 'llm':
                  // LLM 流式块（从ref读取累加结果）/ LLM streaming chunk (read accumulated result from ref)
                  // 如果有 HOPE 答案，在新行显示 LLM 答案
                  // (If HOPE answer exists, display LLM answer on new line)
                  if (lastMessage.hopeAnswer) {
                    if (!lastMessage.llmAnswer) {
                      lastMessage.llmAnswer = ''
                    }
                    lastMessage.llmAnswer = streamingLLMAnswerRef.current
                    lastMessage.content = streamingContentRef.current + '\n\n--- LLM 详细回答 ---\n' + streamingLLMAnswerRef.current
                  } else {
                    lastMessage.content = streamingLLMAnswerRef.current
                  }
                  break

                case 'complete':
                  // 完成 / Complete
                  lastMessage.streaming = false
                  lastMessage.sessionId = data.sessionId
                  if (data.sources) {
                    lastMessage.sources = data.sources
                  }
                  break

                case 'error':
                  // 错误 / Error
                  lastMessage.type = 'error'
                  lastMessage.content = data.error || t('qa.error.failed')
                  lastMessage.streaming = false
                  break

                default:
                  // 兼容旧格式 / Compatible with old format
                  if (data.content) {
                    streamingLLMAnswerRef.current += data.content
                    lastMessage.content = streamingLLMAnswerRef.current
                  }
                  if (data.done) {
                    lastMessage.streaming = false
                    lastMessage.sessionId = data.sessionId
                  }
              }
            }
            return newMessages
          })
        }
      )

      // 保存 sessionId 和 eventSource / Save sessionId and eventSource
      if (result && result.sessionId) {
        setMessages(prev => {
          const newMessages = [...prev]
          const lastMessage = newMessages[newMessages.length - 1]
          if (lastMessage) {
            lastMessage.sessionId = result.sessionId
          }
          return newMessages
        })
      }

      // 保存 EventSource 引用以便停止生成 / Save EventSource reference for stopping
      if (result && result.eventSource) {
        setCurrentEventSource(result.eventSource)
      }

      // 获取相似问题 / Get similar questions
      try {
        const similarData = await qaApi.getSimilarQuestions(question)
        // axios 拦截器已返回 response.data (Axios interceptor returns response.data)
        if (similarData) {
          setSimilarQuestions(similarData)
        }
      } catch (err) {
        console.warn('⚠️ Failed to get similar questions:', err)
      }

    } catch (error) {
      console.error('❌ Failed to ask question:', error)
      // 添加错误消息 / Add error message
      setMessages(prev => {
        const newMessages = [...prev]
        const lastMessage = newMessages[newMessages.length - 1]
        if (lastMessage && lastMessage.streaming) {
          lastMessage.type = 'error'
          lastMessage.content = error.message || t('qa.error.failed')
          lastMessage.streaming = false
        }
        return newMessages
      })
    } finally {
      setLoading(false)
      setCurrentEventSource(null)
    }
  }

  /**
   * 非流式问答（带 thinking 动画）
   * Non-streaming Q&A with thinking animation
   */
  const handleSubmitQuestionNonStreaming = async (question) => {
    if (!question.trim()) return

    // 添加用户问题
    const userMessage = {
      id: Date.now(),
      type: 'question',
      content: question,
      timestamp: new Date().toISOString(),
    }
    setMessages(prev => [...prev, userMessage])
    setCurrentQuestion(question)
    setLoading(true)

    try {
      // 创建 thinking 状态的答案
      const answerMessage = {
        id: Date.now() + 1,
        type: 'answer',
        content: '',
        thinking: true,  // Thinking 状态
        timestamp: new Date().toISOString(),
        sessionId: null,
        sources: [],
      }
      setMessages(prev => [...prev, answerMessage])

      // 调用非流式 API
      const response = await qaApi.ask({ 
        question,
        useKnowledgeBase  // 是否使用知识库
      })

      // 更新答案内容
      setMessages(prev => {
        const newMessages = [...prev]
        const lastMessage = newMessages[newMessages.length - 1]
        if (lastMessage && lastMessage.thinking) {
          lastMessage.thinking = false
          lastMessage.content = response.answer
          lastMessage.sessionId = response.sessionId
          lastMessage.sources = response.sources || []
        }
        return newMessages
      })

      // 获取相似问题
      try {
        const similarData = await qaApi.getSimilarQuestions(question)
        if (similarData) {
          setSimilarQuestions(similarData)
        }
      } catch (err) {
        console.warn('⚠️ Failed to get similar questions:', err)
      }

    } catch (error) {
      console.error('❌ Failed to ask question:', error)
      setMessages(prev => {
        const newMessages = [...prev]
        const lastMessage = newMessages[newMessages.length - 1]
        if (lastMessage && lastMessage.thinking) {
          lastMessage.type = 'error'
          lastMessage.content = error.message || t('qa.error.failed')
          lastMessage.thinking = false
        }
        return newMessages
      })
    } finally {
      setLoading(false)
    }
  }

  /**
   * 停止生成
   * Stop generation
   */
  const handleStopGeneration = () => {
    if (currentEventSource) {
      console.log('🛑 Stopping generation...')
      currentEventSource.close()
      setCurrentEventSource(null)
      setLoading(false)

      // 标记最后一条消息为已完成
      // Mark last message as completed
      setMessages(prev => {
        const newMessages = [...prev]
        const lastMessage = newMessages[newMessages.length - 1]
        if (lastMessage && lastMessage.streaming) {
          lastMessage.streaming = false
          lastMessage.stopped = true
        }
        return newMessages
      })
    }
  }

  /**
   * 处理相似问题点击
   * @param {string} question - 问题内容
   */
  const handleSimilarQuestionClick = (question) => {
    handleSubmitQuestion(question)
  }

  /**
   * 处理答案反馈
   * @param {string} answerId - 答案 ID
   * @param {number} rating - 评分
   */
  const handleFeedback = async (answerId, rating) => {
    try {
      await qaApi.feedback({ answerId, rating })
      console.log('✅ Feedback submitted')
    } catch (error) {
      console.error('❌ Failed to submit feedback:', error)
    }
  }

  /**
   * 切换历史记录侧边栏
   */
  const toggleHistory = () => {
    setHistoryVisible(!historyVisible)
  }

  return (
    <>
      <Layout className="qa-panel">
        {/* 左侧：对话历史（可折叠） */}
        {historyVisible && (
          <Sider
            width={280}
            className="qa-panel__history-sider"
            theme="light"
          >
            <ConversationHistory
              onClose={() => setHistoryVisible(false)}
              onSelectQuestion={handleSubmitQuestion}
            />
          </Sider>
        )}

        {/* 中间：主聊天区域 */}
        <Content className="qa-panel__main">
        <div className="qa-panel__container">
          {/* 聊天框 */}
          <ChatBox
            messages={messages}
            loading={loading}
            onFeedback={handleFeedback}
            onToggleHistory={toggleHistory}
            onStopGeneration={handleStopGeneration}
            isGenerating={!!currentEventSource}
            isStreamingMode={isStreamingMode}
            onToggleStreamingMode={toggleStreamingMode}
            useKnowledgeBase={useKnowledgeBase}
            onToggleKnowledgeBase={toggleKnowledgeBase}
          />

          {/* 输入框 */}
          <QuestionInput
            onSubmit={handleSubmitQuestion}
            loading={loading}
            placeholder={t('qa.input.placeholder')}
          />
        </div>
      </Content>

        {/* 右侧：相似问题推荐 */}
        <Sider
          width={300}
          className="qa-panel__similar-sider"
          theme="light"
        >
          <SimilarQuestions
            questions={similarQuestions}
            currentQuestion={currentQuestion}
            onQuestionClick={handleSimilarQuestionClick}
          />
        </Sider>
      </Layout>
    </>
  )
}

export default QAPanel

