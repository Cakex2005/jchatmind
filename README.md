# JChatMind

JChatMind 是一个面向复杂任务的 AI Agent 应用。后端基于 Spring Boot 3 和 Spring AI，前端基于 React 19、TypeScript 与 Ant Design，支持多轮对话、工具调用、知识库检索、模型路由以及执行状态实时推送。

## 核心能力

- **Agent 工作流**：围绕 Think-Execute 循环完成任务规划、工具执行和结果汇总，支持多轮工具调用与最大步数控制。
- **工具系统**：统一管理固定工具和可选工具，当前包含数据库、邮件、文件系统、知识库等能力。
- **RAG 知识库**：支持 Markdown 文档解析、文本分块、向量生成与 PostgreSQL + pgvector 相似度检索。
- **多模型路由**：通过 `ChatClientRegistry` 注册和管理不同模型客户端，业务代码无需绑定具体模型。
- **实时状态推送**：使用 SSE 将 Agent 的思考、执行和完成状态实时发送到前端。
- **会话管理**：持久化会话、消息、Agent、知识库和文档数据，支持完整的历史记录与上下文管理。

## 技术栈

| 模块 | 技术 |
| --- | --- |
| 后端 | Java 17、Spring Boot 3.5、Spring AI、MyBatis |
| 数据库 | PostgreSQL、pgvector |
| 前端 | React 19、TypeScript、Vite、Ant Design X、Tailwind CSS |
| 通信 | REST API、SSE |

## 项目结构

```text
.
├── jchatmind
│   ├── src/main/java       # 后端业务代码
│   ├── src/main/resources  # 配置、MyBatis Mapper
│   └── src/test            # 后端测试
├── ui
│   ├── src                 # 前端业务代码
│   └── package.json
└── README.md
```

## 本地运行

### 环境要求

- JDK 17
- Maven 3.9+，或直接使用项目自带的 Maven Wrapper
- Node.js 20+
- PostgreSQL 15+，并安装 `pgvector` 扩展

### 1. 配置后端

```powershell
cd jchatmind
Copy-Item src/main/resources/application.example.yaml src/main/resources/application.yaml
```

按实际环境设置以下环境变量：

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/jchatmind"
$env:DB_USERNAME="postgres"
$env:DB_PASSWORD="your-password"
$env:MAIL_HOST="smtp.example.com"
$env:MAIL_PORT="587"
$env:MAIL_USERNAME="your-email"
$env:MAIL_PASSWORD="your-mail-password"
$env:OPENAI_API_KEY="your-api-key"
$env:OPENAI_BASE_URL="https://your-model-endpoint/v1"
$env:OPENAI_CHAT_MODEL="your-chat-model"
$env:OPENAI_EMBEDDING_MODEL="your-embedding-model"
$env:DOCUMENT_STORAGE_PATH="./data/documents"
```

启动后端：

```powershell
.\mvnw.cmd spring-boot:run
```

### 2. 启动前端

```powershell
cd ui
npm install
npm run dev
```

Vite 默认会在 `http://localhost:5173` 启动开发服务器。

## 构建

后端：

```powershell
cd jchatmind
.\mvnw.cmd clean package
```

前端：

```powershell
cd ui
npm run build
```

## 配置与安全

- `application.example.yaml` 只包含环境变量占位符，可以作为公开配置模板。
- `application.yaml`、`application-local.yaml`、`application-dev.yaml` 和 `ui/.env` 已加入 `.gitignore`。
- API Key、数据库密码、邮箱密码等敏感信息应通过环境变量注入，不应提交到仓库。
- 如果需要新增配置项，请同步更新示例文件，不要在源码中写入真实凭据。

## License

本项目采用 MIT License，详见 [LICENSE](LICENSE)。