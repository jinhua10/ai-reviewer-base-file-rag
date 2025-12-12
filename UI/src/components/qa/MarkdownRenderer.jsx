/**
 * Markdown 渲染器组件 (Markdown Renderer Component)
 *
 * 支持完整的 Markdown 语法：标题、列表、表格、代码块、引用、图片等
 * (Supports full Markdown syntax: headings, lists, tables, code blocks, quotes, images, etc.)
 *
 * 流式渲染优化：自动检测未完成的Markdown结构，避免渲染错误
 * (Streaming optimization: Auto-detect incomplete Markdown structures to avoid rendering errors)
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

/**
 * 检测内容中是否有未完成的Markdown结构
 * Detects incomplete Markdown structures in content
 */
const hasIncompleteStructure = (content) => {
  if (!content) return false;
  
  // 检测未闭合的代码块
  const codeBlockMatches = content.match(/```/g);
  if (codeBlockMatches && codeBlockMatches.length % 2 !== 0) {
    return true;
  }
  
  // 检测不完整的表格（以|开头但行数少于2）
  const lines = content.split('\n');
  const tableLines = lines.filter(line => line.trim().startsWith('|'));
  if (tableLines.length === 1) {
    return true; // 只有一行表格，可能不完整
  }
  
  // 检测表格分隔符是否完整
  const hasTableHeader = tableLines.some(line => /^\|[\s-:|]+\|$/.test(line.trim()));
  if (tableLines.length > 0 && !hasTableHeader && lines[lines.length - 1].trim().startsWith('|')) {
    return true; // 表格开始了但没有分隔符
  }
  
  return false;
};

/**
 * 处理流式内容，确保Markdown结构完整
 * Process streaming content to ensure complete Markdown structures
 */
const sanitizeStreamingContent = (content) => {
  if (!content) return '';
  
  let sanitized = content;
  
  // 处理未闭合的代码块：临时闭合
  const codeBlockMatches = sanitized.match(/```/g);
  if (codeBlockMatches && codeBlockMatches.length % 2 !== 0) {
    sanitized += '\n```';
  }
  
  // 处理不完整的表格：如果最后一行是表格行但没有完整结构，暂时移除
  const lines = sanitized.split('\n');
  const lastNonEmptyIndex = lines.findLastIndex(line => line.trim() !== '');
  if (lastNonEmptyIndex >= 0) {
    const lastLine = lines[lastNonEmptyIndex];
    if (lastLine.trim().startsWith('|')) {
      // 检查是否是完整的表格
      const tableLines = [];
      for (let i = lastNonEmptyIndex; i >= 0 && lines[i].trim().startsWith('|'); i--) {
        tableLines.unshift(lines[i]);
      }
      
      // 如果没有分隔符行，说明表格不完整
      const hasTableDivider = tableLines.some(line => /^\|[\s-:|]+\|$/.test(line.trim()));
      if (!hasTableDivider && tableLines.length < 2) {
        // 移除不完整的表格行
        lines.splice(lastNonEmptyIndex, 1);
        sanitized = lines.join('\n');
      }
    }
  }
  
  return sanitized;
};

function MarkdownRenderer(props) {
  const { content, isStreaming } = props
  
  // 如果是流式输出，清理不完整的Markdown结构
  const processedContent = useMemo(() => {
    if (isStreaming) {
      return sanitizeStreamingContent(content);
    }
    return content || '';
  }, [content, isStreaming])

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

    // 图片 - 添加样式和懒加载，完全信赖后端返回的路径
    img({ node, src, alt, ...props }) {
      // 只记录日志，不做任何路径修改
      if (src) {
        console.log('🖼️ Image src from backend:', src);
      }
      
      return (
        <img
          src={src}
          alt={alt || ''}
          className="markdown-renderer__image"
          loading="lazy"
          onError={(e) => {
            console.error('❌ Image failed to load:', src);
            console.error('Error event:', e);
            // 显示占位符而不是隐藏
            e.target.alt = alt || '图片加载失败 (Image load failed)';
          }}
          onLoad={(e) => {
            console.log('✅ Image loaded successfully:', src);
          }}
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
        {processedContent}
      </ReactMarkdown>
    </div>
  )
}

export default MarkdownRenderer

