package com.sjx.gbck.cloudpos.common.cache.assist;

import lombok.Builder;
import lombok.Data;
import org.springframework.lang.NonNull;

import java.util.List;

/**
 * @author lee
 **/
@Data
@Builder
public class BatchLoadCacheKey<T> {

    @NonNull
    private String cacheName;

    @NonNull
    private List<String> keys;

    @NonNull
    private Class<T> valueType;

}
