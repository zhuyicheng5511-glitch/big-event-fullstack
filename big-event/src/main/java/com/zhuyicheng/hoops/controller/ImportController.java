package com.zhuyicheng.hoops.controller;

import com.zhuyicheng.hoops.pojo.ImportResult;
import com.zhuyicheng.hoops.pojo.Result;
import com.zhuyicheng.hoops.service.ImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/import")
public class ImportController {

    @Autowired
    private ImportService importService;

    @PostMapping("/{entity}")
    public Result<ImportResult> importCsv(@PathVariable String entity, @RequestParam("file") MultipartFile file) {
        ImportResult result = importService.importCsv(entity, file);
        if (result.getFailureRows() > 0) {
            return new Result<>(1, "导入失败，存在 " + result.getFailureRows() + " 行错误", result);
        }
        return Result.success(result);
    }
}
