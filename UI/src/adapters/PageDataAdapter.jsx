/**
 * 页面数据适配器 / Page Data Adapter
 * 
 * 统一的数据层，所有主题Shell都通过这个适配器获取数据
 * 后端联调时只需要修改这一个文件
 * 
 * Unified data layer, all theme Shells get data through this adapter
 * Only need to modify this file when integrating with backend
 * 
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import { useState, useEffect } from 'react';

/**
 * QA页面数据适配器
 * 所有主题的QA Shell都使用这个Hook获取数据
 */
export function useQAPageData() {
  const [data, setData] = useState({
    stats: {
      totalQuestions: 0,
      averageResponseTime: 0,
      satisfactionRate: 0,
      activeUsers: 0
    },
    recentQuestions: [],
    loading: true,
    error: null
  });

  useEffect(() => {
    // TODO: 后端联调时，替换为真实的API调用
    // TODO: When integrating backend, replace with real API calls
    
    // 模拟API调用
    const fetchData = async () => {
      try {
        // const response = await fetch('/api/qa/stats');
        // const result = await response.json();
        
        // 模拟数据
        setTimeout(() => {
          setData({
            stats: {
              totalQuestions: 1234,
              averageResponseTime: 2.5,
              satisfactionRate: 95,
              activeUsers: 456
            },
            recentQuestions: [
              { id: 1, question: '如何使用AI审查?', status: 'answered' },
              { id: 2, question: '系统支持哪些文件格式?', status: 'pending' }
            ],
            loading: false,
            error: null
          });
        }, 500);
      } catch (error) {
        setData(prev => ({ ...prev, loading: false, error: error.message }));
      }
    };

    fetchData();
  }, []);

  return data;
}

/**
 * 首页数据适配器
 */
export function useHomePageData() {
  const [data, setData] = useState({
    summary: {
      documents: 0,
      reviews: 0,
      users: 0,
      aiScore: 0
    },
    recentActivities: [],
    loading: true,
    error: null
  });

  useEffect(() => {
    // TODO: 替换为真实API
    setTimeout(() => {
      setData({
        summary: {
          documents: 856,
          reviews: 2341,
          users: 128,
          aiScore: 92
        },
        recentActivities: [],
        loading: false,
        error: null
      });
    }, 500);
  }, []);

  return data;
}

/**
 * 文档管理页面数据适配器
 */
export function useDocumentsPageData() {
  const [data, setData] = useState({
    documents: [],
    stats: {
      total: 0,
      pending: 0,
      completed: 0,
      rejected: 0
    },
    loading: true,
    error: null
  });

  useEffect(() => {
    // TODO: 替换为真实API
    setTimeout(() => {
      setData({
        documents: [],
        stats: {
          total: 856,
          pending: 45,
          completed: 789,
          rejected: 22
        },
        loading: false,
        error: null
      });
    }, 500);
  }, []);

  return data;
}

/**
 * 协作空间页面数据适配器
 */
export function useCollaborationPageData() {
  const [data, setData] = useState({
    members: [],
    projects: [],
    messages: [],
    loading: true,
    error: null
  });

  useEffect(() => {
    // TODO: 替换为真实API
    setTimeout(() => {
      setData({
        members: [],
        projects: [],
        messages: [],
        loading: false,
        error: null
      });
    }, 500);
  }, []);

  return data;
}

/**
 * 数据分析页面数据适配器
 */
export function useAnalyticsPageData() {
  const [data, setData] = useState({
    charts: {
      trends: [],
      distribution: [],
      performance: []
    },
    metrics: {
      accuracy: 0,
      speed: 0,
      satisfaction: 0
    },
    loading: true,
    error: null
  });

  useEffect(() => {
    // TODO: 替换为真实API
    setTimeout(() => {
      setData({
        charts: {
          trends: [],
          distribution: [],
          performance: []
        },
        metrics: {
          accuracy: 94,
          speed: 88,
          satisfaction: 92
        },
        loading: false,
        error: null
      });
    }, 500);
  }, []);

  return data;
}

/**
 * 系统设置页面数据适配器
 */
export function useSettingsPageData() {
  const [data, setData] = useState({
    user: {
      name: '',
      email: '',
      avatar: ''
    },
    preferences: {
      language: 'zh',
      theme: 'bubble',
      notifications: true
    },
    loading: true,
    error: null
  });

  useEffect(() => {
    // TODO: 替换为真实API
    setTimeout(() => {
      setData({
        user: {
          name: 'Admin',
          email: 'admin@example.com',
          avatar: ''
        },
        preferences: {
          language: 'zh',
          theme: 'bubble',
          notifications: true
        },
        loading: false,
        error: null
      });
    }, 500);
  }, []);

  return data;
}

/**
 * 通用的API调用函数
 * 后端联调时统一配置baseURL、headers等
 */
export async function apiCall(endpoint, options = {}) {
  // TODO: 配置统一的API基础URL
  const baseURL = process.env.REACT_APP_API_BASE_URL || 'http://localhost:8080/api';
  
  const defaultOptions = {
    headers: {
      'Content-Type': 'application/json',
      // 添加认证token等
      // 'Authorization': `Bearer ${getToken()}`
    },
    ...options
  };

  try {
    const response = await fetch(`${baseURL}${endpoint}`, defaultOptions);
    
    if (!response.ok) {
      throw new Error(`API Error: ${response.status}`);
    }
    
    return await response.json();
  } catch (error) {
    console.error('API call failed:', error);
    throw error;
  }
}

/**
 * 示例：真实API调用的实现方式
 * 
 * export function useQAPageData() {
 *   const [data, setData] = useState({ loading: true, error: null });
 *   
 *   useEffect(() => {
 *     async function fetchData() {
 *       try {
 *         const stats = await apiCall('/qa/stats');
 *         const questions = await apiCall('/qa/recent');
 *         
 *         setData({
 *           stats,
 *           recentQuestions: questions,
 *           loading: false,
 *           error: null
 *         });
 *       } catch (error) {
 *         setData(prev => ({ ...prev, loading: false, error: error.message }));
 *       }
 *     }
 *     
 *     fetchData();
 *   }, []);
 *   
 *   return data;
 * }
 */
