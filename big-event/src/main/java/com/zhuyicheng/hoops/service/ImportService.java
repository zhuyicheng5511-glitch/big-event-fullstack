package com.zhuyicheng.hoops.service;

import com.zhuyicheng.hoops.pojo.ImportResult;
import org.springframework.web.multipart.MultipartFile;

public interface ImportService {
    ImportResult importCsv(String entity, MultipartFile file);
}
