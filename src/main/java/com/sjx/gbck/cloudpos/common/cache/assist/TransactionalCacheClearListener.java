package com.sjx.gbck.cloudpos.common.cache.assist;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionalEventListenerFactory;

/**
 * @author lee
 **/
@Component
@ConditionalOnBean(TransactionalEventListenerFactory.class)
class TransactionalCacheClearListener {

    @Autowired
    private CacheClear clear;

    @TransactionalEventListener
    public void onClearEvent1(ClearCacheEvent event) {
        clear.clear(event.getName(), event.getKey());
    }

}
