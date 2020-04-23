package com.sjx.gbck.cloudpos.common.cache;


import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;

@Slf4j
public class ClearLocalCacheMessageListener implements MessageListener {

    private RedisTemplate<String, Object> redisTemplate;

    private CaffeineRedisCacheManager redisCaffeineCacheManager;

    public ClearLocalCacheMessageListener(RedisTemplate<String, Object> redisTemplate,
                                          CaffeineRedisCacheManager redisCaffeineCacheManager) {
        this.redisTemplate = redisTemplate;
        this.redisCaffeineCacheManager = redisCaffeineCacheManager;
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        ClearLocalCacheMessage clearLocalCacheMessage = (ClearLocalCacheMessage) redisTemplate.getValueSerializer().deserialize(message.getBody());
        log.debug("收到清除本地缓存消息, name:{}, key:{}", clearLocalCacheMessage.getCacheName(), clearLocalCacheMessage.getKey());
        redisCaffeineCacheManager.clearLocal(clearLocalCacheMessage.getCacheName(), clearLocalCacheMessage.getKey());
    }

}