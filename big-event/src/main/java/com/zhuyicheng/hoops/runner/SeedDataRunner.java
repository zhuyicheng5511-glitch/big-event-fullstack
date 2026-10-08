package com.zhuyicheng.hoops.runner;

import com.zhuyicheng.hoops.mapper.PlayerMapper;
import com.zhuyicheng.hoops.mapper.SeasonMapper;
import com.zhuyicheng.hoops.mapper.TeamMapper;
import com.zhuyicheng.hoops.pojo.ImportError;
import com.zhuyicheng.hoops.pojo.ImportResult;
import com.zhuyicheng.hoops.service.ImportService;
import com.zhuyicheng.hoops.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * 种子数据导入 Runner —— 把打包进 jar 的 5 个 CSV 灌进全新的环境
 * （Docker initdb 刚建完表时是【全空的，连用户都没有】），由启动参数 --import-seed 触发。
 *
 * 流水线顺序（必须显式 @Order：Spring 对多个 CommandLineRunner 的默认顺序不保证，
 * 而后两者依赖 season/team/player 已存在，跑到 seed 前面会打印 missing data 静默跳过）：
 *   @Order(10) seed CSV → @Order(20) game/stat 补数 → @Order(30) shot 补数
 *
 * 幂等：season/team/player 任一表有数据就视为「种子已导入」，整批跳过。
 * 不能靠导入服务逐行报「已存在」——那会触发全有或全无回滚并把跳过变成失败。
 * 因此 seed 容器可以安全重跑（docker compose up 每次都会再启动它一次）。
 */
@Component
@Order(10)
public class SeedDataRunner implements CommandLineRunner {

    @Autowired
    private ImportService importService;

    @Autowired
    private UserService userService;

    @Autowired
    private SeasonMapper seasonMapper;

    @Autowired
    private TeamMapper teamMapper;

    @Autowired
    private PlayerMapper playerMapper;

    /** {导入实体名, classpath 路径}，顺序即外键依赖顺序：game 引用 season/team，stat 引用 game/player */
    private static final List<String[]> SEEDS = List.of(
            new String[]{"season", "seed/seasons.csv"},
            new String[]{"team", "seed/teams.csv"},
            new String[]{"player", "seed/players.csv"},
            new String[]{"game", "seed/games.csv"},
            new String[]{"player-game-stat", "seed/player_game_stats.csv"}
    );

    /** 演示账号：README 与验收清单都用它登录；密码经 UserService.register 的 BCrypt 路径入库，仓库不含哈希 */
    private static final String DEMO_USERNAME = "paulgeorge8";
    private static final String DEMO_PASSWORD = "asdfgh";

    @Override
    public void run(String... args) {
        if (!hasArg(args, "--import-seed")) {
            return;
        }

        if (seedAlreadyImported()) {
            System.out.println("[SeedData] 检测到已有种子数据，跳过 CSV 导入");
        } else {
            for (String[] seed : SEEDS) {
                importOne(seed[0], seed[1]);
            }
        }

        ensureDemoUser();
    }

    /**
     * 导入单个实体；失败（failureRows > 0）直接抛异常终止启动。
     * 种子文件是随仓库入库的，导不进去说明环境或代码有问题 —— 必须大声失败，
     * 让 seed 容器以非零码退出，而不是带着空表继续启动去「演示」。
     */
    private void importOne(String entity, String path) {
        Resource resource = new ClassPathResource(path);
        if (!resource.exists()) {
            throw new IllegalStateException("种子文件缺失: " + path);
        }

        ImportResult result = importService.importCsv(entity, new ClassPathMultipartFile(path, resource));
        Integer failure = result.getFailureRows();
        if (failure == null || failure > 0) {
            StringBuilder sb = new StringBuilder();
            for (ImportError e : result.getErrors()) {
                sb.append(String.format("%n  第 %d 行 [%s] %s", e.getRowNumber(), e.getField(), e.getReason()));
            }
            throw new IllegalStateException("种子导入失败 (" + entity + "):" + sb);
        }
        System.out.println("[SeedData] " + entity + " 导入 " + result.getSuccessRows() + " 行");
    }

    private boolean seedAlreadyImported() {
        return !seasonMapper.list(null).isEmpty()
                || !teamMapper.list(null).isEmpty()
                || !playerMapper.list(null, null).isEmpty();
    }

    private void ensureDemoUser() {
        if (userService.findByUsername(DEMO_USERNAME) != null) {
            return;
        }
        userService.register(DEMO_USERNAME, DEMO_PASSWORD);
        System.out.println("[SeedData] 已创建演示账号 " + DEMO_USERNAME);
    }

    private boolean hasArg(String[] args, String target) {
        if (args == null) {
            return false;
        }
        for (String arg : args) {
            if (target.equals(arg)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 把 classpath 资源适配成 MultipartFile —— ImportService 的入口按 HTTP 上传设计，
     * 复用它的校验与事务，而不是再开一个 InputStream 入口。
     * 实际实现只消费 isEmpty() 与 getInputStream()，其余方法按契约补齐。
     */
    private record ClassPathMultipartFile(String filename, Resource resource) implements MultipartFile {

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return filename;
        }

        @Override
        public String getContentType() {
            return "text/csv";
        }

        @Override
        public boolean isEmpty() {
            try {
                return resource.contentLength() == 0;
            } catch (IOException e) {
                return true;
            }
        }

        @Override
        public long getSize() {
            try {
                return resource.contentLength();
            } catch (IOException e) {
                return 0;
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            try (InputStream in = getInputStream()) {
                return in.readAllBytes();
            }
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return resource.getInputStream();
        }

        @Override
        public void transferTo(File dest) throws IOException, IllegalStateException {
            try (InputStream in = getInputStream()) {
                Files.copy(in, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }

        @Override
        public void transferTo(Path dest) throws IOException, IllegalStateException {
            try (InputStream in = getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
