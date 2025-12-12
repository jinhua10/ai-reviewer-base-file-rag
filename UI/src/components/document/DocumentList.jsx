/**
 * 文档列表组件 / Document List Component
 * 
 * 提供文档管理的完整功能，包括：
 * - 文档列表展示（带分页）
 * - 文档搜索功能
 * - 文档上传功能
 * - 文档删除操作
 * - 文档下载功能
 * - 文档详情查看
 * 
 * Provides complete document management features including:
 * - Document list display (with pagination)
 * - Document search functionality
 * - Document upload functionality
 * - Document deletion operations
 * - Document download functionality
 * - Document detail viewing
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
  // ============================================================================
  // Hooks / 钩子
  // ============================================================================
  const { t } = useLanguage()

  // ============================================================================
  // State / 状态管理
  // ============================================================================
  
  // 文档列表状态 (Document list state)
  const [documents, setDocuments] = useState([]) // 文档数组 (Documents array)
  const [loading, setLoading] = useState(false) // 加载状态 (Loading state)
  const [total, setTotal] = useState(0) // 文档总数 (Total documents count)
  
  // UI 状态 (UI state)
  const [uploadVisible, setUploadVisible] = useState(false) // 上传对话框可见性 (Upload dialog visibility)
  const [detailVisible, setDetailVisible] = useState(false) // 详情对话框可见性 (Detail dialog visibility)
  const [selectedDocument, setSelectedDocument] = useState(null) // 选中的文档 (Selected document)
  
  // 搜索参数 (Search parameters)
  const [searchParams, setSearchParams] = useState({
    keyword: '', // 搜索关键词 (Search keyword)
    page: 1, // 当前页码 (Current page number)
    pageSize: 20, // 每页数量 (Items per page)
  })

  // ============================================================================
  // API Functions / API 函数
  // ============================================================================
  
  /**
   * 加载文档列表 (Load documents list)
   * 
   * 根据当前搜索参数从后端获取文档列表
   * Fetch documents list from backend based on current search parameters
   * 
   * @returns {Promise<void>}
   */
  const loadDocuments = useCallback(async () => {
    setLoading(true)
    try {
      const response = await documentApi.getList(searchParams)
      console.log('API Response:', response) // 调试日志 (Debug log)
      if (response) {
        // axios 拦截器已返回 response.data，直接使用 (Axios interceptor returns response.data directly)
        // 后端返回 documents 字段 (Backend returns documents field)
        // ListResponse: { success, documents: DocumentInfo[], total, page, pageSize, totalPages }
        const documentsList = response.documents || []
        console.log('Documents:', documentsList) // 调试日志 (Debug log)
        
        // 映射后端字段到前端期望的字段 (Map backend fields to frontend expected fields)
        const mappedDocuments = documentsList.map(doc => ({
          ...doc,
          name: doc.fileName, // 后端返回 fileName (Backend returns fileName)
          size: doc.fileSize, // 后端返回 fileSize (Backend returns fileSize)
          id: doc.fileName, // 使用 fileName 作为 id (Use fileName as id)
        }))
        
        setDocuments(mappedDocuments)
        setTotal(response.total || 0)
      }
    } catch (error) {
      console.error('Failed to load documents:', error)
      message.error(t('document.loadFailed'))
    } finally {
      setLoading(false)
    }
  }, [searchParams, t])

  /**
   * 初始化 - 加载文档列表 (Initialize - load documents list)
   */
  useEffect(() => {
    loadDocuments()
  }, [loadDocuments])

  // ============================================================================
  // Event Handlers / 事件处理函数
  // ============================================================================
  
  /**
   * 处理搜索事件 (Handle search event)
   * 
   * 更新搜索关键词并重置到第一页
   * Update search keyword and reset to first page
   * 
   * @param {string} keyword - 搜索关键词 (Search keyword)
   */
  const handleSearch = useCallback((keyword) => {
    setSearchParams(prev => ({
      ...prev,
      keyword,
      page: 1,
    }))
  }, [])

  /**
   * 处理刷新事件 (Handle refresh event)
   * 
   * 重新加载文档列表
   * Reload documents list
   */
  const handleRefresh = useCallback(() => {
    loadDocuments()
  }, [loadDocuments])

  /**
   * 处理上传成功事件 (Handle upload success event)
   * 
   * 关闭上传对话框，显示成功消息，并刷新文档列表
   * Close upload dialog, show success message, and refresh documents list
   * 
   * @returns {Promise<void>}
   */
  const handleUploadSuccess = useCallback(() => {
    setUploadVisible(false)
    message.success(t('document.uploadSuccess'))
    loadDocuments()
  }, [loadDocuments, t])

  /**
   * 处理查看详情事件 (Handle view detail event)
   * 
   * 打开文档详情对话框
   * Open document detail dialog
   * 
   * @param {Object} doc - 文档对象 (Document object)
   */
  const handleViewDetail = useCallback((doc) => {
    setSelectedDocument(doc)
    setDetailVisible(true)
  }, [])

  /**
   * 处理文档删除事件 (Handle document deletion event)
   * 
   * 显示确认对话框，确认后删除文档并刷新列表
   * Show confirmation dialog, delete document after confirmation and refresh list
   * 
   * @param {Object} doc - 要删除的文档对象 (Document object to delete)
   * @param {string} doc.id - 文档ID (Document ID)
   * @param {string} doc.name - 文档名称 (Document name)
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
          // 后端删除接口使用 fileName 作为路径参数 (Backend delete API uses fileName as path parameter)
          await documentApi.delete(doc.fileName || doc.name)
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
   * 处理文档下载事件 (Handle document download event)
   * 
   * 从后端下载文档并触发浏览器下载
   * Download document from backend and trigger browser download
   * 
   * @param {Object} doc - 要下载的文档对象 (Document object to download)
   * @param {string} doc.id - 文档ID (Document ID)
   * @param {string} doc.name - 文档名称 (Document name)
   * @returns {Promise<void>}
   */
  const handleDownload = useCallback(async (doc) => {
    try {
      // 后端下载接口使用 fileName 作为路径参数 (Backend download API uses fileName as path parameter)
      const blob = await documentApi.download(doc.fileName || doc.name)
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

