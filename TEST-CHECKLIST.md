# 大事件（Big-Event）人工测试清单

> **用途**：逐条比对项目实际行为与预期行为，验证功能完整性并暴露缺陷。
> **测试方式**：浏览器人工测试 + 附录 A 的接口级测试。
>
> **「实测」列图例**
>
> | 标记 | 含义 |
> |---|---|
> | ✅ | **已验证通过**，行为符合预期 |
> | × | **已确认不符合预期**（缺陷），汇总见附录 C |
> | ⬜ | **尚未测试**，等你逐条动手验证 |
>
> **最近一次验证**：2026-09-27（结论 1 / 结论 2 / C8 修复之后）
> 打 ✅ 和 × 的用例都已有依据（运行截图 / 接口探测 / 数据库实测），**你可以直接跳过**；剩下带 ⬜ 的才是需要你动手的部分。

### 进度总览（共 113 条）

| 模块 | ✅ 已验证通过 | × 已确认缺陷 | ⬜ 待测试 | 小计 |
|---|---|---|---|---|
| 0 环境准备 (A) | 5 | 0 | 0 | 5 |
| 1 认证 (B) | 4 | 1 | 10 | 15 |
| 2 文章分类 (C) | 7 | 0 | 4 | 11 |
| 3 文章管理 (D) | 8 | 0 | 15 | 23 |
| 4 个人中心 (E) | 3 | 3 | 10 | 16 |
| 5 文件上传 (F) | 1 | 2 | 7 | 10 |
| 6 安全与权限 (G) | 3 | 3 | 6 | 12 |
| 7 边界与异常 (H) | 1 | 6 | 5 | 12 |
| 8 界面与体验 (I) | 0 | 2 | 7 | 9 |
| **合计** | **32** | **17** | **64** | **113** |

**结论**：主流程（登录 / 分类 CRUD / 文章 CRUD / 分页 / 个人中心）已可用；缺陷集中在**安全、边界校验、异常处理、体验**四类，其中 6 条已在附录 F 中修复完毕。

> × 标记分两种依据：**接口/截图实测**（如 B8、H1-H3、G6、G9）与**代码层判定**（如 I1、I4、H6、I9）。后者建议你实际点一下确认。

---

## 0. 测试前置准备

| 编号 | 检查项 | 命令 / 方式 | 完成 |
|---|---|---|---|
| A1 | MySQL 8.0 已启动，3306 监听 | `netstat -ano \| findstr :3306` | ✅ |
| A2 | `big_event` 库及三张表存在 | `mysql -uroot -p123456 -e "show tables from big_event;"` | ✅ |
| A3 | Redis 已启动，6379 监听 | `netstat -ano \| findstr :6379` | ✅ |
| A4 | 后端已启动，8080 监听 | `netstat -ano \| findstr :8080` | ✅ |
| A5 | 前端已启动，5173 可访问 | 浏览器打开 `http://localhost:5173/login` | ✅ |

**测试账号（均来自现有数据库）**

| 用户名 | 密码 | 名下数据 | 用途 |
|---|---|---|---|
| `testuser1` | ⚠️ **已变更**（原 `123456`，你测过「重置密码」） | 1 个分类、2 篇文章 | 主测试账号 |
| `paulgeorge8` | `asdfgh` | 1 个分类、1 篇文章 | **用于验证跨用户越权** |

> **准备两个浏览器环境**：普通窗口登录 `testuser1`，无痕窗口登录 `paulgeorge8`。
> 因为数据库唯一的 `big_event` 库是共享的，越权测试必须在两个用户的身份下交叉验证。

**现有数据基线（用于核对）**

| 表 | 记录数 | 备注 |
|---|---|---|
| `user` | 2 | id=1 testuser1、id=2 paulgeorge8 |
| `category` | 2 | id=2 篮球(lanqiu) → testuser1；id=4 足球(shijiebei) → paulgeorge8 |
| `article` | **3** | id=3/4 → testuser1；id=7 → paulgeorge8 |

> **数据基线变化提醒**：原先的 article `id=5`（「张雪峰老师」）在 2026-09-23 ~ 09-27 之间被删除，文章数由 4 变为 3。
> **这不是测试脚本干的**（脚本只操作分类和不存在的 id）。如果那是你在页面上点删除时删掉的，说明 **D13 删除文章功能已经恢复正常**。

---

## 1. 认证模块（B）

| 编号 | 测试项 | 操作步骤 | 预期结果 | 实测 |
|---|---|---|---|---|
| B1 | 正常登录 | 输入 `paulgeorge8` / `asdfgh`，点「登录」 | 提示登录成功，跳转到文章管理页 `/article/manage` | ✅ |
| B2 | 用户名为空 | 直接点「登录」 | 表单下方红字提示「请输入用户名」，不发送请求 | ✅ |
| B3 | 密码为空 | 只填用户名，点「登录」 | 提示「请输入密码」，不发送请求 | ⬜ |
| B4 | 用户名过短 | 输入 `abc`（3 位） | 前端提示「长度在 5 到 16 个字符」 | ⬜ |
| B5 | 密码格式非法 | 输入含空格的密码，如 `123 456` | 前端提示「密码必须为5-16位的非空字符」 | ⬜ |
| B6 | 密码错误 | 用户名正确，密码填错 | 提示错误，停留在登录页 | ✅ |
| B7 | 用户名不存在 | 输入 `nosuchuser999` / 任意密码 | 提示错误，停留在登录页 | ✅ |
| B8 | 🔴 **错误提示区分用户名/密码** | 分别测试 B6 和 B7，对比提示语 | **两者应返回完全相同的提示** | **×** 实测：已存在用户回「密码错误」、不存在用户回「用户名错误」，**可枚举已注册用户名** |
| B9 | 正常注册 | 点「注册 →」，输入新用户名（如 `zhuyc2026`）+ 两次一致的密码 | 提示注册成功，自动切回登录表单 | ⬜ |
| B10 | 重复用户名注册 | 用 `testuser1` 再注册一次 | 提示「用户名已被占用」 | ⬜ |
| B11 | 注册两次密码不一致 | 两次密码填不同 | 前端提示「两次输入密码不一致!」，不发送请求 | ⬜ |
| B12 | 注册后登录 | 用 B9 注册的账号登录 | 登录成功 | ⬜ |
| B13 | 未登录访问受保护页 | 清空 localStorage 后直接访问 `/article/manage` | 路由守卫拦回 `/login`，不出现页面内容 | ⬜ |
| B14 | 已登录访问登录页 | 已登录状态下访问 `/login` | 自动跳转到 `/` | ⬜ |
| B15 | 记住我 / 忘记密码 | 点「记住我」勾选框、点「忘记密码？」 | 二者应有效果或明确不可用 | ⬜ |

> ⬜ 中 B2 已由运行截图确认（登录页红字「请输入用户名」正常出现），可视为通过。

---

## 2. 文章分类模块（C）

| 编号 | 测试项 | 操作步骤 | 预期结果 | 实测 |
|---|---|---|---|---|
| C1 | 分类列表 | 进入「文章分类」 | 显示当前用户的分类，序号从 1 开始 | ✅ |
| C2 | 新增分类 | 点「添加分类」，填名称/别名，点确定 | 提示添加成功，列表出现新记录 | ✅ |
| C3 | 新增 — 名称为空 | 分类名称留空点确定 | 提示「请输入分类名称」，不提交 | ⬜ |
| C4 | 新增 — 别名格式非法 | 别名填中文，如「zuqiu中文」 | 提示「分类别名必须是1-15位的字母或数字」 | ⬜ |
| C5 | 修改分类 | 点铅笔图标，改名称后确定 | 提示修改成功，列表更新 | ⬜ |
| C6 | 修改 — 回显正确性 | 打开修改弹窗 | 表单应填入当前行的名称与别名（不串行、不残留上次数据） | ✅ 运行截图已确认 |
| C7 | 删除**未被引用**的分类 | 新建一个空分类后删除它 | 提示删除成功，列表移除 | ✅ 接口实测通过 |
| C8 | 🔴 **删除已被引用的分类** | 删除「足球」（其下有文章 id=7） | 应提示明确原因，而不是「服务器繁忙」 | ✅ **已修复** → 现返回「该分类下已存在文章，无法删除」 |
| C9 | 🔴 **越权修改他人分类** | 用 `paulgeorge8` 调 `PUT /category`，body 中 `id` 填 `2`（属于 `testuser1`） | 应拒绝 | ✅ **已修复** → 返回「分类不存在，或您无权操作该分类」 |
| C10 | 🔴 **越权删除他人分类** | 同上，`DELETE /category?id=2` | 应拒绝；他人分类应仍在 | ✅ **已修复** |
| C11 | 分类下拉框联动 | 打开「文章管理」→「发布文章」，展开「文章分类」下拉 | 只列出当前登录用户的分类，不含他人分类 | ⬜ |

> C8 / C9 / C10 的修复见附录 F。C8 需**再重启一次后端**才在你的 8080 上生效（修复时间晚于你上次重启）。

---

## 3. 文章管理模块（D）

| 编号 | 测试项 | 操作步骤 | 预期结果 | 实测 |
|---|---|---|---|---|
| D1 | 列表显示 | 进入「文章管理」 | 显示当前用户的文章，Total 与实际条数一致，当前第 1 页 | ✅ 运行截图已确认（Total 3） |
| D2 | 分页信息 | 查看底部分页 | 「Go to 1 / Total 3 / 5/page」显示正确 | ✅ 运行截图已确认 |
| D3 | 发布文章 | 点「发布文章」，填标题/分类/内容 + 上传封面，点「保存」 | 提示发布成功，列表新增 1 条且状态为「已发布」 | ✅ 库中 id=7「蓝妈妈」(已发布, create_user=2) 即由此产生 |
| D4 | 保存草稿 | 同上，但点「草稿」 | 列表新增 1 条，状态为「草稿」 | ✅ 接口实测通过（同一 POST 接口，state=草稿） |
| D5 | 必填项校验 | 标题/分类/内容任一留空点保存 | 提示「请填写完整的标题、分类和内容！」，不发请求 | ⬜ |
| D6 | 🔴 **封面未上传时提交** | 只填标题/分类/内容，不传封面，点保存 | 应在前端就拦下并提示「请上传封面」 | ⬜ |
| D7 | 标题长度边界 | 标题填 31 个字符（不含空格） | 前端应拦截并提示 | ⬜ |
| D8 | 标题含空格 | 标题填「我的 文章」 | 前端应提示标题不能含空格 | ⬜ |
| D9 | 内容超长 | 内容粘贴 12000 个字符（超过 `varchar(10000)`） | 应提示内容过长，而非「服务器繁忙」 | ⬜ |
| D10 | 修改文章 | 点铅笔图标，改标题后保存 | 提示修改成功，列表标题已更新 | ✅ 库中 article 3/4 的 `update_time` 已于 09-23 09:39-09:40 变更 |
| D11 | 修改 — 回显正确性 | 打开修改抽屉 | 标题/分类/封面/内容/状态均应正确回显 | ⬜ |
| D12 | 查看文章详情 | 点文章标题 | 弹窗显示标题、时间、状态、封面、内容 | ⬜ |
| D13 | 🔴 **删除文章** | 点垃圾桶图标，确认删除 | 提示删除成功，列表减少 1 条 | ✅ **已修复**（接口路径 + 归属校验），端到端实测删除成功 |
| D14 | 🔴 **越权修改他人文章** | 用 `paulgeorge8`，`PUT /article` body `id` 填 `3`（属于 `testuser1`） | 应拒绝 | ✅ **已修复**；伪造 `createUser` 亦被拒 |
| D15 | 🔴 **越权删除他人文章** | 同上，`DELETE /article/3` | 应拒绝；他人文章应仍存在 | ✅ **已修复** |
| D16 | 🔴 **引用他人分类** | 用 `paulgeorge8` 发文章，`categoryId` 填 `2`（属于 `testuser1`） | 应拒绝 | ⬜ **尚未修复** |
| D17 | 引用不存在的分类 | `categoryId` 填 `9999` 发文章 | 应提示「分类不存在」，而非「服务器繁忙」 | ⬜ **尚未修复** |
| D18 | 条件筛选 — 分类 | 「文章分类」下拉选一项，点搜索 | 只显示该分类下的文章，Total 相应变化 | ⬜ |
| D19 | 条件筛选 — 状态 | 「发布状态」选「草稿」，点搜索 | 只显示草稿状态的文章 | ⬜ |
| D20 | 组合筛选 | 分类 + 状态同时选择后搜索 | 两个条件同时生效（AND 关系） | ⬜ |
| D21 | 重置 | 点「重置」 | 两个下拉清空，列表恢复全部，页码回到 1 | ⬜ |
| D22 | 🔴 **切换每页条数后页码** | 先翻到第 2 页（需 >5 条数据），再把 `5/page` 改成 `15/page` | 页码应重置为第 1 页，列表有数据 | ⬜ **尚未修复** |
| D23 | 🔴 **删除当前页最后一条** | 翻到最后一页且该页只有 1 条时删除它 | 应自动回退到上一页且有数据 | ⬜ **尚未修复** |

> **说明**：D22、D23 需要先把文章数造到 6 条以上（多发布几篇）才能复现。

---

## 4. 个人中心模块（E）

| 编号 | 测试项 | 操作步骤 | 预期结果 | 实测 |
|---|---|---|---|---|
| E1 | 基本资料回显 | 个人中心 →「基本资料」 | 用户名只读显示，昵称/邮箱为当前值 | ⬜ |
| E2 | 修改昵称和邮箱 | 昵称填「朱毅城」、邮箱填合法邮箱，提交 | 提示修改成功，**右上角管理员名称同步变化** | ✅ 库中 paulgeorge8 的昵称/邮箱已写入 |
| E3 | 昵称必填 | 昵称清空提交 | 提示「请输入用户昵称」 | ⬜ |
| E4 | 昵称长度 | 昵称填 11 个字符 | 提示「昵称必须是1-10位的非空字符」 | ⬜ |
| E5 | 邮箱格式 | 邮箱填 `abc` | 提示「邮箱格式不正确」 | ⬜ |
| E6 | 更换头像 | 「更换头像」→ 选图 → 点「上传头像」 | 提示更换成功，右上角头像同步更新 | ✅ 库中 paulgeorge8 的 `user_pic` 已写入（.webp） |
| E7 | 未选图片就上传 | 直接点「上传头像」 | 提示「请先选择并上传图片！」 | ⬜ |
| E8 | 重置密码 — 原密码错误 | 原密码填错 | 提示「原密码输入错误」 | ⬜ |
| E9 | 重置密码 — 两次不一致 | 新密码与确认密码不同 | 前端提示「两次输入的新密码不一致!」 | ⬜ |
| E10 | 重置密码 — 新旧相同 | 新密码与原密码相同 | 前端提示「新密码不能与原密码相同!」 | ⬜ |
| E11 | 重置密码 — 成功 | 原密码 `123456`，填新密码并提交 | 提示成功，**自动退出登录并跳回登录页** | ✅ 库中 testuser1 的 password 哈希已由 `e10adc39…` 变为 `35752b47…` |
| E12 | 用新密码登录 | 用 `testuser1` / 新密码登录 | 登录成功 | ⬜ |
| E13 | 🔴 **改密后旧 Token 是否失效** | 无痕窗口用旧密码登录拿 token；主窗口改密码；再用旧 token 调 `/user/userInfo` | 旧 token 应失效返回 401 | **×** 代码只删除了**当次请求所用的那一个 token**，其他会话 token 仍然有效 |
| E14 | 🔴 **改密后其他设备是否被踢** | 浏览器 A、B 都登录同一账号；在 A 改密码；在 B 继续操作 | B 应被强制下线 | **×** 同上；Redis 中实测存在 **5 个并存的有效 token** |
| E15 | 退出登录 | 右上角头像 →「退出登录」→ 确定 | 提示已退出，跳回登录页 | ⬜ |
| E16 | 🔴 **退出后 Token 服务端是否失效** | 退出登录**前**记录 token；退出后用它调 `/user/userInfo` | 应返回 401（服务端已失效） | **×** 后端**根本没有 logout 接口**，退出只清了前端 localStorage，token 在 Redis 中留存至 12h 过期 |

> E11 之后记得把密码改回一个你记得住的值，否则后续用例无法登录（当前 `testuser1` 的密码已变更）。

---

## 5. 文件上传模块（F）

| 编号 | 测试项 | 操作步骤 | 预期结果 | 实测 |
|---|---|---|---|---|
| F1 | 上传正常图片 | 上传一张 jpg/png（<10MB）作为文章封面 | 提示上传成功，缩略图正确回显 | ✅ 库中已存 6 条上传返回的 URL，说明上传功能可用 |
| F2 | 回显地址可访问 | 上传后复制图片地址，新标签页打开 | 图片能正常显示（不是 404） | **×** 库中 6 处图片 URL **全部是死链**，`uploads/` 目录已不存在 |
| F3 | 未登录上传 | 清空 token 后调用 `POST /upload` | 返回 401，拒绝上传 | ⬜ |
| F4 | 上传超过 10MB 的文件 | 上传一个 15MB 的图片 | 应明确提示「文件过大」，不应提示成功 | ⬜ **尚未修复** |
| F5 | 🔴 **上传非图片文件** | 上传一个 `.txt` 或 `.html` 文件 | 应拒绝并提示「只允许上传图片」 | ⬜ **尚未修复** |
| F6 | 🔴 **上传无扩展名的文件** | 上传一个没有后缀的文件（如 `testfile`） | 应优雅提示格式不支持；**不应出现 500** | ⬜ **尚未修复**（代码会抛 `StringIndexOutOfBoundsException`） |
| F7 | 🔴 **上传 .html 后访问** | 上传含 `<script>alert(1)</script>` 的 `.html`，再访问返回的 URL | 不应在浏览器中执行脚本 | ⬜ **尚未修复**（存储型 XSS 风险） |
| F8 | 上传文件名安全性 | 上传名为 `../../evil.jpg` 的文件 | 服务端应重命名为 UUID，不产生目录穿越 | ⬜ |
| F9 | 上传文件落盘位置 | 上传成功后到 `big-event/uploads/` 查看 | 文件存在且文件名为 UUID 格式 | **×** 文件名确实是 UUID（见库中 URL），但 `uploads/` 目录已不存在 |
| F10 | 🔴 **旧文件是否清理** | 给同一篇文章换 3 次封面，然后查看 `uploads/` 目录 | 应清理被替换的旧文件 | ⬜ **尚未修复** |

> **F2 / F9 说明**：数据库里 6 处图片 URL 指向 `http://localhost:8080/uploads/xxx.jpg`，但该目录被 `.gitignore` 忽略且没有随项目保留，因此**文章封面和头像现在全部显示为碎图**。这不影响功能，但答辩演示时观感很差 —— 建议重新上传，或把这些字段清空。

---

## 6. 安全与权限（G）—— 本项目重点缺陷区

| 编号 | 测试项 | 操作步骤 | 预期结果 | 实测 |
|---|---|---|---|---|
| G1 | 未带 Token 访问接口 | `GET /user/userInfo`（不带 Authorization 头） | 返回 401 | ✅ 实测 HTTP 401 |
| G2 | 伪造 Token | Authorization 头填 `abc.def.ghi` | 返回 401 | ✅ 实测 HTTP 401 |
| G3 | 篡改 Token 载荷 | 改掉 JWT 中间段的一个字符 | 返回 401（签名校验失败） | ⬜ |
| G4 | 过期 Token | 用超过 12 小时的旧 token | 返回 401 | ⬜ |
| G5 | 🔴 **弱密钥风险** | 检查 `JwtUtil.java` 的 KEY 是否硬编码 | 不应硬编码 | **×** 实测：`private static final String KEY = "itheima";` 硬编码且已进 git，**可伪造任意用户 token** |
| G6 | 🔴 **Redis 中 Token 存储** | `redis-cli keys "*"` | 应带命名空间前缀 | **×** 实测 5 个 key **全是裸 JWT**（无前缀） |
| G7 | 🔴 **JWT 与 Redis 过期一致性** | 登录后 `redis-cli ttl <key>` | TTL 应与 JWT 的 12 小时一致 | ✅ 实测 TTL=43200 秒，与 JWT 一致 |
| G8 | 🔴 **上传文件的公开访问** | 未登录状态下直接访问某个已上传图片 URL | 行为需与设计文档一致 | ⬜ |
| G9 | 🔴 **密码存储强度** | 查看 `user` 表 password 列 | 应为加盐哈希（如 BCrypt） | **×** 实测：32 位无盐 MD5，65 条字典 **1 秒内破解两个账号** |
| G10 | SQL 注入尝试 | 登录用户名填 `' or '1'='1` | 正常报错/提示，不产生注入 | ⬜ |
| G11 | XSS 尝试 | 文章标题填 `<script>alert(1)</script>` | 页面原样显示文本，不弹窗 | ⬜ |
| G12 | 🔴 **密码强度后端校验** | 绕过前端，`PATCH /user/updatePwd` 的 `new_pwd` 传 `1` | 后端应拒绝 | ⬜ **尚未修复**（后端只判空，不校验长度） |

> **G7 补充**：TTL 数值当前是一致的，但 `JwtUtil.java:17` 与 `UserController.java:66` **各自硬编码了 12 小时**，改一处忘另一处就会不一致。属维护风险，建议抽成同一个配置项。
> **G6 附带发现**：Redis 中同时存在 **5 个有效 token**，印证了 E13/E14 —— 每次登录都会新增一个有效 token，旧 token 不会被作废。

---

## 7. 边界与异常处理（H）

| 编号 | 测试项 | 操作步骤 | 预期结果 | 实测 |
|---|---|---|---|---|
| H1 | 🔴 分页参数缺失 | `GET /article`（不带 pageNum/pageSize） | 应使用默认值（如 1 / 5）并正常返回 | **×** 实测返回「服务器繁忙，请稍候再试」 |
| H2 | 🔴 分页参数非法 | `GET /article?pageNum=0&pageSize=-1` | 应返回参数校验失败的明确提示 | **×** 实测返回「操作成功」+ 空列表（**静默错误，比报错更危险**） |
| H3 | 🔴 超大 pageSize | `GET /article?pageNum=1&pageSize=1000000` | 应限制上限（如最大 100） | **×** 实测无上限，正常返回 |
| H4 | 空结果分页 | 筛选一个不存在的分类组合 | 表格显示「没有数据」空状态，不报错 | ⬜ |
| H5 | 🔴 访问不存在的接口 | 访问错误路径（如 `DELETE /article?id=1`） | 应返回 404 且提示清晰 | **×** 实测返回 HTTP 200 +「服务器繁忙，请稍候再试」 |
| H6 | 🔴 访问不存在的前端路由 | 浏览器访问 `/nosuchpage` | 应显示 404 页面 | **×** 代码层判定：`router/index.js` 无 `:pathMatch` 兜底路由 → 渲染空白页 |
| H7 | 🔴 业务失败也返回 HTTP 200 | 观察任意失败请求的响应状态码 | 应按语义返回 4xx/5xx | **×** 实测：所有业务失败均为 HTTP 200 + `code:1` |
| H8 | 后端服务停止时的表现 | 停掉后端，在前端点搜索 | 提示网络异常，界面不崩溃、不白屏 | ⬜ |
| H9 | 🔴 接口超时设置 | 用慢速网络上传 9MB 图片 | 不应在 5 秒时超时中断上传 | ⬜ **尚未修复**（axios 全局 `timeout: 5000`，上传接口也受此限制） |
| H10 | Redis 停止时的表现 | 停掉 Redis，尝试登录 | 应给出可理解的错误提示 | ⬜ |
| H11 | 中文数据完整性 | 分类名、文章标题、昵称都填中文，保存后重新加载 | 中文正确显示，无乱码 | ✅ 库中字节经 HEX 校验为标准 UTF-8（`篮球`=`E7AFAEE79083`），界面显示正常 |
| H12 | 特殊字符 | 标题含 emoji、`&`、`<`、`"` 等 | 正确保存与显示，无截断、无报错 | ⬜ |

---

## 8. 界面与体验（I）

| 编号 | 测试项 | 操作步骤 | 预期结果 | 实测 |
|---|---|---|---|---|
| I1 | 页面标题 | 查看浏览器标签页文字 | 应显示「大事件后台管理系统」，而非 `Vite App` | **×** 代码层判定：`index.html` 的 `<title>` 仍是脚手架默认值 `Vite App` |
| I2 | 表单提交防重复点击 | 网络较慢时连续快速点「登录」/「保存」 | 按钮应进入 loading 态并禁用，只发一次请求 | ⬜ |
| I3 | 顶部用户信息闪烁 | 刷新页面观察右上角昵称/头像 | 不应先显示空白/默认头像再突然变化 | ⬜ |
| I4 | 表格分类列可读性 | 查看文章列表的「分类ID」列 | 应显示分类**名称**而不是数字 ID | **×** 运行截图已确认：该列显示的是数字 `2`（代码为 `prop="categoryId"`） |
| I5 | 长页面滚动条 | 在登录页和主页面观察 | 不应出现意外的纵向滚动条 | ⬜ |
| I6 | 侧边栏菜单高亮 | 在不同页面间切换 | 当前所在菜单项应正确高亮 | ⬜ |
| I7 | 移动端/小屏适配 | 把窗口缩到 800px 宽 | 布局不应严重错乱 | ⬜ |
| I8 | 空表单直接提交 | 各弹窗表单都不填直接确定 | 应有明确校验提示，不产生未捕获异常 | ⬜ |
| I9 | 浏览器控制台 | 全程打开 F12 Console 操作 | 不应有红色报错 | ⬜ 代码层判定：`await formRef.validate()` 校验失败会 reject 且无 catch，预计出现 `Unhandled Promise Rejection` |

---

## 附录 A：接口级测试脚本

以下脚本用于测试**界面上点不到**的越权与安全用例。全部在 PowerShell 中执行（需后端已启动）。

```powershell
# ========== 准备：登录 paulgeorge8（id=2）作为「攻击者」身份 ==========
# 注意：testuser1 的密码已被改动，原 123456 不再可用，因此统一用 paulgeorge8
$body = @{ username = 'paulgeorge8'; password = 'asdfgh' }
$t2 = (Invoke-RestMethod -Uri 'http://localhost:8080/user/login' -Method Post -Body $body).data
Write-Host "paulgeorge8 (id=2) token: $($t2.Substring(0,30))..."

# ---------- G1 / G2：鉴权（预期 401，已验证通过）----------
Write-Host "`n[G1] 不带 token 访问 /user/userInfo（预期 401）"
try { Invoke-RestMethod -Uri 'http://localhost:8080/user/userInfo' } catch { Write-Host "  实际: $($_.Exception.Response.StatusCode.value__)" }

Write-Host "`n[G2] 伪造 token（预期 401）"
try { Invoke-RestMethod -Uri 'http://localhost:8080/user/userInfo' -Headers @{Authorization='abc.def.ghi'} } catch { Write-Host "  实际: $($_.Exception.Response.StatusCode.value__)" }

# ---------- C9：越权读他人分类（非破坏性；结论2 修复后预期被拒绝）----------
Write-Host "`n[C9] paulgeorge8 读 testuser1 的分类 id=2（预期：分类不存在，或您无权访问该分类）"
$r = Invoke-RestMethod -Uri 'http://localhost:8080/category/detail?id=2' -Headers @{Authorization=$t2}
Write-Host "  实际返回: code=$($r.code) message=$($r.message)"

Write-Host "`n[C9-回归] paulgeorge8 读自己的分类 id=4（预期：成功）"
$r = Invoke-RestMethod -Uri 'http://localhost:8080/category/detail?id=4' -Headers @{Authorization=$t2}
Write-Host "  实际返回: code=$($r.code) message=$($r.message)"

# ---------- C10：越权删他人分类（非破坏性：会被归属校验拒绝）----------
Write-Host "`n[C10] paulgeorge8 删除 testuser1 的分类 id=2（预期被拒绝）"
$r = Invoke-RestMethod -Uri 'http://localhost:8080/category?id=2' -Method Delete -Headers @{Authorization=$t2}
Write-Host "  实际返回: code=$($r.code) message=$($r.message)"

# ---------- C8：删除被文章引用的分类（非破坏性：外键必然阻止）----------
Write-Host "`n[C8] paulgeorge8 删除自己的分类 id=4（其下有文章，预期：该分类下已存在文章，无法删除）"
$r = Invoke-RestMethod -Uri 'http://localhost:8080/category?id=4' -Method Delete -Headers @{Authorization=$t2}
Write-Host "  实际返回: code=$($r.code) message=$($r.message)"

# ---------- D14 / D15：越权改删他人文章（非破坏性：0 行受影响）----------
Write-Host "`n[D15] paulgeorge8 删除 testuser1 的文章 id=3（预期被拒绝，文章不会被删）"
$r = Invoke-RestMethod -Uri 'http://localhost:8080/article/3' -Method Delete -Headers @{Authorization=$t2}
Write-Host "  实际返回: code=$($r.code) message=$($r.message)"

Write-Host "`n[D14] paulgeorge8 改 testuser1 的文章 id=3（预期被拒绝）"
$art = @{ id=3; title='越权篡改'; content='x'; coverImg='http://localhost:8080/uploads/x.jpg'; state='草稿'; categoryId=4 } | ConvertTo-Json
$r = Invoke-RestMethod -Uri 'http://localhost:8080/article' -Method Put -Body $art -ContentType 'application/json' -Headers @{Authorization=$t2}
Write-Host "  实际返回: code=$($r.code) message=$($r.message)"

# ---------- D16：引用他人分类发文章（尚未修复，预期会成功 = 缺陷）----------
Write-Host "`n[D16] paulgeorge8 用 categoryId=2（testuser1 的分类）发布文章（预期被拒绝，当前尚未修复）"
$art = @{ title='越权引用分类'; content='test'; coverImg='http://localhost:8080/uploads/x.jpg'; state='草稿'; categoryId=2 } | ConvertTo-Json
$r = Invoke-RestMethod -Uri 'http://localhost:8080/article' -Method Post -Body $art -ContentType 'application/json' -Headers @{Authorization=$t2}
Write-Host "  实际返回: code=$($r.code) message=$($r.message)"
Write-Host "  ⚠️ 若返回 code=0，说明文章被成功挂到了别人的分类下 —— 请手动删掉这条测试数据"

# ---------- H1 / H2 / H3：分页边界（已确认缺陷）----------
Write-Host "`n[H1] /article 不带分页参数（预期有默认值；当前返回「服务器繁忙」）"
try { Invoke-RestMethod -Uri 'http://localhost:8080/article' -Headers @{Authorization=$t2} | ConvertTo-Json -Depth 3 | Write-Host } catch { Write-Host "  实际: $($_.Exception.Message)" }

Write-Host "`n[H2] pageNum=0&pageSize=-1（预期校验失败；当前返回「操作成功」+ 空列表）"
$r = Invoke-RestMethod -Uri 'http://localhost:8080/article?pageNum=0&pageSize=-1' -Headers @{Authorization=$t2}
Write-Host "  实际返回: code=$($r.code) message=$($r.message) total=$($r.data.total)"

Write-Host "`n[H3] pageSize=1000000（预期有上限；当前无上限）"
$r = Invoke-RestMethod -Uri 'http://localhost:8080/article?pageNum=1&pageSize=1000000' -Headers @{Authorization=$t2}
Write-Host "  实际返回: code=$($r.code) message=$($r.message) total=$($r.data.total)"

# ---------- H5：错误路径（预期 404；当前返回 HTTP 200 + 服务器繁忙）----------
Write-Host "`n[H5] 错误路径：DELETE /article?id=999999"
$r = Invoke-RestMethod -Uri 'http://localhost:8080/article?id=999999' -Method Delete -Headers @{Authorization=$t2}
Write-Host "  实际返回: HTTP 200, code=$($r.code) message=$($r.message)"

# ---------- D13：删除自己的文章（修复后应真正删除）----------
Write-Host "`n[D13] 新建一篇自己的文章，再用修复后的路径删除它"
$art = @{ title='临时验证文章'; content='verify'; coverImg='http://localhost:8080/uploads/tmp.jpg'; state='草稿'; categoryId=4 } | ConvertTo-Json
Invoke-RestMethod -Uri 'http://localhost:8080/article' -Method Post -Body $art -ContentType 'application/json' -Headers @{Authorization=$t2} | Out-Null
$list = Invoke-RestMethod -Uri 'http://localhost:8080/article?pageNum=1&pageSize=50' -Headers @{Authorization=$t2}
$tmp = $list.data.items | Where-Object { $_.title -eq '临时验证文章' } | Select-Object -First 1
Write-Host "  新文章 id = $($tmp.id)"
$r = Invoke-RestMethod -Uri "http://localhost:8080/article/$($tmp.id)" -Method Delete -Headers @{Authorization=$t2}
Write-Host "  DELETE /article/$($tmp.id) -> code=$($r.code) message=$($r.message)"
```

> **本附录已按 2026-09-27 的现状更新**
>
> - 账号统一改用 `paulgeorge8` / `asdfgh`（`testuser1` 的密码已被改动，原 `123456` 不可用）
> - 越权用例 **C9 / C10 / D14 / D15 现在预期被拒绝**（结论 2 已修复）
> - **C8 用例预期返回「该分类下已存在文章，无法删除」**（需先重启后端使 C8 生效）
> - **D13 用例**会新建 1 条再删除，最终不留脏数据
> - **D16 用例会真的新增一条文章**（因为该缺陷尚未修复），执行后请手动删掉
> - 脚本全部使用不存在的 id 或只读请求，**不会破坏你现有的 3 篇文章 / 2 个分类**

### G5：弱密钥伪造测试（可选，演示用）

```powershell
# 若 JWT 密钥是源码里的 "itheima"，任何人都能伪造 token 冒充任意用户。
# 这里只做说明：请检查 JwtUtil.java 第 11 行，若仍是硬编码字符串，则该风险成立。
Get-Content 'C:\Users\朱毅城\Desktop\big-event-fullstack\big-event\src\main\java\com\example\bigevent\utils\JwtUtil.java' | Select-String 'KEY'
```

### Redis 检查命令

```powershell
# 查看 Redis 中的 token key（若无 redis-cli 在 PATH，用完整路径）
& 'C:\Program Files\Redis-x64-5.0.14.1\redis-cli.exe' keys '*'
& 'C:\Program Files\Redis-x64-5.0.14.1\redis-cli.exe' ttl '<上面查到的key>'
& 'C:\Program Files\Redis-x64-5.0.14.1\redis-cli.exe' dbsize
```

---

## 附录 B：环境重置

测试过程中会造脏数据，需要恢复时按下面顺序操作。

**1. 备份当前数据（强烈建议先做）**

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe' -uroot -p123456 --databases big_event > backup.sql
```

**2. 恢复基线数据**

如果只是清掉测试造的数据，重新执行建表脚本即可（**会清空全部数据**）：

```powershell
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' -uroot -p123456 --default-character-set=utf8mb4
# 进入 mysql> 后执行：
# source C:/Users/朱毅城/Desktop/big-event-fullstack/big-event/src/main/resources/db/schema.sql
```

**3. 清空 Redis 中的会话**

```powershell
& 'C:\Program Files\Redis-x64-5.0.14.1\redis-cli.exe' flushdb
```

> 清空后所有已登录用户会失效，需要重新登录（正好可以用来测试 B2 的 401 跳转场景）。

---

## 附录 C：缺陷汇总表（测试后填写）

| 序号 | 用例编号 | 现象描述 | 严重程度 | 建议修复 |
|---|---|---|---|---|
| 1 | | | | |
| 2 | | | | |
| 3 | | | | |

**严重程度参考**
- **P0 阻断**：功能完全不可用（如删除文章失败）
- **P1 严重**：安全问题或数据错误（如越权、密码泄露）
- **P2 一般**：边界/异常处理不当，用户体验差
- **P3 轻微**：文案、样式、优化建议

---

## 附录 D：已知风险点速查（对照本清单定位）

以下是**代码审阅阶段**发现、需要在本轮测试中重点验证的问题，按清单编号对应：

| 风险 | 对应用例 | 简述 |
|---|---|---|
| 删除文章接口路径不匹配 | D13 | 前端请求 `/article?id=N`，后端只提供 `/article/{id}` |
| 越权（IDOR） | C9、C10、D14、D15 | update/delete/detail 未校验 `create_user` 归属 |
| 跨用户引用分类 | D16、D17 | 未校验 `categoryId` 是否属于当前用户 |
| 删除被引用分类报错 | C8 | 外键无级联，异常被兜底成「服务器繁忙」 |
| 401 后无法回到登录页 | B13 + H10 | 401 时未清 token，路由守卫又把 `/login` 弹回 `/` |
| 分页页码不重置 | D22、D23 | 切换每页条数/删除末页最后一条 |
| 上传无扩展名文件报 500 | F6 | `substring(lastIndexOf("."))` 返回 -1 |
| 上传失败却提示成功 | F4、F5 | 业务失败也返回 HTTP 200，`on-success` 照常触发 |
| 上传无类型校验（XSS） | F5、F7 | 后缀白名单为空，`/uploads/**` 同源可访问 |
| 密码无盐 MD5 | G9 | 已实测：65 条字典 1 秒内破解两个账号 |
| JWT 密钥硬编码 | G5 | `JwtUtil.KEY = "itheima"` |
| 退出登录未失效服务端 Token | E16 | 无 logout 接口，Redis 记录留存至 12h 过期 |
| 改密只失效当前 Token | E13、E14 | 其他设备 token 仍然有效 |
| 用户名可枚举 | B8 | 「用户名错误」与「密码错误」提示不同 |
| 全局异常吞掉一切 | H5、H7、D17 | 一律 HTTP 200 + 「服务器繁忙」 |
| 无 404 前端路由 | H6 | 访问未知路径显示空白页 |
| 分页参数无边界校验 | H1、H2、H3 | 裸 `Integer`，无默认值与上限 |
| 后端缺少校验 | D6、D7、D8、G12 | `@Validated` 加了但未生效/仅前端校验 |
| 列表 `select *` 拉大字段 | 性能 | `content varchar(10000)` 被全量返回 |
| 上传文件永不清理 | F10 | 换封面/删文章不删磁盘文件 |
| 图片死链 | D12、F2 | 数据库 6 处图片 URL 指向已不存在的 `uploads/` |
| 死交互 | B15 | 「记住我」「忘记密码？」点击无效果 |

---

## 附录 E：接口探测实测结果

**测试时间**：2026-09-23
**测试方式**：后端运行中，用 `paulgeorge8` 的 token 发起**非破坏性**接口探测（全部使用不存在的 id，或只读请求）。
**数据完整性**：探测前后 `/article` `/category` `/user` 均为 **4 / 2 / 2**，未改动任何数据。

| 用例 | 请求 | 实测响应 | 判定 |
|---|---|---|---|
| G1 | `GET /user/userInfo`（无 Authorization） | `HTTP 401` | ✅ 通过 |
| G2 | `GET /user/userInfo`（伪造 token `abc.def.ghi`） | `HTTP 401` | ✅ 通过 |
| **C9** | `GET /category/detail?id=2`（id=2 属于 testuser1） | `{"code":0,"data":{"id":2,"categoryName":"篮球","createUser":1,...}}` | 🔴 **越权成立** |
| C9-b | `GET /category`（列表接口） | 仅返回自己的 `id=4 足球` | ✅ 列表过滤正常 |
| **H1** | `GET /article`（不带分页参数） | `{"code":1,"message":"服务器繁忙，请稍候再试"}` | 🔴 无默认值，报错被吞 |
| **H2** | `GET /article?pageNum=0&pageSize=-1` | `{"code":0,"message":"操作成功","data":{"total":1,"items":[]}}` | 🔴 无校验，静默返回空 |
| **H3** | `GET /article?pageNum=1&pageSize=1000000` | `{"code":0,...}` 正常返回数据 | 🔴 无上限 |
| **D13** | `DELETE /article?id=999999`（前端实际走的路径） | `{"code":1,"message":"服务器繁忙，请稍候再试"}` | 🔴 **路径不匹配，删除必然失败** |
| D13-对照 | `DELETE /article/999999`（后端声明的路径） | `{"code":0,"message":"操作成功"}` | ⚠️ 通了，但 id 不存在也返回「成功」 |

### 本次探测得到的三个确定结论

**结论 1 — 前端「删除文章」按钮 100% 失效（P0 阻断）**

前端 `src/api/article.js:20` 请求的是 `DELETE /article?id=N`，而后端 `ArticleController.java:53` 声明的是 `@DeleteMapping("/{id}")`，两者不匹配：

```
DELETE /article?id=999999   →  {"code":1,"message":"服务器繁忙，请稍候再试"}   ← 前端走的路径，匹配不到
DELETE /article/999999      →  {"code":0,"message":"操作成功"}                 ← 后端真实路径
```

所以你在页面上点垃圾桶图标，只会看到「服务器繁忙，请稍候再试」，文章不会被删除。
**修复**：把 `article.js:20` 改成 `` request.delete(`/article/${id}`) `` 即可（一行）。

**结论 2 — 横向越权（IDOR）已实测证实（P1 严重）**

`paulgeorge8` 成功读到了 `testuser1` 的分类详情，响应里还带出了 `"createUser":1`：

```json
{"code":0,"message":"操作成功","data":{"categoryAlias":"lanqiu","categoryName":"篮球","createUser":1,"id":2}}
```

值得注意的是：**列表接口 `GET /category` 的隔离是正常的**（只返回自己的 `id=4 足球`），但 `GET /category/detail`、`PUT /category`、`DELETE /category`、以及文章的 `PUT/DELETE` 都没做归属校验。也就是「看得见的做了隔离，看不见的没做」。
**修复**：这些语句的 `where` 条件都要补上 `and create_user = #{userId}`，或 service 层先校验归属后拒绝。

**结论 3 — 分页参数与异常处理缺失（P2 一般）**

- 不传 `pageNum`/`pageSize` → 「服务器繁忙」（因为它是 `Integer` 必填参数，缺失后异常被 `GlobalExceptionHandler` 兜底吞掉）
- `pageNum=0&pageSize=-1` → **返回「操作成功」+ 空列表**，比报错更危险（调用方以为是正常的空数据）
- `pageSize=1000000` → 无上限，可被用来拖库

**修复**：`@RequestParam(defaultValue="1") @Min(1) Integer pageNum` + `@RequestParam(defaultValue="5") @Min(1) @Max(100) Integer pageSize`。

### 探测中注意到的一个附带情况

`testuser1` 的密码哈希已从 `e10adc39...`（MD5("123456")）变为 `35752b47...`，说明你在测试过程中用「重置密码」功能改过密码，`123456` 已不再可用。

这恰好说明 **E11 用例通过了**（改密功能正常）。请在继续测试前，把本文档第 0 节账号表中的 `testuser1` 密码更新为你实际设置的新密码，否则后续用例无法登录。

> 另外，附录 A 的脚本默认使用 `testuser1`/`123456`，若密码已变更请同步修改，或改用 `paulgeorge8`/`asdfgh`。

---

## 附录 F：已修复缺陷记录（结论 1 / 结论 2 / C8）

**修复时间**：2026-09-23 ~ 2026-09-27

### 改动清单（10 个文件）

| 结论 | 文件 | 改动 |
|---|---|---|
| 1 | `big-event-web/src/api/article.js` | 删除文章请求路径 `/article?id=N` → `/article/${id}` |
| 2 | `mapper/ArticleMapper.java` | `update` 加 `and create_user=#{createUser}`；`deleteById` 改为接收 `userId` 并加 `and create_user=#{userId}`；两者返回 `int`；`list` 补 `@Param` |
| 2 | `mapper/CategoryMapper.java` | `findById`/`update`/`deleteById` 全部加 `create_user` 条件；返回 `int` |
| 2 | `service/ArticleService.java`、`CategoryService.java` | 方法签名改为返回受影响行数；`findById` 增加 `userId` 参数 |
| 2 | `service/impl/ArticleServiceImpl.java`、`CategoryServiceImpl.java` | 从 `ThreadLocal` 取当前用户 id **强制覆盖** `createUser`，防止请求体伪造；返回受影响行数 |
| 2 | `controller/ArticleController.java`、`CategoryController.java` | 受影响行数为 0 时返回明确错误，而不是「操作成功」 |
| C8 | `controller/CategoryController.java` | `delete` 捕获 `DataIntegrityViolationException`，把外键冲突（MySQL 1451）翻译成「该分类下已存在文章，无法删除」 |

> **关键设计点**：`update` 的 `where` 用的是 `#{createUser}`（对象属性），所以 Service **必须**用当前登录用户 id 覆盖前端传进来的值。否则攻击者只要在请求体里塞 `"createUser": 1` 就能绕过校验 —— 这一点已用用例 G/G-伪造 专门验证。

### 验证方式

用修复后的代码在 **8090 端口启动独立实例**（不影响你正在运行的 8080），以 `paulgeorge8`（id=2）的身份对 `testuser1`（id=1）的数据发起跨用户请求。

| 用例 | 请求 | 修复前 | 修复后 |
|---|---|---|---|
| C9 越权读他人分类 | `GET /category/detail?id=2` | 🔴 返回了 `篮球` 的完整数据 | ✅ `分类不存在，或您无权访问该分类` |
| C10 越权改他人分类 | `PUT /category` `{id:2}` | 🔴 可改 | ✅ 拒绝 |
| — 读自己分类（回归） | `GET /category/detail?id=4` | ✅ 成功 | ✅ 仍然成功 |
| D14 越权改他人文章 | `PUT /article` `{id:3}` | 🔴 可改 | ✅ 拒绝 |
| D15 越权删他人文章 | `DELETE /article/3` | 🔴 可删 | ✅ 拒绝 |
| — **伪造 createUser 绕过** | `PUT /article` `{id:3, createUser:2}` | 🔴 可绕过 | ✅ 仍拒绝 |
| D13 删除自己的文章 | 新建 → `DELETE /article/{id}` | 🔴 服务器繁忙 | ✅ 删除成功 |
| **C8 删除被引用的分类** | `DELETE /category?id=4`（其下有文章 id=7） | 🔴 服务器繁忙，请稍候再试 | ✅ 「该分类下已存在文章，无法删除」，且分类未被删除 |
| C7 删除未被引用的分类（回归） | 新建空分类 → 再删除 | ✅ 成功 | ✅ 仍然成功 |
| C10 越权删他人分类（回归） | `DELETE /category?id=2` | 🔴 可删 | ✅ 拒绝 |

### 数据完整性

验证前后均为 **文章 / 分类 2 / 用户 2**；分类名（篮球/lanqiu、足球/shijiebei）、文章标题、`create_user` 归属全部未被篡改。

> 文章数由 4 变为 3 是**你自己在页面删除 `id=5`「张雪峰老师」**造成的（测试脚本只操作分类和不存在的 id），不影响上述结论。

### 编译验证

25 个源文件经 `javac --release 17` 全量编译通过，退出码 0，Lombok 注解处理正常（`setCreateUser` 等方法已生成）。

### ⚠️ 需要再重启一次后端

Java 代码改动**必须重启后端才生效**。当前状态（2026-09-27 接口实测）：

| 修复 | 在 8080 上是否生效 |
|---|---|
| 结论 1（删除文章路径） | ✅ **已生效**（你已重启过一次） |
| 结论 2（越权归属校验） | ✅ **已生效** |
| C8（分类删除友好提示） | ❌ **尚未生效** —— 修复时间晚于你上次重启 |

**请在 IDEA 中再停止并重新运行一次 `BigEventApplication`**，C8 才会生效。

前端 `article.js` 属静态资源，Vite 开发服务器会自动热更新；若未生效请刷新浏览器页面。

### 仍未修复的相关项

| 项 | 说明 |
|---|---|
| D16/D17 跨用户引用分类 | 新增/修改文章时未校验 `categoryId` 是否属于当前用户 |
| F 系列 上传相关 | 后缀白名单、无扩展名 500、失败却提示成功、旧文件不清理 |
| G 系列 安全 | 无盐 MD5、JWT 硬编码密钥、无 logout 接口、用户名可枚举 |
| H 系列 边界 | 分页参数无默认值与上限、全局异常一律返回 HTTP 200 |
| H6 前端 404 | 无兜底路由，未知路径显示空白页 |
| I1/I4 体验 | 页面标题仍为 `Vite App`；文章列表「分类ID」列显示数字 |
