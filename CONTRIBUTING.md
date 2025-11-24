# 🤝 贡献指南 / Contributing Guide

[English](#english) | [中文](#中文)

---

## 中文

感谢你有兴趣为 **AI Reviewer Base File RAG** 做出贡献！我们欢迎所有形式的贡献，包括但不限于：

- 🐛 报告 Bug
- 💡 提出新功能建议
- 📝 改进文档
- 🔧 提交代码修复或新功能
- 🌍 翻译文档
- 📊 性能优化
- ✅ 增加测试用例

---

## 📋 目录

- [行为准则](#行为准则)
- [开始之前](#开始之前)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [Pull Request 流程](#pull-request-流程)
- [问题反馈](#问题反馈)
- [社区支持](#社区支持)

---

## 🎯 行为准则

参与本项目，即表示你同意遵守我们的行为准则：

- ✅ 尊重所有贡献者和用户
- ✅ 使用友善和包容的语言
- ✅ 理解和接受不同的观点
- ✅ 优雅地接受建设性批评
- ✅ 关注对社区最有利的事情

❌ 不可接受的行为包括：
- 使用性别化语言或图像
- 恶意评论、侮辱或人身攻击
- 公开或私下骚扰
- 未经许可发布他人隐私信息
- 其他不专业或不受欢迎的行为

---

## 🚀 开始之前

### 1. 搜索现有 Issues

在创建新 Issue 或提交 PR 之前，请先搜索：
- [现有 Issues](https://github.com/jinhua10/ai-reviewer-base-file-rag/issues)
- [Pull Requests](https://github.com/jinhua10/ai-reviewer-base-file-rag/pulls)
- [Discussions](https://github.com/jinhua10/ai-reviewer-base-file-rag/discussions)

避免重复工作。

### 2. 理解项目架构

阅读以下文档了解项目结构：
- [README.md](README.md) - 项目概览
- [项目分析报告.md](md/项目分析报告.md) - 架构设计
- [API 文档](docs/API-REFERENCE.md) - API 说明

### 3. 准备开发环境

确保你的环境满足以下要求：
- **Java**: 11+ (推荐 Java 17)
- **Maven**: 3.6+
- **IDE**: IntelliJ IDEA / Eclipse / VS Code
- **Git**: 最新版本

---

## 🔧 开发流程

### 1. Fork 仓库

点击右上角的 "Fork" 按钮，将项目 Fork 到你的 GitHub 账号。

### 2. 克隆到本地

```bash
git clone https://github.com/YOUR_USERNAME/ai-reviewer-base-file-rag.git
cd ai-reviewer-base-file-rag
```

### 3. 添加上游仓库

```bash
git remote add upstream https://github.com/jinhua10/ai-reviewer-base-file-rag.git
```

### 4. 创建分支

```bash
# 同步最新代码
git fetch upstream
git checkout main
git merge upstream/main

# 创建特性分支
git checkout -b feature/your-feature-name
# 或 bugfix/your-bugfix-name
```

### 5. 开发和测试

```bash
# 构建项目
mvn clean install

# 运行测试
mvn test

# 运行特定测试
mvn test -Dtest=YourTestClass

# 跳过测试构建
mvn clean package -DskipTests
```

### 6. 提交更改

```bash
git add .
git commit -m "feat: add new feature"
git push origin feature/your-feature-name
```

### 7. 创建 Pull Request

访问你的 Fork 仓库页面，点击 "New Pull Request"。

---

## 📐 代码规范

### Java 代码风格

遵循 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)：

#### 命名规范

```java
// ✅ 正确
public class DocumentParser { }           // 类名：PascalCase
private String documentPath;              // 变量：camelCase
private static final int MAX_SIZE = 100;  // 常量：UPPER_SNAKE_CASE
public void parseDocument() { }           // 方法：camelCase

// ❌ 错误
public class document_parser { }          // 类名不应使用下划线
private String DocumentPath;              // 变量不应大写开头
private static final int maxSize = 100;   // 常量应全大写
```

#### 注释规范

```java
/**
 * 文档解析器 - 支持多种格式的文档解析
 * Document Parser - Supports parsing multiple formats
 *
 * @author Your Name
 * @since 1.0
 */
public class DocumentParser {
    
    /**
     * 解析文档
     * Parse document
     *
     * @param filePath 文档路径 / Document path
     * @return 解析结果 / Parse result
     * @throws IOException 文件读取异常 / File read exception
     */
    public Document parse(String filePath) throws IOException {
        // 实现逻辑
    }
}
```

#### 代码格式

- **缩进**: 4 个空格（不使用 Tab）
- **行宽**: 建议不超过 120 字符
- **空行**: 方法之间添加一个空行
- **括号**: 使用 K&R 风格（左括号不换行）

```java
// ✅ 正确
public void method() {
    if (condition) {
        doSomething();
    } else {
        doOtherthing();
    }
}

// ❌ 错误
public void method()
{
    if(condition)
    {
        doSomething();
    }
    else
    {
        doOtherthing();
    }
}
```

### 测试规范

- 所有新功能必须包含单元测试
- 测试覆盖率应 > 80%
- 测试类命名：`XxxTest.java`
- 测试方法命名：`testMethodName_Scenario_ExpectedResult()`

```java
@Test
public void testParseDocument_WithPdfFile_ShouldReturnDocument() {
    // Given
    String filePath = "test.pdf";
    
    // When
    Document result = parser.parse(filePath);
    
    // Then
    assertNotNull(result);
    assertEquals("Expected Title", result.getTitle());
}
```

### 日志规范

使用 SLF4J + Logback：

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YourClass {
    
    public void method() {
        log.debug("Debug message: {}", variable);      // 调试信息
        log.info("Info message: {}", variable);        // 一般信息
        log.warn("Warning message: {}", variable);     // 警告信息
        log.error("Error message: {}", variable, e);   // 错误信息（带异常）
    }
}
```

---

## 📝 提交规范

使用 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

### 提交类型

| 类型 | 说明 | 示例 |
|------|------|------|
| **feat** | 新功能 | `feat: add vector search support` |
| **fix** | Bug 修复 | `fix: resolve memory leak in cache` |
| **docs** | 文档更新 | `docs: update README installation guide` |
| **style** | 代码格式（不影响功能） | `style: format code according to style guide` |
| **refactor** | 重构（不增加功能也不修复 Bug） | `refactor: extract method for better readability` |
| **perf** | 性能优化 | `perf: optimize index search algorithm` |
| **test** | 测试相关 | `test: add unit tests for DocumentParser` |
| **chore** | 构建/工具相关 | `chore: update Maven dependencies` |
| **ci** | CI/CD 相关 | `ci: add GitHub Actions workflow` |

### 提交格式

```
<type>(<scope>): <subject>

<body>

<footer>
```

### 示例

```
feat(search): add hybrid search mode

- Implement BM25 + vector search fusion
- Add configuration options for hybrid mode
- Update documentation with usage examples

Closes #123
```

```
fix(cache): resolve concurrent modification exception

The cache implementation had a race condition when multiple
threads accessed the same entry. Fixed by using ConcurrentHashMap.

Fixes #456
```

---

## 🔄 Pull Request 流程

### 1. PR 标题

使用与提交信息相同的格式：

```
feat: add new feature
fix: resolve bug in component
docs: update contributing guide
```

### 2. PR 描述模板

```markdown
## 📋 变更描述 / Change Description

简要描述这个 PR 的目的和内容。

## 🎯 相关 Issue / Related Issues

Closes #123
Related to #456

## 🔧 变更类型 / Change Type

- [ ] Bug 修复 / Bug fix
- [ ] 新功能 / New feature
- [ ] 文档更新 / Documentation update
- [ ] 重构 / Refactoring
- [ ] 性能优化 / Performance improvement
- [ ] 测试 / Testing

## ✅ 测试 / Testing

描述如何测试这些变更：
- [ ] 单元测试通过 / Unit tests pass
- [ ] 集成测试通过 / Integration tests pass
- [ ] 手动测试通过 / Manual testing completed

## 📸 截图 / Screenshots

如果适用，添加截图。

## ✔️ 检查清单 / Checklist

- [ ] 代码遵循项目的代码规范 / Code follows style guidelines
- [ ] 我已经进行了自我审查 / I have performed a self-review
- [ ] 我已经添加了必要的注释 / I have commented my code
- [ ] 我已经更新了相关文档 / I have updated documentation
- [ ] 我的更改没有产生新的警告 / My changes generate no new warnings
- [ ] 我已经添加了测试用例 / I have added tests
- [ ] 所有测试都通过 / All tests pass
```

### 3. 代码审查

- PR 必须通过至少 1 位维护者的审查
- 必须通过所有自动化测试
- 必须解决所有审查意见

### 4. 合并

审查通过后，维护者会将 PR 合并到主分支。

---

## 🐛 问题反馈

### Bug 报告

请使用 [Bug Report 模板](.github/ISSUE_TEMPLATE/bug_report.md) 创建 Issue，包含：

- 清晰的 Bug 描述
- 复现步骤
- 期望行为 vs 实际行为
- 环境信息（OS、Java 版本等）
- 相关日志和截图

### 功能请求

请使用 [Feature Request 模板](.github/ISSUE_TEMPLATE/feature_request.md) 创建 Issue，包含：

- 功能描述
- 使用场景
- 建议的实现方案
- 优先级评估

### 问题咨询

请使用 [Question 模板](.github/ISSUE_TEMPLATE/question.md) 或前往 [Discussions](https://github.com/jinhua10/ai-reviewer-base-file-rag/discussions)。

---

## 💬 社区支持

### 交流渠道

- **GitHub Issues**: Bug 报告和功能请求
- **GitHub Discussions**: 一般性讨论和问题
- **Email**: 1015770492@qq.com

### 响应时间

- Issues: 通常在 48 小时内回复
- Pull Requests: 通常在 3-5 个工作日内审查
- 紧急问题: 请通过邮件联系

---

## 🏆 贡献者

感谢所有为项目做出贡献的开发者！

<!-- ALL-CONTRIBUTORS-LIST:START -->
<!-- 贡献者列表将在这里自动更新 -->
<!-- ALL-CONTRIBUTORS-LIST:END -->

---

## 📄 许可证

贡献代码即表示你同意将代码以 [Apache License 2.0](LICENSE.txt) 许可证发布。

---

<div align="center">

**再次感谢你的贡献！🎉**

Made with ❤️ by the AI Reviewer Team

</div>

---
---

## English

Thank you for your interest in contributing to **AI Reviewer Base File RAG**! We welcome all forms of contributions, including but not limited to:

- 🐛 Reporting bugs
- 💡 Suggesting new features
- 📝 Improving documentation
- 🔧 Submitting code fixes or new features
- 🌍 Translating documentation
- 📊 Performance optimization
- ✅ Adding test cases

---

## 📋 Table of Contents

- [Code of Conduct](#code-of-conduct)
- [Before You Start](#before-you-start)
- [Development Workflow](#development-workflow)
- [Code Style](#code-style)
- [Commit Convention](#commit-convention)
- [Pull Request Process](#pull-request-process)
- [Issue Reporting](#issue-reporting)
- [Community Support](#community-support)

---

## 🎯 Code of Conduct

By participating in this project, you agree to abide by our code of conduct:

- ✅ Be respectful to all contributors and users
- ✅ Use welcoming and inclusive language
- ✅ Accept differing viewpoints and experiences
- ✅ Gracefully accept constructive criticism
- ✅ Focus on what is best for the community

❌ Unacceptable behavior includes:
- Using sexualized language or imagery
- Trolling, insulting, or derogatory comments
- Public or private harassment
- Publishing others' private information without permission
- Other unprofessional or unwelcome conduct

---

## 🚀 Before You Start

### 1. Search Existing Issues

Before creating a new issue or PR, please search:
- [Existing Issues](https://github.com/jinhua10/ai-reviewer-base-file-rag/issues)
- [Pull Requests](https://github.com/jinhua10/ai-reviewer-base-file-rag/pulls)
- [Discussions](https://github.com/jinhua10/ai-reviewer-base-file-rag/discussions)

Avoid duplicate work.

### 2. Understand Project Architecture

Read the following documentation:
- [README.md](README.md) - Project overview
- [Project Analysis Report](md/项目分析报告.md) - Architecture design
- [API Documentation](docs/API-REFERENCE.md) - API reference

### 3. Set Up Development Environment

Ensure your environment meets these requirements:
- **Java**: 11+ (Java 17 recommended)
- **Maven**: 3.6+
- **IDE**: IntelliJ IDEA / Eclipse / VS Code
- **Git**: Latest version

---

## 🔧 Development Workflow

### 1. Fork the Repository

Click the "Fork" button at the top right.

### 2. Clone to Local

```bash
git clone https://github.com/YOUR_USERNAME/ai-reviewer-base-file-rag.git
cd ai-reviewer-base-file-rag
```

### 3. Add Upstream Remote

```bash
git remote add upstream https://github.com/jinhua10/ai-reviewer-base-file-rag.git
```

### 4. Create Branch

```bash
# Sync latest code
git fetch upstream
git checkout main
git merge upstream/main

# Create feature branch
git checkout -b feature/your-feature-name
# or bugfix/your-bugfix-name
```

### 5. Develop and Test

```bash
# Build project
mvn clean install

# Run tests
mvn test

# Run specific test
mvn test -Dtest=YourTestClass

# Build without tests
mvn clean package -DskipTests
```

### 6. Commit Changes

```bash
git add .
git commit -m "feat: add new feature"
git push origin feature/your-feature-name
```

### 7. Create Pull Request

Visit your fork on GitHub and click "New Pull Request".

---

## 📐 Code Style

### Java Code Style

Follow [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html):

#### Naming Convention

```java
// ✅ Correct
public class DocumentParser { }           // Class: PascalCase
private String documentPath;              // Variable: camelCase
private static final int MAX_SIZE = 100;  // Constant: UPPER_SNAKE_CASE
public void parseDocument() { }           // Method: camelCase

// ❌ Wrong
public class document_parser { }
private String DocumentPath;
private static final int maxSize = 100;
```

#### Documentation

```java
/**
 * Document Parser - Supports parsing multiple formats
 *
 * @author Your Name
 * @since 1.0
 */
public class DocumentParser {
    
    /**
     * Parse document
     *
     * @param filePath Document path
     * @return Parse result
     * @throws IOException File read exception
     */
    public Document parse(String filePath) throws IOException {
        // Implementation
    }
}
```

#### Formatting

- **Indentation**: 4 spaces (no tabs)
- **Line width**: Max 120 characters recommended
- **Blank lines**: One between methods
- **Braces**: K&R style (opening brace on same line)

### Testing Convention

- All new features must include unit tests
- Test coverage should be > 80%
- Test class naming: `XxxTest.java`
- Test method naming: `testMethodName_Scenario_ExpectedResult()`

### Logging Convention

Use SLF4J + Logback:

```java
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class YourClass {
    
    public void method() {
        log.debug("Debug message: {}", variable);
        log.info("Info message: {}", variable);
        log.warn("Warning message: {}", variable);
        log.error("Error message: {}", variable, e);
    }
}
```

---

## 📝 Commit Convention

Use [Conventional Commits](https://www.conventionalcommits.org/):

### Commit Types

| Type | Description | Example |
|------|-------------|---------|
| **feat** | New feature | `feat: add vector search support` |
| **fix** | Bug fix | `fix: resolve memory leak in cache` |
| **docs** | Documentation | `docs: update README installation guide` |
| **style** | Code formatting | `style: format code according to style guide` |
| **refactor** | Refactoring | `refactor: extract method for readability` |
| **perf** | Performance | `perf: optimize index search algorithm` |
| **test** | Testing | `test: add unit tests for DocumentParser` |
| **chore** | Build/tools | `chore: update Maven dependencies` |
| **ci** | CI/CD | `ci: add GitHub Actions workflow` |

---

## 🔄 Pull Request Process

### 1. PR Title

Use the same format as commit messages:

```
feat: add new feature
fix: resolve bug in component
docs: update contributing guide
```

### 2. Code Review

- PR must be reviewed by at least 1 maintainer
- All automated tests must pass
- All review comments must be addressed

### 3. Merge

After approval, maintainers will merge the PR.

---

## 🐛 Issue Reporting

### Bug Report

Use the [Bug Report template](.github/ISSUE_TEMPLATE/bug_report.md).

### Feature Request

Use the [Feature Request template](.github/ISSUE_TEMPLATE/feature_request.md).

### Questions

Use the [Question template](.github/ISSUE_TEMPLATE/question.md) or [Discussions](https://github.com/jinhua10/ai-reviewer-base-file-rag/discussions).

---

## 💬 Community Support

### Communication Channels

- **GitHub Issues**: Bug reports and feature requests
- **GitHub Discussions**: General discussions and questions
- **Email**: 1015770492@qq.com

### Response Time

- Issues: Usually within 48 hours
- Pull Requests: Usually within 3-5 business days
- Urgent issues: Contact via email

---

## 🏆 Contributors

Thanks to all contributors!

<!-- ALL-CONTRIBUTORS-LIST:START -->
<!-- Contributors list will be updated automatically -->
<!-- ALL-CONTRIBUTORS-LIST:END -->

---

## 📄 License

By contributing, you agree that your contributions will be licensed under [Apache License 2.0](LICENSE.txt).

---

<div align="center">

**Thank you for your contribution! 🎉**

Made with ❤️ by the AI Reviewer Team

</div>

