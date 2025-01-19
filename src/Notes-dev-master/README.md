# Mi Notes - Android笔记应用

Mi Notes是一个功能完整的Android笔记应用，支持本地笔记管理和Google Tasks同步功能。

## 功能特性

- 笔记管理
  - 创建、编辑、删除笔记
  - 支持文件夹组织笔记
  - 支持笔记回收站
  - 支持通话记录关联
  - 支持笔记提醒功能

- 数据同步
  - 与Google Tasks同步
  - 支持离线操作
  - 自动冲突解决

- UI组件
  - 支持桌面小部件(2x和4x尺寸)
  - 支持笔记搜索
  - 支持系统偏好设置

## 系统架构

### 数据层
- `Notes`: 定义数据结构和内容提供者URI
- `NotesDatabaseHelper`: SQLite数据库管理
- `NotesProvider`: 内容提供者实现
- `Contact`: 通讯录集成

### 同步层
- `GTaskManager`: 同步管理器
- `GTaskClient`: Google Tasks API客户端
- `GTaskSyncService`: 同步服务
- `GTaskASyncTask`: 异步同步任务

### 数据模型
- `Node`: 基础节点类
- `Task`: 任务数据模型
- `TaskList`: 任务列表模型
- `MetaData`: 元数据管理
- `SqlNote`/`SqlData`: 本地数据模型

### 异常处理
- `ActionFailureException`: 操作失败异常
- `NetworkFailureException`: 网络异常

## 技术特点

1. 数据存储
   - 使用SQLite数据库
   - 实现ContentProvider接口
   - 支持触发器和事务

2. 同步机制
   - 增量同步
   - 双向同步
   - 断点续传
   - 冲突处理

3. 性能优化
   - 异步数据加载
   - 后台同步服务
   - 缓存机制

## 系统要求

- 最低SDK版本: 21
- 目标SDK版本: 35
- 编译SDK版本: 35
- 构建工具版本: 34.0.0

## 使用方法
直接解压后在Android Studio中打开即可(注意打开的是Notes-dev-master包)