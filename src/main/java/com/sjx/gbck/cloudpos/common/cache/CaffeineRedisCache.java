package com.sjx.gbck.cloudpos.common.cache;

import com.alibaba.fastjson.JSONObject;
import com.github.benmanes.caffeine.cache.Cache;
import com.sjx.gbck.cloudpos.common.cache.assist.BatchLoadCacheKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.support.AbstractValueAdaptingCache;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;


@Slf4j
public class CaffeineRedisCache extends AbstractValueAdaptingCache {

    private String name;

    private RedisTemplate<String, Object> redisTemplate;

    private Cache<String, Object> caffeineCache;

    private String cachePrefix;

    /**
     * seconds 默认redis 缓存过期时间
     */
    private long defaultExpiration = 60 * 60;

    private Map<String, Long> expires;

    private CacheCaffeineRedisProperties.LogSwitch logSwitch;

    /**
     * 发送清除事件
     */
    private String topic;

    /**
     * Create an {@code AbstractValueAdaptingCache} with the given setting.
     */
    CaffeineRedisCache(String name, RedisTemplate<String, Object> redisTemplate, Cache<String, Object> caffeineCache, CacheCaffeineRedisProperties cacheConfig) {

        super(cacheConfig.isCacheNullValues());
        if (StringUtils.isEmpty(name)) {
            throw new IllegalArgumentException("创建缓存名称不能为空白或者null");
        }

        this.logSwitch = cacheConfig.getLogSwitch();

        this.name = name;
        this.redisTemplate = redisTemplate;
        this.caffeineCache = caffeineCache;
        this.cachePrefix = cacheConfig.getCachePrefix();

        this.defaultExpiration = cacheConfig.getRedis().getDefaultExpiration();
        this.expires = cacheConfig.getRedis().getExpires();

        this.topic = cacheConfig.getRedis().getClearLocalTopic();
    }

    @Override
    protected Object lookup(Object key) {

        if (key instanceof BatchLoadCacheKey) {
            return batchLookUp((BatchLoadCacheKey) key);
        }


        Object caffeineStoreValue = lookupFromFirst(key);

        if (caffeineStoreValue != null) {
            return caffeineStoreValue;
        }
        Object redisStoreValue = lookupFromSecond(key);
        if (redisStoreValue != null) {
            caffeineCache.put(key.toString(), redisStoreValue);
        }
        return redisStoreValue;
    }

    private Object batchLookUp(BatchLoadCacheKey cacheKey) {

        // 直接从redis 获取, 无需从一级缓存获取
        List<String> keys = cacheKey.getKeys();

        if (keys == null) {
            throw new NullPointerException();
        }

        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        RedisSerializer<String> keySerializer = (RedisSerializer<String>) redisTemplate.getKeySerializer();

        return redisTemplate.executePipelined((RedisCallback) (con) -> {
            con.openPipeline();
            for (String key : keys) {
                con.get(keySerializer.serialize(key));
            }
            return null;
        });
    }

    private Object lookupFromFirst(Object key) {

        Object storeValue = caffeineCache.getIfPresent(key.toString());
        if (storeValue != null) {
            if (logSwitch.isLogHitValue()) {
                log.info("从caffeine获取到值：name:{}, key:{}, value:{}", name, key, JSONObject.toJSONString(storeValue));
            } else if (logSwitch.isLogHit()) {

                if (storeValue == NullVal.instance) {
                    log.info("从caffeine获取到 NullVal 值,name:{}, key:{}", name, key);
                } else {
                    log.info("从caffeine获取到值, name:{}, key: {}", name, key);
                }
            }
            return storeValue;
        }

        if (logSwitch.isLogMiss()) {
            log.info("未从caffeine获取到值,将从redis获取，name:{}, key: {}", name, key);
        }
        return null;
    }

    private Object lookupFromSecond(Object key) {

        String cacheKey = getRedisKey(key);
        Object storeValue = redisTemplate.opsForValue().get(cacheKey);

        if (storeValue != null) {

            if (logSwitch.isLogHitValue()) {
                log.info("从redis获取到值, name:{}, redisKey: {}, value:{}", name, cacheKey, JSONObject.toJSONString(storeValue));
            } else if (logSwitch.isLogHit()) {

                if (storeValue == NullVal.instance) {
                    log.info("从redis获取到 NullVal值, name:{}, redisKey:{}", name, cacheKey);
                } else {
                    log.info("从redis获取到值, name:{}, redisKey: {}", name, cacheKey);
                }
            }


            return storeValue;
        }

        if (logSwitch.isLogMiss()) {
            log.info("未从redis获取到值，name:{}, redisKey: {}", name, cacheKey);
        }
        return null;
    }


    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {

        Object storeValue = lookup(key);

        if (storeValue != null) {
            return (T) fromStoreValue(storeValue);
        }

        Object freshValue = loadCacheVal(key, valueLoader);
        put(key, freshValue);
        return (T) freshValue;
    }

    private Object loadCacheVal(Object key, Callable<?> valueLoader) {
        try {
            log.info("未命中缓存，使用 valueLoader 加载， key:{}", key);
            return valueLoader.call();
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object freshValue) {
        if (!super.isAllowNullValues() && freshValue == null) {
            this.evict(key);
            return;
        }
        long expire = getExpire();

        String redisKey = getRedisKey(key);
        Object storeVal = toStoreValue(freshValue);

        if (expire > 0) {
            redisTemplate.opsForValue().set(getRedisKey(key), storeVal, expire, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(redisKey, storeVal);
        }
        if (logSwitch.isLogWriteCache()) {
            log.info("写入redis缓存，name:{}, key:{}, expire:{}s", name, redisKey, expire);
        }

        caffeineCache.put(key.toString(), storeVal);
    }

    @Override
    public ValueWrapper putIfAbsent(Object key, Object value) {

        final String redisKey = getRedisKey(key);
        Object caffeineStoreValue = lookupFromFirst(key);
        if (caffeineStoreValue != null) {
            log.info("缓存在一级缓存中存在，无需更新， key:{} ", key);
            return toValueWrapper(caffeineStoreValue);
        }

        Object redisStoreValue = lookupFromSecond(key);

        if (redisStoreValue != null) {

            log.info("缓存在二级缓存中存在，只更新一级缓存， key:{} ", key);
            caffeineCache.put(key.toString(), toStoreValue(value));
            return toValueWrapper(redisStoreValue);
        }


        long expire = getExpire();
        if (expire > 0) {
            redisTemplate.opsForValue().set(redisKey, toStoreValue(value), expire, TimeUnit.SECONDS);
        } else {
            redisTemplate.opsForValue().set(redisKey, toStoreValue(value));
        }
        if (logSwitch.isLogWriteCache()) {
            log.info("写入二级缓存, key：{}", redisKey);
        }

        caffeineCache.put(key.toString(), toStoreValue(value));
        if (logSwitch.isLogWriteCache()) {
            log.info("写入一级缓存, key:{}", key.toString());
        }
        return toValueWrapper(null);
    }

    private long getExpire() {
        long expire = defaultExpiration;
        Long configExpire = expires.get(this.name);
        return configExpire == null ? expire : configExpire.intValue();
    }

    @Override
    public void evict(Object key) {
        if (logSwitch.isLogEvictCache()) {
            log.info("驱逐缓存, name:{}, key:{}", name, key);
        }
        try {
            log.info("驱逐二级缓存, name:{}, key:{}", name, key);
            redisTemplate.delete(getRedisKey(key));
        } finally {
            log.info("发送驱逐一级缓存事件, name:{}, key:{}", name, key);
//            caffeineCache.invalidate(key.toString());
            publish(new ClearLocalCacheMessage(this.name, key.toString()));
        }
    }

    @Override
    public void clear() {

        if (logSwitch.isLogClearCache()) {
            log.info("清空缓存, name:{}", name);
        }

        Set<String> keys = redisTemplate.keys(this.name.concat(":"));
        for (String key : keys) {
            redisTemplate.delete(key);
        }

        log.info("发送清空一级缓存事件, name:{}, key:{}", name, null);
        publish(new ClearLocalCacheMessage(this.name, null));
        //caffeineCache.invalidateAll();
    }

    void clearLocal(Object key) {
        if (logSwitch.isLogEvictCache()) {
            log.info("通过监听消息驱逐缓存本地缓存, name:{}, key: {}", name, key);
        }
        if (key == null) {
            caffeineCache.invalidateAll();
        } else {
            caffeineCache.invalidate(key.toString());
        }
    }

    private void publish(ClearLocalCacheMessage message) {
        redisTemplate.convertAndSend(topic, message);
    }


    @Override
    protected Object fromStoreValue(Object storeValue) {
        if (isAllowNullValues() && storeValue == NullVal.instance) {
            return null;
        }
        return storeValue;
    }

    @Override
    public Object toStoreValue(Object userValue) {
        if (isAllowNullValues() && userValue == null) {
            return NullVal.instance;
        }
        return userValue;
    }

    private String getRedisKey(Object key) {
        return RedisCacheKeyUtil.getKey(name, cachePrefix, key);
    }

}
