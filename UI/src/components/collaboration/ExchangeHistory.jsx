/**
 * 交换历史组件 (Exchange History Component)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useState, useEffect } from 'react'
import { Table, Tag, Empty, Spin } from 'antd'
import { useLanguage } from '../../contexts/LanguageContext'
import collaborationApi from '../../api/modules/collaboration'
import '../../assets/css/collaboration/exchange-history.css'

function ExchangeHistory() {
  const { t } = useLanguage()
  const [loading, setLoading] = useState(false)
  const [history, setHistory] = useState([])

  useEffect(() => {
    loadHistory()
  }, [])

  const loadHistory = async () => {
    setLoading(true)
    try {
      const response = await collaborationApi.getExchangeHistory()
      if (response?.data) {
        setHistory(response.data || [])
      }
    } catch (error) {
      console.error('Failed to load history:', error)
    } finally {
      setLoading(false)
    }
  }

  const getTypeColor = (type) => {
    const colors = {
      send: 'blue',
      receive: 'green',
      sync: 'orange',
    }
    return colors[type] || 'default'
  }

  const columns = [
    {
      title: t('collaboration.time'),
      dataIndex: 'timestamp',
      key: 'timestamp',
      render: (time) => new Date(time).toLocaleString(),
      width: 180,
    },
    {
      title: t('collaboration.type'),
      dataIndex: 'type',
      key: 'type',
      render: (type) => (
        <Tag color={getTypeColor(type)}>
          {t(`collaboration.exchangeType.${type}`)}
        </Tag>
      ),
      width: 100,
    },
    {
      title: t('collaboration.peer'),
      dataIndex: 'peerName',
      key: 'peerName',
      width: 150,
    },
    {
      title: t('collaboration.content'),
      dataIndex: 'content',
      key: 'content',
      ellipsis: true,
    },
    {
      title: t('collaboration.status'),
      dataIndex: 'status',
      key: 'status',
      render: (status) => (
        <Tag color={status === 'success' ? 'green' : 'red'}>
          {t(`collaboration.exchangeStatus.${status}`)}
        </Tag>
      ),
      width: 100,
    },
  ]

  if (loading) {
    return (
      <div className="exchange-history__loading">
        <Spin tip={t('common.loading')} size="large">
          <div style={{ padding: 50 }} />
        </Spin>
      </div>
    )
  }

  if (history.length === 0) {
    return (
      <Empty
        image={Empty.PRESENTED_IMAGE_SIMPLE}
        description={t('collaboration.noHistory')}
      />
    )
  }

  return (
    <div className="exchange-history">
      <Table
        dataSource={history}
        columns={columns}
        rowKey="id"
        pagination={{ pageSize: 10 }}
      />
    </div>
  )
}

export default ExchangeHistory

