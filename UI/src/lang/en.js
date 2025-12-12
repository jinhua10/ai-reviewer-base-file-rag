/**
 * English Language Pack
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

export default {
  // ============================================================================
  // Common
  // ============================================================================
  common: {
    confirm: 'Confirm',
    cancel: 'Cancel',
    save: 'Save',
    delete: 'Delete',
    edit: 'Edit',
    add: 'Add',
    search: 'Search',
    filter: 'Filter',
    reset: 'Reset',
    submit: 'Submit',
    close: 'Close',
    back: 'Back',
    next: 'Next',
    prev: 'Previous',
    finish: 'Finish',
    loading: 'Loading...',
    success: 'Success',
    error: 'Error',
    warning: 'Warning',
    info: 'Info',
    upload: 'Upload',
    download: 'Download',
    refresh: 'Refresh',
    more: 'More',
    collapse: 'Collapse',
    expand: 'Expand',
    loadMore: 'Load More',
  },

  // ============================================================================
  // Navigation
  // ============================================================================
  nav: {
    home: 'Home',
    qa: 'Q&A',
    documents: 'Documents',
    roles: 'Roles',
    feedback: 'Feedback',
    collaboration: 'Collaboration',
    wish: 'Wish List',
    aiService: 'AI Services',
    profile: 'Profile',
    settings: 'Settings',
    admin: 'Admin',
  },

  // ============================================================================
  // Document Management
  // ============================================================================
  document: {
    title: 'Document Management',
    upload: 'Upload Document',
    uploadTip: 'Click or drag files to this area to upload',
    uploadHint: 'Support PDF, Word, Excel, PPT formats, max file size 100MB',
    uploadSuccess: 'Upload successful',
    uploadFailed: 'Upload failed',
    uploadLimit: 'File size cannot exceed {size}MB',
    uploadFirst: 'Upload first document',
    list: 'Document List',
    total: 'Total {count} documents',
    name: 'Document Name',
    size: 'Size',
    uploadTime: 'Upload Time',
    action: 'Action',
    view: 'View',
    delete: 'Delete',
    deleteConfirm: 'Are you sure you want to delete this document?',
    deleteSuccess: 'Delete successful',
    deleteFailed: 'Delete failed',
    download: 'Download',
    downloadSuccess: 'Download successful',
    downloadFailed: 'Download failed',
    preview: 'Preview',
    detail: 'Detail',
    category: 'Category',
    tags: 'Tags',
    description: 'Description',
    searchPlaceholder: 'Search documents by name, tags...',
    noDocuments: 'No documents yet',
    noSearchResults: 'No matching documents found',
    loadFailed: 'Failed to load',
  },

  // ============================================================================
  // Q&A
  // ============================================================================
  qa: {
    title: 'Intelligent Q&A',
    emptyMessage: 'Start asking questions to begin your intelligent conversation',

    // Input
    input: {
      placeholder: 'Enter your question...',
      hint: 'Ctrl+Enter to send',
      send: 'Send',
      characters: 'characters',
    },

    // Similar Questions
    similarQuestions: {
      title: 'Similar Questions',
      noResults: 'No similar questions',
      askFirst: 'Similar questions will appear after you ask',
    },

    // History
    history: {
      title: 'Chat History',
      searchPlaceholder: 'Search history...',
      noResults: 'No history records',
      today: 'Today',
      yesterday: 'Yesterday',
      daysAgo: 'days ago',
    },

    // Feedback
    feedback: {
      like: 'Like',
      dislike: 'Dislike',
      copy: 'Copy',
      copied: 'Copied',
    },

    // Error
    error: {
      failed: 'Sorry, failed to get answer. Please try again later',
      network: 'Network connection failed',
      timeout: 'Request timeout',
    },
    clearHistory: 'Clear History',
    copyAnswer: 'Copy Answer',
    copySuccess: 'Copied successfully',
  },

  // ============================================================================
  // Role Management
  // ============================================================================
  role: {
    title: 'Role Management',
    list: 'Role List',
    total: 'Total {count} roles',
    create: 'Create Role',
    createFirst: 'Create first role',
    createSuccess: 'Created successfully',
    createFailed: 'Create failed',
    edit: 'Edit Role',
    updateSuccess: 'Updated successfully',
    updateFailed: 'Update failed',
    delete: 'Delete',
    deleteConfirm: 'Are you sure you want to delete this role?',
    deleteSuccess: 'Deleted successfully',
    deleteFailed: 'Delete failed',
    name: 'Role Name',
    namePlaceholder: 'Enter role name',
    nameRequired: 'Please enter role name',
    description: 'Description',
    descriptionPlaceholder: 'Enter role description',
    descriptionRequired: 'Please enter description',
    icon: 'Icon',
    keywords: 'Keywords',
    keywordPlaceholder: 'Enter keyword and press Enter',
    keywordHint: 'Add characteristic keywords for question matching',
    addKeyword: 'Add Keyword',
    status: 'Status',
    enabled: 'Enabled',
    disabled: 'Disabled',
    statistics: 'Statistics',
    usageCount: 'Usage Count',
    successRate: 'Success Rate',
    noRoles: 'No roles yet',
    loadFailed: 'Failed to load',
  },

  // ============================================================================
  // Feedback & Evolution
  // ============================================================================
  feedback: {
    title: 'Feedback & Evolution',
    conflictList: 'Conflict List',
    voting: 'Voting',
    evolution: 'Evolution History',
    quality: 'Quality Monitor',
    conceptA: 'Concept A',
    conceptB: 'Concept B',
    vote: 'Vote',
    voteA: 'Choose A',
    voteB: 'Choose B',
    voteSuccess: 'Vote successful',
    timeline: 'Timeline',
  },

  // ============================================================================
  // Collaboration
  // ============================================================================
  collaboration: {
    title: 'Collaboration Network',
    peers: 'Peers',
    addPeer: 'Add Peer',
    connectionCode: 'Connection Code',
    generateCode: 'Generate Code',
    enterCode: 'Enter Code',
    connect: 'Connect',
    exchange: 'Knowledge Exchange',
    contribution: 'Contribution',
    networkGraph: 'Network Graph',
  },

  // ============================================================================
  // Wish List
  // ============================================================================
  wish: {
    title: 'Wish List',
    submit: 'Submit Wish',
    vote: 'Vote',
    ranking: 'Ranking',
    myWishes: 'My Wishes',
    allWishes: 'All Wishes',
    wishTitle: 'Wish Title',
    wishDescription: 'Description',
    voteCount: 'Votes',
    status: 'Status',
    pending: 'Pending',
    inProgress: 'In Progress',
    completed: 'Completed',
  },

  // ============================================================================
  // AI Service
  // ============================================================================
  aiService: {
    title: 'AI Service Market',
    market: 'Market',
    installed: 'Installed',
    available: 'Available',
    install: 'Install',
    uninstall: 'Uninstall',
    configure: 'Configure',
    usage: 'Usage',
    pptGenerator: 'PPT Generator',
    modelSwitcher: 'Model Switcher',
    localModel: 'Local Model',
    onlineModel: 'Online Model',
  },

  // ============================================================================
  // User Profile
  // ============================================================================
  profile: {
    title: 'Profile',
    info: 'Personal Info',
    editInfo: 'Edit Info',
    statistics: 'Statistics',
    contribution: 'Contribution',
    achievement: 'Achievement',
    settings: 'Settings',
    avatar: 'Avatar',
    nickname: 'Nickname',
    email: 'Email',
    bio: 'Bio',
  },

  // ============================================================================
  // Admin
  // ============================================================================
  admin: {
    title: 'System Admin',
    systemConfig: 'System Config',
    modelConfig: 'Model Config',
    logViewer: 'Log Viewer',
    monitor: 'Monitor',
    healthCheck: 'Health Check',
    backup: 'Backup',
  },

  // ============================================================================
  // Error Messages
  // ============================================================================
  error: {
    networkError: 'Network error, please check connection',
    serverError: 'Server error, please try again later',
    notFound: 'Resource not found',
    unauthorized: 'Unauthorized, please login first',
    forbidden: 'Access forbidden',
    validationError: 'Validation failed',
    unknownError: 'Unknown error',
  },

  // ============================================================================
  // Success Messages
  // ============================================================================
  success: {
    saved: 'Saved successfully',
    deleted: 'Deleted successfully',
    updated: 'Updated successfully',
    created: 'Created successfully',
    uploaded: 'Uploaded successfully',
  },
}

