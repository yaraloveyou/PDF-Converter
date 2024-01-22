package org.converter.config;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Optional;

@Log4j2
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Component
public class RedisLock {
    RedisTemplate<String, Long> redisTemplate;

    private static final String KEY_TEMPLATE = "lock:%s";

    /**
     * Метод запроса блокировки
     * @param expireMillis - время, на которое данная задача будет заблокирована в миллисекундах
     * @param taskKey - ключ, по  которому будет заблокирована задача
     * @return статус захвата (захвачено/не захвачено)
     */
    public boolean acquireLock(long expireMillis, String taskKey) {

        // Получаем ключ блокировки
        String lockKey = getLockKey(taskKey);

        Long expireAt = redisTemplate.opsForValue().get(lockKey);

        long currentTimeMillis = System.currentTimeMillis();

        // Если блокировка есть
        if (Objects.nonNull(expireAt)) {
            // И она истекает, то удаляем ее
            if (expireAt <= currentTimeMillis) {
                redisTemplate.delete(lockKey);
            } else {
                return false;
            }
        }

        // Иначе создаем блокировку, и прибавляем к текущему времени, время, на которое блокируется задача
        Long expire = currentTimeMillis + expireMillis;

        return Optional.
                ofNullable(redisTemplate.opsForValue().setIfAbsent(lockKey, expire))
                .orElse(false);
    }

    /**
     * Метод освобождения блокировки
     * @param taskKey - ключ, по которому бедут разлокировка задачи
     */
    public void releaseLock(String taskKey) {
        String lockKey = getLockKey(taskKey);

        redisTemplate.delete(lockKey);
    }

    private String getLockKey(String key) {
        return String.format(KEY_TEMPLATE, key);
    }
}
