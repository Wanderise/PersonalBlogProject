package com.third.common.exception;

import com.third.common.enumerate.RespondCode;

public class BaseExcpetion extends RuntimeException {
    private final Integer code;
    public BaseExcpetion(RespondCode code){
        super(code.getMessage());
        this.code = code.getCode();
    }
    public BaseExcpetion(RespondCode code, String message){
        super(message);
        this.code = code.getCode();
    }
}
