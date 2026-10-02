package com.zhuyicheng.hoops.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportError {
    private int rowNumber;   // 数据行号（从 2 开始，表头为第 1 行）
    private String field;    // 出错字段
    private String reason;   // 失败原因
}
