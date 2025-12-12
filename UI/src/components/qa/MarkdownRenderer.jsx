/**
 * Markdown 渲染器组件 (Markdown Renderer Component)
 *
 * 渲染 Markdown 格式文本，支持代码高亮
 * (Renders Markdown formatted text with code highlighting)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useRef } from 'react'
import CodeBlock from './CodeBlock'
import '../../assets/css/qa/markdown-renderer.css'

function MarkdownRenderer(props) {
  const { content } = props
  const containerRef = useRef(null)

  const parseMarkdown = (text) => {
    if (!text) return []

    const elements = []
    const lines = text.split('\n')
    let i = 0
    let codeBlock = null
    let codeLines = []

    while (i < lines.length) {
      const line = lines[i]

      if (line.trim().startsWith('```')) {
        if (codeBlock === null) {
          const language = line.trim().slice(3).trim() || 'text'
          codeBlock = language
          codeLines = []
        } else {
          elements.push({
            type: 'code',
            language: codeBlock,
            content: codeLines.join('\n'),
            key: `code-${elements.length}`,
          })
          codeBlock = null
          codeLines = []
        }
      } else if (codeBlock !== null) {
        codeLines.push(line)
      } else {
        elements.push({
          type: 'text',
          content: line,
          key: `text-${elements.length}`,
        })
      }

      i++
    }

    if (codeBlock !== null && codeLines.length > 0) {
      elements.push({
        type: 'code',
        language: codeBlock,
        content: codeLines.join('\n'),
        key: `code-${elements.length}`,
      })
    }

    return elements
  }

  const renderText = (text) => {
    if (!text) return null

    let processed = text.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    processed = processed.replace(/\*(.+?)\*/g, '<em>$1</em>')
    processed = processed.replace(/`(.+?)`/g, '<code class="markdown-renderer__inline-code">$1</code>')
    processed = processed.replace(/\[(.+?)\]\((.+?)\)/g, '<a href="$2" target="_blank" rel="noopener noreferrer">$1</a>')

    return <span dangerouslySetInnerHTML={{ __html: processed }} />
  }

  const elements = parseMarkdown(content)

  return (
    <div ref={containerRef} className="markdown-renderer">
      {elements.map((element) => {
        if (element.type === 'code') {
          return <CodeBlock key={element.key} code={element.content} language={element.language} />
        } else {
          return (
            <p key={element.key} className="markdown-renderer__paragraph">
              {renderText(element.content)}
            </p>
          )
        }
      })}
    </div>
  )
}

export default MarkdownRenderer

