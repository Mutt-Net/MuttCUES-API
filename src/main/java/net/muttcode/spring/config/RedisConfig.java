package net.muttcode.spring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.util.Optional;

/**
 * Redis connection configuration.
 * Configures Redis connection using environment variables or defaults.
 */
@Configuration
public class RedisConfig {

  @Value("${spring.data.redis.host:localhost}")
  private String redisHost;

  @Value("${spring.data.redis.port:6379}")
  private int redisPort;

  @Value("${spring.data.redis.password:}")
  private Optional<String> redisPassword;

  /**
   * Creates a Redis connection factory using configured host, port, and optional password.
   * @return JedisConnectionFactory
   */
  @Bean
  public JedisConnectionFactory jedisConnectionFactory() {
    RedisStandaloneConfiguration config = new RedisStandaloneConfiguration(redisHost, redisPort);
    redisPassword.ifPresent(config::setPassword);
    return new JedisConnectionFactory(config);
  }
}
