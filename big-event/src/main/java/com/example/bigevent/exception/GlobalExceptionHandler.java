package com.example.bigevent.exception;

import com.example.bigevent.pojo.Result;
import jakarta.validation.ConstraintViolationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice // 声明这是一个全局的控制器增强类，它会自动将返回值转为 JSON
public class GlobalExceptionHandler {

    // 1. 拦截并处理：单参数校验失败异常（例如：注册时 @Pattern 限制的用户名长度不符）
    @ExceptionHandler(ConstraintViolationException.class)
    public Result handleConstraintViolationException(ConstraintViolationException e) {
        e.printStackTrace(); // 在控制台打印详细报错，方便开发时排查

        // 自动提取我们在注解（如 @Pattern(message="...")）中写好的自定义错误提示语
        String message = e.getConstraintViolations().iterator().next().getMessage();

        // 返回标准失败格式
        return Result.error(message);
    }

    // 2. 拦截并处理：JSON对象属性校验失败异常（例如：新增文章分类时，@NotEmpty 限制的分类名为空）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        e.printStackTrace();

        // 提取首个校验失败字段的默认错误提示语
        FieldError fieldError = e.getBindingResult().getFieldError();
        String message = fieldError != null ? fieldError.getDefaultMessage() : "参数校验失败";

        return Result.error(message);
    }

    // 3. 拦截并处理：所有未知的常规/系统异常（例如：数据库连接失败、空指针、SQL语法错误等）
    // 生产环境下的通用异常处理写法：
    @ExceptionHandler(Exception.class)
    public Result handleException(Exception e) {
        e.printStackTrace(); // 后台控制台依然打印详细报错，供开发排查

        // 隐藏真实的 SQL 报错，只给前端返回一句温馨提示，防止数据库泄露
        return Result.error("服务器繁忙，请稍候再试");
    }
}