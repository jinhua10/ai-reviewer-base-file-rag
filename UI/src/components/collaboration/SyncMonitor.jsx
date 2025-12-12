/**
 * 同步监控组件 (Sync Monitor Component)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useState, useEffect } from 'react'
import { Card, Row, Col, Statistic, Progress, List, Tag, Spin } from 'antd'
import { SyncOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons'
import { useLanguage } from '../../contexts/LanguageContext'
import collaborationApi from '../../api/modules/collaboration'
import '../../assets/css/collaboration/sync-monitor.css'

function SyncMonitor() {
  const { t } = useLanguage()
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState(null)

  useEffect(() => {
    loadSyncData()
    const interval = setInterval(loadSyncData, 10000)
    return () => clearInterval(interval)
  }, [])

  const loadSyncData = async () => {
    setLoading(true)
    try {
      const response = await collaborationApi.getSyncStatus()
      if (response) {
        // axios 拦截器已返回 response.data (Axios interceptor returns response.data)
        setData(response)
      }
    } catch (error) {
      console.error('Failed to load sync data:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading && !data) {
    return (
      <div className="sync-monitor__loading">
        <Spin tip={t('common.loading')} size="large">
          <div style={{ padding: 50 }} />
        </Spin>
      </div>
    )
  }

  if (!data) {
    return null
  }

  return (
    <div className="sync-monitor">
      <Row gutter={[16, 16]} className="sync-monitor__stats">
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title={t('collaboration.totalSyncs')}
              value={data.totalSyncs}
              prefix={<SyncOutlined />}
              valueStyle={{ color: '#667eea' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title={t('collaboration.successSyncs')}
              value={data.successSyncs}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title={t('collaboration.failedSyncs')}
              value={data.failedSyncs}
              prefix={<CloseCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title={t('collaboration.syncRate')}
              value={Math.round((data.successSyncs / (data.totalSyncs || 1)) * 100)}
              suffix="%"
              valueStyle={{ color: '#667eea' }}
            />
          </Card>
        </Col>
      </Row>

      <Card
        className="sync-monitor__activity"
        title={t('collaboration.recentActivity')}
      >
        <List
          dataSource={data.recentSyncs || []}
          renderItem={(item) => (
            <List.Item>
              <List.Item.Meta
                title={
                  <div className="sync-monitor__activity-title">
                    <span>{item.peerName}</span>
                    <Tag color={item.status === 'success' ? 'green' : 'red'}>
                      {t(`collaboration.syncStatus.${item.status}`)}
                    </Tag>
                  </div>
                }
                description={
                  <div className="sync-monitor__activity-desc">
                    <span>{item.description}</span>
                    <span className="sync-monitor__activity-time">
                      {new Date(item.timestamp).toLocaleString()}
                    </span>
                  </div>
                }
              />
              {item.progress !== undefined && (
                <div className="sync-monitor__progress">
                  <Progress
                    percent={item.progress}
                    size="small"
                    status={item.status === 'success' ? 'success' : 'active'}
                  />
                </div>
              )}
            </List.Item>
          )}
        />
      </Card>
    </div>
  )
}

export default SyncMonitor

