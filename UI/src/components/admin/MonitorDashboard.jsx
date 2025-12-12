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
      // 确保正确提取数据
      const metricsData = response?.data || response;
      console.log('Loaded metrics:', metricsData);
      setMetrics(metricsData);
    } catch (error) {
      console.error('Failed to load metrics:', error);
      // 设置默认值以防止错误
      setMetrics({ cpu: 0, memory: 0, requests: 0, errors: 0 });
    }
  };

  if (!metrics) return null;

  // 确保metrics是一个有效的对象
  const cpu = typeof metrics.cpu === 'number' ? metrics.cpu : 0;
  const memory = typeof metrics.memory === 'number' ? metrics.memory : 0;
  const requests = typeof metrics.requests === 'number' ? metrics.requests : 0;
  const errors = typeof metrics.errors === 'number' ? metrics.errors : 0;

  return (
    <div className="monitor-dashboard">
      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic title={t('admin.monitor.cpu')} value={cpu} suffix="%" />
            <Progress percent={cpu} status={cpu > 80 ? 'exception' : 'normal'} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic title={t('admin.monitor.memory')} value={memory} suffix="%" />
            <Progress percent={memory} status={memory > 80 ? 'exception' : 'normal'} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic title={t('admin.monitor.requests')} value={requests} />
          </Card>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <Card>
            <Statistic title={t('admin.monitor.errors')} value={errors} valueStyle={{ color: '#cf1322' }} />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default MonitorDashboard;

