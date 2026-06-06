package com.third.common.exception;

import com.third.common.enumerate.RespondCode;

public class NoSuchArticle extends BaseExcpetion {

    public NoSuchArticle(RespondCode code) {
        super(code);
    }

}
