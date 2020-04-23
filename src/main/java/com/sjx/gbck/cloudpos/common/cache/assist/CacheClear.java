package com.sjx.gbck.cloudpos.common.cache.assist;


import java.util.List;

public interface CacheClear {

    void clear(String name, Object key);

    void clearAll();

    List<String> cacheNames();

}
