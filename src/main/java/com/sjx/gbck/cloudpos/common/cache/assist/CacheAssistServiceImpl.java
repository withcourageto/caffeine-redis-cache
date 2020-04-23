package com.sjx.gbck.cloudpos.common.cache.assist;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
@Slf4j
class CacheAssistServiceImpl implements CacheAssistService {

    @Autowired
    private CacheManager cacheManager;

    @Override
    public <V> BatchLoadCacheResult<String, V> batchLoadFromCache(BatchLoadCacheKey<V> req, Function<V, String> keyExtractor) {

        String cacheName = req.getCacheName();
        List<String> keys = req.getKeys();

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("未获取到缓存，不能从缓存加载数据，请查看是否开启动态创建缓存，或者缓存名称是否存在配置文件中，cacheName:{}", cacheName);
            return new BatchLoadCacheResult<>(Collections.emptyList(), keys);
        }

        Cache.ValueWrapper valueWrapper = cache.get(req);
        BatchLoadCacheResult.BatchLoadCacheResultBuilder<String, V> builder = BatchLoadCacheResult.builder();

        if (valueWrapper == null) {
            return builder
                    .missKeys(keys)
                    .build();
        }

        Object object = valueWrapper.get();
        if (object == null) {
            return builder
                    .missKeys(keys)
                    .build();
        }

        List<V> hits = ((List<V>) object).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        List<String> hitKeys = hits.stream()
                .map(keyExtractor)
                .collect(Collectors.toList());

        List<String> missKeys = new ArrayList<>(keys);
        missKeys.removeAll(hitKeys);

        return builder
                .hits(hits)
                .missKeys(missKeys)
                .build();
    }

    @Override
    public <K, V> BatchLoadCacheResult<K, V> batchLoadFromCache(String cacheName, List<K> keys, Class<V> type) {

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("未获取到缓存，不能从缓存加载数据，请查看是否开启动态创建缓存，或者缓存名称是否存在配置文件中，cacheName:{}", cacheName);
            return new BatchLoadCacheResult<>(Collections.emptyList(), keys);
        }

        List<V> hits = new ArrayList<>(keys.size() / 2);
        List<K> missKeys = new ArrayList<>(keys.size() / 2);
        for (K key : keys) {
            V value = cache.get(key, type);
            if (value != null) {
                hits.add(value);
            } else {
                missKeys.add(key);
            }
        }
        return new BatchLoadCacheResult<>(hits, missKeys);
    }


    @Override
    @Async
    public <V> void batchPutToCache(String cacheName, List<V> values, Function<V, Object> keyGenerator) {

        if (StringUtils.isEmpty(cacheName)) {
            throw new IllegalArgumentException("批量添加缓存，缓存名称不能为null或者空白");
        }

        if (keyGenerator == null) {
            throw new NullPointerException("批量添加缓存，keyGenerator 参数不能为null");
        }

        if (values.isEmpty()) {
            log.info("需要批量缓存的数据为空，无需任何操作");
            return;
        }

        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("未获取到缓存，不能将数据存入缓存，请查看是否开启动态创建缓存，或者缓存名称是否存在配置文件中，cacheName:{}", cacheName);
            return;
        }

        for (V value : values) {
            cache.putIfAbsent(keyGenerator.apply(value), value);
        }
    }


}
