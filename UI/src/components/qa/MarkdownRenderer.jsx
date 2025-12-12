/**
 * Markdown 渲染器组件 (Markdown Renderer Component)
 *
 * 支持完整的 Markdown 语法：标题、列表、表格、代码块、引用、图片等
 * (Supports full Markdown syntax: headings, lists, tables, code blocks, quotes, images, etc.)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useMemo } from 'react'
import ReactMarkdown from 'react-markdown'
import remarkGfm from 'remark-gfm'
import rehypeRaw from 'rehype-raw'
import CodeBlock from './CodeBlock'
import '../../assets/css/qa/markdown-renderer.css'

function MarkdownRenderer(props) {
  const { content } = props

  // 自定义组件渲染
  const components = useMemo(() => ({
    // 代码块 - 使用自定义 CodeBlock 组件
    code({ node, inline, className, children, ...props }) {
      const match = /language-(\w+)/.exec(className || '')
      const language = match ? match[1] : 'text'
      const code = String(children).replace(/\n$/, '')

      return !inline ? (
        <CodeBlock code={code} language={language} />
      ) : (
        <code className="markdown-renderer__inline-code" {...props}>
          {children}
        </code>
      )
    },

    // 图片 - 添加样式和懒加载
    img({ node, src, alt, ...props }) {
      return (
        <img
          src={src}
          alt={alt || ''}
          className="markdown-renderer__image"
          loading="lazy"
          {...props}
        />
      )
    },

    // 链接 - 添加安全属性
    a({ node, href, children, ...props }) {
      return (
        <a
          href={href}
          target="_blank"
          rel="noopener noreferrer"
          {...props}
        >
          {children}
        </a>
      )
    },

    // 表格 - 添加响应式容器
    table({ node, children, ...props }) {
      return (
        <div className="markdown-renderer__table-container">
          <table className="markdown-renderer__table" {...props}>
            {children}
          </table>
        </div>
      )
    },

    // 标题 - 添加锚点
    h1({ node, children, ...props }) {
      return <h1 className="markdown-renderer__h1" {...props}>{children}</h1>
    },
    h2({ node, children, ...props }) {
      return <h2 className="markdown-renderer__h2" {...props}>{children}</h2>
    },
    h3({ node, children, ...props }) {
      return <h3 className="markdown-renderer__h3" {...props}>{children}</h3>
    },
    h4({ node, children, ...props }) {
      return <h4 className="markdown-renderer__h4" {...props}>{children}</h4>
    },
    h5({ node, children, ...props }) {
      return <h5 className="markdown-renderer__h5" {...props}>{children}</h5>
    },
    h6({ node, children, ...props }) {
      return <h6 className="markdown-renderer__h6" {...props}>{children}</h6>
    },

    // 引用块
    blockquote({ node, children, ...props }) {
      return (
        <blockquote className="markdown-renderer__blockquote" {...props}>
          {children}
        </blockquote>
      )
    },

    // 列表
    ul({ node, children, ...props }) {
      return <ul className="markdown-renderer__ul" {...props}>{children}</ul>
    },
    ol({ node, children, ...props }) {
      return <ol className="markdown-renderer__ol" {...props}>{children}</ol>
    },
    li({ node, children, ...props }) {
      return <li className="markdown-renderer__li" {...props}>{children}</li>
    },

    // 段落
    p({ node, children, ...props }) {
      return <p className="markdown-renderer__paragraph" {...props}>{children}</p>
    },

    // 分隔线
    hr({ node, ...props }) {
      return <hr className="markdown-renderer__hr" {...props} />
    },

    // 任务列表（GitHub Flavored Markdown）
    input({ node, ...props }) {
      return <input className="markdown-renderer__checkbox" {...props} />
    },
  }), [])

  return (
    <div className="markdown-renderer">
      <ReactMarkdown
        remarkPlugins={[remarkGfm]}  // 支持 GitHub Flavored Markdown（表格、任务列表、删除线等）
        rehypePlugins={[rehypeRaw]}  // 支持原始 HTML
        components={components}
      >
        {content || ''}
      </ReactMarkdown>
    </div>
  )
}

export default MarkdownRenderer

