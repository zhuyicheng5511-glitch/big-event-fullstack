package com.example.bigevent.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.URL;
import java.time.LocalDateTime;

@Data
public class Article {
    private Integer id; // 主键ID

    @NotEmpty(message = "文章标题不能为空")
    @Pattern(regexp = "^\\S{1,30}$", message = "标题长度必须在1-30个字符之间，且不能包含空格")
    private String title; // 文章标题

    @NotEmpty(message = "文章内容不能为空")
    private String content; // 文章内容

    @NotEmpty(message = "文章封面不能为空")
    @URL(message = "封面地址格式不正确") // 校验必须是合法的 URL 格式
    private String coverImg; // 文章封面图片地址

    @Pattern(regexp = "^(已发布|草稿)$", message = "状态只能是已发布或草稿")
    private String state; // 文章状态：已发布/草稿

    @NotNull(message = "分类ID不能为空")
    private Integer categoryId; // 文章分类ID

    private Integer createUser; // 创建人ID

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 修改时间
}