/**
 * 文档列表组件 (Document List Component)
 *
 * 展示文档列表，支持搜索、上传、删除等操作
 * (Displays document list with search, upload, delete operations)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useState, useEffect, useCallback } from 'react'
import { Button, Space, message, Modal } from 'antd'
import { PlusOutlined, ReloadOutlined } from '@ant-design/icons'
import DocumentCard from './DocumentCard'
import DocumentUpload from './DocumentUpload'
import DocumentSearch from './DocumentSearch'
import DocumentDetail from './DocumentDetail'
import { Loading } from '../common'
import { useLanguage } from '../../contexts/LanguageContext'
import documentApi from '../../api/modules/document'
import '../../assets/css/document/document-list.css'

function DocumentList() {
  const { t } = useLanguage()

  // 状态管理
  const [documents, setDocuments] = useState([])
  const [loading, setLoading] = useState(false)
  const [uploadVisible, setUploadVisible] = useState(false)
  const [detailVisible, setDetailVisible] = useState(false)
  const [selectedDocument, setSelectedDocument] = useState(null)
  const [searchParams, setSearchParams] = useState({
    keyword: '',
    page: 1,
    pageSize: 20,
  })
  const [total, setTotal] = useState(0)

  /**
   * 加载文档列表
   */
  const loadDocuments = useCallback(async () => {
    setLoading(true)
    try {
      const response = await documentApi.getList(searchParams)
      if (response?.data) {
        setDocuments(response.data.list || [])
        setTotal(response.data.total || 0)
      }
    } catch (error) {
      console.error('Failed to load documents:', error)
      message.error(t('document.loadFailed'))
    } finally {
      setLoading(false)
    }
  }, [searchParams, t])

  /**
   * 初始加载
   */
  useEffect(() => {
    loadDocuments()
  }, [loadDocuments])

  /**
   * 处理搜索
   */
  const handleSearch = useCallback((keyword) => {
    setSearchParams(prev => ({
      ...prev,
      keyword,
      page: 1,
    }))
  }, [])

  /**
   * 处理刷新
   */
  const handleRefresh = useCallback(() => {
    loadDocuments()
  }, [loadDocuments])

  /**
   * 处理上传成功
   */
  const handleUploadSuccess = useCallback(() => {
    setUploadVisible(false)
    message.success(t('document.uploadSuccess'))
    loadDocuments()
  }, [loadDocuments, t])

  /**
   * 处理查看详情
   */
  const handleViewDetail = useCallback((doc) => {
    setSelectedDocument(doc)
    setDetailVisible(true)
  }, [])

  /**
   * 处理删除
   */
  const handleDelete = useCallback((doc) => {
    Modal.confirm({
      title: t('document.deleteConfirm'),
      content: doc.name,
      okText: t('common.confirm'),
      cancelText: t('common.cancel'),
      okType: 'danger',
      onOk: async () => {
        try {
          await documentApi.delete(doc.id)
          message.success(t('document.deleteSuccess'))
          loadDocuments()
        } catch (error) {
          console.error('Failed to delete document:', error)
          message.error(t('document.deleteFailed'))
        }
      },
    })
  }, [loadDocuments, t])

  /**
   * 处理下载
   */
  const handleDownload = useCallback(async (doc) => {
    try {
      const blob = await documentApi.download(doc.id)
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = doc.name
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      message.success(t('document.downloadSuccess'))
    } catch (error) {
      console.error('Failed to download document:', error)
      message.error(t('document.downloadFailed'))
    }
  }, [t])

  return (
    <div className="document-list">
      {/* 顶部工具栏 */}
      <div className="document-list__header">
        <div className="document-list__title">
          <h2>{t('document.title')}</h2>
          <span className="document-list__count">
            {t('document.total', { count: total })}
          </span>
        </div>

        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={handleRefresh}
            loading={loading}
          >
            {t('common.refresh')}
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setUploadVisible(true)}
          >
            {t('document.upload')}
          </Button>
        </Space>
      </div>

      {/* 搜索栏 */}
      <div className="document-list__search">
        <DocumentSearch onSearch={handleSearch} />
      </div>

      {/* 文档列表 */}
      <div className="document-list__content">
        {loading ? (
          <Loading spinning={true} tip={t('common.loading')} />
        ) : documents.length === 0 ? (
          <div className="document-list__empty">
            <div className="document-list__empty-icon">📄</div>
            <p className="document-list__empty-text">
              {searchParams.keyword
                ? t('document.noSearchResults')
                : t('document.noDocuments')}
            </p>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => setUploadVisible(true)}
            >
              {t('document.uploadFirst')}
            </Button>
          </div>
        ) : (
          <div className="document-list__grid">
            {documents.map((doc) => (
              <DocumentCard
                key={doc.id}
                document={doc}
                onView={handleViewDetail}
                onDelete={handleDelete}
                onDownload={handleDownload}
              />
            ))}
          </div>
        )}
      </div>

      {/* 上传对话框 */}
      <DocumentUpload
        visible={uploadVisible}
        onCancel={() => setUploadVisible(false)}
        onSuccess={handleUploadSuccess}
      />

      {/* 详情对话框 */}
      <DocumentDetail
        visible={detailVisible}
        document={selectedDocument}
        onClose={() => setDetailVisible(false)}
      />
    </div>
  )
}

export default DocumentList

