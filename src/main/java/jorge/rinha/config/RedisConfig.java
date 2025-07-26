package jorge.rinha.config;

import java.math.BigDecimal;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericToStringSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;



@Configuration
public class RedisConfig {

    @Bean
    public LettuceConnectionFactory redisConnectionFactory(
            @Value("${spring.redis.host:localhost}") String host,
            @Value("${spring.redis.port:6379}") int port) {
        RedisStandaloneConfiguration cfg = new RedisStandaloneConfiguration(host, port);
        return new LettuceConnectionFactory(cfg);
    }

    @Bean
    public RedisTemplate<String, Integer> redisTemplateInt(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, Integer> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySer = new StringRedisSerializer();
        GenericToStringSerializer<Integer> intSer = new GenericToStringSerializer<>(Integer.class);

        tpl.setKeySerializer(keySer);
        tpl.setValueSerializer(intSer);
        tpl.setHashKeySerializer(keySer);
        tpl.setHashValueSerializer(intSer);
        tpl.afterPropertiesSet();
        return tpl;
    }

    @Bean
    public RedisTemplate<String, BigDecimal> redisTemplateBigDecimal(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, BigDecimal> tpl = new RedisTemplate<>();
        tpl.setConnectionFactory(connectionFactory);

        StringRedisSerializer keySer = new StringRedisSerializer();
        GenericToStringSerializer<BigDecimal> bigDecimalSer = new GenericToStringSerializer<>(BigDecimal.class);

        tpl.setKeySerializer(keySer);
        tpl.setValueSerializer(bigDecimalSer);
        tpl.setHashKeySerializer(keySer);
        tpl.setHashValueSerializer(bigDecimalSer);
        tpl.afterPropertiesSet();
        return tpl;
    }
}

