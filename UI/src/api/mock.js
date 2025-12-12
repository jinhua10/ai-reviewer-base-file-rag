/**
 * Mock 数据服务 (Mock Data Service)
 *
 * 在后端未启动时提供模拟数据
 * (Provides mock data when backend is not available)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

// 是否启用 Mock 数据 (Enable mock data)
const ENABLE_MOCK = import.meta.env.VITE_ENABLE_MOCK === 'true' || false

/**
 * 模拟延迟 (Simulate delay)
 */
const delay = (ms = 500) => new Promise(resolve => setTimeout(resolve, ms))

/**
 * Mock 数据生成器 (Mock data generators)
 */
export const mockData = {
  // 文档列表
  documents: {
    list: [
      {
        id: '1',
        name: 'React 开发指南.pdf',
        size: 2048000,
        uploadTime: new Date().toISOString(),
        tags: ['前端', 'React'],
        category: '技术文档',
      },
      {
        id: '2',
        name: 'Spring Boot 最佳实践.docx',
        size: 1536000,
        uploadTime: new Date(Date.now() - 86400000).toISOString(),
        tags: ['后端', 'Java'],
        category: '技术文档',
      },
      {
        id: '3',
        name: '项目需求文档.md',
        size: 512000,
        uploadTime: new Date(Date.now() - 172800000).toISOString(),
        tags: ['需求', '项目'],
        category: '项目文档',
      },
    ],
    total: 3,
  },

  // 角色列表
  roles: {
    list: [
      {
        id: '1',
        name: '前端开发',
        description: '负责前端相关的问题',
        keywords: ['React', 'Vue', 'JavaScript', 'CSS', 'HTML'],
        icon: '💻',
        enabled: true,
        usageCount: 150,
      },
      {
        id: '2',
        name: '后端开发',
        description: '负责后端相关的问题',
        keywords: ['Java', 'Spring', 'MySQL', 'Redis'],
        icon: '🔧',
        enabled: true,
        usageCount: 120,
      },
      {
        id: '3',
        name: '运维部署',
        description: '负责部署和运维相关的问题',
        keywords: ['Docker', 'K8s', 'CI/CD', 'Nginx'],
        icon: '🚀',
        enabled: false,
        usageCount: 80,
      },
    ],
  },

  // 冲突列表
  conflicts: {
    list: [
      {
        id: '1',
        question: 'React Hooks 的最佳实践是什么？',
        conceptA: '使用 useEffect 处理所有副作用',
        conceptB: '优先使用 useMemo 和 useCallback 优化性能',
        status: 'pending',
        voteA: 0,
        voteB: 0,
        createdAt: new Date().toISOString(),
      },
      {
        id: '2',
        question: 'Spring Boot 如何配置数据源？',
        conceptA: '使用 application.yml 配置',
        conceptB: '使用 Java Config 类配置',
        status: 'voting',
        voteA: 15,
        voteB: 8,
        createdAt: new Date(Date.now() - 86400000).toISOString(),
      },
    ],
  },

  // 协作伙伴
  peers: {
    list: [
      {
        id: '1',
        name: '开发服务器-01',
        status: 'online',
        sharedDocs: 25,
        lastSync: new Date(Date.now() - 3600000).toISOString(),
      },
      {
        id: '2',
        name: '测试环境',
        status: 'offline',
        sharedDocs: 12,
        lastSync: new Date(Date.now() - 86400000).toISOString(),
      },
    ],
  },

  // 演化历史
  evolution: [
    {
      id: '1',
      type: 'created',
      title: '创建新概念',
      description: 'React Hooks 概念已创建',
      timestamp: new Date(Date.now() - 172800000).toISOString(),
    },
    {
      id: '2',
      type: 'updated',
      title: '概念更新',
      description: 'React Hooks 最佳实践已更新',
      timestamp: new Date(Date.now() - 86400000).toISOString(),
      changes: {
        before: '旧的实践方式',
        after: '新的实践方式',
      },
    },
  ],

  // 质量监控
  quality: {
    totalConflicts: 45,
    resolvedConflicts: 32,
    pendingConflicts: 13,
    averageQuality: 0.85,
    concepts: [
      {
        concept: 'React Hooks',
        conflictCount: 8,
        resolvedCount: 6,
        qualityScore: 0.9,
      },
      {
        concept: 'Spring Boot',
        conflictCount: 12,
        resolvedCount: 10,
        qualityScore: 0.88,
      },
    ],
  },

  // 交换历史
  exchangeHistory: [
    {
      id: '1',
      timestamp: new Date().toISOString(),
      type: 'send',
      peerName: '开发服务器-01',
      content: '分享了 React 开发指南',
      status: 'success',
    },
    {
      id: '2',
      timestamp: new Date(Date.now() - 3600000).toISOString(),
      type: 'receive',
      peerName: '测试环境',
      content: '接收了测试文档',
      status: 'success',
    },
  ],

  // 网络拓扑
  topology: {
    nodes: [
      { id: '1', name: '开发服务器-01' },
      { id: '2', name: '测试环境' },
      { id: '3', name: '生产环境' },
    ],
    connections: 3,
  },

  // 同步状态
  syncStatus: {
    totalSyncs: 120,
    successSyncs: 110,
    failedSyncs: 10,
    recentSyncs: [
      {
        id: '1',
        peerName: '开发服务器-01',
        status: 'success',
        description: '同步完成',
        timestamp: new Date().toISOString(),
        progress: 100,
      },
      {
        id: '2',
        peerName: '测试环境',
        status: 'failed',
        description: '连接超时',
        timestamp: new Date(Date.now() - 3600000).toISOString(),
        progress: 50,
      },
    ],
  },

  // 角色统计
  roleStatistics: [
    {
      id: '1',
      name: '前端开发',
      usageCount: 150,
      successRate: 0.92,
    },
    {
      id: '2',
      name: '后端开发',
      usageCount: 120,
      successRate: 0.88,
    },
  ],
}

/**
 * Mock API 拦截器
 */
export async function mockRequest(url, method = 'GET', data = null) {
  if (!ENABLE_MOCK) {
    return null // 不使用 mock
  }

  await delay(300) // 模拟网络延迟

  // 文档 API
  if (url.includes('/documents')) {
    if (method === 'GET' && !url.includes('/')) {
      return { data: mockData.documents }
    }
  }

  // 角色 API
  if (url.includes('/roles')) {
    if (method === 'GET' && url === '/roles') {
      return { data: mockData.roles }
    }
    if (url.includes('/statistics')) {
      return { data: mockData.roleStatistics }
    }
  }

  // 反馈 API
  if (url.includes('/feedback/conflicts')) {
    return { data: mockData.conflicts }
  }
  if (url.includes('/feedback/evolution')) {
    return { data: mockData.evolution }
  }
  if (url.includes('/feedback/quality-monitor')) {
    return { data: mockData.quality }
  }

  // 协作 API
  if (url.includes('/collaboration/peers')) {
    return { data: mockData.peers }
  }
  if (url.includes('/collaboration/exchange-history')) {
    return { data: mockData.exchangeHistory }
  }
  if (url.includes('/collaboration/topology')) {
    return { data: mockData.topology }
  }
  if (url.includes('/collaboration/sync-status')) {
    return { data: mockData.syncStatus }
  }

  return null
}

export { ENABLE_MOCK }

