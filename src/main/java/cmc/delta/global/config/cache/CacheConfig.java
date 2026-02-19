package cmc.delta.global.config.cache;

import cmc.delta.global.cache.CacheNames;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	public CacheManager cacheManager(
		RedisConnectionFactory connectionFactory,
		ObjectMapper objectMapper) {
		ObjectMapper cacheObjectMapper = buildCacheObjectMapper(objectMapper);
		GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

		RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
			.disableCachingNullValues()
			.serializeKeysWith(
				RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(
				RedisSerializationContext.SerializationPair.fromSerializer(
					valueSerializer));

		RedisCacheConfiguration wrongAnswerPages = base.entryTtl(Duration.ofMinutes(5));

		return RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(base)
			.withCacheConfiguration(CacheNames.WRONG_ANSWER_PAGES, wrongAnswerPages)
			.transactionAware()
			.build();
	}

	private ObjectMapper buildCacheObjectMapper(ObjectMapper base) {
		PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
			.allowIfSubType("cmc.delta.")
			.allowIfSubType("java.lang.")
			.allowIfSubType("java.math.")
			.allowIfSubType("java.time.")
			.allowIfSubType("java.util.")
			.build();

		ObjectMapper mapper = base.copy();
		mapper.registerModule(new JavaTimeModule());
		mapper.activateDefaultTyping(ptv, ObjectMapper.DefaultTyping.EVERYTHING, JsonTypeInfo.As.PROPERTY);
		return mapper;
	}
}
