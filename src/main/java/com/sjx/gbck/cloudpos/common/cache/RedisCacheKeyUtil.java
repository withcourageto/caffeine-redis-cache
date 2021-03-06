package com.sjx.gbck.cloudpos.common.cache;


import org.springframework.util.StringUtils;

class RedisCacheKeyUtil {

    static String getKey(String name, String cachePrefix, Object key) {
        return name + ":" + (StringUtils.isEmpty(cachePrefix) ? key.toString() : cachePrefix.concat(":").concat(key.toString()));
    }
}
