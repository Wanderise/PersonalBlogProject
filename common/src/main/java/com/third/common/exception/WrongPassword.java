package com.third.common.exception;

import com.third.common.enumerate.RespondCode;

public class WrongPassword extends BaseExcpetion{
    public WrongPassword(RespondCode code){
        super(code);
    }
}
