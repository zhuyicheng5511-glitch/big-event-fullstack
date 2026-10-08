# FIX-LIST — 待修复缺陷清单

> **用途**：把当前已确认的缺陷整理成可直接执行的修复任务，交给下一个 Agent。
> **生成时间**：2026-10-02
> **仓库**：`C:\Users\朱毅城\Desktop\big-event-fullstack`
> **分支**：`feature/basketball`（HEAD = `6908ce9`，**领先 origin 7 个提交，未推送**）
> **工作区**：干净（仅 `AGENT_HANDOFF.md` 未跟踪）

**配套文档**：`DESIGN.md`（重构方案）、`TEST-CHECKLIST.md`（113 条测试用例 + 附录 D 风险点速查）、`AGENT_HANDOFF.md`（交接说明）

**修复原则**

1. **一次只修一项**，修完立刻验证（编译 + 接口实测 + 数据核对）再提交，保持主干随时可运行
2. **不要扩大范围**：只修本文件列出的项，不要顺手重构、不要加新功能
3. **不要执行 `git push`**（见第 4 节环境约束）
4. **不要运行 `schema.sql` / `schema-basketball.sql`**（开头有 `DROP TABLE`，会清空真实数据）
5. 写文件一律 **UTF-8 无 BOM**；PowerShell 是 5.1，`Set-Content -Encoding UTF8` 会写 BOM（这正是 FIX-1 的成因）

---

## 一、必修项（含详细修复指引）

| 编号 | 项 | 来源 | 状态 |
|---|---|---|---|
| **FIX-1** | CSV 带 BOM 导致整表导入失败 | 2026-10-02 验收实测发现 | ✅ **已修复并提交**（`b1a6191`，已实测验证） |
| **FIX-2** | `ImportError.rowNumber` 语义与注释不符 | 2026-10-02 验收实测发现 | ✅ **已修复并提交**（`b1a6191`，已实测验证） |
| **FIX-3** | 401 后无法回到登录页（无限重定向，用户卡死） | 历史遗留 P1，本轮提升为必修 | ✅ **已修复并提交**（`f7f4777`，代码已含清 token 逻辑） |
| **FIX-4** | 上传无类型校验（存储型 XSS）+ 无扩展名文件 500 | 历史遗留 P1/P2，本轮提升为必修 | ✅ **已修复并提交**（`f7f4777`，白名单+魔数校验+前端 code 判断均已落地） |

> **FIX-1 ~ FIX-4 全部已完成**（2026-10-08 逐一核对代码确认，本节旧版状态列未更新）。
> 当前剩余工作见**第七节**：A 已完成（`b864b34`）；剩 B = 测试与 CI、C = 遗留 P1/P2。

---

### FIX-1 🔴 CSV 文件带 BOM 会导致整表导入失败，且报错信息完全误导

#### 现象（实测对比）

同一个"含重复行"的 CSV，仅仅因为编码带不带 BOM，返回结果完全不同：

```jsonc
// ❌ 带 BOM（EF BB BF）—— 报错原因与事实不符
{"code":1,"message":"导入失败，存在 2 行错误","data":{"errors":[
  {"rowNumber":1,"field":"name","reason":"字段不能为空"},
  {"rowNumber":2,"field":"name","reason":"字段不能为空"}
],"failureRows":2,"successRows":0,"totalRows":2}}

// ✅ 无 BOM —— 正确报出"重复"，还带上了具体的名字
{"code":1,"message":"导入失败，存在 1 行错误","data":{"errors":[
  {"rowNumber":2,"field":"name","reason":"文件内重复: 验收测试队B"}
],"failureRows":1,"successRows":0,"totalRows":1}}
```

文件内容明明有值，却报「字段不能为空」—— 用户会完全摸不着头脑。

#### 根因

`big-event/src/main/java/com/zhuyicheng/hoops/service/impl/ImportServiceImpl.java` 第 360-373 行：

```java
private void parse(MultipartFile file, RecordConsumer consumer) {
    CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build();
    // ↓ 第 366 行：InputStreamReader 用 UTF-8 解码，但【不会跳过 BOM】
    try (CSVParser parser = new CSVParser(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8), format)) {
        for (CSVRecord record : parser) {
            consumer.accept(record);
        }
    } catch (IOException e) {
        throw new RuntimeException("CSV 解析失败", e);
    }
}
```

BOM（`EF BB BF` = `\uFEFF`）会被当作第一个字符粘在表头第一列上：列名 `name` 变成 `\uFEFFname`。

于是 `value()`（第 375-377 行）里 `record.isMapped("name")` 返回 `false` → 返回 `null` → `missing()`（第 379-385 行）判定为空 → 每一行的第一列都报「字段不能为空」。

**这个缺陷影响面很大**：Excel「另存为 → CSV UTF-8（逗号分隔）」和 Windows 记事本导出的 CSV **默认都带 BOM**，也就是说**用户从 Excel 导出的文件 100% 会导入失败**。种子 CSV 恰好是无 BOM 的，所以之前一直没暴露。

#### 修复方案（推荐：不加新依赖）

在 `parse()` 里包一层跳过 BOM 的流。完整替换第 360-373 行：

```java
private void parse(MultipartFile file, RecordConsumer consumer) {
    CSVFormat format = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setTrim(true)
            .build();
    try (InputStream raw = file.getInputStream();
         InputStream in = skipBom(raw);
         CSVParser parser = new CSVParser(new InputStreamReader(in, StandardCharsets.UTF_8), format)) {
        for (CSVRecord record : parser) {
            consumer.accept(record);
        }
    } catch (IOException e) {
        throw new RuntimeException("CSV 解析失败", e);
    }
}

/**
 * 跳过 UTF-8 BOM（EF BB BF）。
 * Excel 与 Windows 记事本导出的 CSV 默认带 BOM，不处理会导致表头第一个列名
 * 变成 "\uFEFFname"，进而让所有行的第一列都被误判为「字段不能为空」。
 */
private InputStream skipBom(InputStream in) throws IOException {
    PushbackInputStream pushback = new PushbackInputStream(in, 3);
    byte[] bom = new byte[3];
    int read = pushback.read(bom, 0, 3);
    boolean isBom = read == 3
            && (bom[0] & 0xFF) == 0xEF
            && (bom[1] & 0xFF) == 0xBB
            && (bom[2] & 0xFF) == 0xBF;
    if (!isBom && read > 0) {
        pushback.unread(bom, 0, read);   // 不是 BOM 就还回去，避免吃掉正常内容
    }
    return pushback;
}
```

需要新增 import：`java.io.InputStream`、`java.io.PushbackInputStream`（`IOException` 已有）。

> **备选方案**：引入 `commons-io` 用 `org.apache.commons.io.input.BOMInputStream`。不推荐 —— 为一个功能引入新依赖不划算，且当前 pom 里没有 commons-io。

#### 可选的加固（非必需，做了更好）

**校验表头**：如果 CSV 完全缺少某个必需列（比如整列名拼错），当前实现会让每一行都报「字段不能为空」，同样误导。可以在 `parse()` 之后、逐行处理之前，先检查必需列是否都存在，缺失则直接返回 `ImportError(1, "header", "缺少必需列: name")`，一次说清。

这不是本次必修项，**是否做由用户决定**。

#### 验收方法

```powershell
$curl = 'C:\Windows\System32\curl.exe'
$TOKEN = ((& $curl -s -X POST http://localhost:8080/user/login -d 'username=paulgeorge8&password=asdfgh' | ConvertFrom-Json).data)

# 1) 造一个【带 BOM】的重复行文件（PowerShell 5.1 的 Set-Content -Encoding UTF8 正好会写 BOM）
$bad = "$env:TEMP\bom_test.csv"
"name,name_en,city,conference,division`n验收测试队C,TestC,测试市,东部,大西洋区`n验收测试队C,TestC2,测试市,东部,大西洋区" | Set-Content $bad -Encoding UTF8
# 确认真的带 BOM（应是 EF BB BF）
(([System.IO.File]::ReadAllBytes($bad))[0..2] | ForEach-Object { $_.ToString('X2') }) -join ' '

# 2) 导入：修复后应报「文件内重复: 验收测试队C」，而【不是】「字段不能为空」
curl.exe -s -X POST http://localhost:8080/import/team -H "Authorization: $TOKEN" -F "file=@$bad"

# 3) 断言：一条都没写进去
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' -uroot -p123456 --default-character-set=utf8mb4 hoops -e "select count(*) from team where name like '验收测试队%';"
# 预期 0
```

**通过标准**：第 2 步返回的 `errors[].reason` 是「文件内重复: 验收测试队C」，且第 3 步为 0。

> ⚠️ 复现前请先确认 Redis 与后端在运行（见第 4 节）。

---

### FIX-2 🟡 `ImportError.rowNumber` 的注释与实际语义不符，会让用户定位错行

#### 现象

导入一个重复行位于**文件第 3 行**（第 1 行是表头）的 CSV，返回的却是 `rowNumber: 2`。

实测两次都符合这个规律：

| 文件内容 | 重复所在物理行 | 返回的 rowNumber |
|---|---|---|
| 表头 + 2 行数据（第 2、3 行重复） | 第 3 行 | **2** |
| 带 BOM 的 2 行数据 | 第 2、3 行 | **1、2** |

#### 根因

`big-event/src/main/java/com/zhuyicheng/hoops/pojo/ImportError.java` 第 11 行：

```java
private int rowNumber;   // 数据行号（从 2 开始，表头为第 1 行）
```

注释说是"从 2 开始、表头算第 1 行"，但实际赋的值来自 `record.getRecordNumber()`（见 `ImportServiceImpl` 第 381、389、399 行等），在本项目的 `CSVFormat` 配置（`setHeader()` + `setSkipHeaderRecord(true)`）下，**表头不计数，数据行从 1 开始**。

结果：用户按 `rowNumber` 去 Excel 里找，会**差一行**。

#### 修复方案（二选一，推荐 A）

**A. 让行号对齐物理行（推荐，对用户最友好）**

在写入错误时把行号 +1，使其等于 CSV 文件里的物理行号（表头为第 1 行）。建议在 `ImportServiceImpl` 里加一个私有辅助方法，替换所有 `(int) record.getRecordNumber()` 调用点：

```java
/** CSV 文件中的物理行号：表头为第 1 行，首条数据为第 2 行。
 *  commons-csv 在 skipHeaderRecord 模式下数据行从 1 开始计数，故统一 +1。 */
private int lineNumber(CSVRecord record) {
    return (int) record.getRecordNumber() + 1;
}
```

然后把第 381、389、399 行等处的 `(int) record.getRecordNumber()` 换成 `lineNumber(record)`，并同步更新 `ImportError` 的注释为：

```java
private int rowNumber;   // CSV 中的物理行号（表头为第 1 行，首条数据为第 2 行）
```

**B. 只改注释，让注释描述实际行为**

把注释改为"数据行序号，从 1 开始，表头不计"，并接受与物理行号差 1。

> 推荐 A —— 导入报告的价值就在于让用户能直接定位到出问题的行，差一行会明显降低可用性。

#### 验收方法

```powershell
# 复用 FIX-1 里的 $bad（带 BOM），修复后应返回 rowNumber = 3（物理第 3 行）
curl.exe -s -X POST http://localhost:8080/import/team -H "Authorization: $TOKEN" -F "file=@$bad"
```

**通过标准**：`errors[].rowNumber` = **3**（重复行在文件中确实是第 3 行）。

---

### FIX-3 🔴 401 后无法回到登录页 —— 用户会卡死

#### 现象

token 失效后（过 12 小时、Redis 重启、在另一个标签页改过密码），用户继续操作会陷入死循环：

```
任意请求 → 401 → request.js 弹「请先登录」并 router.push('/login')
       → 路由守卫发现 localStorage 里 token 还在 → 弹回 '/'
       → Layout 挂载 → 调 /user/userInfo → 又 401 → 再 push('/login') → …
```

vue-router 检测到无限重定向后会**中止导航**。结果是**用户被卡在当前页，点任何菜单都进不去登录页，只能手动清 localStorage 才能恢复**。

#### 根因（两处代码共同造成）

```js
// ① big-event-web/src/utils/request.js:37-39 —— 只跳转，没清 token
if (error.response && error.response.status === 401) {
    ElMessage.error('请先登录')
    router.push('/login')          // ← token 还留在 localStorage / Pinia 里
}

// ② big-event-web/src/router/index.js:46-48 —— 守卫把 /login 弹回去
if (to.path === '/login' && userStore.token) {
    return '/'                     // ← 于是又被弹回首页
}
```

关键在 ①：**只要 token 残留，② 就必然把用户弹回去**。

> ⚠️ **注意一个常见的错误修法**：单纯把 `router.push('/login')` 换成 `window.location.href = '/login'` **解决不了问题** —— 整页刷新后 localStorage 里的 token 仍在，守卫照样把用户弹回 `/`，继续 401 循环。**修复的核心是清 token，不是改跳转方式。**

#### 修复方案

改 `big-event-web/src/utils/request.js`。

新增 import（放在文件顶部）：

```js
import { useUserStore } from '@/stores/user'
```

把响应拦截器的 error 分支（当前第 35-44 行）替换为：

```js
    error => {
        // 401 = 未登录 / 登录已过期。必须【先清 token 再跳转】：
        // 否则路由守卫看到 token 仍在，会把 /login 弹回 '/'，形成无限重定向。
        if (error.response && error.response.status === 401) {
            const userStore = useUserStore()
            userStore.removeToken()
            userStore.removeUserInfo()
            ElMessage.error('登录已过期，请重新登录')
            router.push('/login')
        } else {
            ElMessage.error('服务异常')
        }
        return Promise.reject(error)
    }
```

注意事项：

1. **`useUserStore()` 必须在拦截器回调里调用**（运行时 pinia 已安装），**不能提到模块顶层** —— 顶层调用会因为 pinia 尚未安装而报错。
2. `big-event-web/src/stores/user.js` 里已有现成的 `removeToken()` / `removeUserInfo()`（第 14-17、23-25 行），直接复用，不用新增。
3. **不需要改 `router/index.js` 的守卫** —— 守卫逻辑本身是对的（有 token 就不该停留在登录页），问题在于不该留着**已失效**的 token。
4. 可选（非本 FIX 必需）：`request.js:8` 的全局 `timeout: 5000` 对上传接口偏短，10MB 文件在慢网络下必然超时，可给上传单独放宽。

#### 验收方法

```powershell
# 1) 浏览器打开 http://localhost:5173/login，用 paulgeorge8 / asdfgh 登录
# 2) 模拟 token 失效：清空 Redis 里的会话记录
& 'C:\Program Files\Redis-x64-5.0.14.1\redis-cli.exe' flushdb

# 3) 回到页面，随便点一个菜单（触发一次请求）
```

**通过标准**：

| 检查 | 预期 |
|---|---|
| 提示 | 弹出「登录已过期，请重新登录」 |
| 页面 | **停留在登录页**（不再被弹回首页） |
| Local Storage | F12 → Application → 里面的 `token` 已被清空 |
| 控制台 | 没有 vue-router 的无限重定向警告 |
| 恢复 | 用正确账号密码能重新登录成功 |

---

### FIX-4 🔴 上传无类型校验（存储型 XSS）+ 无扩展名文件报 500

#### 现象（两个独立问题 + 一个连带问题）

**问题 A：无后缀白名单 → 存储型 XSS**

`/uploads/**` 是**同源**静态资源映射（`big-event/src/main/java/com/zhuyicheng/hoops/config/WebConfig.java`），而上传接口完全不校验类型。上传一个内容含 `<script>` 的 `.html`：

```
POST /upload  (file=evil.html)
→ 返回 http://localhost:8080/uploads/xxx.html
→ 浏览器访问该 URL 时，脚本在【项目同源】下执行
→ 可以读走 localStorage 里的 JWT → 账号接管
```

`.svg`（可内嵌脚本）同理。

**问题 B：无扩展名文件 → 500**

```java
// FileUploadController.java:25
String extName = originalFilename.substring(originalFilename.lastIndexOf("."));
```

文件名里没有 `.` 时 `lastIndexOf` 返回 `-1` → `substring(-1)` 抛 `StringIndexOutOfBoundsException`。**它不是 `IOException`，不会被第 44 行的 catch 捕获**，直接冒泡到全局异常处理器 → 用户只看到「服务器繁忙，请稍候再试」。

另外 `getOriginalFilename()` 可能返回 `null` → NPE，同样是 500。

**问题 C（连带）：前端"上传失败却提示成功"**

上传接口失败时返回 `{code:1, data:null}`，但 **HTTP 状态码是 200**。所以 axios 不报错、Element Plus 的 `on-success` 照常触发 → 弹「上传成功」但图片地址其实是空的。**只修后端不够，前端也要改。**

#### 根因位置

- 后端：`big-event/src/main/java/com/zhuyicheng/hoops/controller/FileUploadController.java`（共 49 行，第 25 行是核心）
- 前端 7 处 `on-success` 回调（均未判断业务 `code`）：
  `ArticleManage.vue:84`、`ArticleManage.vue:221`、`UserAvatar.vue:17`、`UserAvatar.vue:51`、`Player.vue:145`、`Player.vue:281`、`Team.vue:217`

#### 修复方案（后端）

用下面的内容**替换 `FileUploadController.java` 全文**：

```java
package com.zhuyicheng.hoops.controller;

import com.zhuyicheng.hoops.pojo.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@RestController
public class FileUploadController {

    /** 允许上传的图片后缀（小写，不含点）。白名单之外一律拒绝。 */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "webp");

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        String ext = extensionOf(file.getOriginalFilename());
        if (ext == null) {
            return Result.error("无法识别文件类型，请上传 jpg / jpeg / png / gif / webp 图片");
        }
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            return Result.error("只允许上传图片（jpg / jpeg / png / gif / webp），当前为 ." + ext);
        }
        if (!looksLikeImage(file)) {
            return Result.error("文件内容不是有效图片");
        }

        try {
            String filename = UUID.randomUUID().toString() + "." + ext;
            String localPath = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(localPath);
            if (!dir.exists() && !dir.mkdirs()) {
                return Result.error("上传目录创建失败");
            }
            file.transferTo(new File(localPath + filename));
            return Result.success("http://localhost:8080/uploads/" + filename);
        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败");
        }
    }

    /**
     * 取小写后缀名；无法确定时返回 null。
     *
     * 原实现直接 substring(lastIndexOf("."))，文件名没有点时 lastIndexOf 返回 -1，
     * substring(-1) 会抛 StringIndexOutOfBoundsException —— 它不是 IOException，
     * 不会被 catch 住，最终被全局异常处理器兜成「服务器繁忙」，用户无从定位。
     */
    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return null;
        }
        int dot = originalFilename.lastIndexOf('.');
        if (dot < 0 || dot == originalFilename.length() - 1) {
            return null;
        }
        return originalFilename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * 校验文件头，防止把非图片改后缀绕过白名单。
     * jpg: FF D8 FF / png: 89 50 4E 47 / gif: 47 49 46 38 / webp: RIFF....WEBP
     */
    private boolean looksLikeImage(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            byte[] head = new byte[12];
            int n = in.read(head);
            if (n < 4) {
                return false;
            }
            int b0 = head[0] & 0xFF, b1 = head[1] & 0xFF, b2 = head[2] & 0xFF, b3 = head[3] & 0xFF;
            if (b0 == 0xFF && b1 == 0xD8 && b2 == 0xFF) return true;                 // JPEG
            if (b0 == 0x89 && b1 == 0x50 && b2 == 0x4E && b3 == 0x47) return true;   // PNG
            if (b0 == 0x47 && b1 == 0x49 && b2 == 0x46 && b3 == 0x38) return true;   // GIF
            return n >= 12
                    && b0 == 0x52 && b1 == 0x49 && b2 == 0x46 && b3 == 0x46          // RIFF
                    && (head[8] & 0xFF) == 0x57 && (head[9] & 0xFF) == 0x45
                    && (head[10] & 0xFF) == 0x42 && (head[11] & 0xFF) == 0x50;       // WEBP
        } catch (IOException e) {
            return false;
        }
    }
}
```

⚠️ **一个实现注意点**：`looksLikeImage()` 读取了 `file.getInputStream()`，之后又调 `file.transferTo(...)`。Spring 的 `StandardMultipartFile` 底层走 `Part.write()`，**不受影响**；但如果实测发现 `transferTo` 报"流已关闭"，改成"先 `file.getBytes()` 读全量、再用 `Files.write()` 写出"即可。

#### 修复方案（前端）

7 处 `on-success` 回调统一改成**先判断业务 code**：

```js
const handleAvatarSuccess = (response) => {
  // 上传接口失败时也返回 HTTP 200，必须判断业务 code，否则会"上传失败却提示成功"
  if (response.code !== 0) {
    ElMessage.error(response.message || '图片上传失败')
    return
  }
  articleModel.value.coverImg = response.data   // 各文件里这里的字段名不同
  ElMessage.success('图片上传成功！')
}
```

> `UserAvatar.vue` 里赋值目标是 `imgUrl.value`；`Player.vue` / `Team.vue` 里是各自的表单字段。模式相同，只是字段名不同。

#### 验收方法

```powershell
$curl = 'C:\Windows\System32\curl.exe'
$TOKEN = ((& $curl -s -X POST http://localhost:8080/user/login -d 'username=paulgeorge8&password=asdfgh' | ConvertFrom-Json).data)

# ① 正常图片应成功（仓库里有现成的测试图 big-event/test.jpg）
curl.exe -s -X POST http://localhost:8080/upload -H "Authorization: $TOKEN" -F "file=@big-event/test.jpg"
# 预期 code=0，返回图片 URL

# ② .txt 应被拒绝
"hello" | Set-Content "$env:TEMP\evil.txt"
curl.exe -s -X POST http://localhost:8080/upload -H "Authorization: $TOKEN" -F "file=@$env:TEMP\evil.txt"
# 预期 code=1「只允许上传图片…」

# ③ 无扩展名文件应被拒绝，且【不能】是「服务器繁忙」
[System.IO.File]::WriteAllText("$env:TEMP\noext", "hello", (New-Object System.Text.UTF8Encoding($false)))
curl.exe -s -X POST http://localhost:8080/upload -H "Authorization: $TOKEN" -F "file=@$env:TEMP\noext"
# 预期 code=1「无法识别文件类型…」（修复前是「服务器繁忙，请稍候再试」）

# ④ .html 应被拒绝
'<script>alert(1)</script>' | Set-Content "$env:TEMP\evil.html"
curl.exe -s -X POST http://localhost:8080/upload -H "Authorization: $TOKEN" -F "file=@$env:TEMP\evil.html"
# 预期 code=1

# ⑤ 改后缀绕过应被拒绝（把 txt 改名成 .jpg）
Copy-Item "$env:TEMP\evil.txt" "$env:TEMP\fake.jpg"
curl.exe -s -X POST http://localhost:8080/upload -H "Authorization: $TOKEN" -F "file=@$env:TEMP\fake.jpg"
# 预期 code=1「文件内容不是有效图片」
```

**通过标准**：① 成功；②③④⑤ 全部返回 `code=1` 且 `message` 是**明确原因**（**不能出现「服务器繁忙」**）。
前端：上传失败时应弹错误提示，而不是「上传成功」。

---

## 二、待修 · 历史遗留缺陷（按优先级）

> 全部来自 `TEST-CHECKLIST.md` 附录 D 的实测/代码层判定结果。
> **下表已剔除在本轮 W1/W2 中已修复的项**（JWT 密钥硬编码、无盐 MD5、建表脚本缺失、前端旧标识、页面标题 `Vite App`、MySQL 导入框架本身等均已修复）。

### P1 — 安全与用户可直接感知

| 编号 | 缺陷 | 位置 | 修复要点 |
|---|---|---|---|
| ~~**B2**~~ | 401 后无法跳回登录页（无限重定向，用户卡死）—— **已提升为必修，见上方 FIX-3** | — | — |
| **B8** | 用户名可枚举：「用户名错误」与「密码错误」提示不同，可批量探测已注册用户名 | `UserController.login` | 两者统一返回「用户名或密码错误」 |
| **E16** | **后端没有 logout 接口**，退出登录只清前端 localStorage，Redis 里的 token 记录留存至 12h 过期 | 需新增 `POST /user/logout` | 新增接口删除当前 token 对应的 Redis 记录 |
| **E13/E14** | 改密只失效**当次请求那个 token**，其他设备/浏览器的 token 仍然有效（Redis 中曾实测同时存在 5 个有效 token） | `UserController.updatePassword` | Redis 增加 `login:user:<userId>` 索引（Set 存该用户所有 token），改密时清空；或在 JWT 里放 `pwdChangedAt` 做校验 |
| **G6** | Redis key 就是裸 token，无命名空间，且无法按用户批量失效 | `UserController.java:81`、`LoginInterceptor.java:30` | 统一改为 `login:token:<token>` 前缀（与 E13/E14 的修复一起做） |

### P2 — 功能与边界

| 编号 | 缺陷 | 位置 | 修复要点 |
|---|---|---|---|
| **D16/D17** | 新增/修改文章未校验 `categoryId` 是否属于当前用户；引用不存在的分类会撞外键、报「服务器繁忙」 | `ArticleMapper` / `ArticleController` | 写入前校验 categoryId 属于当前用户，不存在则返回明确错误 |
| **D22/D23** | 切换每页条数不重置页码；删除当前页最后一条后不回退页码 | `big-event-web/src/views/ArticleManage.vue` | `handleSizeChange` 里 `pageNum = 1`；删除后若当前页已空则 `pageNum--` |
| **H1/H2/H3** | `article` / `category` 的分页参数**无默认值、无边界校验**（不传报「服务器繁忙」，`pageNum=0&pageSize=-1` 返回"操作成功"+空列表，`pageSize=1000000` 无上限） | `ArticleController.java:36-37`、`CategoryController` | **直接照抄已修好的篮球接口写法**：`big-event/src/main/java/com/zhuyicheng/hoops/controller/TeamController.java:32-33` |
| ~~**F4/F5/F6/F7**~~ | 上传无类型校验（存储型 XSS）、无扩展名文件 500、上传失败却提示成功 —— **已提升为必修，见上方 FIX-4** | — | — |
| **F10** | 上传的文件永不清理，换封面/删文章都不删磁盘旧文件 | `FileUploadController` | 替换/删除时清理旧文件，或加定时任务清理孤儿文件 |
| **G12** | 改密接口后端只判空，不校验长度（绕过前端可设 1 位密码） | `UserController.updatePassword:108` | 建 DTO + `@NotBlank @Pattern(regexp="^\\S{5,16}$")` |

### P3 — 工程质量与体验

| 编号 | 缺陷 | 位置 | 修复要点 |
|---|---|---|---|
| **H7** | 所有业务失败都返回 HTTP 200 + `code:1`，REST 语义不清（连带 H5：错误路径也返回「服务器繁忙」） | `exception/GlobalExceptionHandler.java` | 参数校验失败→400、未授权→401、无权限→403、资源不存在→404、服务端异常→500；`Result.code` 可细分错误类型 |
| **H6** | 前端无 404 兜底路由，访问未知路径是**完全空白页** | `big-event-web/src/router/index.js` | 加 `{ path: '/:pathMatch(.*)*', component: NotFound }` |
| **I4** | 文章列表「分类ID」列显示的是数字而不是分类名 | `big-event-web/src/views/ArticleManage.vue` | 用页面已有的 `categorys` 做 id→name 映射（篮球页面已做，可参考） |
| **Q1** | **PageHelper 1.4.7 与 Spring Boot 4 不匹配**（1.4.7 配套的是 Boot 2.7.12 / mybatis-spring-boot-starter 2.3.1；本地 `.m2` 中确实同时存在 2.3.1 与 4.0.1 两个版本） | `big-event/pom.xml` | 升到 **`pagehelper-spring-boot-starter 4.1.1`**。⚠️ **不要选 4.0.0**（要求 JDK 21+，本项目是 JDK 17） |
| **Q2** | 没有 Maven Wrapper，且 `.gitignore` 主动忽略了 `**/mvnw`、`**/.mvn/` | `.gitignore`、`big-event/` | 生成并提交 mvnw，删掉 `.gitignore` 里那两行 |
| **Q13/Q14** | 上传返回的 URL 硬编码 `http://localhost:8080/uploads/...`（换环境即死链，且已被写进数据库）；上传目录基于 `System.getProperty("user.dir")`，随启动方式漂移 | `FileUploadController.java:41`、`config/WebConfig.java:26` | 返回相对路径 `/uploads/xxx` 或从配置读 base-url；上传目录抽成配置项（`file.upload-dir`），两处共用 |
| — | `.gitattributes` 只声明了 `mvnw` / `*.cmd`，其余文件未约定换行符，提交时持续出现 `LF will be replaced by CRLF` 警告 | `big-event/.gitattributes` | 加一行 `* text=auto eol=lf`（或按需配置） |

### 非代码问题（数据/环境）

| 项 | 说明 |
|---|---|
| 图片死链 | `hoops` 库中 5 处图片引用有 **2 处文件缺失**（`07f9f5e9-…jpg` 是文章 id=7 的封面、`ec8bace5-…webp` 是 paulgeorge8 的头像）。`uploads/` 目录被 `.gitignore` 忽略，**换机器/重新 clone 后图片会再次全部失效** |
| ~~`shot` 表为空~~ | ✅ 已解决（W4 前已造数）：`shot` = 100000 |
| 数据真实性 | `game` 1200 场接近真实（约 1230 场），但 `player_game_stat` 30024 行 / 39 人 ≈ 每人 770 场，真实球员一赛季约 82 场 —— 程序生成的合成数据，面试被问需如实说明 |

---

## 三、当前进度与下一步

> ⚠️ 本节是 **2026-10-02 W2 阶段**的快照，已过时。当前（2026-10-08）是 **W1–W4 全部完成**，剩余工作见**第七节**。

### 已完成（W1 全部 + W2 大部分）

- W1：改名（包名 `com.zhuyicheng.hoops` / 主类 `HoopsApplication` / 库名 `hoops`）、JWT 密钥配置化、密码改 BCrypt（含历史 MD5 自动升级）、6 张篮球表建模、前端菜单与品牌清理
- W2：6 实体 CRUD（含 stat/shot）、**CSV 批量导入框架**（`POST /import/{entity}`，事务 + 校验 + 导入报告，实测可用）、种子数据、10 万条 shot 造数 Runner、**进阶数据计算引擎 `StatCalculator` + 参数化单测**

### 验收已达成的部分（2026-10-02 实测）

| 项 | 结果 |
|---|---|
| CSV 导入（season → team → player → game → player-game-stat） | ✅ 数据量全部正确：**1 / 30 / 39 / 8 / 24** |
| 全有或全无（含错文件 → 一条不插） | ✅ 导入后库中 0 条残留，`team` 总数未变 |

### 尚未验证 / 未执行

| 项 | 怎么验 |
|---|---|
| 10 万条 shot 造数 | IDEA 运行配置的 Program arguments 填 `--generate-shots`（Runner 由该参数触发，不会每次启动都跑）；预期控制台输出 `[ShotDataGenerator] done: 100000 shots in xxx ms`，`select count(*) from shot` = 100000 |
| 单元测试 | `cd big-event && mvn test`；预期 `StatCalculatorTest` 的 **15 个测试方法全绿**（9 `@Test` + 6 `@ParameterizedTest`）。⚠️ `HoopsApplicationTests.contextLoads` 是空壳且需要 MySQL+Redis，红了不用管 |
| 幂等/重复检测（与库中已存在的数据冲突） | 导入成功后**再导一次 `teams.csv`**，预期 `code=1`、`failureRows=30`、每条「球队名已存在」，`team` 仍为 30 |
| 前端 6 个菜单与页面 | 浏览器 `http://localhost:5173/login`，登录 `paulgeorge8` / `asdfgh`，逐页测列表/搜索/重置/翻页/新增/编辑回显/删除 |

> ⚠️ **前端没有导入入口**，CSV 导入只能走接口。这是设计如此，不是缺漏。

---

## 四、环境与约束（必须遵守）

### 工具路径（`mvn` / `mysql` / `redis-*` 都不在 PATH）

| 工具 | 路径 |
|---|---|
| JDK 17 | `C:\Users\朱毅城\.jdks\ms-17.0.19` |
| **Maven 3.9.9** | `D:\BaiduNetdiskDownload\IntelliJ IDEA 2025.1.1.1\plugins\maven\lib\maven3\bin\mvn.cmd` |
| MySQL 客户端 | `C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe` |
| Redis | `C:\Program Files\Redis-x64-5.0.14.1\`（5.0.14.1） |

### 启动方式

```powershell
# Redis（未注册成 Windows 服务，不会自启）
& 'C:\Program Files\Redis-x64-5.0.14.1\redis-server.exe' --port 6379 --dir "$env:TEMP\redis-data"
# ⚠️ 不要加 --save ''（空参数会导致启动失败）

# 后端
cd C:\Users\朱毅城\Desktop\big-event-fullstack\big-event
& 'D:\BaiduNetdiskDownload\IntelliJ IDEA 2025.1.1.1\plugins\maven\lib\maven3\bin\mvn.cmd' spring-boot:run
# 或在 IDEA 中运行 HoopsApplication
```

### 关键约束

1. **不可从 Agent shell 执行 `git push`** —— 沙箱禁止访问 Windows TLS 凭证存储（`schannel: SEC_E_NO_CREDENTIALS`，**连 `https://www.baidu.com` 都访问不了**）。推送必须由用户在 IDEA 或普通 shell 中执行。
2. **Maven 需要写 `C:\Users\朱毅城\.m2`**（工作区外）。若沙箱拒绝，需申请更宽的文件权限（本会话曾以 `danger-full-access` 提权成功过一次，此后 Maven 命令行构建可用）。新引入依赖时可能再次需要。
3. **验证时不要动用户正在运行的实例**，用独立端口（如 8090）起临时后端。
4. **PowerShell 是 5.1**（不是 pwsh 7）：读 UTF-8 文件用 `[System.IO.File]::ReadAllText($p, [System.Text.Encoding]::UTF8)`；写无 BOM 用 `New-Object System.Text.UTF8Encoding($false)`；`<` 重定向必须走 `cmd /c`。
5. **curl 发带中文的 JSON body** 用 `curl.exe` + UTF-8 文件 `--data-binary @file`（PowerShell 的 `Invoke-WebRequest` 会报 `Invalid URI`）。
6. **本项目是课程项目重构而来，用户明确说过不要提新功能需求**。只修本清单列出的项。

### 测试账号与数据库

| 账号 | 密码 | 说明 |
|---|---|---|
| `paulgeorge8` | `asdfgh` | 密码已是 BCrypt，**验证时用这个** |
| `testuser1` | **未知**（用户改过） | 密码仍是历史 MD5，下次登录成功会自动升级为 BCrypt |

- 数据库：**`hoops`**（9 张表，在用）；`big_event` 是旧库，**保留作退路，不要删**
- 当前 `hoops` 数据：`user` 3 / `category` 2 / `article` 3 / `season` 1 / `team` 30 / `player` 39 / `game` 1200 / `player_game_stat` 30024 / `shot` 100000

---

## 五、建议的修复顺序

> ⚠️ 本节是 **W2 阶段**的历史顺序（FIX-1 ~ FIX-4 及 B2 等均已修复）。当前（2026-10-08）的剩余工作与顺序见**第七节**，以第七节为准。

```
1. FIX-1（BOM）        ← 本次验收新发现，影响所有 Excel 用户，必修
2. FIX-2（行号）       ← 与 FIX-1 同文件，一起改一起验
   ↓ 提交
3. B2（401 死循环）    ← 用户可直接感知，改动小
4. E16 + E13/E14 + G6  ← 三个是同一块（会话失效），建议一起做
   ↓ 提交
5. H1/H2/H3（分页校验）← 照抄 TeamController 即可，最省事
6. F4/F5/F6/F7（上传） ← 安全项，改动中等
   ↓ 提交
7. 其余 P2/P3 项按需
```

**每完成一组就提交一次**，提交信息用中文 + Conventional Commits 前缀（`fix:` / `feat:` / `docs:` 等），正文用 `-F <UTF-8 文件>` 传入（**不要用 `-m` 直接传中文**，PowerShell 5.1 控制台是 cp936 会乱码）。

---

## 六、给接手 Agent 的说明

你好。这个项目当前状态是：**W1–W4 全部完成并已推送**，剩余工作见第七节（A / B / C 三项）。

**请从第七节的 B 开始（测试覆盖 + CI）。** 第一节 FIX-1 ~ FIX-4、原 NEW-1 ~ NEW-6、Q1、以及第七节 A（Docker 自动造数，`b864b34`）均已完成。

**注意区分**：
- 第一节的 FIX-1 ~ FIX-4 是**已修复**的历史必修项，其「修复指引」仍可作为同类问题的参考
- 第七节是**当前剩余工作**：A = ~~Docker 演示数据~~ ✅ 已完成、B = 测试覆盖 + CI（P1，下一个）、C = 遗留 P1/P2 小毛病（引用第二节）
- 第二节的表格是**历史遗留缺陷总表**，「位置」列给了具体文件与行号；第七节 C 的编号与第二节一一对应
- 已修复的项**没有**列进本文件（如 JWT 密钥硬编码、无盐 MD5、前端旧标识等，见 `TEST-CHECKLIST.md` 附录 F/G）

**不确定时**：先查 `TEST-CHECKLIST.md` 附录 D（风险点速查），再 `git log -p` 看最近提交的正文（改动原因都写在 commit message 里），仍不确定就问用户，**不要猜**。

---

## 七、剩余工作（交接清单 · 2026-10-08）

> **已修复并验证（2026-10-04 Docker 实机通过）**：原 NEW-1 ~ NEW-6、Q1 已全部完成。
> - `650d365` nginx `/api` 剥前缀 + `.dockerignore` + swagger 路径
> - `4f0916c` 上传相对路径 + JWT_SECRET 外置 `.env`
> - `041b088` Redis `KEYS` → `SCAN`
> - `df804e7` PageHelper `1.4.7 → 4.1.1`
> - `ff41c1f` nginx `client_max_body_size 10m`
>
> 数据现状：本地 `game` 1200 / `player_game_stat` 30024 / `shot` 100000；
> **Docker 全新环境现已自动造数**（`b864b34`，见下方 A 的完成记录）：
> `down -v && up` 后 season=1 / team=30 / player=39 / game=1200 / stat=**30000** / shot=100000 / user=1。
> ⚠️ 本地 stat=30024 与 Docker 的 30000 差 24 条是**规格差异，不是丢了数据**：
> 旧 stats Runner 每场固定「新插 25 条」，8 场种子（已有 24 条）变成每场 28 条；
> 新逻辑改为「每场补到 25 条」，所以全新环境恰好 30000。本地库未重造，保留旧数字。

### A. ✅ Docker 演示缺数据 —— **已完成**（`b864b34`，2026-10-08 实机验收）

**当时的结论**：方案②的加强版 —— 不 dump 满血库（①会把密码哈希与大 SQL 提交进公开仓库），
而是新增 `SeedDataRunner`（`--import-seed`，导入 classpath 的 5 个种子 CSV + 创建演示账号 `paulgeorge8`），
compose 加一次性 `seed` 服务串起 `--import-seed → --generate-stats → --generate-shots`。

**过程中实测推翻了本节 3 处旧假设**（都已修，接手人不必再踩）：

| 旧假设 | 实测 | 处置 |
|---|---|---|
| 「两个 Runner 幂等,可安全重跑」 | **都不幂等**：stats 每场只数「本次新插」凑满 25 就停，重跑二跑 30024→46800；shots 每次无条件插 10 万 | stats 改为按场统计已有条数、只补差额；shots 改为补到 10 万 |
| seed 服务只要 `restart: "no"` 跑完退出 | Runner 跑完 web 容器不退出，seed 永驻 | 命令加 `--spring.main.web-application-type=none`，跑完 JVM 自然退出 |
| Runner 间顺序无需处理 | Spring 不保证多个 `CommandLineRunner` 顺序，stats 抢在 seed 前会静默跳过 | 显式 `@Order(10/20/30)` |
| （附带发现）`GameMapper.add` 无需回填主键 | stats Runner 插完比赛立刻用 `game.getId()` 挂 stat，null 直接 `Column 'game_id' cannot be null` | `@Options(useGeneratedKeys=true, keyProperty="id")` |

**验收已达成**：`down -v && up -d --build` 一次成功（seed `Exited (0)`），
`paulgeorge8` 直接登录，`/rank/points`、`/shot/chart`、`/player-game-stat/summary` 均返回真实数据，
`nginx /api` 代理路径同步通过；**二次 `up` seed 约 5 秒跳过且所有数字不变**。
**用户已按验收步骤亲自复验通过（2026-10-08），A 项关闭。**

<details>
<summary>以下是 A 项当时的原始问题记录（保留供回溯）</summary>

**问题（2026-10-08 核实）**：`docker compose down -v && up` 之后是 **9 张全空表**：
- `schema.sql` / `schema-basketball.sql` 都是纯 `CREATE TABLE`，**0 条 INSERT**；
- 后端**没有任何启动时自动导入 seed 的逻辑**——`resources/seed/*.csv`（teams/players/seasons/games/player_game_stats 5 个）只是躺在 classpath，靠 `/import` 接口手工导；
- `docker-compose.yml` 的 mysql 只把两个建表脚本挂到 `/docker-entrypoint-initdb.d`，没有数据脚本。

结果：**`user` 表也是空的 → 没有 `paulgeorge8`，登录页都进不去**；即便现场注册一个账号，
球队/球员/赛季/比赛/热图/雷达/排行榜也全部为空。之前 10-04 记的「种子 CSV 导入 1/30/39/8/24」
是**手工**调 `/import` 接口导入的，不是 compose 自动做的。

**根因**：数据从三处来，全都不是自动的：
- 5 个 seed CSV → 靠 `POST /import/{entity}` 手工导；
- `PlayerGameStatDataGeneratorRunner` → `--generate-stats`（补到 1200 场 + 每场 25 条 stat；幂等，跳过已存在的 `(gameId,playerId)`）
- `ShotDataGeneratorRunner` → `--generate-shots`（按全量比赛生成投篮点；幂等）
- 用户账号 → 靠 `POST /user/register` 手工注册（或 SQL 导入）
- `RankPerfTestRunner` → `--perf-test`（仅性能对比，演示可不用）

Docker 后端 `ENTRYPOINT ["java","-jar","app.jar"]` 没带任何参数，所以上面全都没发生。

**修复（三选一，推荐①）**：
1. **把本地满血库整个 dump 挂载进 Docker MySQL**（一次解决「数据 + 账号」两个问题，演示零操作）：
   `mysqldump -uroot -p hoops > big-event/db/init/hoops.sql`，
   `docker-compose.yml` 的 mysql 服务加 `volumes: - ./big-event/db/init:/docker-entrypoint-initdb.d:ro`。
   dump 里已含建表语句 + `paulgeorge8` 等用户 + 1200 场/30024 stat/100000 shot，首次 `up`（空数据卷）自动导入。
   ⚠️ `initdb.d` 只在**空数据卷**首次初始化时执行，改完要 `down -v` 才会重跑。
2. **compose 加一次性 seed 服务**（`restart: "no"` + `depends_on: backend`），按顺序跑：
   ① `/import` 导 5 个 CSV ② `--generate-stats` ③ `--generate-shots` ④ **还要导/注册一个用户**（否则仍无法登录）。
   比①复杂，且 CSV 只有 8 场/24 stat，规模远小于本地满血数据。
3. **只写文档**：README 写清「演示前按顺序：导入 CSV → 生成数据 → 注册账号」。现场步骤最多、最容易翻车，不推荐用于面试。

**顺序约束**：`--generate-stats` 需要 season/team/player 先存在（否则打印 `missing season/team/player data` 并跳过），
所以正确顺序是「建表（compose 自动）→ 导入 seed CSV（`/import`）→ `--generate-stats` → `--generate-shots` → 注册/导入用户」。

**验收**：`docker compose down -v && docker compose up -d --build` 后，
① 能直接用 `paulgeorge8` 登录；② 容器内 `mysql -e "select count(*) from shot"` = 100000、`game` = 1200、`player_game_stat` ≈ 30000；
③ 浏览器打开热图 / 雷达 / 排行榜均有数据。

</details>

### B. 🟡 测试覆盖过薄，CI 没真正跑测试

**现状**：后端只有 `StatCalculatorTest`（1 个纯函数工具类，约 130 行）；`HoopsApplicationTests.contextLoads` 被 `@Disabled` 跳过；
`.gitee-ci.yml` 的 `mvn test` 因此没测任何业务逻辑；前端零单测。

**做法**：
1. 把 `HoopsApplicationTests` 换成真正能跑的集成测试：H2（`MODE=MySQL`）或 Testcontainers MySQL。
   先验证 MyBatis 的 `LIMIT` / `GROUP BY` / `RANK() OVER` 等方言 SQL 在 H2 下能跑通，跑不通就上 Testcontainers。
2. 给至少 2 个 Service 写单测（Mockito `@MockBean` 隔离 DB/Redis），推荐：
   - `PlayerGameStatServiceImpl.summary`（聚合字段映射、seasonId 过滤、命中率除零）
   - `RankServiceImpl`（缓存命中 / 回源 / 失效）
3. 修 `.gitee-ci.yml`：跑 `mvn test`，去掉或替换 `@Disabled`，让 CI 红灯/绿灯反映真实结果。

**验收**：`cd big-event && mvn test` 全绿且 `Tests run: N` 有意义；CI 日志可见实际测试通过数。
（前端组件测试可选，不做不扣分。）

### C. 🟢 遗留 P1/P2 小毛病（详见第二节，位置列已给）

按「改动小 → 安全 → 用户可感知」排序，每组一个 commit：

| 顺序 | 编号 | 一句话 | 位置 |
|---|---|---|---|
| 1 | H1/H2/H3 | article/category 分页参数无默认值/边界校验 | 照抄 `TeamController` |
| 2 | G12 | 改密接口后端不校验长度 | `UserController.updatePassword` |
| 3 | B8 | 用户名可枚举（「用户名错误/密码错误」提示不同） | `UserController.login` |
| 4 | D16/D17 | 文章 `categoryId` 未校验归属 | `ArticleMapper`/`ArticleController` |
| 5 | E16+E13/E14+G6 | 无 logout；改密旧 token 不失效；Redis key 无命名空间（三件套一起做） | `UserController`/`LoginInterceptor` |
| 6 | H6 | 前端无 404 兜底路由（未知路径白屏） | `router/index.js` |
| 7 | H7 | REST 语义：业务失败全返回 200+code:1 | `GlobalExceptionHandler` |
| 8 | F10 | 上传文件永不清理 | `FileUploadController` |
| 9 | D22/D23 | 分页页码不重置/回退 | `ArticleManage.vue` |
| 10 | I4 | 「分类ID」列显示数字 | `ArticleManage.vue` |
| 11 | Q2 | 无 Maven Wrapper | `.gitignore` + 生成 mvnw |
| 12 | Q13/Q14 | 上传目录随 `user.dir` 漂移 | `WebConfig` + 配置化 |

**每条验收**：对照 `TEST-CHECKLIST.md` 同编号用例，改完跑那条。

### ⚠️ 面试兜底（不改代码，写进 README）

- 数据是程序生成的合成数据；`player_game_stat` 每人 ≈770 场，真实球员一赛季约 82 场，被问需如实说明。
- README 加「3 分钟演示脚本」：登录 → 排行榜 → 热图 → 雷达，附关键数字（排行榜 P95 200ms→2ms 等）。
