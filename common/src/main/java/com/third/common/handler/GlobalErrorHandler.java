package com.third.common.handler;

import com.third.common.exception.BaseExcpetion;
import com.third.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalErrorHandler {

    // 业务异常返回其携带的真实HTTP状态码（401/403/404等），而非统一400
    @ExceptionHandler(value = BaseExcpetion.class)
    public ResponseEntity<Result> handleException(BaseExcpetion e) {
      log.error("错误信息：{}", e.getMessage());
      return ResponseEntity.status(e.getCode()).body(Result.error(e.getMessage()));
    }
    // 兜底处理未预期的运行时异常，统一返回500
    @ExceptionHandler(value = Exception.class)
    public ResponseEntity<Result> handleUnknownException(Exception e) {
        log.error("未捕获异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error("服务器内部错误"));
    }

}
