package com.itheima.reggie.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 全局异常处理器
 */
@RestControllerAdvice
@ResponseBody
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(SQLIntegrityConstraintViolationException.class)
    public R<String> exceptionHandler(SQLIntegrityConstraintViolationException e){
        log.error(e.getMessage());

        if (e.getMessage().contains("Duplicate entry")) {
            String[] split = e.getMessage().split(" ");
            String msg = split[2] + "已存在";
            return R.error(msg);
        }

        return R.error("未知错误");
    }

    @ExceptionHandler(CustomException.class)
    public R<String> CustomExceptionHandler(CustomException e) {
        log.error(e.getMessage());

        return R.error(e.getMessage());
    }


}
