package cmc.delta.global.config.security;

import java.util.List;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import cmc.delta.domain.user.application.port.in.UserStatusQuery;
import cmc.delta.global.config.security.handler.RestAccessDeniedHandler;
import cmc.delta.global.config.security.handler.RestAuthenticationEntryPoint;
import cmc.delta.global.config.security.jwt.JwtAuthenticationFilter;
import cmc.delta.global.config.security.jwt.JwtProperties;
import cmc.delta.global.config.security.jwt.OnboardingBlockFilter;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(JwtProperties.class)
public class SecurityConfig {

	private static final String[] PUBLIC_GET_PATHS = {
		"/oauth/**", "/favicon.ico", "/error", "/swagger-ui/**", "/v3/api-docs/**", "/actuator/health", "/health"
	};

	private static final String[] PUBLIC_POST_PATHS = {
		"/api/v1/auth/**"
	};

	private final JwtAuthenticationFilter jwtAuthenticationFilter;
	private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
	private final RestAccessDeniedHandler restAccessDeniedHandler;

	@Bean
	public FilterRegistrationBean<JwtAuthenticationFilter> jwtAuthenticationFilterRegistration(JwtAuthenticationFilter filter) {
		FilterRegistrationBean<JwtAuthenticationFilter> registration = new FilterRegistrationBean<>(filter);
		registration.setEnabled(false);
		return registration;
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http, UserStatusQuery userStatusQuery) throws Exception {
		OnboardingBlockFilter onboardingBlockFilter = new OnboardingBlockFilter(userStatusQuery);

		http.csrf(AbstractHttpConfigurer::disable)
			.formLogin(AbstractHttpConfigurer::disable)
			.httpBasic(AbstractHttpConfigurer::disable)
			.cors(Customizer.withDefaults())
			.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
			.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
			.addFilterAfter(onboardingBlockFilter, JwtAuthenticationFilter.class)
			.exceptionHandling(e -> e
				.authenticationEntryPoint(restAuthenticationEntryPoint)
				.accessDeniedHandler(restAccessDeniedHandler)
			)
			.authorizeHttpRequests(auth -> auth
				.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
				.requestMatchers(HttpMethod.GET, PUBLIC_GET_PATHS).permitAll()
				.requestMatchers(HttpMethod.POST, PUBLIC_POST_PATHS).permitAll()
				.anyRequest().authenticated()
			);

		return http.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

		CorsConfiguration appleCallback = new CorsConfiguration();
		appleCallback.setAllowCredentials(false);
		appleCallback.setAllowedOriginPatterns(List.of("*"));
		appleCallback.setAllowedMethods(List.of("POST", "OPTIONS"));
		appleCallback.setAllowedHeaders(List.of("*"));

		source.registerCorsConfiguration("/api/v1/auth/apple/**", appleCallback);
		source.registerCorsConfiguration("/api/v1/auth/apple", appleCallback);

		CorsConfiguration api = new CorsConfiguration();
		api.setAllowCredentials(true);
		api.setAllowedOriginPatterns(List.of(
			"https://deltasemo.cloud",
			"http://localhost:*"
		));
		api.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
		api.setAllowedHeaders(List.of("*"));
		api.setExposedHeaders(List.of("Authorization", "X-Refresh-Token", "X-Trace-Id"));

		source.registerCorsConfiguration("/**", api);
		return source;
	}

}
