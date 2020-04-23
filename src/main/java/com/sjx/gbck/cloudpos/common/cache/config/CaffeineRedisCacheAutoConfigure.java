package com.sjx.gbck.cloudpos.common.cache.config;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.sjx.gbck.cloudpos.common.cache.CacheCaffeineRedisProperties;
import com.sjx.gbck.cloudpos.common.cache.CaffeineRedisCacheManager;
import com.sjx.gbck.cloudpos.common.cache.ClearLocalCacheMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@AutoConfigureAfter(RedisAutoConfiguration.class)
@ConditionalOnBean(RedisConnectionFactory.class)
@EnableConfigurationProperties(CacheCaffeineRedisProperties.class)
public class CaffeineRedisCacheAutoConfigure {

    @Autowired
    private CacheCaffeineRedisProperties config;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    private static final String redis_template_bean_name = "cacheRedisTemplate";


    @Bean
    public Jackson2JsonRedisSerializer cacheValueRedisSerializer() {
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<>(Object.class);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);

        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        return jackson2JsonRedisSerializer;
    }


    @Bean(name = redis_template_bean_name)
    @ConditionalOnMissingBean(name = redis_template_bean_name)
    @ConditionalOnBean(RedisConnectionFactory.class)
    public RedisTemplate<String, Object> cacheRedisTemplate() {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);

        // 设置value的序列化规则和 key的序列化规则
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(cacheValueRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }

    @Bean
    @ConditionalOnBean(RedisTemplate.class)
    @Autowired
    public CaffeineRedisCacheManager cacheManager(@Qualifier(redis_template_bean_name) RedisTemplate<String, Object> cacheRedisTemplate) {

        return new CaffeineRedisCacheManager(config, cacheRedisTemplate);
    }


    @Bean
    @ConditionalOnBean(value = {RedisTemplate.class, CaffeineRedisCacheManager.class}, name = redis_template_bean_name)
    @Autowired
    public RedisMessageListenerContainer redisMessageListenerContainer(@Qualifier(redis_template_bean_name) RedisTemplate<String, Object> redisTemplate,
                                                                       CaffeineRedisCacheManager redisCaffeineCacheManager) {

        RedisMessageListenerContainer redisMessageListenerContainer = new RedisMessageListenerContainer();
        redisMessageListenerContainer.setConnectionFactory(redisTemplate.getConnectionFactory());
        ClearLocalCacheMessageListener clearLocalCacheMessageListener = new ClearLocalCacheMessageListener(redisTemplate, redisCaffeineCacheManager);
        redisMessageListenerContainer.addMessageListener(clearLocalCacheMessageListener, new ChannelTopic(config.getRedis().getClearLocalTopic()));
        return redisMessageListenerContainer;
    }


}
