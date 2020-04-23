package com.sjx.gbck.cloudpos.common.cache;


import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@ConfigurationProperties("sjx.cache")
@Data
public class CacheCaffeineRedisProperties {

    private Set<String> cacheNames = new HashSet<>();

    /**
     * 是否存储空值，默认true，防止缓存穿透
     */
    private boolean cacheNullValues = true;

    /**
     * 是否动态根据cacheName创建Cache的实现，默认true
     */
    private boolean dynamic = true;


    /**
     * 缓存key的前缀
     */
    private String cachePrefix = "second";

    private Redis redis = new Redis();

    private Caffeine caffeine = new Caffeine();

    private LogSwitch logSwitch = new LogSwitch();


    @Data
    public class Redis {

        private final Long one_day_seconds = (long) 60 * 60 * 24;

        //private  final Long one_hour_seconds = (long) 60 * 60 ;

        /**
         * 全局过期时间，单位秒，默认不过期
         */
        private long defaultExpiration = one_day_seconds;

        /**
         * 每个cacheName的过期时间，单位秒，优先级比defaultExpiration高
         */
        private Map<String, Long> expires = new HashMap<>();

        /**
         * 清除本地缓存topic
         */
        private String clearLocalTopic = "cache:caffeine:redis:topic:evict";

    }

    @Data
    public class Caffeine {

        /**
         * 访问后过期时间，单位秒
         */
        private long expireAfterAccess = 600;

        /**
         * 写入后过期时间，单位秒
         */
        private long expireAfterWrite = 600;

        /**
         * 写入后刷新时间，单位秒
         */
        private long refreshAfterWrite = 60;

        /**
         * 初始化大小
         */
        private int initialCapacity = 2048;

        /**
         * 假设 一个对象 512 byte, 4个对象 1k,  20480 个对象  10m 内存左右
         * <p>
         * 最大缓存对象个数，超过此数量时之前放入的缓存将失效
         */
        private long maximumSize = 20480 ;

    }

    @Data
    public class LogSwitch {

        private boolean logHit = true;

        private boolean logHitValue = true;

        private boolean logWriteCache = true;

        private boolean logEvictCache = true;

        private boolean logClearCache = true;

        private boolean logMiss = true;

        private boolean logRefreshFirst = false;
    }
}