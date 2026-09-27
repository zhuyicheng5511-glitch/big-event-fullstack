\# 🚀 大事件（Big-Event）全栈后台管理系统



本项目是一套基于 \*\*Spring Boot 3\*\* 和 \*\*Vue 3\*\* 构建的企业级前后端分离后台管理系统。系统涵盖了用户认证、文章分类管理、文章发布与分页检索、本地静态文件存储以及基于 Redis 的双重令牌会话控制等核心功能。



\---



\## 🛠️ 技术栈蓝图



\### 1. 后端技术（Backend）

\*   \*\*核心框架\*\*：Spring Boot 3.x

\*   \*\*持久层框架\*\*：MyBatis

\*   \*\*安全与会话\*\*：JWT (JSON Web Token) + ThreadLocal 线程安全上下文

\*   \*\*高速缓存\*\*：Redis (用于 Token 黑名单与主动下线机制)

\*   \*\*分页插件\*\*：PageHelper (实现自动化物理分页)

\*   \*\*参数校验\*\*：Jakarta Validation (JSR-383 规范校验)

\*   \*\*持久化数据库\*\*：MySQL 8.x



\### 2. 前端技术（Frontend）

\*   \*\*核心框架\*\*：Vue 3 (Composition API / <script setup>)

\*   \*\*构建工具\*\*：Vite

\*   \*\*UI 组件库\*\*：Element Plus (响应式卡片与抽屉式组件)

\*   \*\*状态管理\*\*：Pinia (存储全局 Token 与用户信息)

\*   \*\*网络请求\*\*：Axios (封装全局请求/响应拦截器，实现 Token 自动携带与 401 拦截)

\*   \*\*路由管理\*\*：Vue Router (支持嵌套子路由与导航守卫)



\---



\## ✨ 核心亮点功能



1\.  \*\*全链路安全体系\*\*：

&#x20;   \*   \*\*后端\*\*：密码采用 \*\*MD5 动态加密\*\* 存库；基于 `LoginInterceptor` 拦截器校验 `Authorization` 请求头。

&#x20;   \*   \*\*前端\*\*：部署全局 \*\*前置导航守卫（Route Guard）\*\*，拦截非法免登录跳转，防止“通过敲击 URL 绕过门禁”的系统漏洞。

2\.  \*\*Redis 联动“强制下线”\*\*：

&#x20;   \*   传统的 JWT 令牌无法主动失效。本系统结合 Redis 缓存，每次校验均对比 Redis 状态。在用户\*\*修改密码\*\*或\*\*安全登出\*\*时，直接删除 Redis 对应缓存，使旧令牌即时作废。

3\.  \*\*多用户数据安全隔离\*\*：

&#x20;   \*   文章分类模块在数据库交互层面，强制通过 `ThreadLocal` 绑定当前登录用户的 ID 进行数据过滤，确保不同用户之间的数据绝对隔离。

4\.  \*\*本地文件系统虚拟化映射\*\*：

&#x20;   \*   开发了 `/upload` 文件上传接口，并利用 `WebMvcConfigurer` 配置了本地物理磁盘目录的虚拟静态映射，实现零成本的高清封面图片上传与回显。

5\.  \*\*全局异常大统一拦截\*\*：

&#x20;   \*   利用 `@RestControllerAdvice` 编写了全局异常处理器，统一捕获参数校验、数据库外键等异常，将其格式化为标准的 JSON 数据，拒绝系统暴露红色 500 报错。



\---



\## 💻 快速运行与环境搭建



\### 1. 准备工作

确保您的本地电脑上已安装好：

\*   \*\*JDK 17\*\* 或更高版本

\*   \*\*Node.js\*\* (v18.x 或 v20.x 推荐)

\*   \*\*MySQL 8.0+\*\*

\*   \*\*Redis\*\* (默认 6379 端口运行)



\### 2. 后端启动 (`big-event`)

1\.  执行项目内置的建表脚本（会自动创建 `big\_event` 数据库及 `user`、`category`、`article` 三张表）：

&#x20;   ```bash

&#x20;   mysql -uroot -p < big-event/src/main/resources/db/schema.sql

&#x20;   ```

&#x20;   也可以在 Navicat / IDEA 的 Database 工具中打开该文件直接执行。

&#x20;   脚本位置：`big-event/src/main/resources/db/schema.sql`

&#x20;   ⚠️ 注意：脚本开头包含 `DROP TABLE` 语句，重复执行会**清空这三张表的全部数据**，请仅在全新的空库上执行；若库中已有数据，请先备份（`mysqldump -uroot -p --databases big\_event > backup.sql`）。

2\.  打开 `big-event/src/main/resources/application.yml`，修改您的 MySQL 密码。

3\.  启动本地 Redis 服务。

4\.  在 IDEA 中运行启动类 `BigEventApplication`。



\### 3. 前端启动 (`big-event-web`)

1\.  打开命令行，进入前端目录：

&#x20;   ```bash

&#x20;   cd big-event-web

&#x20;   ```

2\.  安装所需的第三方依赖包：

&#x20;   ```bash

&#x20;   npm install

&#x20;   ```

3\.  以开发模式运行项目：

&#x20;   ```bash

&#x20;   npm run dev

&#x20;   ```

4\.  用浏览器打开控制台输出的地址即可进行访问：`http://localhost:5173/login`



\---



\## 🧑‍💻 开发者信息



\*   \*\*作者\*\*：朱毅城

\*   \*\*单位\*\*：厦门理工学院

\*   \*\*毕业/实训大作\*\*：大事件后台管理系统 (Big-Event Fullstack)

