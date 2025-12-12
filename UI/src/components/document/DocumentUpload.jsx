/**
 * 文档上传组件 (Document Upload Component)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useState } from 'react'
import { Modal, message } from 'antd'
import UploadDropZone from './UploadDropZone'
import { useLanguage } from '../../contexts/LanguageContext'
import documentApi from '../../api/modules/document'

function DocumentUpload(props) {
  const { visible, onCancel, onSuccess } = props
  const { t } = useLanguage()
  const [uploading, setUploading] = useState(false)
  const [progress, setProgress] = useState(0)

  const handleUpload = async (file) => {
    const formData = new FormData()
    formData.append('file', file)

    setUploading(true)
    setProgress(0)

    try {
      await documentApi.upload(formData, (percent) => {
        setProgress(percent)
      })

      if (onSuccess) {
        onSuccess()
      }
    } catch (error) {
      console.error('Upload failed:', error)
      message.error(t('document.uploadFailed'))
    } finally {
      setUploading(false)
      setProgress(0)
    }
  }

  return (
    <Modal
      title={t('document.upload')}
      open={visible}
      onCancel={onCancel}
      footer={null}
      width={600}
    >
      <UploadDropZone
        onUpload={handleUpload}
        uploading={uploading}
        progress={progress}
      />
    </Modal>
  )
}

export default DocumentUpload

