package com.sjx.gbck.cloudpos.common.cache.assist;

import lombok.Builder;
import lombok.Data;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * @author lee
 **/
@Data
@Builder
public class ClearCacheEvent {

    @NonNull
    private String name;

    @Nullable
    private Object key;

}
