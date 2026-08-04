package com.inshort.be;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "RUN_REDIS_INTEGRATION_TEST", matches = "true")
class RedisConnectionIntegrationTests {

  @Autowired private StringRedisTemplate redisTemplate;

  @Test
  void savesReadsExpiresAndDeletesValues() throws InterruptedException {
    String prefix = "test:connection:";
    String expiringKey = prefix + UUID.randomUUID();

    redisTemplate.opsForValue().set(expiringKey, "success", Duration.ofSeconds(1));

    assertThat(redisTemplate.opsForValue().get(expiringKey)).isEqualTo("success");
    assertThat(redisTemplate.getExpire(expiringKey)).isPositive();

    Thread.sleep(1_500);
    assertThat(redisTemplate.opsForValue().get(expiringKey)).isNull();

    String deletedKey = prefix + UUID.randomUUID();
    redisTemplate.opsForValue().set(deletedKey, "success", Duration.ofMinutes(1));
    assertThat(redisTemplate.delete(deletedKey)).isTrue();
    assertThat(redisTemplate.opsForValue().get(deletedKey)).isNull();
  }
}
