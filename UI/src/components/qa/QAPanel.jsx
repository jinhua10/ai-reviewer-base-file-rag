/**
 * 问答主面板 (Q&A Main Panel)
 *
 * 智能问答系统的主界面容器
 * (Main interface container for intelligent Q&A system)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useState } from 'react'
import { Layout } from 'antd'
import ChatBox from './ChatBox'
import QuestionInput from './QuestionInput'
import SimilarQuestions from './SimilarQuestions'
import ConversationHistory from './ConversationHistory'
import { useLanguage } from '../../contexts/LanguageContext'
import qaApi from '../../api/modules/qa'
import '../../assets/css/qa/qa-panel.css'

const { Content, Sider } = Layout

/**
 * 问答主面板组件
 * @returns {JSX.Element}
 */
function QAPanel() {
  const { t } = useLanguage()

  // 状态管理
  const [messages, setMessages] = useState([]) // 消息列表
  const [loading, setLoading] = useState(false) // 加载状态
  const [similarQuestions, setSimilarQuestions] = useState([]) // 相似问题
  const [historyVisible, setHistoryVisible] = useState(false) // 历史记录可见性
  const [currentQuestion, setCurrentQuestion] = useState('') // 当前问题
  const [currentEventSource, setCurrentEventSource] = useState(null) // 当前 EventSource 连接

  /**
   * 处理问题提交
   * @param {string} question - 问题内容
   */
  const handleSubmitQuestion = async (question) => {
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

      // 调用流式 API（双轨输出）/ Call streaming API (Dual Track)
      const result = await qaApi.askStreaming(
        { question },
        (data) => {
          console.log('📨 Received data in QAPanel:', data)
          
          // 实时更新答案内容 / Update answer content in real-time
          setMessages(prev => {
            const newMessages = [...prev]
            const lastMessage = newMessages[newMessages.length - 1]
            
            console.log('📝 Current last message:', lastMessage)
            
            if (lastMessage && lastMessage.streaming) {
              // 处理不同类型的数据 / Handle different types of data
              switch (data.type) {
                case 'hope':
                  // HOPE 快速答案（立即显示）/ HOPE fast answer (display immediately)
                  lastMessage.content = data.content
                  lastMessage.source = `HOPE (${data.source})`
                  lastMessage.confidence = data.confidence
                  lastMessage.hopeAnswer = data.content
                  lastMessage.canDirectAnswer = data.canDirectAnswer
                  break

                case 'llm':
                  // LLM 流式块（追加显示）/ LLM streaming chunk (append display)
                  // 如果有 HOPE 答案，在新行显示 LLM 答案
                  // (If HOPE answer exists, display LLM answer on new line)
                  if (lastMessage.hopeAnswer) {
                    if (!lastMessage.llmAnswer) {
                      lastMessage.llmAnswer = ''
                      lastMessage.content += '\n\n--- LLM 详细回答 ---\n'
                    }
                    lastMessage.llmAnswer += data.content
                    lastMessage.content += data.content
                  } else {
                    lastMessage.content += data.content
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
                    lastMessage.content += data.content
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
  )
}

export default QAPanel

