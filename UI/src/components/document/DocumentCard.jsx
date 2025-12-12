/**
 * 文档卡片组件 (Document Card Component)
 *
 * 展示单个文档的卡片视图
 * (Displays a card view of a single document)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React from 'react'
import { Card, Button, Space, Tooltip, Tag } from 'antd'
import {
  EyeOutlined,
  DownloadOutlined,
  DeleteOutlined,
  FileWordOutlined,
  FileExcelOutlined,
  FilePptOutlined,
  FilePdfOutlined,
  FileTextOutlined,
  FileImageOutlined,
  FileZipOutlined,
  FileMarkdownOutlined,
  CodeOutlined,
  FileOutlined,
} from '@ant-design/icons'
import { useLanguage } from '../../contexts/LanguageContext'
import '../../assets/css/document/document-card.css'

function DocumentCard(props) {
  const { document, onView, onDelete, onDownload } = props
  const { t } = useLanguage()

  const formatFileSize = (bytes) => {
    if (bytes === 0) return '0 B'
    const k = 1024
    const sizes = ['B', 'KB', 'MB', 'GB']
    const i = Math.floor(Math.log(bytes) / Math.log(k))
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i]
  }

  const formatDate = (dateString) => {
    const date = new Date(dateString)
    return date.toLocaleDateString() + ' ' + date.toLocaleTimeString()
  }

  const getFileIcon = () => {
    const ext = document.name?.split('.').pop()?.toLowerCase()
    
    const iconConfig = {
      // Office Word - 蓝色
      doc: { icon: <FileWordOutlined />, color: '#2B579A', label: 'DOC' },
      docx: { icon: <FileWordOutlined />, color: '#2B579A', label: 'DOCX' },
      // Office Excel - 绿色
      xls: { icon: <FileExcelOutlined />, color: '#217346', label: 'XLS' },
      xlsx: { icon: <FileExcelOutlined />, color: '#217346', label: 'XLSX' },
      // Office PowerPoint - 橙红色
      ppt: { icon: <FilePptOutlined />, color: '#D24726', label: 'PPT' },
      pptx: { icon: <FilePptOutlined />, color: '#D24726', label: 'PPTX' },
      // PDF - 红色
      pdf: { icon: <FilePdfOutlined />, color: '#F40F02', label: 'PDF' },
      // Markdown
      md: { icon: <FileMarkdownOutlined />, color: '#000000', label: 'MD' },
      // 文本文件
      txt: { icon: <FileTextOutlined />, color: '#666666', label: 'TXT' },
      // 图片
      jpg: { icon: <FileImageOutlined />, color: '#87CEEB', label: 'JPG' },
      jpeg: { icon: <FileImageOutlined />, color: '#87CEEB', label: 'JPEG' },
      png: { icon: <FileImageOutlined />, color: '#87CEEB', label: 'PNG' },
      gif: { icon: <FileImageOutlined />, color: '#87CEEB', label: 'GIF' },
      bmp: { icon: <FileImageOutlined />, color: '#87CEEB', label: 'BMP' },
      svg: { icon: <FileImageOutlined />, color: '#FFB13B', label: 'SVG' },
      // 压缩文件
      zip: { icon: <FileZipOutlined />, color: '#FFA500', label: 'ZIP' },
      rar: { icon: <FileZipOutlined />, color: '#FFA500', label: 'RAR' },
      '7z': { icon: <FileZipOutlined />, color: '#FFA500', label: '7Z' },
      tar: { icon: <FileZipOutlined />, color: '#FFA500', label: 'TAR' },
      gz: { icon: <FileZipOutlined />, color: '#FFA500', label: 'GZ' },
      // 代码文件
      js: { icon: <CodeOutlined />, color: '#F7DF1E', label: 'JS' },
      jsx: { icon: <CodeOutlined />, color: '#61DAFB', label: 'JSX' },
      ts: { icon: <CodeOutlined />, color: '#3178C6', label: 'TS' },
      tsx: { icon: <CodeOutlined />, color: '#3178C6', label: 'TSX' },
      java: { icon: <CodeOutlined />, color: '#007396', label: 'JAVA' },
      py: { icon: <CodeOutlined />, color: '#3776AB', label: 'PY' },
      cpp: { icon: <CodeOutlined />, color: '#00599C', label: 'CPP' },
      c: { icon: <CodeOutlined />, color: '#A8B9CC', label: 'C' },
      html: { icon: <CodeOutlined />, color: '#E34F26', label: 'HTML' },
      css: { icon: <CodeOutlined />, color: '#1572B6', label: 'CSS' },
      json: { icon: <FileTextOutlined />, color: '#000000', label: 'JSON' },
      xml: { icon: <FileTextOutlined />, color: '#FF6600', label: 'XML' },
      yaml: { icon: <FileTextOutlined />, color: '#CB171E', label: 'YAML' },
      yml: { icon: <FileTextOutlined />, color: '#CB171E', label: 'YML' },
    }
    
    return iconConfig[ext] || { icon: <FileOutlined />, color: '#666666', label: ext?.toUpperCase() || 'FILE' }
  }

  const fileIconInfo = getFileIcon()

  return (
    <Card
      className="document-card"
      hoverable
      onClick={() => onView && onView(document)}
    >
      <div className="document-card__icon" style={{ position: 'relative' }}>
        <span 
          className="document-card__icon-emoji" 
          style={{ fontSize: '48px', color: fileIconInfo.color }}
        >
          {fileIconInfo.icon}
        </span>
        <span 
          style={{
            position: 'absolute',
            bottom: '-8px',
            left: '50%',
            transform: 'translateX(-50%)',
            fontSize: '10px',
            fontWeight: 'bold',
            color: fileIconInfo.color,
            backgroundColor: 'rgba(255, 255, 255, 0.9)',
            padding: '2px 6px',
            borderRadius: '4px',
            border: `1px solid ${fileIconInfo.color}`,
            whiteSpace: 'nowrap',
          }}
        >
          {fileIconInfo.label}
        </span>
      </div>

      <div className="document-card__info">
        <Tooltip title={document.name}>
          <h3 className="document-card__name">{document.name}</h3>
        </Tooltip>

        <div className="document-card__meta">
          <span className="document-card__size">
            {formatFileSize(document.size || 0)}
          </span>
          <span className="document-card__separator">•</span>
          <span className="document-card__date">
            {formatDate(document.uploadTime || document.createdAt)}
          </span>
        </div>

        {document.tags && document.tags.length > 0 && (
          <div className="document-card__tags">
            {document.tags.slice(0, 3).map((tag, index) => (
              <Tag key={index} className="document-card__tag">
                {tag}
              </Tag>
            ))}
            {document.tags.length > 3 && (
              <Tag className="document-card__tag">
                +{document.tags.length - 3}
              </Tag>
            )}
          </div>
        )}
      </div>

      <div
        className="document-card__actions"
        onClick={(e) => e.stopPropagation()}
      >
        <Space>
          <Tooltip title={t('document.view')}>
            <Button
              type="text"
              size="small"
              icon={<EyeOutlined />}
              onClick={() => onView && onView(document)}
            />
          </Tooltip>
          <Tooltip title={t('document.download')}>
            <Button
              type="text"
              size="small"
              icon={<DownloadOutlined />}
              onClick={() => onDownload && onDownload(document)}
            />
          </Tooltip>
          <Tooltip title={t('document.delete')}>
            <Button
              type="text"
              size="small"
              danger
              icon={<DeleteOutlined />}
              onClick={() => onDelete && onDelete(document)}
            />
          </Tooltip>
        </Space>
      </div>
    </Card>
  )
}

export default DocumentCard

