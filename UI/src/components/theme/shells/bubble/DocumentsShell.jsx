/**
 * 气泡主题 - 文档管理 Shell / Bubble Theme - Documents Shell
 */

import React from 'react';
import './bubble-common.css';

function DocumentsShell() {
  return (
    <div className="bubble-documents-shell">
      <div className="docs-hero-section">
        <div className="hero-orb">
          <div className="orb-glow"></div>
          <div className="orb-content">
            <span className="hero-icon">📄</span>
            <h1 className="hero-title">文档管理</h1>
            <p className="hero-subtitle">智能文档处理与管理平台</p>
          </div>
        </div>
      </div>

      <div className="docs-content-grid">
        <div className="docs-card bubble-glass-card">
          <div className="card-icon">📁</div>
          <h3>文件库</h3>
          <p>集中管理所有文档</p>
        </div>
        
        <div className="docs-card bubble-glass-card">
          <div className="card-icon">🔍</div>
          <h3>智能搜索</h3>
          <p>快速定位所需文件</p>
        </div>
        
        <div className="docs-card bubble-glass-card">
          <div className="card-icon">✏️</div>
          <h3>在线编辑</h3>
          <p>实时协作编辑文档</p>
        </div>
        
        <div className="docs-card bubble-glass-card">
          <div className="card-icon">🔒</div>
          <h3>安全存储</h3>
          <p>企业级数据安全</p>
        </div>
      </div>
    </div>
  );
}

export default DocumentsShell;
