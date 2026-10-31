# 逆向工程文档 · Hoops 篮球数据平台

> 对象：`big-event-fullstack`（Spring Boot 4.0.7 + Vue 3 + MySQL + Redis）
> 生成：2026-10-31 · **静态只读分析**（未运行应用、未连接数据库/Redis）
> 方法：每条结论都带源码出处，证据见 [evidence-index.md](evidence-index.md)

本页把两张 Mermaid 图**直接渲染**出来；可编辑源仍保留在 `.mmd` 文件里。

- 架构图源文件：[01-architecture.mmd](01-architecture.mmd)
- 依赖图源文件：[02-module-dependencies.mmd](02-module-dependencies.mmd)
- 证据索引：[evidence-index.md](evidence-index.md)

> **修改约定**：先改 `.mmd`，再把同样内容同步到本页的 `mermaid` 代码块，保持两份一致。

---

## 1. 系统架构

```mermaid
%% ============================================================================
%% 01 · 系统架构图（证据驱动）
%% 项目：big-event-fullstack（Hoops 篮球数据平台）
%% 生成：2026-10-31
%% 依据：docs/reverse-engineering/evidence-index.md
%% 说明：只画有证据的组件；未验证项见 evidence-index.md 的 H 节
%% ============================================================================
flowchart LR
    subgraph CLIENT["客户端（Vue 3 SPA）"]
        UI["views/*.vue<br/>14 个视图文件"]
        API["api/*.js<br/>10 个模块 · axios"]
        REQ["utils/request.js<br/>baseURL='/api' · timeout 5s<br/>401 → 清 token + 跳 /login"]
    end

    subgraph EDGE["接入层（开发/生产二选一）"]
        VITE["Vite dev server<br/>/api → localhost:8080<br/>rewrite 剥掉 /api<br/>/uploads → 8080"]
        NGX["nginx 容器<br/>/api/ → backend:8080/（剥前缀）<br/>/uploads/ · /swagger-ui · /v3/api-docs<br/>client_max_body_size 10m"]
    end

    subgraph APP["Spring Boot 4.0.7 · Java 17 · 单 Maven 模块"]
        INT["LoginInterceptor<br/>WebConfig 注册<br/>排除 login/register/uploads/error/swagger"]
        CTL["Controller ×13<br/>user · article · category · team · player<br/>season · game · player-game-stat · rank<br/>shot · shot/chart · import · upload"]
        SVC["Service 接口 ×12<br/>ServiceImpl ×12"]
        MAP["Mapper ×11<br/>MyBatis 注解 SQL（无 XML）"]
        EXC["GlobalExceptionHandler<br/>@RestControllerAdvice"]
        DOC["springdoc OpenAPI 2.7.0"]
        UP["/uploads/** 静态资源映射<br/>WebConfig.addResourceHandlers"]
        UPC["FileUploadController<br/>不经 Service · 直接写 uploads 目录"]
    end

    subgraph BATCH["启动期任务（CommandLineRunner · 仅带参数运行）"]
        SEED["SeedDataRunner<br/>--import-seed"]
        GS["PlayerGameStatDataGeneratorRunner<br/>--generate-stats"]
        GSH["ShotDataGeneratorRunner<br/>--generate-shots"]
        PERF["RankPerfTestRunner<br/>--perf-test"]
    end

    subgraph DATA["数据层"]
        MYSQL[("MySQL · hoops<br/>9 张表")]
        REDIS[("Redis<br/>会话 token · TTL 12h<br/>rank:points:* · 10min / 空结果 1min")]
    end

    UI --> API --> REQ
    REQ --> VITE
    REQ -.->|"生产环境"| NGX
    VITE --> INT
    NGX --> INT
    INT -->|"preHandle 放行"| CTL
    INT -->|"get(token) 校验会话"| REDIS
    CTL -->|"12 个 Controller（upload 除外）"| SVC
    SVC --> MAP
    MAP --> MYSQL
    SVC -->|"仅 RankServiceImpl 读写"| REDIS
    CTL -.->|"抛出异常"| EXC
    CTL -.->|"注解扫描"| DOC
    UPC -.->|"写入同一目录"| UP
    NGX -->|"静态文件"| UP
    SEED --> SVC
    SEED --> MAP
    GS --> MAP
    GSH --> MAP
    PERF --> SVC
```

**一句话读法**：浏览器 → `request.js`（baseURL `/api`）→ Vite/nginx 剥掉 `/api` → `LoginInterceptor`（Redis 会话 + JWT 双校验）→ Controller → Service → Mapper → MySQL；排行缓存只在 `RankServiceImpl` 里读写 Redis。

---

## 2. 模块依赖

```mermaid
%% ============================================================================
%% 02 · 模块依赖图（只看有证据的边）
%% 生成：2026-10-31
%% 依据：docs/reverse-engineering/evidence-index.md
%% ============================================================================
flowchart TD
    subgraph CTRL["controller ×13"]
        UC["UserController"]
        RC["RankController"]
        IC["ImportController"]
        FC["FileUploadController<br/>（不经 Service）"]
        OC["其余 9 个 CRUD Controller"]
    end

    subgraph SVCG["service / service.impl ×12 + ×12"]
        US["UserService(Impl)"]
        RS["RankService(Impl)"]
        IS["ImportService(Impl)"]
        OS["其余 9 个 Service(Impl)"]
    end

    subgraph MAPG["mapper ×11（均为接口 + 注解 SQL）"]
        UM["UserMapper"]
        RM["RankMapper"]
        OM["其余 9 个 Mapper"]
    end

    subgraph CROSS["config / interceptors / utils / exception"]
        WC["WebConfig"]
        LI["LoginInterceptor"]
        JU["JwtUtil"]
        PE["PasswordEncoder"]
        TL["ThreadLocalUtil"]
        GEH["GlobalExceptionHandler"]
    end

    subgraph POJOG["pojo ×16"]
        RES["Result"]
        ENT["实体 / PageBean / RankItem 等"]
    end

    subgraph INFRA["外部依赖与基础设施"]
        MYB["mybatis-spring-boot-starter 4.0.1"]
        DB[("MySQL")]
        RD[("Redis")]
        PAGE["pagehelper 4.1.1"]
        BCRYPT["spring-security-crypto<br/>BCryptPasswordEncoder"]
    end

    WC -->|"addInterceptors"| LI
    LI --> JU
    LI --> TL
    LI -->|"opsForValue().get"| RD

    UC --> US
    UC --> JU
    UC --> PE
    UC -->|"set / delete token"| RD
    RC --> RS
    RS --> RM
    RS -->|"get / set / scan"| RD
    IC --> IS

    US --> UM
    US --> PE
    US --> TL
    IS --> OM
    OC --> OS

    UM --> MYB
    RM --> MYB
    OM --> MYB
    MYB --> DB

    OS -->|"7 个分页 Impl 调 startPage"| PAGE

    PE --> BCRYPT
    GEH --> RES
    UC --> RES
    RC --> RES
    IC --> RES
    RES -->|"data 字段"| ENT
```

**一句话读法**：`WebConfig` 注册 `LoginInterceptor`；`LoginInterceptor` 依赖 `JwtUtil` + `ThreadLocalUtil` + Redis；Controller 调 Service，Service 调 Mapper，Mapper 经 MyBatis 访问 MySQL；分页由 7 个 ServiceImpl 调 `PageHelper.startPage`。

---

## 3. 看图的几种方式

| 方式 | 适用 | 说明 |
|---|---|---|
| **Gitee / GitHub 仓库页** | 最省事 | 直接打开本文件，平台通常会自动渲染 `mermaid` 代码块 |
| **IDEA 插件** | 常驻 IDE | `Settings → Plugins` 搜 `Mermaid`，装完双击 `.mmd` 即预览 |
| **VS Code 插件** | 常驻编辑器 | 搜 `Mermaid Editor` 或 `Markdown Preview Mermaid Support` |
| **Mermaid Live** | 零安装、可导出图片 | 打开 <https://mermaid.live>，把 `.mmd` 内容粘进左侧 Code 面板 |

> 如果平台不渲染：说明该平台/版本不支持 Mermaid，改用本地插件或 Mermaid Live 即可，图本身没有损坏。

---

## 4. 已知限制

- 本次是**静态只读分析**：未启动应用、未连接数据库/Redis，因此运行时行为（如路由优先级、缓存实际命中）未验证。
- 未验证项与不确定项统一列在 [evidence-index.md](evidence-index.md) 的 **H 节**。
- 图中「其余 N 个」为概括节点，逐条对应关系未全部核实（见 H6）。
