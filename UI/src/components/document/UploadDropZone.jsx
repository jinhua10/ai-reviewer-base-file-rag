/**
 * 拖拽上传区域组件 (Upload Drop Zone Component)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useCallback } from 'react'
import { Upload, Progress } from 'antd'
import { InboxOutlined } from '@ant-design/icons'
import { useLanguage } from '../../contexts/LanguageContext'
import '../../assets/css/document/upload-dropzone.css'

const { Dragger } = Upload

function UploadDropZone(props) {
  const { onUpload, uploading, progress } = props
  const { t } = useLanguage()

  const handleBeforeUpload = useCallback((file) => {
    const isValidSize = file.size / 1024 / 1024 < 100
    if (!isValidSize) {
      return false
    }

    if (onUpload) {
      onUpload(file)
    }
    return false
  }, [onUpload])

  return (
    <div className="upload-dropzone">
      <Dragger
        name="file"
        multiple={false}
        beforeUpload={handleBeforeUpload}
        showUploadList={false}
        disabled={uploading}
        className="upload-dropzone__dragger"
      >
        <p className="upload-dropzone__icon">
          <InboxOutlined />
        </p>
        <p className="upload-dropzone__text">{t('document.uploadTip')}</p>
        <p className="upload-dropzone__hint">{t('document.uploadHint')}</p>
      </Dragger>

      {uploading && (
        <div className="upload-dropzone__progress">
          <Progress percent={progress} status="active" />
        </div>
      )}
    </div>
  )
}

export default UploadDropZone

