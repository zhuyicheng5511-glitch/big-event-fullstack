package com.zhuyicheng.hoops;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class HoopsApplicationTests {

    @Test
    @Disabled("需要本地 MySQL/Redis 服务，CI 环境跳过")
    void contextLoads() {
    }

}
