package com.example.bigevent.controller;

import com.example.bigevent.pojo.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

@RestController
public class FileUploadController {

    @PostMapping("/upload")
    public Result<String> upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }

        try {
            // 1. 获取原始文件名 (例如: my_avatar.jpg)
            String originalFilename = file.getOriginalFilename();

            // 2. 提取文件后缀名 (例如: .jpg)
            String extName = originalFilename.substring(originalFilename.lastIndexOf("."));

            // 3. 利用 UUID 重新生成唯一文件名，防止重名文件覆盖
            String filename = UUID.randomUUID().toString() + extName;

            // 4. 定义本地存储路径（存放在当前项目根目录下的 uploads 文件夹内）
            String localPath = System.getProperty("user.dir") + "/uploads/";
            File dir = new File(localPath);
            if (!dir.exists()) {
                dir.mkdirs(); // 如果文件夹不存在，自动创建
            }

            // 5. 将文件保存到本地磁盘中
            file.transferTo(new File(localPath + filename));

            // 6. 返回前端可直接访问的虚拟静态资源 URL
            String fileUrl = "http://localhost:8080/uploads/" + filename;
            return Result.success(fileUrl);

        } catch (IOException e) {
            e.printStackTrace();
            return Result.error("文件上传失败");
        }
    }
}