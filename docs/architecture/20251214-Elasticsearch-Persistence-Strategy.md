# 🔍 ElasticSearch 持久化策略详解

> **文档编号**: 20251214-Elasticsearch-Persistence-Strategy  
> **创建日期**: 2025-12-14  
> **策略类型**: ElasticSearch 搜索引擎  
> **适用场景**: 海量数据 + 全文搜索

---

## 🎯 为什么选择 ElasticSearch？

### 核心优势

1. **🔍 强大的全文搜索**
   - 支持中文分词
   - 支持拼音搜索
   - 支持模糊搜索
   - 支持同义词搜索

2. **⚡ 实时搜索和分析**
   - 近实时（Near Real-Time）搜索
   - 聚合分析（Aggregations）
   - 复杂查询（Bool Query）

3. **📈 水平扩展**
   - 分布式架构
   - 自动分片（Sharding）
   - 自动副本（Replica）
   - 高可用性

4. **🎨 灵活的数据模型**
   - JSON文档存储
   - 动态映射（Dynamic Mapping）
   - 支持嵌套对象

---

## 💡 适用场景

### 场景1: 海量数据搜索

**数据规模**: 1,000,000+ 问题类型

```yaml
question-classifier:
  persistence:
    strategy: elasticsearch
    
    elasticsearch:
      hosts: es-cluster-01:9200,es-cluster-02:9200,es-cluster-03:9200
      scheme: http
      index-prefix: "qc-prod-"
```

**优势**:
- ✅ 毫秒级查询响应
- ✅ 支持复杂过滤条件
- ✅ 自动负载均衡

---

### 场景2: 智能问题推荐

**需求**: 根据用户输入，推荐相似问题

```json
// ElasticSearch Query DSL
{
  "query": {
    "more_like_this": {
      "fields": ["question", "keywords"],
      "like": "如何配置环境变量",
      "min_term_freq": 1,
      "max_query_terms": 12
    }
  }
}
```

**结果**:
- "如何设置环境变量"
- "怎么配置系统变量"
- "环境变量配置教程"

---

### 场景3: 多维度搜索

**需求**: 同时搜索问题、关键词、答案

```json
{
  "query": {
    "multi_match": {
      "query": "Docker 部署",
      "fields": ["question^3", "keywords^2", "answer"]
    }
  }
}
```

**特点**:
- ✅ 问题权重最高（^3）
- ✅ 关键词次之（^2）
- ✅ 答案也参与匹配

---

### 场景4: 实时统计分析

**需求**: 分析问题分类分布

```json
{
  "size": 0,
  "aggs": {
    "type_distribution": {
      "terms": {
        "field": "type",
        "size": 20
      }
    }
  }
}
```

**结果**:
```json
{
  "aggregations": {
    "type_distribution": {
      "buckets": [
        {"key": "procedural", "doc_count": 35000},
        {"key": "conceptual", "doc_count": 28000},
        {"key": "troubleshooting", "doc_count": 22000}
      ]
    }
  }
}
```

---

## 🏗️ 数据模型设计

### 索引结构

```json
{
  "settings": {
    "number_of_shards": 3,
    "number_of_replicas": 2,
    "analysis": {
      "analyzer": {
        "ik_smart_pinyin": {
          "type": "custom",
          "tokenizer": "ik_smart",
          "filter": ["pinyin_filter"]
        }
      }
    }
  },
  "mappings": {
    "properties": {
      "id": {
        "type": "keyword"
      },
      "name": {
        "type": "text",
        "analyzer": "ik_smart_pinyin",
        "fields": {
          "keyword": {
            "type": "keyword"
          }
        }
      },
      "priority": {
        "type": "integer"
      },
      "complexity": {
        "type": "keyword"
      },
      "keywords": {
        "type": "text",
        "analyzer": "ik_smart_pinyin"
      },
      "patterns": {
        "type": "keyword"
      },
      "enabled": {
        "type": "boolean"
      },
      "created_at": {
        "type": "date"
      },
      "updated_at": {
        "type": "date"
      }
    }
  }
}
```

---

## 📊 性能对比

### 搜索性能

| 操作 | JSON文件 | MongoDB | ElasticSearch |
|------|---------|---------|---------------|
| **精确查询** | 10ms | 5ms | 3ms |
| **模糊搜索** | ❌ 不支持 | 50ms | 5ms |
| **全文搜索** | ❌ 不支持 | 100ms+ | **10ms** |
| **聚合分析** | ❌ 不支持 | 200ms+ | **20ms** |
| **分页查询** | 慢 | 中等 | **快** |

---

### 数据规模对比

| 数据量 | JSON文件 | MongoDB | ElasticSearch |
|--------|---------|---------|---------------|
| 1K | ✅ 最优 | ✅ 优 | ✅ 优 |
| 10K | ✅ 优 | ✅ 最优 | ✅ 优 |
| 100K | ⚠️ 可用 | ✅ 最优 | ✅ 最优 |
| 1M | ❌ 不推荐 | ✅ 优 | ✅ **最优** |
| 10M+ | ❌ 不可用 | ⚠️ 可用 | ✅ **最优** |

---

## 🚀 配置示例

### 开发环境（单节点）

```yaml
question-classifier:
  persistence:
    strategy: elasticsearch
    cache-size: 200
    
    elasticsearch:
      hosts: localhost:9200
      scheme: http
      index-prefix: "qc-dev-"
      connection-timeout: 5000
      socket-timeout: 30000
```

---

### 生产环境（集群）

```yaml
question-classifier:
  persistence:
    strategy: elasticsearch
    cache-size: 1000
    
    elasticsearch:
      # 集群节点（负载均衡）
      hosts: es-node-01:9200,es-node-02:9200,es-node-03:9200
      scheme: https
      
      # 安全认证
      username: ${ES_USERNAME}
      password: ${ES_PASSWORD}
      
      # 索引配置
      index-prefix: "qc-prod-"
      
      # 超时配置
      connection-timeout: 10000
      socket-timeout: 60000
      max-retry-timeout: 120000
      
      # 节点嗅探（自动发现新节点）
      sniff-on-failure: true
```

---

## 🔧 高级特性

### 1. 中文分词

**使用 IK 分词器**:

```json
{
  "settings": {
    "analysis": {
      "analyzer": {
        "ik_max_word": {
          "type": "ik_max_word"
        },
        "ik_smart": {
          "type": "ik_smart"
        }
      }
    }
  }
}
```

**示例**:
- 输入: "如何配置Docker环境变量"
- IK分词: ["如何", "配置", "Docker", "环境", "变量"]

---

### 2. 拼音搜索

**插件**: elasticsearch-analysis-pinyin

```json
{
  "settings": {
    "analysis": {
      "filter": {
        "pinyin_filter": {
          "type": "pinyin",
          "keep_first_letter": true,
          "keep_full_pinyin": true
        }
      }
    }
  }
}
```

**效果**:
- 输入: "pz" → 匹配 "配置"
- 输入: "huanjingbianliang" → 匹配 "环境变量"

---

### 3. 同义词搜索

```json
{
  "settings": {
    "analysis": {
      "filter": {
        "synonym_filter": {
          "type": "synonym",
          "synonyms": [
            "配置,设置,config",
            "部署,发布,deploy",
            "错误,异常,error"
          ]
        }
      }
    }
  }
}
```

**效果**:
- 搜索 "配置" → 同时匹配 "设置"、"config"

---

### 4. 相似度搜索

```json
{
  "query": {
    "more_like_this": {
      "fields": ["question", "keywords"],
      "like": [
        {
          "_index": "qc-prod-types",
          "_id": "social_001"
        }
      ],
      "min_term_freq": 1,
      "min_doc_freq": 1
    }
  }
}
```

---

## 💰 成本考虑

### 资源需求

**最小配置**:
- CPU: 2核
- 内存: 4GB
- 磁盘: 20GB
- 适合: <100K数据

**推荐配置**:
- CPU: 4核+
- 内存: 8GB+
- 磁盘: 100GB+
- 适合: >100K数据

**生产集群**:
- 节点数: 3个+
- 每节点: 8核 + 32GB内存
- 磁盘: SSD 500GB+
- 适合: >1M数据

---

### 成本对比

| 场景 | JSON文件 | MongoDB | ElasticSearch |
|------|---------|---------|---------------|
| **开发** | $0 | $0 | **$0** (本地) |
| **小规模生产** | $0 | $50/月 | **$100/月** |
| **中等规模** | ❌ | $200/月 | **$300/月** |
| **大规模** | ❌ | $500/月 | **$1000/月** |

---

## ⚠️ 注意事项

### 1. 写入延迟

ElasticSearch 是**近实时**的，写入后需要 ~1秒才能搜索到。

**解决方案**:
```java
// 强制刷新（生产环境慎用）
client.indices().refresh(r -> r.index(indexName));
```

---

### 2. 内存占用

ES 需要大量内存，建议分配 JVM 堆内存不超过 32GB。

**配置**:
```yaml
# elasticsearch.yml
-Xms8g
-Xmx8g
```

---

### 3. 数据备份

**推荐使用快照备份**:

```json
PUT /_snapshot/my_backup
{
  "type": "fs",
  "settings": {
    "location": "/mount/backups/my_backup"
  }
}
```

---

## 🎊 总结

### ElasticSearch 适合

✅ **海量数据** - 1M+ 问题类型  
✅ **全文搜索** - 需要模糊搜索、拼音搜索  
✅ **实时分析** - 需要统计、聚合  
✅ **复杂查询** - 多条件组合查询  
✅ **分布式** - 需要高可用、水平扩展  

---

### 不适合的场景

❌ **小规模数据** - <10K，用 JSON文件更简单  
❌ **简单CRUD** - 只需要增删改查，用 H2/SQLite  
❌ **低成本** - 预算有限，用文件存储  
❌ **事务性操作** - 需要强一致性事务，用 PostgreSQL  

---

## 📚 扩展阅读

- [ElasticSearch 官方文档](https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html)
- [IK 分词器](https://github.com/medcl/elasticsearch-analysis-ik)
- [拼音分词器](https://github.com/medcl/elasticsearch-analysis-pinyin)
- [ElasticSearch 性能优化](https://www.elastic.co/guide/en/elasticsearch/reference/current/tune-for-search-speed.html)

---

**完成日期**: 2025-12-14  
**适用版本**: ElasticSearch 8.x+  
**推荐场景**: 海量数据 + 全文搜索

🔍 **ElasticSearch - 为搜索而生！**

