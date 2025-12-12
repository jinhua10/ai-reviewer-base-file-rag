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
    const iconMap = {
      pdf: '📕',
      doc: '📘',
      docx: '📘',
      txt: '📄',
      md: '📝',
      xls: '📊',
      xlsx: '📊',
      ppt: '📙',
      pptx: '📙',
      zip: '📦',
      rar: '📦',
    }
    return iconMap[ext] || '📄'
  }

  return (
    <Card
      className="document-card"
      hoverable
      onClick={() => onView && onView(document)}
    >
      <div className="document-card__icon">
        <span className="document-card__icon-emoji">{getFileIcon()}</span>
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

