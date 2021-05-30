package com.baidu.redis2.config;

import com.alibaba.fastjson.support.spring.FastJsonRedisSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

@Configuration
@EnableRedisHttpSession
public class RedisConfiguration {


//    @Bean
    RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
//        template.setKeySerializer(new StringRedisSerializer());
//        template.setHashValueSerializer(new GenericToStringSerializer<>(Object.class));
//        template.setValueSerializer(new GenericToStringSerializer<>(Object.class));
        return template;
    }

    @Bean
    RedisSerializer<Object> springSessionDefaultRedisSerializer() {
//        return new CustomRedisSerializer();
        return new FastJsonRedisSerializer(Object.class);
    }
}