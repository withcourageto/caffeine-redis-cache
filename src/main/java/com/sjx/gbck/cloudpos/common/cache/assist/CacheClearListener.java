package com.sjx.gbck.cloudpos.common.cache.assist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * @author lee
 **/
@Component
class CacheClearListener {

    @Autowired
    private CacheClear clear;

    @EventListener
    public void onClearEvent1(ClearCacheEvent event) {
        clear.clear(event.getName(), event.getKey());
    }

}
