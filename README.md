# 基于卷积神经网络的水果识别系统

本科毕业设计。用户上传水果图片，系统调用 CNN 模型返回 Top-5 识别结果，并保存历史记录与分类统计。

**技术栈**：Spring Boot 3 + MyBatis-Plus + MySQL 8 ｜ Vue 3 + Vite + Element Plus ｜ PyTorch + Fruits-360

## 系统架构

```
浏览器 (Vue3 :5173)
      │  /api 代理
Spring Boot 后端 (:8081) ── MyBatis-Plus ── MySQL 8
      │
      ├── ProcessBuilder 同步调用
      └── Python 推理进程 (predict_json.py + 模型权重) ── stdout JSON
```

后端不常驻 Python 服务：上传时用 `ProcessBuilder` 拉起推理脚本、读取单行 JSON。零额外端口，代价是每次冷启动约 5–8 秒（异步化列入后续演进）。

## 功能

- 登录 / 注册（JWT，BCrypt 存密码）
- 上传图片识别，返回 Top-5 候选与置信度
- 识别历史分页列表（普通用户看自己，管理员看全部）
- 识别详情：模型版本、耗时、全部候选
- 分类识别数量统计
- 统一响应、全局异常、上传格式白名单、识别失败回滚与文件清理

## 目录结构

```
├── fruit-server/     Spring Boot 后端（业务、鉴权、持久化）
│   └── src/test/     单元测试（JUnit 5 + Mockito）
├── fruit-web/        Vue 3 前端
├── demo/             Python 推理脚本与示例图
├── uploads/          运行时上传目录（不入库）
├── 建表.sql          MySQL 建表脚本
├── 数据库设计-v0.md
├── 系统设计-v0.md    架构 / 类图 / 时序图 / 接口 / 决策记录
└── 启动说明.md
```

## 快速启动

配置与启动步骤见 [启动说明.md](启动说明.md)。数据库密码可用环境变量 `DB_PASSWORD` 覆盖，默认本机 `123456`。

## 测试

```bash
cd fruit-server
mvn test
```

当前 11 个用例：`UserServiceTest`（6）、`ImageServiceTest`（3）、`JwtUtilTest`（2）。

## 设计文档

- [系统设计文档](系统设计-v0.md)：架构、模块划分、类图、关键时序、接口定义、设计决策
- [数据库设计](数据库设计-v0.md)：四张核心表及字段说明
