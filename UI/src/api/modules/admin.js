/**
 * 管理 API 模块 (Admin API Module)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import { request } from '../index'

const adminApi = {
  /**
   * 获取系统配置 (Get system configuration)
   */
  getSystemConfig() {
    return request.get('/admin/config')
  },

  /**
   * 更新系统配置 (Update system configuration)
   */
  updateSystemConfig(data) {
    return request.put('/admin/config', data)
  },

  /**
   * 获取模型配置 (Get model configuration)
   */
  getModelConfig() {
    return request.get('/admin/model-config')
  },

  /**
   * 更新模型配置 (Update model configuration)
   */
  updateModelConfig(data) {
    return request.put('/admin/model-config', data)
  },

  /**
   * 获取日志 (Get logs)
   */
  getLogs(params) {
    return request.get('/admin/logs', params)
  },

  /**
   * 获取性能监控数据 (Get performance monitoring data)
   */
  getPerformanceMonitor() {
    return request.get('/admin/performance-monitor')
  },

  /**
   * 健康检查 (Health check)
   */
  healthCheck() {
    return request.get('/admin/health')
  },

  /**
   * 创建备份 (Create backup)
   */
  createBackup() {
    return request.post('/admin/backup')
  },

  /**
   * 获取备份列表 (Get backup list)
   */
  getBackupList() {
    return request.get('/admin/backups')
  },

  /**
   * 恢复备份 (Restore backup)
   */
  restoreBackup(backupId) {
    return request.post(`/admin/backups/${backupId}/restore`)
  },
}

export default adminApi

