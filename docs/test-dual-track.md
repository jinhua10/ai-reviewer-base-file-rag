# HOPE 流式双轨 - 测试脚本
# HOPE Streaming Dual-Track - Test Scripts

> **文件**: test-dual-track.md  
> **用途**: 测试验证指南

---

## 🧪 测试脚本集合

### 测试 1: 浏览器控制台测试

**打开浏览器控制台** (F12)，运行以下代码：

```javascript
// ========================================
// 测试 1: 基本渲染测试
// ========================================
console.log('🧪 测试 1: DualTrackAnswer 基本渲染');

// 创建测试容器
const testContainer = document.createElement('div');
testContainer.id = 'dual-track-test';
testContainer.style.cssText = `
    margin: 20px;
    padding: 20px;
    border: 3px dashed #667eea;
    border-radius: 12px;
    background: #f9fafb;
`;
document.body.appendChild(testContainer);

// 渲染组件
ReactDOM.render(
    React.createElement(window.DualTrackAnswer, {
        question: '什么是RAG？',
        sessionId: 'test-' + Date.now(),
        onComplete: (result) => {
            console.log('✅ 测试完成！');
            console.log('📊 结果统计：');
            console.log('  - HOPE 答案:', result.hope?.content?.substring(0, 50) + '...');
            console.log('  - HOPE 来源:', result.hope?.source);
            console.log('  - HOPE 置信度:', result.hope?.confidence);
            console.log('  - HOPE 耗时:', result.hope?.responseTime + 'ms');
            console.log('  - LLM 答案长度:', result.llm?.length);
            console.log('  - 加速比:', (result.totalTime.llm / result.totalTime.hope).toFixed(1) + 'x');
        }
    }),
    testContainer
);

console.log('✅ 组件已挂载到页面，请观察渲染效果');
```

---

### 测试 2: 多语言切换测试

```javascript
// ========================================
// 测试 2: 国际化测试
// ========================================
console.log('🧪 测试 2: 国际化切换测试');

// 切换到英文
window.LanguageModule.changeLanguage('en');
console.log('✅ 已切换到英文，请检查界面文字');

// 等待 3 秒后切换回中文
setTimeout(() => {
    window.LanguageModule.changeLanguage('zh');
    console.log('✅ 已切换回中文');
}, 3000);
```

---

### 测试 3: API 直接测试（需要后端运行）

```javascript
// ========================================
// 测试 3: SSE API 测试
// ========================================
console.log('🧪 测试 3: SSE API 直接测试');

const testSSE = (question) => {
    const url = `/api/qa/stream/dual-track?question=${encodeURIComponent(question)}`;
    const eventSource = new EventSource(url);
    
    const startTime = Date.now();
    let hopeReceived = false;
    let llmChunks = 0;
    
    eventSource.addEventListener('hope', (e) => {
        const msg = JSON.parse(e.data);
        hopeReceived = true;
        const elapsed = Date.now() - startTime;
        console.log(`💡 HOPE 答案收到 (${elapsed}ms):`, {
            source: msg.hopeSource,
            confidence: msg.confidence,
            responseTime: msg.responseTime
        });
    });
    
    eventSource.addEventListener('llm', (e) => {
        const msg = JSON.parse(e.data);
        llmChunks++;
        if (llmChunks === 1) {
            const elapsed = Date.now() - startTime;
            console.log(`🤖 首个 LLM 块收到 (${elapsed}ms)`);
        }
    });
    
    eventSource.addEventListener('complete', (e) => {
        const msg = JSON.parse(e.data);
        const totalTime = Date.now() - startTime;
        console.log(`✅ 流式响应完成 (${totalTime}ms):`, {
            hopeReceived,
            llmChunks: msg.totalChunks,
            llmTime: msg.totalTime
        });
        eventSource.close();
    });
    
    eventSource.onerror = (e) => {
        console.error('❌ SSE 连接错误:', e);
        eventSource.close();
    };
    
    console.log('📡 SSE 连接已建立，等待响应...');
};

// 执行测试
testSSE('什么是RAG？');
```

---

### 测试 4: 反馈 API 测试

```javascript
// ========================================
// 测试 4: 反馈 API 测试
// ========================================
console.log('🧪 测试 4: 反馈 API 测试');

const testFeedback = async () => {
    try {
        const result = await window.api.submitDualTrackChoice(
            '测试问题',
            'HOPE',
            {
                content: '测试 HOPE 答案',
                source: 'PERMANENT_LAYER',
                confidence: 0.95,
                responseTime: 150
            },
            '测试 LLM 答案',
            'test-session'
        );
        
        console.log('✅ 反馈提交成功:', result);
    } catch (error) {
        console.error('❌ 反馈提交失败:', error);
    }
};

testFeedback();
```

---

### 测试 5: 错误处理测试

```javascript
// ========================================
// 测试 5: 错误处理测试
// ========================================
console.log('🧪 测试 5: 错误处理测试');

// 测试空问题
const testContainer5 = document.createElement('div');
testContainer5.id = 'error-test';
document.body.appendChild(testContainer5);

ReactDOM.render(
    React.createElement(window.DualTrackAnswer, {
        question: '',  // 空问题
        sessionId: 'error-test',
        onComplete: (result) => {
            console.log('完成（不应该到这里）:', result);
        }
    }),
    testContainer5
);

console.log('✅ 错误处理测试已启动，观察错误提示');
```

---

### 测试 6: 性能测试

```javascript
// ========================================
// 测试 6: 性能测试
// ========================================
console.log('🧪 测试 6: 性能测试（连续 5 次请求）');

const performanceTest = async () => {
    const questions = [
        '什么是RAG？',
        '什么是向量数据库？',
        '什么是 Embedding？',
        '什么是 LLM？',
        '什么是提示词工程？'
    ];
    
    const results = [];
    
    for (let i = 0; i < questions.length; i++) {
        console.log(`📊 测试 ${i + 1}/${questions.length}: ${questions[i]}`);
        
        const startTime = Date.now();
        
        // 模拟单个请求
        await new Promise((resolve) => {
            const url = `/api/qa/stream/dual-track?question=${encodeURIComponent(questions[i])}`;
            const es = new EventSource(url);
            
            let hopeTime = 0;
            
            es.addEventListener('hope', () => {
                hopeTime = Date.now() - startTime;
            });
            
            es.addEventListener('complete', () => {
                const totalTime = Date.now() - startTime;
                results.push({
                    question: questions[i],
                    hopeTime,
                    totalTime
                });
                es.close();
                resolve();
            });
            
            es.onerror = () => {
                es.close();
                resolve();
            };
        });
        
        // 间隔 1 秒
        await new Promise(resolve => setTimeout(resolve, 1000));
    }
    
    console.log('✅ 性能测试完成！');
    console.log('📊 统计结果:');
    console.table(results);
    
    const avgHopeTime = results.reduce((sum, r) => sum + r.hopeTime, 0) / results.length;
    const avgTotalTime = results.reduce((sum, r) => sum + r.totalTime, 0) / results.length;
    
    console.log(`📈 平均 HOPE 耗时: ${avgHopeTime.toFixed(0)}ms`);
    console.log(`📈 平均总耗时: ${avgTotalTime.toFixed(0)}ms`);
};

performanceTest();
```

---

## ✅ 验收检查清单

### 功能验收
- [ ] DualTrackAnswer 组件正常渲染
- [ ] HOPE 轨道显示快速答案（< 300ms）
- [ ] LLM 轨道流式显示文本
- [ ] 光标闪烁动画正常
- [ ] 对比面板在完成后显示
- [ ] 三个选择按钮可点击
- [ ] Toast 提示正常显示

### 样式验收
- [ ] 渐变背景正确（紫色/粉色）
- [ ] 滑入动画流畅
- [ ] hover 效果正常
- [ ] 响应式布局适配（桌面/移动）
- [ ] 深色模式兼容

### 性能验收
- [ ] HOPE 响应 < 300ms
- [ ] LLM 首字节 < 500ms
- [ ] 流式渲染流畅（无卡顿）
- [ ] 内存使用正常

### 国际化验收
- [ ] 中文显示正确
- [ ] 英文显示正确
- [ ] 切换语言正常

---

## 🐛 常见问题排查

### 问题 1: 组件无法渲染
**症状**: `window.DualTrackAnswer is undefined`

**解决方案**:
1. 检查 `index.html` 是否引入了 `DualTrackAnswer.jsx`
2. 检查浏览器控制台是否有 JSX 编译错误
3. 确保 Babel 正确加载

---

### 问题 2: SSE 连接失败
**症状**: `EventSource failed` 或 连接立即关闭

**解决方案**:
1. 确认后端已启动：`mvn spring-boot:run`
2. 检查 URL 是否正确
3. 查看后端日志是否有错误
4. 检查防火墙/代理设置

---

### 问题 3: HOPE 答案不显示
**症状**: HOPE 轨道显示 "暂无 HOPE 答案"

**原因**: 
- HOPE 系统未启用
- 问题在 HOPE 中无匹配
- HOPE 查询超时

**解决方案**:
1. 检查 `application.yml` 中 `knowledge.qa.hope.enabled: true`
2. 查看后端日志中的 HOPE 查询信息
3. 尝试其他问题

---

### 问题 4: 样式显示异常
**症状**: 布局混乱，无渐变背景

**解决方案**:
1. 检查 `index.html` 是否引入了 `dual-track-answer.css`
2. 清除浏览器缓存
3. 检查 CSS 文件路径是否正确

---

## 📝 测试报告模板

```markdown
# 双轨流式响应测试报告

**测试日期**: 2025-12-10
**测试人员**: [姓名]
**测试环境**: 
- 浏览器: Chrome 120
- 后端: Spring Boot 3.x
- JDK: 21

## 测试结果

### 功能测试
- [x] ✅ 组件渲染正常
- [x] ✅ HOPE 快速响应
- [x] ✅ LLM 流式生成
- [x] ✅ 对比选择功能
- [ ] ❌ xxx 功能异常

### 性能测试
- HOPE 平均响应时间: 180ms
- LLM 平均首字节: 420ms
- 加速比: 18.5x

### 问题记录
1. [问题描述]
2. [解决方案]

### 总体评价
[评价内容]
```

---

**测试完成后，记得清理测试容器**:
```javascript
// 清理测试容器
document.querySelectorAll('[id*="test"]').forEach(el => el.remove());
```

