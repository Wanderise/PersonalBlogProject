package com.third.common.handler;

import com.third.common.exception.BaseExcpetion;
import com.third.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalErrorHandler {

    @ExceptionHandler(value = BaseExcpetion.class)
    public Result handleException(BaseExcpetion e) {
      log.error("错误信息：" + e.getMessage());
      return Result.error(e.getMessage());
    }
    @ExceptionHandler(value = Exception.class)
    public Result handleUnknownException(Exception e) {
        log.error("未捕获异常", e);
        return Result.error("服务器内部错误");
    }

}
