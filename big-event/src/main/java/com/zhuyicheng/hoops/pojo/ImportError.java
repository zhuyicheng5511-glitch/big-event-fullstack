package com.zhuyicheng.hoops.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportError {
    private int rowNumber;   // CSV 中的物理行号（表头为第 1 行，首条数据为第 2 行）
    private String field;    // 出错字段
    private String reason;   // 失败原因
}
