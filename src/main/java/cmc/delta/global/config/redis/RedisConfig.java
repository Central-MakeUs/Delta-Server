package cmc.delta.global.config.redis;

import java.time.Duration;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.util.StringUtils;

@Configuration
public class RedisConfig {

	@Bean
	public RedisConnectionFactory redisConnectionFactory(RedisProperties props) {
		RedisStandaloneConfiguration standalone = new RedisStandaloneConfiguration(props.getHost(), props.getPort());
		if (StringUtils.hasText(props.getPassword())) {
			standalone.setPassword(RedisPassword.of(props.getPassword()));
		}
		Duration timeout = (props.getTimeout() != null) ? props.getTimeout() : Duration.ofSeconds(2);

		LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder().commandTimeout(timeout).build();

		return new LettuceConnectionFactory(standalone, clientConfig);
	}

	@Bean
	public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
		return new StringRedisTemplate(connectionFactory);
	}
}
