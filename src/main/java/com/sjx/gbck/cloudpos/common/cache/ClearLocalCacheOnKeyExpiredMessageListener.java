package com.sjx.gbck.cloudpos.common.cache;


import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisKeyExpiredEvent;

@Slf4j
public class ClearLocalCacheOnKeyExpiredMessageListener {


    @EventListener
    public void onKeyExpired(RedisKeyExpiredEvent event) {
        // TODO, 处理过期事件
    }


}