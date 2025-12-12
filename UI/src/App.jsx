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
import { ConfigProvider, theme as antdTheme } from 'antd'
import { LanguageProvider, useLanguage } from './contexts/LanguageContext'
import { ThemeProvider, useTheme } from './contexts/ThemeContext'
import { ModernLayout } from './components/layout'
import { ErrorBoundary } from './components/common'
import { QAPanel } from './components/qa'
import { DocumentList } from './components/document'
import { RoleList } from './components/role'
import { FeedbackPanel } from './components/feedback'
import { CollaborationPanel } from './components/collaboration'
import { WishList } from './components/wish'
import { ServiceMarket } from './components/service'
import { UserProfile } from './components/profile'
import { AdminPanel } from './components/admin'
import './assets/css/main.css'
import './assets/css/error-boundary.css'

/**
 * 应用内容组件 / App Content Component
 * 使用语言和主题上下文 / Uses language and theme context
 */
function AppContent() {
  const { t } = useLanguage()
  const { theme: currentTheme, themeName } = useTheme()
  const [activeMenu, setActiveMenu] = useState('qa')

  // 菜单点击处理 / Menu click handler
  const handleMenuClick = (key) => {
    setActiveMenu(key)
    console.log('Navigate to:', key)
  }

  /**
   * 渲染页面内容 / Render page content
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
      case 'admin':
        return <AdminPanel />
      default:
        return <QAPanel />
    }
  }

  // 配置Ant Design主题 / Configure Ant Design theme
  const antdThemeConfig = {
    algorithm: themeName === 'dark' ? antdTheme.darkAlgorithm : antdTheme.defaultAlgorithm,
    token: {
      colorPrimary: currentTheme.primary,
      colorBgContainer: currentTheme.surface,
      colorBgElevated: currentTheme.surface,
      colorText: currentTheme.text,
      colorTextSecondary: currentTheme.textSecondary,
      colorBorder: currentTheme.border,
    },
  }

  return (
    <ConfigProvider theme={antdThemeConfig}>
      <ErrorBoundary>
        <ModernLayout
          activeKey={activeMenu}
          onMenuChange={handleMenuClick}
        >
          {renderContent()}
        </ModernLayout>
      </ErrorBoundary>
    </ConfigProvider>
  )
}

/**
 * 主应用组件 (Main App Component)
 * 包装 LanguageProvider 和 ThemeProvider (Wraps LanguageProvider and ThemeProvider)
 */
function App() {
  return (
    <ThemeProvider>
      <LanguageProvider>
        <AppContent />
      </LanguageProvider>
    </ThemeProvider>
  )
}

export default App
