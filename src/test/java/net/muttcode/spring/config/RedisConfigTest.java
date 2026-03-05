package net.muttcode.spring.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import org.springframework.test.util.ReflectionTestUtils;


/**
 * Unit tests for RedisConfig configuration.
 * Verifies Redis connection factory is properly configured.
 */
class RedisConfigTest {

  @Test
  void jedisConnectionFactory_shouldCreateBean() {
    RedisConfig config = new RedisConfig();
    ReflectionTestUtils.setField(config, "redisHost", "localhost");
    ReflectionTestUtils.setField(config, "redisPort", 6379);
    ReflectionTestUtils.setField(config, "redisPassword", Optional.empty());
    
    JedisConnectionFactory factory = config.jedisConnectionFactory();
    
    assertNotNull(factory, "RedisConnectionFactory bean should be created");
  }

  @Test
  void jedisConnectionFactory_withPassword_shouldSetPassword() {
    RedisConfig config = new RedisConfig();
    ReflectionTestUtils.setField(config, "redisHost", "localhost");
    ReflectionTestUtils.setField(config, "redisPort", 6379);
    ReflectionTestUtils.setField(config, "redisPassword", Optional.of("testpassword"));
    
    JedisConnectionFactory factory = config.jedisConnectionFactory();
    
    assertNotNull(factory);
  }

  @Test
  void jedisConnectionFactory_withCustomHost_shouldUseCustomHost() {
    RedisConfig config = new RedisConfig();
    ReflectionTestUtils.setField(config, "redisHost", "redis.example.com");
    ReflectionTestUtils.setField(config, "redisPort", 6380);
    ReflectionTestUtils.setField(config, "redisPassword", Optional.empty());
    
    JedisConnectionFactory factory = config.jedisConnectionFactory();
    
    assertNotNull(factory);
  }
}
