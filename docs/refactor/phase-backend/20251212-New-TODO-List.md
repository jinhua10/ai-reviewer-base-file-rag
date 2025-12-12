# 新产生的 TODO 清单
# New TODO List

> **创建日期**: 2025-12-12  
> **状态**: 📋 待处理  
> **优先级**: 按类别标注

---

## 📊 TODO 总览

### 统计
```yaml
总数: 10 个
优先级分布:
  P1 (高): 4 个
  P2 (中): 4 个
  P3 (低): 2 个
```

---

## 🎯 按优先级分类

### P1 - 高优先级（核心功能待完善）

#### 1. 愿望单评论列表获取 🔴
**位置**: `WishController.java:166`
```java
// TODO: 实现获取评论列表
```

**详细说明**:
- 当前返回空列表
- 需要从 wish 的 metadata 中读取评论
- 构建嵌套的评论树结构
- 支持排序和过滤

**实现建议**:
```java
public ResponseEntity<?> getComments(@PathVariable String id) {
    // 1. 获取 wish 文档
    Document doc = ragService.getDocument(id);
    
    // 2. 从 metadata 读取 comments
    List<Map<String, Object>> commentsData = 
        (List<Map<String, Object>>) doc.getMetadata().get("comments");
    
    // 3. 转换为 CommentDTO 列表
    List<CommentDTO> comments = buildCommentTree(commentsData);
    
    return ResponseEntity.ok(comments);
}
```

**工作量**: 1-2 小时

---

#### 2. 愿望单评论点赞 🔴
**位置**: `WishController.java:223`
```java
// TODO: 实现点赞评论
```

**详细说明**:
- 当前只返回成功但未实现逻辑
- 需要更新评论的 likedBy 集合
- 更新 likeCount
- 防止重复点赞

**实现建议**:
```java
public ResponseEntity<?> likeComment(@PathVariable String commentId, String userId) {
    // 1. 查找包含该评论的 wish
    // 2. 找到对应的 comment
    // 3. 检查 likedBy 集合
    // 4. 添加/移除用户 ID
    // 5. 更新 likeCount
    // 6. 重新索引文档
}
```

**工作量**: 1-2 小时

---

#### 3. PPT 生成实际实现 🔴
**位置**: `PPTGeneratorService.java:40`
```java
// TODO: 实际实现需要调用 LLM 生成大纲，然后使用 Apache POI 或其他库生成 PPT
```

**详细说明**:
- 当前只是模拟生成
- 需要实际调用 LLM 生成大纲
- 使用 Apache POI 或 python-pptx 生成 PPT 文件
- 保存文件并返回下载链接

**实现建议**:
1. **生成大纲**:
   ```java
   // 调用 LLM 生成 PPT 大纲
   String outline = llmService.generate(
       "根据主题生成PPT大纲: " + request.getTopic()
   );
   ```

2. **生成 PPT**:
   ```java
   // 使用 Apache POI
   XMLSlideShow ppt = new XMLSlideShow();
   XSLFSlide slide = ppt.createSlide();
   // 添加标题、内容等
   ```

3. **保存文件**:
   ```java
   File file = new File("ppt/" + fileName);
   ppt.write(new FileOutputStream(file));
   ```

**依赖**:
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

**工作量**: 4-6 小时

---

#### 4. 模型切换实际实现 🔴
**位置**: `ServiceController.java:200`
```java
// TODO: 实现实际的模型切换逻辑
```

**详细说明**:
- 当前只返回成功消息
- 需要实际切换 LLM 模型配置
- 支持本地模型和在线模型切换

**实现建议**:
```java
public ResponseEntity<?> switchModel(@RequestBody Map<String, String> request) {
    String modelType = request.get("modelType");
    
    if ("local".equals(modelType)) {
        // 切换到本地模型
        llmConfig.setEndpoint("http://localhost:11434");
        llmConfig.setModel("qwen2.5:latest");
    } else if ("online".equals(modelType)) {
        // 切换到在线模型
        llmConfig.setEndpoint("https://api.openai.com");
        llmConfig.setModel("gpt-4");
    }
    
    // 重新初始化 LLM 客户端
    llmService.reinitialize(llmConfig);
}
```

**工作量**: 2-3 小时

---

### P2 - 中优先级（配置和管理功能）

#### 5. 系统配置更新实现 🟡
**位置**: `AdminController.java:36`
```java
// TODO: 实现实际的配置更新逻辑
```

**详细说明**:
- 当前只返回成功消息
- 需要实际更新系统配置
- 持久化配置到文件或数据库

**实现建议**:
```java
public ResponseEntity<?> updateSystemConfig(@RequestBody Map<String, Object> config) {
    // 1. 验证配置
    validateConfig(config);
    
    // 2. 更新配置对象
    systemConfig.update(config);
    
    // 3. 保存到文件
    configService.save(systemConfig);
    
    // 4. 重新加载需要的服务
    applicationContext.refresh();
}
```

**工作量**: 2-3 小时

---

#### 6. P2P 网络连接实现 🟡
**位置**: `P2PCollaborationManager.java:101`
```java
// TODO: 实现实际的网络连接
```

**详细说明**:
- 当前只创建连接对象
- 需要实现实际的 P2P 网络连接
- 可以使用 WebSocket 或 Socket

**实现建议**:
```java
// 使用 WebSocket
WebSocketClient client = new WebSocketClient(peerAddress);
client.connect();

// 或使用 Java NIO
SocketChannel channel = SocketChannel.open();
channel.connect(new InetSocketAddress(peerAddress, peerPort));
```

**工作量**: 6-8 小时（较复杂）

---

#### 7. P2P 知识发送实现 🟡
**位置**: `P2PCollaborationManager.java:157`
```java
// TODO: 实际发送到对方
```

**详细说明**:
- 需要通过网络传输知识内容
- 使用已建立的连接发送数据

**实现建议**:
```java
// 通过 WebSocket 发送
connection.getWebSocket().send(encrypted);

// 或通过 Socket 发送
connection.getChannel().write(ByteBuffer.wrap(encrypted.getBytes()));
```

**依赖**: 需要先完成 TODO #6

**工作量**: 2-3 小时

---

#### 8. P2P 验证请求实现 🟡
**位置**: `P2PCollaborationManager.java:213`
```java
// TODO: 实现验证请求逻辑
```

**详细说明**:
- 发送验证请求给对方
- 等待对方反馈

**实现建议**:
```java
VerificationRequest request = new VerificationRequest();
request.setKnowledgeId(knowledgeId);
request.setRequesterId(currentUserId);

String requestJson = objectMapper.writeValueAsString(request);
connection.send(requestJson);
```

**工作量**: 1-2 小时

---

### P3 - 低优先级（优化和扩展）

#### 9. P2P 反馈发送实现 🟢
**位置**: `P2PCollaborationManager.java:249`
```java
// TODO: 发送反馈给对方
```

**详细说明**:
- 发送验证反馈给对方
- 类似验证请求的实现

**实现建议**:
```java
String feedbackJson = objectMapper.writeValueAsString(feedback);
connection.send(feedbackJson);
```

**工作量**: 1 小时

---

#### 10. AIServiceRegistry PPT 生成 🟢
**位置**: `AIServiceRegistry.java:277`
```java
// TODO: 实现实际的PPT生成逻辑
```

**详细说明**:
- 这是另一个 PPT 生成实现点
- 可能与 TODO #3 重复
- 需要确认是否需要单独实现

**工作量**: 待定（可能重复）

---

## 📋 实施计划

### 阶段 1: 核心功能完善（P1）
**预计时间**: 8-13 小时

```
Week 1:
  Day 1: TODO #1, #2 - 愿望单评论功能 (2-4小时)
  Day 2: TODO #3 - PPT 生成实现 (4-6小时)
  Day 3: TODO #4 - 模型切换实现 (2-3小时)
```

### 阶段 2: 配置和管理（P2）
**预计时间**: 11-16 小时

```
Week 2:
  Day 1-2: TODO #6 - P2P 网络连接 (6-8小时)
  Day 3: TODO #5, #7, #8 - 配置和P2P通信 (5-8小时)
```

### 阶段 3: 优化和完善（P3）
**预计时间**: 1-2 小时

```
Week 3:
  Day 1: TODO #9, #10 - 反馈和其他 (1-2小时)
```

**总计**: 20-31 小时（约 3-4 周）

---

## 🎯 优先级建议

### 立即实施（本周）
1. ✅ TODO #1 - 愿望单评论列表
2. ✅ TODO #2 - 评论点赞

### 近期实施（下周）
3. ✅ TODO #4 - 模型切换
4. ✅ TODO #5 - 系统配置

### 中期实施（2-3周）
5. ✅ TODO #3 - PPT 实际生成
6. ✅ TODO #6, #7, #8 - P2P 网络功能

### 可选实施
7. ✅ TODO #9, #10 - 其他优化

---

## 📝 注意事项

### 技术依赖
```xml
<!-- PPT 生成需要 -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- WebSocket 支持 -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-websocket</artifactId>
</dependency>
```

### 兼容性
- 所有实现应保持与现有架构一致
- 继续使用基于文档的存储方式
- 保持国际化支持

### 测试
- 每个 TODO 完成后需要添加单元测试
- 进行集成测试
- 前后端联调测试

---

## 🔄 更新记录

```yaml
2025-12-12:
  - 创建 TODO 清单
  - 识别 10 个待完成项
  - 制定实施计划
```

---

## 📚 相关文档

- **代码规范**: `20251209-23-00-00-CODE_STANDARDS.md`
- **后端完成报告**: `20251212-All-Tasks-Complete.md`
- **任务清单**: `20251212-BACKEND_TODO_PLAN.md`

---

**文档版本**: v1.0  
**创建日期**: 2025-12-12  
**作者**: AI Programming Assistant  
**状态**: 📋 待实施

