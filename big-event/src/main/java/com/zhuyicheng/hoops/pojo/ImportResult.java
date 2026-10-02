package com.zhuyicheng.hoops.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportResult {
    private Integer totalRows;
    private Integer successRows;
    private Integer failureRows;
    private List<ImportError> errors;

    public ImportResult(Integer totalRows) {
        this.totalRows = totalRows;
        this.successRows = 0;
        this.failureRows = 0;
        this.errors = new ArrayList<>();
    }

    public static ImportResult empty() {
        return new ImportResult(0, 0, 0, new ArrayList<>());
    }
}
