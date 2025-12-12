/**
 * 流式答案组件 (Streaming Answer Component)
 *
 * 实现打字机效果的流式文本展示
 * (Implements typewriter effect for streaming text display)
 *
 * @author AI Reviewer Team
 * @since 2025-12-12
 */

import React from 'react'
import '../../assets/css/qa/streaming-answer.css'

function StreamingAnswer(props) {
  const { content } = props

  return (
    <div className="streaming-answer">
      <div className="streaming-answer__text">
        {content}
        <span className="streaming-answer__cursor">|</span>
      </div>
    </div>
  )
}

export default StreamingAnswer

