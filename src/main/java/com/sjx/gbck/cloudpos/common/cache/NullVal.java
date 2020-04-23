package com.sjx.gbck.cloudpos.common.cache;

import com.fasterxml.jackson.annotation.JsonCreator;

public class NullVal {

    static final NullVal instance = new NullVal();

    private NullVal() {
    }

    @JsonCreator
    public static NullVal getInstance() {
        return instance;
    }
}
