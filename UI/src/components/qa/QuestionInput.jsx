/**
 * 问题输入框组件 (Question Input Component)
 *
 * 支持多行输入、快捷键提交、字数统计
 * (Supports multi-line input, keyboard shortcuts, character count)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useState, useRef, useCallback } from 'react'
import { Button, Input } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import { useLanguage } from '../../contexts/LanguageContext'
import '../../assets/css/qa/question-input.css'

const { TextArea } = Input

function QuestionInput(props) {
  const { onSubmit, loading, placeholder } = props
  const { t } = useLanguage()
  const [value, setValue] = useState('')
  const textAreaRef = useRef(null)

  const handleChange = useCallback((e) => {
    setValue(e.target.value)
  }, [])

  const handleSubmit = useCallback(() => {
    const trimmedValue = value.trim()
    if (trimmedValue && !loading) {
      onSubmit(trimmedValue)
      setValue('')
      if (textAreaRef.current) {
        textAreaRef.current.focus()
      }
    }
  }, [value, loading, onSubmit])

  const handleKeyDown = useCallback((e) => {
    if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
      e.preventDefault()
      handleSubmit()
    }
  }, [handleSubmit])

  const getCharCountText = () => {
    const count = value.length
    if (count === 0) return ''
    return `${count} ${t('qa.input.characters')}`
  }

  return (
    <div className="question-input">
      <div className="question-input__container">
        <TextArea
          ref={textAreaRef}
          value={value}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          placeholder={placeholder || t('qa.input.placeholder')}
          autoSize={{ minRows: 2, maxRows: 6 }}
          disabled={loading}
          className="question-input__textarea"
        />

        <div className="question-input__footer">
          <div className="question-input__hints">
            <span className="question-input__hint">{t('qa.input.hint')}</span>
            {value.length > 0 && (
              <span className="question-input__char-count">{getCharCountText()}</span>
            )}
          </div>

          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleSubmit}
            loading={loading}
            disabled={!value.trim() || loading}
            className="question-input__submit-btn"
          >
            {t('qa.input.send')}
          </Button>
        </div>
      </div>
    </div>
  )
}

export default QuestionInput

