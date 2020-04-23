
package com.sjx.gbck.cloudpos.common.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CaffeineRedisCacheManager implements CacheManager {

    private ConcurrentMap<String, CaffeineRedisCache> cacheMap = new ConcurrentHashMap<>();

    private CacheCaffeineRedisProperties cacheConfig;

    private CacheCaffeineRedisProperties.LogSwitch logSwitch;

    private RedisTemplate<String, Object> redisTemplate;

    private boolean dynamic = true;

    // 如果是非动态生成缓存，需要从这个地方配置缓存名称
    private Set<String> cacheNames;

    public CaffeineRedisCacheManager(CacheCaffeineRedisProperties config, RedisTemplate<String, Object> redisTemplate) {
        super();
        this.cacheConfig = config;
        this.logSwitch = cacheConfig.getLogSwitch();
        this.redisTemplate = redisTemplate;
        this.dynamic = cacheConfig.isDynamic();
        this.cacheNames = cacheConfig.getCacheNames() == null ? new HashSet<>() : cacheConfig.getCacheNames();
    }

    @Override
    public CaffeineRedisCache getCache(String name) {
        CaffeineRedisCache cache = cacheMap.get(name);
        if (cache != null) {
            return cache;
        }
        if (!dynamic && !cacheNames.contains(name)) {
            return null;
        }

        cache = new CaffeineRedisCache(name, redisTemplate, newCaffeineCache(name), cacheConfig);
        CaffeineRedisCache oldCache = cacheMap.putIfAbsent(name, cache);
        log.debug("create cache instance, the cache name is : {}", name);
        return oldCache == null ? cache : oldCache;
    }


    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }


    private LoadingCache<String, Object> newCaffeineCache(String name) {
        Caffeine<Object, Object> cacheBuilder = Caffeine.newBuilder();

        if (cacheConfig.getCaffeine().getExpireAfterAccess() > 0) {
            cacheBuilder.expireAfterAccess(cacheConfig.getCaffeine().getExpireAfterAccess(), TimeUnit.SECONDS);
        }
        if (cacheConfig.getCaffeine().getExpireAfterWrite() > 0) {
            cacheBuilder.expireAfterWrite(cacheConfig.getCaffeine().getExpireAfterWrite(), TimeUnit.SECONDS);
        }
        if (cacheConfig.getCaffeine().getInitialCapacity() > 0) {
            cacheBuilder.initialCapacity(cacheConfig.getCaffeine().getInitialCapacity());
        }
        if (cacheConfig.getCaffeine().getMaximumSize() > 0) {
            cacheBuilder.maximumSize(cacheConfig.getCaffeine().getMaximumSize());
        }
        if (cacheConfig.getCaffeine().getRefreshAfterWrite() > 0) {
            cacheBuilder.refreshAfterWrite(cacheConfig.getCaffeine().getRefreshAfterWrite(), TimeUnit.SECONDS);
        }
        return cacheBuilder.build(key -> {

            String redisCacheKey = getKey(name, key);
            Object redisVal = redisTemplate.opsForValue().get(redisCacheKey);

            if (logSwitch.isLogRefreshFirst()) {
                log.info("从caffeine加载一级缓存获取的信息：name:{}, redisKey:{}, value:{}", name, redisCacheKey, redisVal);
            }

            if (redisVal == NullVal.instance) {
                return null;
            }

            return redisVal;
        });
    }

    private String getKey(String name, Object key) {
        return RedisCacheKeyUtil.getKey(name, cacheConfig.getCachePrefix(), key);
    }


    void clearLocal(String cacheName, Object key) {
        CaffeineRedisCache cache = cacheMap.get(cacheName);
        if (cache == null) {
            return;
        }

        cache.clearLocal(key);
    }

}