/**
 * 应用入口文件 (Application Entry Point)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './assets/css/reset.css'
import './assets/css/main.css'

// 渲染应用 (Render application)
ReactDOM.createRoot(document.getElementById('root')).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>
)

