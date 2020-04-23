package com.sjx.gbck.cloudpos.common.cache.assist;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

/**
 * @param <K> 键类型
 * @param <V> 值类型
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BatchLoadCacheResult<K, V> {


    private List<V> hits = Collections.emptyList();

    private List<K> missKeys = Collections.emptyList();
}
