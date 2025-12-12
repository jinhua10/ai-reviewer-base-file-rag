/**
 * 浮动AI分析面板 (Floating AI Analysis Panel)
 *
 * 可拖动、可最小化的浮动窗口，支持多文档联合分析
 * (Draggable, minimizable floating window for multi-document analysis)
 *
 * @author AI Reviewer Team
 * @since 2025-12-13
 */

import React, { useState, useRef, useEffect, useCallback } from 'react'
import { Button, Select, Input, Card, Tag, Tooltip, Spin } from 'antd'
import {
  CloseOutlined,
  MinusOutlined,
  PlusOutlined,
  DeleteOutlined,
  FileTextOutlined,
  SyncOutlined,
  ExpandOutlined,
  CompressOutlined,
  BorderOutlined,
  ColumnWidthOutlined,
  ColumnHeightOutlined,
} from '@ant-design/icons'
import { useQA } from '../../contexts/QAContext'
import { useLanguage } from '../../contexts/LanguageContext'
import MarkdownRenderer from '../qa/MarkdownRenderer'
import DockDropZone from './DockDropZone'
import '../../assets/css/ai/floating-ai-panel.css'

const { TextArea } = Input
const { Option } = Select

// 停靠位置常量
export const DOCK_POSITIONS = {
  NONE: 'none',       // 浮动模式
  LEFT: 'left',       // 左侧停靠
  RIGHT: 'right',     // 右侧停靠
  TOP: 'top',         // 顶部停靠
  BOTTOM: 'bottom',   // 底部停靠
}

// 停靠检测阈值（像素）
const DOCK_THRESHOLD = 50

// 默认窗口配置
const DEFAULT_CONFIG = {
  x: window.innerWidth - 500,
  y: 100,
  width: 450,
  height: 600,
  dockPosition: DOCK_POSITIONS.NONE,
  dockSize: 450, // 停靠时的宽度或高度
  lastFloatingConfig: null, // 停靠前的浮动配置
}

// 从localStorage加载配置
const loadPanelConfig = () => {
  try {
    const saved = localStorage.getItem('floating_ai_panel_config')
    if (saved) {
      const config = JSON.parse(saved)
      // 验证位置是否在屏幕内
      if (config.x < 0) config.x = 0
      if (config.y < 0) config.y = 0
      if (config.x + config.width > window.innerWidth) {
        config.x = window.innerWidth - config.width
      }
      if (config.y + config.height > window.innerHeight) {
        config.y = window.innerHeight - config.height
      }
      return config
    }
  } catch (e) {
    console.error('Failed to load panel config:', e)
  }
  return DEFAULT_CONFIG
}

// 保存配置到localStorage
const savePanelConfig = (config) => {
  try {
    localStorage.setItem('floating_ai_panel_config', JSON.stringify(config))
  } catch (e) {
    console.error('Failed to save panel config:', e)
  }
}

// 检测拖拽时是否靠近屏幕边缘
const detectDockPosition = (x, y) => {
  const windowWidth = window.innerWidth
  const windowHeight = window.innerHeight

  if (x < DOCK_THRESHOLD) return DOCK_POSITIONS.LEFT
  if (x > windowWidth - DOCK_THRESHOLD) return DOCK_POSITIONS.RIGHT
  if (y < DOCK_THRESHOLD) return DOCK_POSITIONS.TOP
  if (y > windowHeight - DOCK_THRESHOLD) return DOCK_POSITIONS.BOTTOM
  
  return DOCK_POSITIONS.NONE
}

/**
 * 分析类型 (Analysis types)
 */
const ANALYSIS_TYPES = {
  SINGLE: 'single',           // 单文档分析
  COMPARE: 'compare',         // 多文档对比
  RELATION: 'relation',       // 关联分析
  SYNTHESIS: 'synthesis',     // 综合报告
  CUSTOM: 'custom',          // 自定义问题
}

function FloatingAIPanel() {
  const { t } = useLanguage()
  const {
    aiAnalysisDocs,
    removeDocFromAIAnalysis,
    clearAIAnalysisDocs,
    showFloatingAI,
    setShowFloatingAI,
  } = useQA()

  // 面板配置状态
  const [config, setConfig] = useState(() => loadPanelConfig())
  const [minimized, setMinimized] = useState(false)
  
  // 拖拽状态
  const [dragging, setDragging] = useState(false)
  const [dragOffset, setDragOffset] = useState({ x: 0, y: 0 })
  const [previewDock, setPreviewDock] = useState(DOCK_POSITIONS.NONE) // 拖拽时预览停靠位置
  
  // 判断是否停靠
  const isDocked = config.dockPosition !== DOCK_POSITIONS.NONE
  
  // 调整大小状态
  const [resizing, setResizing] = useState(false)
  const [resizeDirection, setResizeDirection] = useState(null)
  const [resizeStart, setResizeStart] = useState({ x: 0, y: 0, width: 0, height: 0 })

  // 分析状态
  const [analysisType, setAnalysisType] = useState(ANALYSIS_TYPES.CUSTOM)
  const [customPrompt, setCustomPrompt] = useState('')
  const [analyzing, setAnalyzing] = useState(false)
  const [analysisResult, setAnalysisResult] = useState(null)

  const panelRef = useRef(null)
  const headerRef = useRef(null)
  const rafRef = useRef(null) // requestAnimationFrame引用，优化性能

  /**
   * 保存配置
   */
  const saveConfig = useCallback((newConfig) => {
    setConfig(newConfig)
    savePanelConfig(newConfig)
  }, [])

  /**
   * 鼠标按下开始拖动
   */
  const handleMouseDown = useCallback((e) => {
    if (e.target === headerRef.current || headerRef.current?.contains(e.target)) {
      // 如果处于停靠状态，先切换到浮动模式
      if (isDocked) {
        const lastFloating = config.lastFloatingConfig || {
          x: e.clientX - 225, // 窗口中心对齐鼠标
          y: e.clientY - 20,
          width: config.dockSize,
          height: 600,
        }
        setConfig({
          ...config,
          ...lastFloating,
          dockPosition: DOCK_POSITIONS.NONE,
        })
        setDragOffset({
          x: 225, // 窗口宽度一半
          y: 20,
        })
      } else {
        setDragOffset({
          x: e.clientX - config.x,
          y: e.clientY - config.y,
        })
      }
      
      setDragging(true)
      e.preventDefault()
    }
  }, [config, isDocked])

  /**
   * 鼠标移动时更新位置（使用requestAnimationFrame优化性能）
   */
  const handleMouseMove = useCallback((e) => {
    if (dragging) {
      if (rafRef.current) {
        cancelAnimationFrame(rafRef.current)
      }
      
      rafRef.current = requestAnimationFrame(() => {
        const newX = e.clientX - dragOffset.x
        const newY = e.clientY - dragOffset.y
        
        // 检测停靠预览
        const dockPos = detectDockPosition(e.clientX, e.clientY)
        setPreviewDock(dockPos)
        
        // 边界限制
        const maxX = window.innerWidth - 100
        const maxY = window.innerHeight - 50
        
        setConfig(prev => ({
          ...prev,
          x: Math.max(0, Math.min(newX, maxX)),
          y: Math.max(0, Math.min(newY, maxY)),
        }))
      })
    }
    
    if (resizing && resizeDirection) {
      if (rafRef.current) {
        cancelAnimationFrame(rafRef.current)
      }
      
      rafRef.current = requestAnimationFrame(() => {
        const deltaX = e.clientX - resizeStart.x
        const deltaY = e.clientY - resizeStart.y
        
        let newWidth = resizeStart.width
        let newHeight = resizeStart.height
        let newX = config.x
        let newY = config.y
        
        // 根据调整方向更新尺寸和位置
        if (resizeDirection.includes('e')) {
          newWidth = Math.max(300, Math.min(resizeStart.width + deltaX, window.innerWidth - config.x))
        }
        if (resizeDirection.includes('s')) {
          newHeight = Math.max(400, Math.min(resizeStart.height + deltaY, window.innerHeight - config.y))
        }
        if (resizeDirection.includes('w')) {
          const widthChange = resizeStart.width - deltaX
          if (widthChange >= 300) {
            newWidth = widthChange
            newX = resizeStart.x + deltaX
          }
        }
        if (resizeDirection.includes('n')) {
          const heightChange = resizeStart.height - deltaY
          if (heightChange >= 400) {
            newHeight = heightChange
            newY = resizeStart.y + deltaY
          }
        }
        
        setConfig(prev => ({
          ...prev,
          x: newX,
          y: newY,
          width: newWidth,
          height: newHeight,
        }))
      })
    }
  }, [dragging, dragOffset, resizing, resizeDirection, resizeStart, config.x, config.y])

  /**
   * 鼠标抬起停止拖动/调整
   */
  const handleMouseUp = useCallback(() => {
    if (dragging) {
      // 如果有停靠预览，执行停靠
      if (previewDock !== DOCK_POSITIONS.NONE) {
        saveConfig({
          ...config,
          dockPosition: previewDock,
          lastFloatingConfig: {
            x: config.x,
            y: config.y,
            width: config.width,
            height: config.height,
          },
        })
      } else {
        saveConfig(config)
      }
      setPreviewDock(DOCK_POSITIONS.NONE)
    }
    if (resizing) {
      saveConfig(config)
    }
    setDragging(false)
    setResizing(false)
    setResizeDirection(null)
  }, [dragging, resizing, config, previewDock, saveConfig])

  /**
   * 开始调整大小
   */
  const handleResizeStart = useCallback((direction, e) => {
    if (config.isMaximized) return
    
    setResizing(true)
    setResizeDirection(direction)
    setResizeStart({
      x: e.clientX,
      y: e.clientY,
      width: config.width,
      height: config.height,
    })
    e.preventDefault()
    e.stopPropagation()
  }, [config.width, config.height, config.isMaximized])

  useEffect(() => {
    if (dragging || resizing) {
      document.addEventListener('mousemove', handleMouseMove)
      document.addEventListener('mouseup', handleMouseUp)
      return () => {
        document.removeEventListener('mousemove', handleMouseMove)
        document.removeEventListener('mouseup', handleMouseUp)
        if (rafRef.current) {
          cancelAnimationFrame(rafRef.current)
        }
      }
    }
  }, [dragging, resizing, handleMouseMove, handleMouseUp])

  /**
   * 最大化/还原
   */
  const toggleMaximize = useCallback(() => {
    if (config.isMaximized) {
      // 还原
      const restored = config.lastNormalConfig || DEFAULT_CONFIG
      saveConfig({
        ...restored,
        isMaximized: false,
        lastNormalConfig: null,
      })
    } else {
      // 最大化
      saveConfig({
        x: 0,
        y: 0,
        width: window.innerWidth,
        height: window.innerHeight,
        isMaximized: true,
        lastNormalConfig: { ...config },
      })
    }
  }, [config, saveConfig])

  /**
   * 停靠 - 左侧
   */
  const snapToLeft = useCallback(() => {
    saveConfig({
      ...config,
      dockPosition: DOCK_POSITIONS.LEFT,
      dockSize: config.width || 450,
      lastFloatingConfig: {
        x: config.x,
        y: config.y,
        width: config.width,
        height: config.height,
      },
    })
  }, [config, saveConfig])

  /**
   * 停靠 - 右侧
   */
  const snapToRight = useCallback(() => {
    saveConfig({
      ...config,
      dockPosition: DOCK_POSITIONS.RIGHT,
      dockSize: config.width || 450,
      lastFloatingConfig: {
        x: config.x,
        y: config.y,
        width: config.width,
        height: config.height,
      },
    })
  }, [config, saveConfig])

  /**
   * 停靠 - 顶部
   */
  const snapToTop = useCallback(() => {
    saveConfig({
      ...config,
      dockPosition: DOCK_POSITIONS.TOP,
      dockSize: config.height || 600,
      lastFloatingConfig: {
        x: config.x,
        y: config.y,
        width: config.width,
        height: config.height,
      },
    })
  }, [config, saveConfig])

  /**
   * 停靠 - 底部
   */
  const snapToBottom = useCallback(() => {
    saveConfig({
      ...config,
      dockPosition: DOCK_POSITIONS.BOTTOM,
      dockSize: config.height || 600,
      lastFloatingConfig: {
        x: config.x,
        y: config.y,
        width: config.width,
        height: config.height,
      },
    })
  }, [config, saveConfig])

  /**
   * 重置到默认位置
   */
  const resetPosition = useCallback(() => {
    saveConfig(DEFAULT_CONFIG)
  }, [saveConfig])

  /**
   * 获取分析提示词
   */
  const getAnalysisPrompt = () => {
    const docNames = aiAnalysisDocs.map(d => d.title || d.name || d.fileName).join('、')
    
    switch (analysisType) {
      case ANALYSIS_TYPES.SINGLE:
        return `请详细分析文档《${docNames}》的内容，包括主要观点、关键信息和结论。`
      
      case ANALYSIS_TYPES.COMPARE:
        return `请对比分析以下文档的异同点：${docNames}。重点关注它们的观点差异、数据对比和结论区别。`
      
      case ANALYSIS_TYPES.RELATION:
        return `请分析以下文档之间的关联性：${docNames}。找出它们之间的逻辑关系、因果联系和相互影响。`
      
      case ANALYSIS_TYPES.SYNTHESIS:
        return `请综合以下文档的内容，生成一份整合报告：${docNames}。包含所有文档的核心观点、数据汇总和综合结论。`
      
      case ANALYSIS_TYPES.CUSTOM:
        return customPrompt || `请分析文档：${docNames}`
      
      default:
        return `请分析文档：${docNames}`
    }
  }

  /**
   * 执行AI分析
   */
  const handleAnalyze = async () => {
    if (aiAnalysisDocs.length === 0) {
      console.warn('⚠️ No documents selected')
      return
    }

    setAnalyzing(true)
    setAnalysisResult(null)

    try {
      const prompt = getAnalysisPrompt()
      
      // TODO: 调用后端API进行分析
      // 这里需要实现一个新的API端点，支持多文档分析
      console.log('📊 Analyzing documents with prompt:', prompt)
      console.log('📚 Documents:', aiAnalysisDocs)

      // 模拟API调用
      await new Promise(resolve => setTimeout(resolve, 2000))

      setAnalysisResult({
        type: analysisType,
        prompt,
        answer: '# 分析结果\n\n这是一个模拟的分析结果。实际使用时需要调用后端API。\n\n## 主要发现\n\n1. 文档A的核心观点是...\n2. 文档B提供了支持数据...\n\n## 综合结论\n\n基于以上分析，我们可以得出...',
        documents: aiAnalysisDocs,
        timestamp: new Date().toISOString(),
      })

    } catch (error) {
      console.error('❌ Analysis failed:', error)
      setAnalysisResult({
        error: error.message || '分析失败',
      })
    } finally {
      setAnalyzing(false)
    }
  }

  if (!showFloatingAI) {
    return null
  }

  // 停靠模式样式
  const dockedClassName = isDocked ? `floating-ai-panel--docked docked-${config.dockPosition}` : ''
  const panelStyle = isDocked 
    ? {} // 停靠模式使用100%宽高
    : {
        transform: `translate(${config.x}px, ${config.y}px)`,
        width: `${config.width}px`,
        height: minimized ? '48px' : `${config.height}px`,
        willChange: dragging || resizing ? 'transform, width, height' : 'auto',
      }

  return (
    <div
      ref={panelRef}
      className={`floating-ai-panel ${dockedClassName} ${minimized ? 'floating-ai-panel--minimized' : ''} ${dragging || resizing ? 'floating-ai-panel--dragging' : ''}`}
      style={panelStyle}
    >
      {/* 调整大小手柄 */}
      {!minimized && !config.isMaximized && (
        <>
          <div className="floating-ai-panel__resize-handle resize-n" onMouseDown={(e) => handleResizeStart('n', e)} />
          <div className="floating-ai-panel__resize-handle resize-s" onMouseDown={(e) => handleResizeStart('s', e)} />
          <div className="floating-ai-panel__resize-handle resize-w" onMouseDown={(e) => handleResizeStart('w', e)} />
          <div className="floating-ai-panel__resize-handle resize-e" onMouseDown={(e) => handleResizeStart('e', e)} />
          <div className="floating-ai-panel__resize-handle resize-nw" onMouseDown={(e) => handleResizeStart('nw', e)} />
          <div className="floating-ai-panel__resize-handle resize-ne" onMouseDown={(e) => handleResizeStart('ne', e)} />
          <div className="floating-ai-panel__resize-handle resize-sw" onMouseDown={(e) => handleResizeStart('sw', e)} />
          <div className="floating-ai-panel__resize-handle resize-se" onMouseDown={(e) => handleResizeStart('se', e)} />
        </>
      )}

      {/* 标题栏 */}
      <div
        ref={headerRef}
        className="floating-ai-panel__header"
        onMouseDown={handleMouseDown}
      >
        <div className="floating-ai-panel__title">
          <FileTextOutlined />
          <span>AI 分析面板</span>
          <Tag color="blue">{aiAnalysisDocs.length}</Tag>
        </div>
        <div className="floating-ai-panel__actions">
          <Tooltip title="左半屏">
            <Button
              type="text"
              size="small"
              icon={<BorderOutlined style={{ transform: 'scaleX(-1)' }} />}
              onClick={snapToLeft}
            />
          </Tooltip>
          <Tooltip title="右半屏">
            <Button
              type="text"
              size="small"
              icon={<BorderOutlined />}
              onClick={snapToRight}
            />
          </Tooltip>
          <Tooltip title="上半屏">
            <Button
              type="text"
              size="small"
              icon={<ColumnHeightOutlined style={{ transform: 'rotate(90deg)' }} />}
              onClick={snapToTop}
            />
          </Tooltip>
          <Tooltip title="下半屏">
            <Button
              type="text"
              size="small"
              icon={<ColumnHeightOutlined style={{ transform: 'rotate(-90deg)' }} />}
              onClick={snapToBottom}
            />
          </Tooltip>
          <Tooltip title={config.isMaximized ? '还原' : '最大化'}>
            <Button
              type="text"
              size="small"
              icon={config.isMaximized ? <CompressOutlined /> : <ExpandOutlined />}
              onClick={toggleMaximize}
            />
          </Tooltip>
          <Tooltip title={minimized ? '展开' : '最小化'}>
            <Button
              type="text"
              size="small"
              icon={minimized ? <PlusOutlined /> : <MinusOutlined />}
              onClick={() => setMinimized(!minimized)}
            />
          </Tooltip>
          <Tooltip title="关闭">
            <Button
              type="text"
              size="small"
              icon={<CloseOutlined />}
              onClick={() => setShowFloatingAI(false)}
            />
          </Tooltip>
        </div>
      </div>

      {/* 内容区域 */}
      {!minimized && (
        <div className="floating-ai-panel__content">
          {/* 文档列表 */}
          <Card
            title="已选文档"
            size="small"
            extra={
              <Button
                type="link"
                size="small"
                danger
                onClick={clearAIAnalysisDocs}
                disabled={aiAnalysisDocs.length === 0}
              >
                清空
              </Button>
            }
            className="floating-ai-panel__docs"
          >
            {aiAnalysisDocs.length === 0 ? (
              <div className="floating-ai-panel__empty">
                <p>暂无文档</p>
                <p className="floating-ai-panel__empty-hint">
                  从QA回答或文档列表中添加文档
                </p>
              </div>
            ) : (
              <div className="floating-ai-panel__doc-list">
                {aiAnalysisDocs.map((doc, index) => {
                  const docName = doc.title || doc.name || doc.fileName || `文档${index + 1}`
                  const docId = doc.id || doc.name || doc.fileName || doc.title
                  
                  return (
                    <div key={docId || index} className="floating-ai-panel__doc-item">
                      <FileTextOutlined className="floating-ai-panel__doc-icon" />
                      <Tooltip title={docName}>
                        <span className="floating-ai-panel__doc-name">{docName}</span>
                      </Tooltip>
                      <Button
                        type="text"
                        size="small"
                        danger
                        icon={<DeleteOutlined />}
                        onClick={() => removeDocFromAIAnalysis(docId)}
                      />
                    </div>
                  )
                })}
              </div>
            )}
          </Card>

          {/* 分析类型选择 */}
          <div className="floating-ai-panel__analysis-type">
            <label>分析类型：</label>
            <Select
              value={analysisType}
              onChange={setAnalysisType}
              style={{ width: '100%' }}
            >
              <Option value={ANALYSIS_TYPES.SINGLE}>📄 单文档分析</Option>
              <Option value={ANALYSIS_TYPES.COMPARE}>🔄 多文档对比</Option>
              <Option value={ANALYSIS_TYPES.RELATION}>🔗 关联分析</Option>
              <Option value={ANALYSIS_TYPES.SYNTHESIS}>📊 综合报告</Option>
              <Option value={ANALYSIS_TYPES.CUSTOM}>✏️ 自定义问题</Option>
            </Select>
          </div>

          {/* 自定义提示词 */}
          {analysisType === ANALYSIS_TYPES.CUSTOM && (
            <div className="floating-ai-panel__custom-prompt">
              <TextArea
                value={customPrompt}
                onChange={(e) => setCustomPrompt(e.target.value)}
                placeholder="输入你的分析问题或要求..."
                rows={3}
                maxLength={500}
                showCount
              />
            </div>
          )}

          {/* 分析按钮 */}
          <Button
            type="primary"
            block
            icon={analyzing ? <SyncOutlined spin /> : <FileTextOutlined />}
            onClick={handleAnalyze}
            disabled={aiAnalysisDocs.length === 0 || analyzing}
            loading={analyzing}
          >
            {analyzing ? '分析中...' : '开始分析'}
          </Button>

          {/* 分析结果 */}
          {analysisResult && (
            <Card
              title="分析结果"
              size="small"
              className="floating-ai-panel__result"
            >
              {analysisResult.error ? (
                <div className="floating-ai-panel__error">
                  ❌ {analysisResult.error}
                </div>
              ) : (
                <div className="floating-ai-panel__result-content">
                  <MarkdownRenderer content={analysisResult.answer} />
                </div>
              )}
            </Card>
          )}
        </div>
      )}

      {/* 停靠预览区域 */}
      {dragging && <DockDropZone previewDock={previewDock} />}
    </div>
  )
}

export default FloatingAIPanel
