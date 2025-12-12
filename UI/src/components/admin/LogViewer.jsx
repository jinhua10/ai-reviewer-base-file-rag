import React, { useState, useEffect } from 'react';
import { Input, Select, Button, List, Tag } from 'antd';
import { SearchOutlined, DownloadOutlined } from '@ant-design/icons';
import { useLanguage } from '../../contexts/LanguageContext';
import { adminApi } from '../../api/modules/admin';
import '../../assets/css/admin/log-viewer.css';

const { Search } = Input;
const { Option } = Select;

const LogViewer = () => {
  const { t } = useLanguage();
  const [logs, setLogs] = useState([]);
  const [level, setLevel] = useState('all');
  const [keyword, setKeyword] = useState('');

  useEffect(() => {
    loadLogs();
  }, [level]);

  const loadLogs = async () => {
    try {
      const response = await adminApi.getLogs({ level: level === 'all' ? undefined : level, keyword });
      setLogs(response.data || []);
    } catch (error) {
      console.error('Failed to load logs:', error);
    }
  };

  const getLevelColor = (level) => {
    const colors = { ERROR: 'red', WARN: 'orange', INFO: 'blue', DEBUG: 'green' };
    return colors[level] || 'default';
  };

  return (
    <div className="log-viewer">
      <div className="log-viewer__toolbar">
        <Search
          placeholder={t('admin.log.searchPlaceholder')}
          onSearch={(value) => { setKeyword(value); loadLogs(); }}
          style={{ width: 300 }}
        />
        <Select value={level} onChange={setLevel} style={{ width: 150 }}>
          <Option value="all">{t('admin.log.all')}</Option>
          <Option value="ERROR">{t('admin.log.error')}</Option>
          <Option value="WARN">{t('admin.log.warn')}</Option>
          <Option value="INFO">{t('admin.log.info')}</Option>
        </Select>
        <Button icon={<DownloadOutlined />}>{t('admin.log.download')}</Button>
      </div>
      <List
        className="log-viewer__list"
        dataSource={logs}
        renderItem={(log) => (
          <List.Item className="log-viewer__item">
            <Tag color={getLevelColor(log.level)}>{log.level}</Tag>
            <span className="log-viewer__time">{log.timestamp}</span>
            <span className="log-viewer__message">{log.message}</span>
          </List.Item>
        )}
      />
    </div>
  );
};

export default LogViewer;

