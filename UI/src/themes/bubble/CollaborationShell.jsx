/**
 * 协作面板 - 气泡主题UI壳子 / Collaboration Panel - Bubble Theme UI Shell
 *
 * 完全不同的UI展示，但使用相同的数据和actions
 * Completely different UI presentation, but uses same data and actions
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useEffect } from 'react';
import { useLanguage } from '../../contexts/LanguageContext';
import { useCollaborationBinding } from '../../adapters/CollaborationAdapter';
import './bubble-collaboration.css';

/**
 * 气泡主题 - 协作面板UI壳子 / Bubble Theme - Collaboration Panel UI Shell
 *
 * 使用浮动气泡式设计，完全不同的交互方式
 * Uses floating bubble design, completely different interaction
 */
function BubbleCollaborationShell() {
  const { t } = useLanguage();

  // 获取相同的数据绑定 / Get same data binding
  const { state, actions } = useCollaborationBinding();

  useEffect(() => {
    if (state.activeTab === 'peers' && state.peers.length === 0) {
      actions.loadPeers?.();
    }
  }, [state.activeTab]); // eslint-disable-line

  // 气泡式导航 / Bubble-style navigation
  const bubbles = [
    {
      key: 'peers',
      icon: '👥',
      label: t('collaboration.peers'),
      color: '#FF6B9D'
    },
    {
      key: 'exchange',
      icon: '🔄',
      label: t('collaboration.exchange'),
      color: '#C44569'
    },
    {
      key: 'topology',
      icon: '🌐',
      label: t('collaboration.topology'),
      color: '#6C5CE7'
    },
    {
      key: 'sync',
      icon: '⚡',
      label: t('collaboration.sync'),
      color: '#00B894'
    },
  ];

  // 渲染当前内容 / Render current content
  const renderContent = () => {
    switch (state.activeTab) {
      case 'peers':
        return (
          <div className="bubble-content">
            <h3 className="bubble-title">👥 {t('collaboration.peers')}</h3>
            {state.loading ? (
              <div className="bubble-loading">加载中...</div>
            ) : (
              <div className="bubble-list">
                {state.peers.map((peer) => (
                  <div key={peer.id} className="bubble-item" style={{ borderColor: bubbles[0].color }}>
                    <div className="bubble-item-icon">🔵</div>
                    <div className="bubble-item-content">
                      <div className="bubble-item-name">{peer.name}</div>
                      <div className="bubble-item-status">{peer.status}</div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        );
      case 'exchange':
        return (
          <div className="bubble-content">
            <h3 className="bubble-title">🔄 {t('collaboration.exchange')}</h3>
            {state.loading ? (
              <div className="bubble-loading">加载中...</div>
            ) : (
              <div className="bubble-list">
                {state.exchanges.map((ex) => (
                  <div key={ex.id} className="bubble-item" style={{ borderColor: bubbles[1].color }}>
                    <div className="bubble-item-content">
                      <div className="bubble-item-name">{ex.from} → {ex.to}</div>
                      <div className="bubble-item-status">{ex.time}</div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        );
      case 'topology':
        return (
          <div className="bubble-content">
            <h3 className="bubble-title">🌐 {t('collaboration.topology')}</h3>
            <div className="bubble-topology">
              {/* 气泡式网络拓扑展示 / Bubble-style network topology */}
              <div className="bubble-network">
                {state.topology.nodes?.map((node) => (
                  <div key={node.id} className="bubble-node" style={{ background: bubbles[2].color }}>
                    {node.label}
                  </div>
                ))}
              </div>
            </div>
          </div>
        );
      case 'sync':
        return (
          <div className="bubble-content">
            <h3 className="bubble-title">⚡ {t('collaboration.sync')}</h3>
            <div className="bubble-sync">
              <div className="bubble-sync-item">
                <span>最后同步:</span>
                <span>{state.syncStatus.lastSync}</span>
              </div>
              <div className="bubble-sync-item">
                <span>状态:</span>
                <span>{state.syncStatus.status}</span>
              </div>
            </div>
          </div>
        );
      default:
        return null;
    }
  };

  return (
    <div className="bubble-collaboration">
      {/* 浮动气泡导航 / Floating bubble navigation */}
      <div className="bubble-nav">
        {bubbles.map((bubble) => (
          <div
            key={bubble.key}
            className={`bubble-nav-item ${state.activeTab === bubble.key ? 'active' : ''}`}
            style={{
              background: state.activeTab === bubble.key ? bubble.color : '#f0f0f0'
            }}
            onClick={() => actions.switchTab(bubble.key)}
          >
            <div className="bubble-icon">{bubble.icon}</div>
            <div className="bubble-label">{bubble.label}</div>
          </div>
        ))}
      </div>

      {/* 内容区域 / Content area */}
      <div className="bubble-main">
        {renderContent()}
      </div>

      {/* 装饰气泡 / Decorative bubbles */}
      <div className="bubble-decoration">
        <div className="deco-bubble deco-1"></div>
        <div className="deco-bubble deco-2"></div>
        <div className="deco-bubble deco-3"></div>
      </div>
    </div>
  );
}

export default BubbleCollaborationShell;

