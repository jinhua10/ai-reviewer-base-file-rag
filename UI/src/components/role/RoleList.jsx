/**
 * 角色列表组件 (Role List Component)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React, { useState, useEffect, useCallback } from 'react'
import { Button, Space, Switch, message, Modal } from 'antd'
import { PlusOutlined, ReloadOutlined, AppstoreOutlined, UnorderedListOutlined } from '@ant-design/icons'
import RoleCard from './RoleCard'
import RoleEditor from './RoleEditor'
import RoleStatistics from './RoleStatistics'
import { Loading } from '../common'
import { useLanguage } from '../../contexts/LanguageContext'
import roleApi from '../../api/modules/role'
import '../../assets/css/role/role-list.css'

function RoleList() {
  const { t } = useLanguage()
  const [roles, setRoles] = useState([])
  const [loading, setLoading] = useState(false)
  const [editorVisible, setEditorVisible] = useState(false)
  const [statsVisible, setStatsVisible] = useState(false)
  const [selectedRole, setSelectedRole] = useState(null)
  const [viewMode, setViewMode] = useState('grid')

  const loadRoles = useCallback(async () => {
    setLoading(true)
    try {
      const response = await roleApi.getList()
      if (response?.data) {
        setRoles(response.data.list || response.data || [])
      }
    } catch (error) {
      console.error('Failed to load roles:', error)
      message.error(t('role.loadFailed'))
    } finally {
      setLoading(false)
    }
  }, [t])

  useEffect(() => {
    loadRoles()
  }, [loadRoles])

  const handleCreate = () => {
    setSelectedRole(null)
    setEditorVisible(true)
  }

  const handleEdit = (role) => {
    setSelectedRole(role)
    setEditorVisible(true)
  }

  const handleDelete = (role) => {
    Modal.confirm({
      title: t('role.deleteConfirm'),
      content: role.name,
      okText: t('common.confirm'),
      cancelText: t('common.cancel'),
      okType: 'danger',
      onOk: async () => {
        try {
          await roleApi.delete(role.id)
          message.success(t('role.deleteSuccess'))
          loadRoles()
        } catch (error) {
          console.error('Failed to delete role:', error)
          message.error(t('role.deleteFailed'))
        }
      },
    })
  }

  const handleToggleStatus = async (role) => {
    try {
      await roleApi.update(role.id, { enabled: !role.enabled })
      message.success(t('role.updateSuccess'))
      loadRoles()
    } catch (error) {
      console.error('Failed to toggle role status:', error)
      message.error(t('role.updateFailed'))
    }
  }

  const handleSaveRole = async (data) => {
    try {
      if (selectedRole) {
        await roleApi.update(selectedRole.id, data)
        message.success(t('role.updateSuccess'))
      } else {
        await roleApi.create(data)
        message.success(t('role.createSuccess'))
      }
      setEditorVisible(false)
      loadRoles()
    } catch (error) {
      console.error('Failed to save role:', error)
      message.error(selectedRole ? t('role.updateFailed') : t('role.createFailed'))
    }
  }

  return (
    <div className="role-list">
      <div className="role-list__header">
        <div className="role-list__title">
          <h2>{t('role.title')}</h2>
          <span className="role-list__count">
            {t('role.total', { count: roles.length })}
          </span>
        </div>

        <Space>
          <Button
            icon={<ReloadOutlined />}
            onClick={loadRoles}
            loading={loading}
          >
            {t('common.refresh')}
          </Button>
          <Button onClick={() => setStatsVisible(true)}>
            {t('role.statistics')}
          </Button>
          <Switch
            checkedChildren={<AppstoreOutlined />}
            unCheckedChildren={<UnorderedListOutlined />}
            checked={viewMode === 'grid'}
            onChange={(checked) => setViewMode(checked ? 'grid' : 'list')}
          />
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={handleCreate}
          >
            {t('role.create')}
          </Button>
        </Space>
      </div>

      <div className="role-list__content">
        {loading ? (
          <Loading spinning={true} tip={t('common.loading')} />
        ) : roles.length === 0 ? (
          <div className="role-list__empty">
            <div className="role-list__empty-icon">👤</div>
            <p className="role-list__empty-text">{t('role.noRoles')}</p>
            <Button type="primary" icon={<PlusOutlined />} onClick={handleCreate}>
              {t('role.createFirst')}
            </Button>
          </div>
        ) : (
          <div className={`role-list__${viewMode}`}>
            {roles.map((role) => (
              <RoleCard
                key={role.id}
                role={role}
                viewMode={viewMode}
                onEdit={handleEdit}
                onDelete={handleDelete}
                onToggleStatus={handleToggleStatus}
              />
            ))}
          </div>
        )}
      </div>

      <RoleEditor
        visible={editorVisible}
        role={selectedRole}
        onCancel={() => setEditorVisible(false)}
        onSave={handleSaveRole}
      />

      <RoleStatistics
        visible={statsVisible}
        onClose={() => setStatsVisible(false)}
      />
    </div>
  )
}

export default RoleList

