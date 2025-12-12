import React, { useState, useEffect } from 'react';
import { Card, Tabs, Avatar, Button, message } from 'antd';
import { UserOutlined, EditOutlined, SettingOutlined } from '@ant-design/icons';
import { useLanguage } from '../../contexts/LanguageContext';
import { profileApi } from '../../api/modules/profile';
import ProfileEditor from './ProfileEditor';
import UsageStatistics from './UsageStatistics';
import ContributionStats from './ContributionStats';
import AchievementPanel from './AchievementPanel';
import UserSettings from './UserSettings';
import '../../assets/css/profile/user-profile.css';

const { TabPane } = Tabs;

const UserProfile = () => {
  const { t } = useLanguage();
  const [userInfo, setUserInfo] = useState(null);
  const [loading, setLoading] = useState(false);
  const [editModalVisible, setEditModalVisible] = useState(false);
  const [activeTab, setActiveTab] = useState('overview');

  // 加载用户信息
  const loadUserInfo = async () => {
    setLoading(true);
    try {
      const response = await profileApi.getUserInfo();
      setUserInfo(response.data);
    } catch (error) {
      console.error('Failed to load user info:', error);
      message.error(t('profile.loadFailed'));
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadUserInfo();
  }, []);

  if (!userInfo && !loading) {
    return null;
  }

  return (
    <div className="user-profile">
      {/* 用户信息卡片 */}
      <Card className="user-profile__card">
        <div className="user-profile__header">
          <div className="user-profile__avatar-section">
            <Avatar
              size={120}
              src={userInfo?.avatar}
              icon={<UserOutlined />}
              className="user-profile__avatar"
            />
            <Button
              type="primary"
              icon={<EditOutlined />}
              onClick={() => setEditModalVisible(true)}
              className="user-profile__edit-btn"
            >
              {t('profile.editInfo')}
            </Button>
          </div>

          <div className="user-profile__info">
            <h2 className="user-profile__name">
              {userInfo?.nickname || t('profile.defaultName')}
            </h2>
            <p className="user-profile__email">{userInfo?.email}</p>
            <p className="user-profile__bio">
              {userInfo?.bio || t('profile.noBio')}
            </p>

            {/* 快速统计 */}
            <div className="user-profile__stats">
              <div className="user-profile__stat-item">
                <span className="user-profile__stat-value">
                  {userInfo?.statistics?.qaCount || 0}
                </span>
                <span className="user-profile__stat-label">
                  {t('profile.qaCount')}
                </span>
              </div>
              <div className="user-profile__stat-item">
                <span className="user-profile__stat-value">
                  {userInfo?.statistics?.documentCount || 0}
                </span>
                <span className="user-profile__stat-label">
                  {t('profile.documentCount')}
                </span>
              </div>
              <div className="user-profile__stat-item">
                <span className="user-profile__stat-value">
                  {userInfo?.statistics?.contributionScore || 0}
                </span>
                <span className="user-profile__stat-label">
                  {t('profile.contributionScore')}
                </span>
              </div>
            </div>
          </div>
        </div>
      </Card>

      {/* 详细信息标签页 */}
      <Card className="user-profile__tabs-card">
        <Tabs activeKey={activeTab} onChange={setActiveTab}>
          <TabPane
            tab={
              <span>
                <UserOutlined />
                {t('profile.statistics')}
              </span>
            }
            key="statistics"
          >
            <UsageStatistics userId={userInfo?.id} />
          </TabPane>

          <TabPane
            tab={
              <span>
                <UserOutlined />
                {t('profile.contribution')}
              </span>
            }
            key="contribution"
          >
            <ContributionStats userId={userInfo?.id} />
          </TabPane>

          <TabPane
            tab={
              <span>
                <UserOutlined />
                {t('profile.achievement')}
              </span>
            }
            key="achievement"
          >
            <AchievementPanel userId={userInfo?.id} />
          </TabPane>

          <TabPane
            tab={
              <span>
                <SettingOutlined />
                {t('profile.settings')}
              </span>
            }
            key="settings"
          >
            <UserSettings />
          </TabPane>
        </Tabs>
      </Card>

      {/* 编辑个人信息模态框 */}
      <ProfileEditor
        visible={editModalVisible}
        userInfo={userInfo}
        onClose={() => setEditModalVisible(false)}
        onSuccess={() => {
          setEditModalVisible(false);
          loadUserInfo();
        }}
      />
    </div>
  );
};

export default UserProfile;

