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

      // 调用流式 API / Call streaming API
      const result = await qaApi.askStreaming(
        { question },
        (data) => {
          // 更新答案内容 / Update answer content
          setMessages(prev => {
            const newMessages = [...prev]
            const lastMessage = newMessages[newMessages.length - 1]
            if (lastMessage && lastMessage.streaming) {
              // 处理不同类型的数据块 / Handle different types of data chunks
              if (data.content) {
                lastMessage.content += data.content
              } else if (data.chunk) {
                lastMessage.content += data.chunk
              }

              // 更新来源信息 / Update source information
              if (data.sources) {
                lastMessage.sources = data.sources
              }

              // 检查是否完成 / Check if done
              if (data.done || data.type === 'done') {
                lastMessage.streaming = false
              }

              // 处理错误 / Handle error
              if (data.error) {
                lastMessage.type = 'error'
                lastMessage.streaming = false
              }
            }
            return newMessages
          })
        }
      )

      // 保存 sessionId / Save sessionId
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

      // 获取相似问题 / Get similar questions
      try {
        const similarData = await qaApi.getSimilarQuestions(question)
        if (similarData?.data) {
          setSimilarQuestions(similarData.data)
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

