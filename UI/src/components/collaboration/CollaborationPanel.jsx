/**
 * 协作网络主面板 (Collaboration Panel Component)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useState } from 'react'
import { Tabs } from 'antd'
import { TeamOutlined, SwapOutlined, ClusterOutlined, SyncOutlined } from '@ant-design/icons'
import PeerList from './PeerList'
import ExchangeHistory from './ExchangeHistory'
import NetworkTopology from './NetworkTopology'
import SyncMonitor from './SyncMonitor'
import { useLanguage } from '../../contexts/LanguageContext'
import '../../assets/css/collaboration/collaboration-panel.css'

function CollaborationPanel() {
  const { t } = useLanguage()
  const [activeTab, setActiveTab] = useState('peers')

  const items = [
    {
      key: 'peers',
      label: (
        <span>
          <TeamOutlined />
          {t('collaboration.peers')}
        </span>
      ),
      children: <PeerList />,
    },
    {
      key: 'exchange',
      label: (
        <span>
          <SwapOutlined />
          {t('collaboration.exchange')}
        </span>
      ),
      children: <ExchangeHistory />,
    },
    {
      key: 'topology',
      label: (
        <span>
          <ClusterOutlined />
          {t('collaboration.topology')}
        </span>
      ),
      children: <NetworkTopology />,
    },
    {
      key: 'sync',
      label: (
        <span>
          <SyncOutlined />
          {t('collaboration.sync')}
        </span>
      ),
      children: <SyncMonitor />,
    },
  ]

  return (
    <div className="collaboration-panel">
      <div className="collaboration-panel__header">
        <h2>{t('collaboration.title')}</h2>
      </div>

      <div className="collaboration-panel__content">
        <Tabs
          activeKey={activeTab}
          onChange={setActiveTab}
          items={items}
          size="large"
          className="collaboration-panel__tabs"
        />
      </div>
    </div>
  )
}

export default CollaborationPanel

