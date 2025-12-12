/**
 * 协作面板适配器 / Collaboration Panel Adapter
 *
 * 将协作面板的业务逻辑和数据提取出来，供不同主题UI使用
 * Extract collaboration panel business logic and data for different theme UIs
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import { useState, useCallback } from 'react';
import { usePageBinding } from '../engine/ThemeRenderEngine';
import collaborationApi from '../api/modules/collaboration';

/**
 * 协作面板初始状态 / Collaboration panel initial state
 */
const INITIAL_STATE = {
  activeTab: 'peers',
  peers: [],
  exchanges: [],
  topology: {},
  syncStatus: {},
  loading: false,
  error: null,
};

/**
 * 协作面板Actions / Collaboration panel actions
 */
export function useCollaborationActions(updateState) {
  /**
   * 切换标签页 / Switch tab
   */
  const switchTab = useCallback((tabKey) => {
    updateState({ activeTab: tabKey });
  }, [updateState]);

  /**
   * 加载节点列表 / Load peer list
   */
  const loadPeers = useCallback(async () => {
    updateState({ loading: true });
    try {
      const response = await collaborationApi.getPeers();
      updateState({ peers: response.peers || [], loading: false });
    } catch (error) {
      console.error('Failed to load peers:', error);
      updateState({ error: error.message, loading: false });
    }
  }, [updateState]);

  /**
   * 加载交换历史 / Load exchange history
   */
  const loadExchanges = useCallback(async () => {
    updateState({ loading: true });
    try {
      const response = await collaborationApi.getExchangeHistory();
      updateState({ exchanges: response.history || [], loading: false });
    } catch (error) {
      console.error('Failed to load exchanges:', error);
      updateState({ error: error.message, loading: false });
    }
  }, [updateState]);

  /**
   * 加载网络拓扑 / Load network topology
   */
  const loadTopology = useCallback(async () => {
    updateState({ loading: true });
    try {
      const response = await collaborationApi.getTopology();
      updateState({ topology: response.topology || {}, loading: false });
    } catch (error) {
      console.error('Failed to load topology:', error);
      updateState({ error: error.message, loading: false });
    }
  }, [updateState]);

  /**
   * 加载同步状态 / Load sync status
   */
  const loadSyncStatus = useCallback(async () => {
    updateState({ loading: true });
    try {
      const response = await collaborationApi.getSyncStatus();
      updateState({ syncStatus: response.syncStatus || {}, loading: false });
    } catch (error) {
      console.error('Failed to load sync status:', error);
      updateState({ error: error.message, loading: false });
    }
  }, [updateState]);

  /**
   * 生成连接码 / Generate connection code
   */
  const generateCode = useCallback(async () => {
    updateState({ loading: true });
    try {
      const response = await collaborationApi.generateCode();
      updateState({ loading: false });
      return response.code;
    } catch (error) {
      console.error('Failed to generate code:', error);
      updateState({ error: error.message, loading: false });
      throw error;
    }
  }, [updateState]);

  /**
   * 使用连接码建立连接 / Connect using code
   */
  const connectWithCode = useCallback(async (code) => {
    updateState({ loading: true });
    try {
      const response = await collaborationApi.connect(code);

      if (response.success) {
        // 连接成功后重新加载节点列表 / Reload peers after successful connection
        await loadPeers();
      }

      updateState({ loading: false });
      return response;
    } catch (error) {
      console.error('Failed to connect:', error);
      updateState({ error: error.message, loading: false });
      throw error;
    }
  }, [updateState, loadPeers]);

  /**
   * 断开连接 / Disconnect from peer
   */
  const disconnectPeer = useCallback(async (peerId) => {
    updateState({ loading: true });
    try {
      await collaborationApi.disconnect(peerId);

      // 断开后重新加载节点列表 / Reload peers after disconnection
      await loadPeers();

      updateState({ loading: false });
    } catch (error) {
      console.error('Failed to disconnect:', error);
      updateState({ error: error.message, loading: false });
      throw error;
    }
  }, [updateState, loadPeers]);

  /**
   * 知识交换 / Exchange knowledge
   */
  const exchangeKnowledge = useCallback(async (peerId, knowledge) => {
    updateState({ loading: true });
    try {
      const response = await collaborationApi.exchange({ peerId, knowledge });

      // 交换后重新加载交换历史 / Reload exchanges after exchange
      await loadExchanges();

      updateState({ loading: false });
      return response;
    } catch (error) {
      console.error('Failed to exchange knowledge:', error);
      updateState({ error: error.message, loading: false });
      throw error;
    }
  }, [updateState, loadExchanges]);

  return {
    switchTab,
    loadPeers,
    loadExchanges,
    loadTopology,
    loadSyncStatus,
    generateCode,
    connectWithCode,
    disconnectPeer,
    exchangeKnowledge,
  };
}

/**
 * 协作面板数据绑定Hook / Collaboration panel data binding hook
 *
 * 返回绑定的数据和actions，供UI壳子使用
 * Returns bound data and actions for UI shell
 */
export function useCollaborationBinding() {
  // 使用页面绑定 / Use page binding
  const binding = usePageBinding(
    'collaboration', // 页面ID / Page ID
    INITIAL_STATE,   // 初始状态 / Initial state
    {}               // Actions将在下面创建 / Actions will be created below
  );

  // 创建actions / Create actions
  const actions = useCollaborationActions(binding.updateState);

  // 返回完整的绑定 / Return complete binding
  return {
    state: binding.state,
    actions: {
      ...binding.actions,
      ...actions,
    },
  };
}

// ========== 真实 API 已集成 / Real API integrated ==========
// 所有 API 调用通过 collaborationApi 模块进行
// All API calls are made through the collaborationApi module

