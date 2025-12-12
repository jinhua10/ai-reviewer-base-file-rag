/**
 * 中文语言包 (Chinese Language Pack)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

export default {
  // ============================================================================
  // 通用 (Common)
  // ============================================================================
  common: {
    confirm: '确认',
    cancel: '取消',
    save: '保存',
    delete: '删除',
    edit: '编辑',
    add: '添加',
    search: '搜索',
    filter: '筛选',
    reset: '重置',
    submit: '提交',
    close: '关闭',
    back: '返回',
    next: '下一步',
    prev: '上一步',
    finish: '完成',
    loading: '加载中...',
    success: '成功',
    error: '错误',
    warning: '警告',
    info: '提示',
    upload: '上传',
    download: '下载',
    refresh: '刷新',
    more: '更多',
    collapse: '收起',
    expand: '展开',
    loadMore: '加载更多',
  },

  // ============================================================================
  // 导航 (Navigation)
  // ============================================================================
  nav: {
    home: '首页',
    qa: '智能问答',
    documents: '文档管理',
    roles: '角色管理',
    feedback: '反馈与演化',
    collaboration: '协作网络',
    wish: '愿望单',
    aiService: 'AI 服务',
    profile: '个人中心',
    settings: '系统设置',
    admin: '系统管理',
  },

  // ============================================================================
  // 文档管理 (Document Management)
  // ============================================================================
  document: {
    title: '文档管理',
    upload: '上传文档',
    uploadTip: '点击或拖拽文件到此区域上传',
    uploadHint: '支持 PDF、Word、Excel、PPT 等格式，单个文件不超过 100MB',
    uploadSuccess: '上传成功',
    uploadFailed: '上传失败',
    uploadLimit: '文件大小不能超过 {size}MB',
    uploadFirst: '上传第一个文档',
    list: '文档列表',
    total: '共 {count} 个文档',
    name: '文档名称',
    size: '大小',
    uploadTime: '上传时间',
    action: '操作',
    view: '查看',
    delete: '删除',
    deleteConfirm: '确定要删除这个文档吗？',
    deleteSuccess: '删除成功',
    deleteFailed: '删除失败',
    download: '下载',
    downloadSuccess: '下载成功',
    downloadFailed: '下载失败',
    preview: '预览',
    detail: '详情',
    category: '分类',
    tags: '标签',
    description: '描述',
    searchPlaceholder: '搜索文档名称、标签...',
    noDocuments: '暂无文档',
    noSearchResults: '未找到匹配的文档',
    loadFailed: '加载失败',
  },

  // ============================================================================
  // 智能问答 (Q&A)
  // ============================================================================
  qa: {
    title: '智能问答',
    emptyMessage: '开始提问，开启智能对话之旅',

    // 输入框 (Input)
    input: {
      placeholder: '请输入您的问题...',
      hint: 'Ctrl+Enter 发送',
      send: '发送',
      characters: '字符',
    },

    // 相似问题 (Similar Questions)
    similarQuestions: {
      title: '相似问题',
      noResults: '暂无相似问题',
      askFirst: '提问后将显示相似问题',
    },

    // 历史记录 (History)
    history: {
      title: '对话历史',
      searchPlaceholder: '搜索历史记录...',
      noResults: '暂无历史记录',
      today: '今天',
      yesterday: '昨天',
      daysAgo: '天前',
    },

    // 反馈 (Feedback)
    feedback: {
      like: '点赞',
      dislike: '点踩',
      copy: '复制',
      copied: '已复制',
    },

    // 错误 (Error)
    error: {
      failed: '抱歉，回答失败了，请稍后重试',
      network: '网络连接失败',
      timeout: '请求超时',
    },
    clearHistory: '清除历史',
    copyAnswer: '复制回答',
    copySuccess: '复制成功',
  },

  // ============================================================================
  // 角色管理 (Role Management)
  // ============================================================================
  role: {
    title: '角色管理',
    list: '角色列表',
    total: '共 {count} 个角色',
    create: '创建角色',
    createFirst: '创建第一个角色',
    createSuccess: '创建成功',
    createFailed: '创建失败',
    edit: '编辑角色',
    updateSuccess: '更新成功',
    updateFailed: '更新失败',
    delete: '删除',
    deleteConfirm: '确定要删除这个角色吗？',
    deleteSuccess: '删除成功',
    deleteFailed: '删除失败',
    name: '角色名称',
    namePlaceholder: '请输入角色名称',
    nameRequired: '请输入角色名称',
    description: '角色描述',
    descriptionPlaceholder: '请输入角色描述',
    descriptionRequired: '请输入角色描述',
    icon: '图标',
    keywords: '关键词',
    keywordPlaceholder: '输入关键词后按回车',
    keywordHint: '添加角色的特征关键词，用于问题匹配',
    addKeyword: '添加关键词',
    status: '状态',
    enabled: '已启用',
    disabled: '已禁用',
    statistics: '使用统计',
    usageCount: '使用次数',
    successRate: '成功率',
    noRoles: '暂无角色',
    loadFailed: '加载失败',
  },

  // ============================================================================
  // 反馈与演化 (Feedback & Evolution)
  // ============================================================================
  feedback: {
    title: '反馈与演化',
    conflictList: '冲突列表',
    voting: '投票',
    evolution: '演化历史',
    quality: '质量监控',

    // 状态
    all: '全部',
    pending: '待处理',
    resolved: '已解决',

    // 冲突
    conceptA: '概念 A',
    conceptB: '概念 B',
    conceptConflict: '概念冲突',
    vote: '投票',
    voteA: '选择 A',
    voteB: '选择 B',
    voteSuccess: '投票成功',
    whichBetter: '您认为哪个更好？',
    context: '上下文',

    // 状态标签
    status: {
      pending: '待处理',
      voting: '投票中',
      resolved: '已解决',
    },

    // 时间线
    timeline: {
      created: '创建',
      updated: '更新',
      resolved: '解决',
    },
    before: '修改前',
    after: '修改后',

    // 质量监控
    concept: '概念',
    conflicts: '冲突数',
    totalConflicts: '总冲突数',
    resolvedConflicts: '已解决',
    pendingConflicts: '待处理',
    avgQuality: '平均质量',
    conceptQuality: '概念质量',

    // 空状态
    noConflicts: '暂无冲突',
    noEvolution: '暂无演化历史',
    loadFailed: '加载失败',
  },

  // ============================================================================
  // 协作网络 (Collaboration)
  // ============================================================================
  collaboration: {
    title: '协作网络',
    peers: '协作伙伴',
    addPeer: '添加伙伴',
    connectionCode: '连接码',
    generateCode: '生成连接码',
    enterCode: '输入连接码',
    connect: '连接',
    exchange: '知识交换',
    contribution: '贡献统计',
    networkGraph: '网络拓扑',
  },

  // ============================================================================
  // 愿望单 (Wish List)
  // ============================================================================
  wish: {
    title: '愿望单',
    submit: '提交愿望',
    vote: '投票',
    ranking: '排行榜',
    myWishes: '我的愿望',
    allWishes: '全部愿望',
    wishTitle: '愿望标题',
    wishDescription: '愿望描述',
    voteCount: '投票数',
    status: '状态',
    pending: '待处理',
    inProgress: '进行中',
    completed: '已完成',
  },

  // ============================================================================
  // AI 服务 (AI Service)
  // ============================================================================
  aiService: {
    title: 'AI 服务市场',
    market: '服务市场',
    installed: '已安装',
    available: '可用服务',
    install: '安装',
    uninstall: '卸载',
    configure: '配置',
    usage: '使用',
    pptGenerator: 'PPT 生成器',
    modelSwitcher: '模型切换',
    localModel: '本地模型',
    onlineModel: '在线模型',
  },

  // ============================================================================
  // 个人中心 (User Profile)
  // ============================================================================
  profile: {
    title: '个人中心',
    info: '个人信息',
    editInfo: '编辑信息',
    statistics: '使用统计',
    contribution: '贡献统计',
    achievement: '成就',
    settings: '设置',
    avatar: '头像',
    nickname: '昵称',
    email: '邮箱',
    bio: '个人简介',
  },

  // ============================================================================
  // 系统管理 (Admin)
  // ============================================================================
  admin: {
    title: '系统管理',
    systemConfig: '系统配置',
    modelConfig: '模型配置',
    logViewer: '日志查看',
    monitor: '性能监控',
    healthCheck: '健康检查',
    backup: '备份管理',
  },

  // ============================================================================
  // 错误消息 (Error Messages)
  // ============================================================================
  error: {
    networkError: '网络错误，请检查连接',
    serverError: '服务器错误，请稍后重试',
    notFound: '未找到资源',
    unauthorized: '未授权，请先登录',
    forbidden: '无权限访问',
    validationError: '数据验证失败',
    unknownError: '未知错误',
  },

  // ============================================================================
  // 成功消息 (Success Messages)
  // ============================================================================
  success: {
    saved: '保存成功',
    deleted: '删除成功',
    updated: '更新成功',
    created: '创建成功',
    uploaded: '上传成功',
  },
}

