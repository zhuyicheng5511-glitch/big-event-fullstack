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
     * 原实现直接 substring(lastIndexOf(\".\"))，文件名没有点时 lastIndexOf 返回 -1，
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
