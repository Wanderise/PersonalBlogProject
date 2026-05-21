package com.third.common.exception;

import com.third.common.enumerate.RespondCode;

public class UserNameHasExist extends BaseExcpetion {
    public UserNameHasExist(RespondCode code) {
        super(code);
    }
}
