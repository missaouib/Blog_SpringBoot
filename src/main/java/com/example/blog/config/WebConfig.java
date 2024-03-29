package com.example.blog.config;

import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.support.config.FastJsonConfig;
import com.alibaba.fastjson.support.spring.FastJsonHttpMessageConverter;
import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.example.blog.exception.AccessDeniedHandlerImpl;
import com.example.blog.exception.AuthenticationEntryPointImpl;
import com.example.blog.filter.JWTAuthTokenFilter;
import com.example.blog.filter.StrangerFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
	/**
	 *  分页插件
	 */
	@Bean
	public MybatisPlusInterceptor mybatisPlusInterceptor() {
		MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
		interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
		return interceptor;
	}

	/**
	 *  配置redis
	 */
	@Bean(name = "template")
	public RedisTemplate<String, Object> template(RedisConnectionFactory factory) {
		RedisTemplate<String, Object> template = new RedisTemplate<>();
		template.setConnectionFactory(factory);
		FastJson2JsonRedisSerializer<Object> fastJsonRedisSerializer = new FastJson2JsonRedisSerializer<>(Object.class);
		StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
		template.setKeySerializer(stringRedisSerializer);
		template.setHashKeySerializer(stringRedisSerializer);
		template.setValueSerializer(fastJsonRedisSerializer);
		template.setHashValueSerializer(fastJsonRedisSerializer);
		template.afterPropertiesSet();
		return template;
	}

	/**
	 *  使用fastjson替换jackson2
	 */
	@Override
	public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
		FastJsonHttpMessageConverter converter = new FastJsonHttpMessageConverter();
		FastJsonConfig config = new FastJsonConfig();
		config.setDateFormat("yyyy-MM-dd");
		config.setCharset(StandardCharsets.UTF_8);
		config.setSerializerFeatures(
				SerializerFeature.WriteClassName,
				SerializerFeature.PrettyFormat,
				SerializerFeature.WriteNullListAsEmpty,
				SerializerFeature.WriteNullStringAsEmpty
		);
		List<MediaType> mediaTypeList = new ArrayList<>();
		mediaTypeList.add(MediaType.APPLICATION_JSON);
		converter.setSupportedMediaTypes(mediaTypeList);
		converter.setFastJsonConfig(config);
		converters.add(0,converter);
	}

	/**
	 *  解决跨域问题
	 */
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**")
				.allowedHeaders("*")
				.maxAge(3600)
				.allowedMethods("*")
				.allowCredentials(true)
				.allowedOriginPatterns("*");

	}

	/**
	 * spring security 相关配置
	 */
	@Configuration
	@EnableWebSecurity
	@EnableGlobalMethodSecurity(prePostEnabled = true)
	public static class SecurityConfig extends WebSecurityConfigurerAdapter{

		@Resource(name = "JWTAuthTokenFilter")
		private JWTAuthTokenFilter jwtAuthTokenFilter;

		@Resource(name = "strangerFilter")
		private StrangerFilter strangerFilter;

		@Bean(name = "authenticationManager")
		@Override
		public AuthenticationManager authenticationManagerBean() throws Exception {
			return super.authenticationManagerBean();
		}

		/**
		 *  主要配置
		 */
		@Override
		protected void configure(HttpSecurity http) throws Exception {
			//配置访问规则
			http
					.csrf().disable()
					//不通过session获取SecurityContext
					.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
					.and()
					.authorizeRequests()
					.antMatchers("/**").permitAll()
					.antMatchers("/signin").anonymous()
					.antMatchers("/signout","/admin/**").authenticated()
					.anyRequest().authenticated();

			//配置jwt过滤器
			http.addFilterBefore(jwtAuthTokenFilter, UsernamePasswordAuthenticationFilter.class)
					.addFilterAfter(strangerFilter,JWTAuthTokenFilter.class);
			//跨域配置
			http.cors();
			//配置自定义异常处理
			http
					.exceptionHandling()
					.authenticationEntryPoint(new AuthenticationEntryPointImpl())
					.accessDeniedHandler(new AccessDeniedHandlerImpl());

		}

		/**
		 * 加密配置;
		 */
		@Bean
		public PasswordEncoder passwordEncoder(){
			return new BCryptPasswordEncoder();
		}
	}
}
