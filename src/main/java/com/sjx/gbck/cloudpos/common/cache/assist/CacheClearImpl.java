package com.sjx.gbck.cloudpos.common.cache.assist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Service
class CacheClearImpl implements CacheClear {

    private final CacheManager cacheManager;

    @Autowired
    public CacheClearImpl(CacheManager cacheManager) {
        this.cacheManager = cacheManager;
    }

    @Override
    public void clear(String name, Object key) {

        Cache cache = cacheManager.getCache(name);
        if (cache == null) {
            return;
        }

        if (key == null) {
            cache.clear();
        } else {
            cache.evict(key.toString());
        }
    }

    @Override
    public void clearAll() {

        Collection<String> cacheNames = cacheManager.getCacheNames();

        for (String cacheName : cacheNames) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }
    }

    @Override
    public List<String> cacheNames() {
        return new ArrayList<>(cacheManager.getCacheNames());
    }

}
