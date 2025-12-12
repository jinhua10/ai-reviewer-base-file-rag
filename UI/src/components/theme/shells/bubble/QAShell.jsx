/**
 * 气泡主题 - QA Shell / Bubble Theme - QA Shell
 * 智能问答页面的气泡主题实现
 */

import React from 'react';
import './bubble-common.css';

function QAShell() {
  return (
    <div className="bubble-qa-shell">
      <div className="qa-hero-section">
        <div className="hero-orb">
          <div className="orb-glow"></div>
          <div className="orb-content">
            <span className="hero-icon">💬</span>
            <h1 className="hero-title">智能问答</h1>
            <p className="hero-subtitle">AI驱动的智能对话系统</p>
          </div>
        </div>
      </div>

      <div className="qa-content-grid">
        <div className="qa-card bubble-glass-card">
          <div className="card-icon">🤖</div>
          <h3>AI助手</h3>
          <p>24/7在线智能问答服务</p>
        </div>
        
        <div className="qa-card bubble-glass-card">
          <div className="card-icon">📚</div>
          <h3>知识库</h3>
          <p>海量知识库支持</p>
        </div>
        
        <div className="qa-card bubble-glass-card">
          <div className="card-icon">⚡</div>
          <h3>快速响应</h3>
          <p>毫秒级响应速度</p>
        </div>
        
        <div className="qa-card bubble-glass-card">
          <div className="card-icon">🎯</div>
          <h3>精准答案</h3>
          <p>高准确度智能分析</p>
        </div>
      </div>
    </div>
  );
}

export default QAShell;
