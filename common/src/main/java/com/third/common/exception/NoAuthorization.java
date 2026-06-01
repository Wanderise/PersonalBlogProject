package com.third.common.exception;

import com.third.common.enumerate.RespondCode;

public class NoAuthorization extends BaseExcpetion {
    public NoAuthorization(RespondCode code) {
        super(code);
    }
}
