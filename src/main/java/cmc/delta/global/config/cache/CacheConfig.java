package cmc.delta.global.config.cache;

import cmc.delta.global.cache.CacheNames;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
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

	private static final Map<String, Duration> CACHE_TTLS = buildCacheTtls();

	private static Map<String, Duration> buildCacheTtls() {
		Map<String, Duration> ttls = new LinkedHashMap<>();
		ttls.put(CacheNames.WRONG_ANSWER_PAGES, Duration.ofMinutes(5));
		ttls.put(CacheNames.PROBLEM_STATS_UNITS, Duration.ofMinutes(3));
		ttls.put(CacheNames.PROBLEM_STATS_TYPES, Duration.ofMinutes(3));
		ttls.put(CacheNames.PROBLEM_STATS_MONTHLY, Duration.ofMinutes(10));
		ttls.put(CacheNames.PRO_CHECKOUT_STATS, Duration.ofMinutes(1));
		ttls.put(CacheNames.CUSTOM_PROBLEM_TYPES, Duration.ofHours(1));
		return ttls;
	}

	@Bean
	public CacheManager cacheManager(
		RedisConnectionFactory connectionFactory,
		ObjectMapper objectMapper) {
		RedisCacheConfiguration base = buildBaseCacheConfiguration(objectMapper);

		RedisCacheManager.RedisCacheManagerBuilder builder = RedisCacheManager.builder(connectionFactory)
			.cacheDefaults(base);
		CACHE_TTLS.forEach((cacheName, ttl) -> builder.withCacheConfiguration(cacheName, base.entryTtl(ttl)));

		return builder
			.transactionAware()
			.enableStatistics()
			.build();
	}

	private RedisCacheConfiguration buildBaseCacheConfiguration(ObjectMapper objectMapper) {
		ObjectMapper cacheObjectMapper = buildCacheObjectMapper(objectMapper);
		GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(cacheObjectMapper);

		return RedisCacheConfiguration.defaultCacheConfig()
			.disableCachingNullValues()
			.serializeKeysWith(
				RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
			.serializeValuesWith(
				RedisSerializationContext.SerializationPair.fromSerializer(
					valueSerializer));
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
