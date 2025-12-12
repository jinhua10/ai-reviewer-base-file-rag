import React, { useState, useEffect } from 'react';
import { Card, Row, Col, Statistic, Progress } from 'antd';
import { useLanguage } from '../../contexts/LanguageContext';
import { adminApi } from '../../api/modules/admin';

const MonitorDashboard = () => {
  const { t } = useLanguage();
  const [metrics, setMetrics] = useState(null);

  useEffect(() => {
    loadMetrics();
    const interval = setInterval(loadMetrics, 5000);
    return () => clearInterval(interval);
  }, []);

  const loadMetrics = async () => {
    try {
      const response = await adminApi.getMetrics();
      setMetrics(response.data);
    } catch (error) {
      console.error('Failed to load metrics:', error);
    }
  };

  if (!metrics) return null;

  return (
    <div className="monitor-dashboard">
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic title={t('admin.monitor.cpu')} value={metrics.cpu || 0} suffix="%" />
            <Progress percent={metrics.cpu || 0} status={metrics.cpu > 80 ? 'exception' : 'normal'} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic title={t('admin.monitor.memory')} value={metrics.memory || 0} suffix="%" />
            <Progress percent={metrics.memory || 0} status={metrics.memory > 80 ? 'exception' : 'normal'} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic title={t('admin.monitor.requests')} value={metrics.requests || 0} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic title={t('admin.monitor.errors')} value={metrics.errors || 0} valueStyle={{ color: '#cf1322' }} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default MonitorDashboard;

