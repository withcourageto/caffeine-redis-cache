package com.sjx.gbck.cloudpos.common.cache.assist;

import java.util.List;
import java.util.function.Function;

public interface CacheAssistService {


    <V> BatchLoadCacheResult<String, V> batchLoadFromCache(BatchLoadCacheKey<V> req, Function<V, String> keyExtractor);

    /**
     * 批量获取缓存
     *
     * @param cacheName 缓存名称
     * @param keys      键
     * @param type      缓存值类型Class对象
     * @param <K>       缓存键传入类型
     * @param <V>       缓存类型
     * @return 命中的缓存对象，未命中的缓存键
     */
    <K, V> BatchLoadCacheResult<K, V> batchLoadFromCache(String cacheName, List<K> keys, Class<V> type);


    /**
     * 异步执行
     *
     * @param cacheName    缓存名称
     * @param values       缓存值
     * @param keyGenerator 缓存键生成策略
     * @param <V>          缓存对象类型
     */
    <V> void batchPutToCache(String cacheName, List<V> values, Function<V, Object> keyGenerator);
}
