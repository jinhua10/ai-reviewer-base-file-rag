/**
 * 主应用组件 (Main Application Component)
 *
 * 管理整体应用状态、布局和路由
 * (Manages overall application state, layout, and routing)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useState } from 'react'
import { Button, Space, Tag } from 'antd'
import { CheckCircleOutlined } from '@ant-design/icons'
import { LanguageProvider, useLanguage } from './contexts/LanguageContext'
import { Layout, Header, Footer } from './components/layout'
import { ErrorBoundary, Loading } from './components/common'
import './assets/css/layout.css'
import './assets/css/header.css'
import './assets/css/footer.css'
import './assets/css/loading.css'
import './assets/css/error-boundary.css'

/**
 * 应用内容组件 (App Content Component)
 * 使用语言上下文 (Uses language context)
 */
function AppContent() {
  const { t, language } = useLanguage()
  const [activeMenu, setActiveMenu] = useState('home')

  // 菜单点击处理 (Menu click handler)
  const handleMenuClick = (key) => {
    setActiveMenu(key)
    console.log('Navigate to:', key)
  }

  return (
    <ErrorBoundary>
      <Layout
        header={
          <Header
            activeKey={activeMenu}
            onMenuClick={handleMenuClick}
            showLanguageToggle={true}
          />
        }
        footer={<Footer />}
      >
        {/* 主内容 (Main content) */}
        <div className="welcome-message">
          <h2>
            <CheckCircleOutlined style={{ color: '#52c41a', marginRight: '8px' }} />
            {language === 'zh' ? '✅ Phase 7.2 组件开发中' : '✅ Phase 7.2 Components Development'}
          </h2>

          <Space direction="vertical" size="large" style={{ width: '100%' }}>
            {/* 已完成功能 (Completed features) */}
            <div className="feature-list">
              <h3>{language === 'zh' ? '✅ 已完成组件' : '✅ Completed Components'}</h3>
              <ul>
                <li>
                  <Tag color="success">Layout</Tag>
                  {language === 'zh' ? '布局容器组件' : 'Layout container component'}
                </li>
                <li>
                  <Tag color="success">Header</Tag>
                  {language === 'zh' ? '导航栏组件（含语言切换）' : 'Navigation bar with language toggle'}
                </li>
                <li>
                  <Tag color="success">Footer</Tag>
                  {language === 'zh' ? '页脚组件' : 'Footer component'}
                </li>
                <li>
                  <Tag color="success">Loading</Tag>
                  {language === 'zh' ? '加载动画组件' : 'Loading animation component'}
                </li>
                <li>
                  <Tag color="success">ErrorBoundary</Tag>
                  {language === 'zh' ? '错误边界组件' : 'Error boundary component'}
                </li>
              </ul>
            </div>

            {/* 测试区域 (Test area) */}
            <div className="test-area">
              <h3>{language === 'zh' ? '🧪 组件测试' : '🧪 Component Testing'}</h3>
              <Space wrap>
                <Button type="primary">{t('common.confirm')}</Button>
                <Button>{t('common.cancel')}</Button>
                <Button type="dashed">{t('common.search')}</Button>
                <Button danger>{t('common.delete')}</Button>
              </Space>
            </div>

            {/* Loading 测试 (Loading test) */}
            <div className="loading-demo">
              <h3>{language === 'zh' ? '📦 Loading 组件演示' : '📦 Loading Component Demo'}</h3>
              <Loading spinning={true} tip={t('common.loading')} />
            </div>
          </Space>
        </div>
      </Layout>
    </ErrorBoundary>
  )
}

/**
 * 主应用组件 (Main App Component)
 * 包装 LanguageProvider (Wraps LanguageProvider)
 */
function App() {
  return (
    <LanguageProvider>
      <AppContent />
    </LanguageProvider>
  )
}

export default App
