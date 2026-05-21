package com.third.common.exception;

import com.third.common.enumerate.RespondCode;

public class UserCountNotExist extends BaseExcpetion {
    public UserCountNotExist(RespondCode code) {
        super(code);
    }
}
