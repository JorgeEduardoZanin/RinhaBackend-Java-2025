package jorge.rinha.config;

import java.time.Duration;

import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.api.StatefulConnection;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;

import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean(destroyMethod = "shutdown")
    public ClientResources lettuceClientResources() {
        return DefaultClientResources.create();
    }

    @Bean
    public GenericObjectPoolConfig<StatefulConnection<?, ?>> lettucePoolConfig(
            @Value("${redis.pool.max-active:50}") int maxActive,
            @Value("${redis.pool.max-idle:20}") int maxIdle,
            @Value("${redis.pool.min-idle:5}") int minIdle,
            @Value("${redis.pool.max-wait-ms:2000}") long maxWaitMillis) {
        GenericObjectPoolConfig<StatefulConnection<?, ?>> cfg = new GenericObjectPoolConfig<>();
        cfg.setMaxTotal(maxActive);
        cfg.setMaxIdle(maxIdle);
        cfg.setMinIdle(minIdle);
        cfg.setMaxWaitMillis(maxWaitMillis);
        return cfg;
    }

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            ClientResources clientResources,
            GenericObjectPoolConfig<StatefulConnection<?, ?>> poolConfig,
            @Value("${spring.redis.host:localhost}") String host,
            @Value("${spring.redis.port:6379}") int port,
            @Value("${spring.redis.timeout:1000}") long commandTimeoutMs) {

        RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(host, port);

        ClientOptions clientOptions = ClientOptions.builder()
                .autoReconnect(true)
                .pingBeforeActivateConnection(true)
                .build();

        LettucePoolingClientConfiguration clientConfig = LettucePoolingClientConfiguration.builder()
                .clientResources(clientResources)
                .commandTimeout(Duration.ofMillis(commandTimeoutMs))
                .shutdownTimeout(Duration.ofMillis(100))
                .poolConfig(poolConfig)
                .clientOptions(clientOptions)
                .build();

        LettuceConnectionFactory factory = new LettuceConnectionFactory(standalone, clientConfig);
        factory.afterPropertiesSet();
        return factory;
    }

    @Bean
    @Primary
    public RedisTemplate<String, Integer> redisTemplateInt(LettuceConnectionFactory cf) {
        RedisTemplate<String, Integer> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);
        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setValueSerializer(new GenericToStringSerializer<>(Integer.class));
        tpl.afterPropertiesSet();
        return tpl;
    }


    @Bean
    public RedisTemplate<String, Long> redisTemplateLong(LettuceConnectionFactory cf) {
        RedisTemplate<String, Long> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(cf);
        tpl.setKeySerializer(new StringRedisSerializer());
        tpl.setValueSerializer(new GenericToStringSerializer<>(Long.class));
        tpl.afterPropertiesSet();
        return tpl;
    }
}