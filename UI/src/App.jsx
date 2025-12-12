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
import { LanguageProvider, useLanguage } from './contexts/LanguageContext'
import { Header } from './components/layout'
import { ErrorBoundary } from './components/common'
import { QAPanel } from './components/qa'
import { DocumentList } from './components/document'
import { RoleList } from './components/role'
import { FeedbackPanel } from './components/feedback'
import { CollaborationPanel } from './components/collaboration'
import { WishList } from './components/wish'
import { ServiceMarket } from './components/service'
import { UserProfile } from './components/profile'
import './assets/css/header.css'
import './assets/css/error-boundary.css'

/**
 * 应用内容组件 (App Content Component)
 * 使用语言上下文 (Uses language context)
 */
function AppContent() {
  const { t } = useLanguage()
  const [activeMenu, setActiveMenu] = useState('qa')

  // 菜单点击处理 (Menu click handler)
  const handleMenuClick = (key) => {
    setActiveMenu(key)
    console.log('Navigate to:', key)
  }

  /**
   * 渲染页面内容 (Render page content)
   */
  const renderContent = () => {
    switch (activeMenu) {
      case 'qa':
        return <QAPanel />
      case 'documents':
        return <DocumentList />
      case 'roles':
        return <RoleList />
      case 'feedback':
        return <FeedbackPanel />
      case 'collaboration':
        return <CollaborationPanel />
      case 'wish':
        return <WishList />
      case 'aiService':
        return <ServiceMarket />
      case 'profile':
        return <UserProfile />
      default:
        return <QAPanel />
    }
  }

  return (
    <ErrorBoundary>
      <div style={{ display: 'flex', flexDirection: 'column', height: '100vh' }}>
        {/* 导航栏 */}
        <Header
          activeKey={activeMenu}
          onMenuClick={handleMenuClick}
          showLanguageToggle={true}
        />

        {/* 主内容 */}
        <div style={{ flex: 1, overflow: 'hidden' }}>
          {renderContent()}
        </div>
      </div>
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
