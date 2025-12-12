import React, { useState } from 'react';
import { Card, Tabs } from 'antd';
import { SettingOutlined, DatabaseOutlined, FileTextOutlined, DashboardOutlined } from '@ant-design/icons';
import { useLanguage } from '../../contexts/LanguageContext';
import SystemConfig from './SystemConfig';
import ModelConfig from './ModelConfig';
import LogViewer from './LogViewer';
import MonitorDashboard from './MonitorDashboard';
import '../../assets/css/admin/admin-panel.css';

const { TabPane } = Tabs;

const AdminPanel = () => {
  const { t } = useLanguage();
  const [activeTab, setActiveTab] = useState('system');

  return (
    <div className="admin-panel">
      <Card className="admin-panel__card">
        <h2 className="admin-panel__title">{t('admin.title')}</h2>

        <Tabs activeKey={activeTab} onChange={setActiveTab} className="admin-panel__tabs">
          <TabPane
            tab={
              <span>
                <SettingOutlined />
                {t('admin.systemConfig')}
              </span>
            }
            key="system"
          >
            <SystemConfig />
          </TabPane>

          <TabPane
            tab={
              <span>
                <DatabaseOutlined />
                {t('admin.modelConfig')}
              </span>
            }
            key="model"
          >
            <ModelConfig />
          </TabPane>

          <TabPane
            tab={
              <span>
                <FileTextOutlined />
                {t('admin.logViewer')}
              </span>
            }
            key="logs"
          >
            <LogViewer />
          </TabPane>

          <TabPane
            tab={
              <span>
                <DashboardOutlined />
                {t('admin.monitor')}
              </span>
            }
            key="monitor"
          >
            <MonitorDashboard />
          </TabPane>
        </Tabs>
      </Card>
    </div>
  );
};

export default AdminPanel;

