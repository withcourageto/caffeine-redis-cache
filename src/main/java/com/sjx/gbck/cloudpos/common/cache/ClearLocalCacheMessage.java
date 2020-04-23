package com.sjx.gbck.cloudpos.common.cache;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClearLocalCacheMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @NotNull
    private String cacheName;

    private Object key;

}