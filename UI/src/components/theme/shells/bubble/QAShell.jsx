/**
 * 气泡主题 - QA Shell / Bubble Theme - QA Shell
 * 智能问答页面的气泡主题实现
 * 
 * 【重要】使用统一的数据适配器获取数据
 * 后端联调时只需修改 PageDataAdapter.jsx
 */

import React from 'react';
import { useQAPageData } from '../../../../adapters/PageDataAdapter';
import './bubble-common.css';

function QAShell() {
  // 使用统一的数据适配器
  const { stats, recentQuestions, loading, error } = useQAPageData();

  if (loading) {
    return (
      <div className="bubble-qa-shell">
        <div className="qa-hero-section">
          <div className="hero-orb">
            <div className="orb-glow"></div>
            <div className="orb-content">
              <span className="hero-icon">⏳</span>
              <h1 className="hero-title">加载中...</h1>
            </div>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bubble-qa-shell">
      <div className="qa-hero-section">
        <div className="hero-orb">
          <div className="orb-glow"></div>
          <div className="orb-content">
            <span className="hero-icon">💬</span>
            <h1 className="hero-title">智能问答</h1>
            <p className="hero-subtitle">AI驱动的智能对话系统</p>
            {stats && <p className="hero-stats">共 {stats.totalQuestions} 个问题</p>}
          </div>
        </div>
      </div>

      <div className="qa-content-grid">
        <div className="qa-card bubble-glass-card">
          <div className="card-icon">🤖</div>
          <h3>AI助手</h3>
          <p>24/7在线智能问答服务</p>
          {stats && <span className="card-stat">{stats.activeUsers} 在线</span>}
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
          {stats && <span className="card-stat">{stats.averageResponseTime}s 平均响应</span>}
        </div>
        
        <div className="qa-card bubble-glass-card">
          <div className="card-icon">🎯</div>
          <h3>精准答案</h3>
          <p>高准确度智能分析</p>
          {stats && <span className="card-stat">{stats.satisfactionRate}% 满意度</span>}
        </div>
      </div>
    </div>
  );
}

export default QAShell;
