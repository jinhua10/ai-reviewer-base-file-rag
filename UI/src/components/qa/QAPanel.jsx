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
      // 创建答案消息占位符
      const answerMessage = {
        id: Date.now() + 1,
        type: 'answer',
        content: '',
        streaming: true,
        timestamp: new Date().toISOString(),
      }
      setMessages(prev => [...prev, answerMessage])

      // 调用流式 API
      const eventSource = qaApi.askStreaming(
        { question },
        (data) => {
          // 更新答案内容
          setMessages(prev => {
            const newMessages = [...prev]
            const lastMessage = newMessages[newMessages.length - 1]
            if (lastMessage.streaming) {
              lastMessage.content += data.chunk || data.content || ''
              if (data.done) {
                lastMessage.streaming = false
              }
            }
            return newMessages
          })
        }
      )

      // 保存 eventSource 以便取消
      answerMessage.eventSource = eventSource

      // 获取相似问题
      const similarData = await qaApi.getSimilarQuestions(question)
      if (similarData?.data) {
        setSimilarQuestions(similarData.data)
      }

    } catch (error) {
      console.error('❌ Failed to ask question:', error)
      // 添加错误消息
      setMessages(prev => [...prev, {
        id: Date.now() + 2,
        type: 'error',
        content: t('qa.error.failed'),
        timestamp: new Date().toISOString(),
      }])
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

