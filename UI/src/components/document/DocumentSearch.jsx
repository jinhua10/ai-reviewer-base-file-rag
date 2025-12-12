/**
 * 文档搜索组件 (Document Search Component)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useState, useCallback } from 'react'
import { Input } from 'antd'
import { SearchOutlined } from '@ant-design/icons'
import { useLanguage } from '../../contexts/LanguageContext'
import '../../assets/css/document/document-search.css'

const { Search } = Input

function DocumentSearch(props) {
  const { onSearch } = props
  const { t } = useLanguage()
  const [value, setValue] = useState('')

  const handleSearch = useCallback((searchValue) => {
    if (onSearch) {
      onSearch(searchValue)
    }
  }, [onSearch])

  return (
    <div className="document-search">
      <Search
        placeholder={t('document.searchPlaceholder')}
        allowClear
        enterButton
        size="large"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        onSearch={handleSearch}
        prefix={<SearchOutlined />}
        className="document-search__input"
      />
    </div>
  )
}

export default DocumentSearch

