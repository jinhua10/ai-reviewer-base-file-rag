/**
 * 聊天框组件 (Chat Box Component)
 *
 * 展示问答消息列表，支持滚动、加载状态
 * (Displays Q&A message list with scrolling and loading states)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useRef, useEffect } from 'react'
import { Button } from 'antd'
import { HistoryOutlined } from '@ant-design/icons'
import AnswerCard from './AnswerCard'
import { useLanguage } from '../../contexts/LanguageContext'
import '../../assets/css/qa/chat-box.css'

function ChatBox(props) {
  const { 
    messages, 
    loading, 
    onFeedback, 
    onToggleHistory, 
    onStopGeneration, 
    isGenerating,
    isStreamingMode,
    onToggleStreamingMode,
    useKnowledgeBase,
    onToggleKnowledgeBase 
  } = props
  const { t } = useLanguage()
  const messagesEndRef = useRef(null)

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages])

  return (
    <div className="chat-box">
      <div className="chat-box__toolbar">
        <div className="chat-box__toolbar-left">
          <Button
            icon={<HistoryOutlined />}
            onClick={onToggleHistory}
            className="chat-box__history-btn"
          >
            {t('qa.history.title')}
          </Button>
        </div>
        
        <div className="chat-box__toolbar-right">
          <Button
            onClick={onToggleKnowledgeBase}
            className={`chat-box__kb-toggle ${useKnowledgeBase ? 'chat-box__kb-toggle--active' : ''}`}
            title={useKnowledgeBase ? t('qa.knowledgeBase.disable') : t('qa.knowledgeBase.enable')}
          >
            {useKnowledgeBase ? '📚 ' + t('qa.knowledgeBase.enabled') : '🤖 ' + t('qa.knowledgeBase.disabled')}
          </Button>
          
          <Button
            onClick={onToggleStreamingMode}
            className="chat-box__mode-toggle"
            title={isStreamingMode ? t('qa.mode.switchToNonStreaming') : t('qa.mode.switchToStreaming')}
          >
            {isStreamingMode ? '⚡ ' + t('qa.mode.streaming') : '💭 ' + t('qa.mode.nonStreaming')}
          </Button>
        </div>
      </div>

      <div className="chat-box__messages">
        {messages.length === 0 ? (
          <div className="chat-box__empty">
            <div className="chat-box__empty-icon">💬</div>
            <p className="chat-box__empty-text">{t('qa.emptyMessage')}</p>
          </div>
        ) : (
          messages.map((message) => (
            <div
              key={message.id}
              className={`chat-box__message chat-box__message--${message.type}`}
            >
              {message.type === 'question' ? (
                <div className="chat-box__question">
                  <div className="chat-box__question-avatar">👤</div>
                  <div className="chat-box__question-content">
                    <div className="chat-box__question-text">{message.content}</div>
                    <div className="chat-box__question-time">
                      {new Date(message.timestamp).toLocaleTimeString()}
                    </div>
                  </div>
                </div>
              ) : message.type === 'error' ? (
                <div className="chat-box__error">
                  <div className="chat-box__error-icon">⚠️</div>
                  <div className="chat-box__error-content">{message.content}</div>
                </div>
              ) : (
                <AnswerCard answer={message} onFeedback={onFeedback} />
              )}
            </div>
          ))
        )}

        {loading && (
          <div className="chat-box__loading">
            <div className="chat-box__loading-dots">
              <span></span>
              <span></span>
              <span></span>
            </div>
          </div>
        )}

        {isGenerating && (
          <div className="chat-box__stop-btn-wrapper">
            <Button
              danger
              onClick={onStopGeneration}
              className="chat-box__stop-btn"
            >
              🛑 {t('qa.stopGeneration')}
            </Button>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>
    </div>
  )
}

export default ChatBox

