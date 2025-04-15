package com.goorm.clonestagram.common.service; // 패키지 경로는 적절히 변경

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final RedissonClient redissonClient;
    private final StringRedisTemplate redisTemplate; // String 직렬화 사용
    private final ObjectMapper objectMapper; // JSON 직렬화/역직렬화

    private static final String IDEMPOTENCY_KEY_PREFIX = "idempotency:";
    private static final long LOCK_WAIT_TIME_SECONDS = 10; // 락 대기 시간
    private static final long LOCK_LEASE_TIME_SECONDS = 30; // 락 유지 시간 (작업 예상 시간보다 길게)
    private static final long RESULT_TTL_MINUTES = 60 * 24; // 결과 저장 TTL (예: 24시간)

    /**
     * 멱등성을 보장하며 주어진 작업을 실행합니다.
     *
     * @param idempotencyKey 멱등성 키
     * @param operation      실행할 실제 작업 (Supplier)
     * @param resultType     결과 객체의 클래스 타입
     * @param <T>            결과 객체의 타입
     * @return 작업 결과
     * @throws IdempotencyProcessingException 멱등성 처리 중 예외 발생 시
     * @throws RuntimeException 작업 실행 중 예외 발생 시
     */
    public <T> T executeWithIdempotency(String idempotencyKey, Supplier<T> operation, Class<T> resultType) {
        if (!StringUtils.hasText(idempotencyKey)) {
            throw new IllegalArgumentException("Idempotency-Key는 필수입니다.");
        }

        String redisKey = IDEMPOTENCY_KEY_PREFIX + idempotencyKey;
        RLock lock = redissonClient.getLock("lock:" + redisKey); // 분산 락 키

        try {
            // 1. 분산 락 획득 시도
            boolean isLocked = lock.tryLock(LOCK_WAIT_TIME_SECONDS, LOCK_LEASE_TIME_SECONDS, TimeUnit.SECONDS);
            if (!isLocked) {
                log.warn("Idempotency Lock 획득 실패: Key={}", idempotencyKey);
                throw new IdempotencyProcessingException("다른 요청이 처리 중입니다. 잠시 후 다시 시도해주세요. Key: " + idempotencyKey);
            }

            // 2. 락 획득 후 Redis에서 결과 확인
            String storedResultJson = redisTemplate.opsForValue().get(redisKey);

            if (storedResultJson != null) {
                // 3. 결과가 이미 존재하면, 저장된 결과 반환
                log.info("Idempotency Hit: Key={}, Result found in Redis.", idempotencyKey);
                try {
                    // JSON 문자열을 실제 객체 타입으로 역직렬화
                    return objectMapper.readValue(storedResultJson, resultType);
                } catch (JsonProcessingException e) {
                    log.error("Idempotency Hit: Redis에 저장된 결과 역직렬화 실패. Key={}", idempotencyKey, e);
                    throw new IdempotencyProcessingException("저장된 결과 처리 중 오류가 발생했습니다. Key: " + idempotencyKey, e);
                }
            } else {
                // 4. 결과가 없으면, 실제 작업 수행
                log.info("Idempotency Miss: Key={}, Executing operation.", idempotencyKey);
                T result = operation.get(); // 실제 로직 실행 (예: DB 저장)

                // 5. 작업 결과 Redis에 저장 (JSON 형태로 직렬화)
                try {
                    String resultJson = objectMapper.writeValueAsString(result);
                    redisTemplate.opsForValue().set(redisKey, resultJson, RESULT_TTL_MINUTES, TimeUnit.MINUTES);
                    log.info("Idempotency Result Stored: Key={}, TTL={} minutes", idempotencyKey, RESULT_TTL_MINUTES);
                    return result;
                } catch (JsonProcessingException e) {
                    log.error("Idempotency Miss: 작업 결과 직렬화 실패. Key={}", idempotencyKey, e);
                    // 중요: 결과 저장 실패 시 멱등성 보장 깨질 수 있음. 복구 로직 또는 명확한 에러 처리 필요.
                    // 예: 저장 실패 시 lock만 해제하고 에러 반환하여 클라이언트가 재시도하도록 유도
                    throw new IdempotencyProcessingException("작업 결과 저장 중 오류가 발생했습니다. Key: " + idempotencyKey, e);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Idempotency Lock 대기 중 인터럽트 발생: Key={}", idempotencyKey, e);
            throw new IdempotencyProcessingException("요청 처리 중 인터럽트가 발생했습니다. Key: " + idempotencyKey, e);
        } catch (Exception e) {
            // operation.get() 에서 발생한 예외 또는 기타 예외 처리
             log.error("Error during idempotent operation for key: {}", idempotencyKey, e);
             // IdempotencyProcessingException 이 아닌 경우 그대로 던지거나, 비즈니스 로직에 맞게 처리
             if (e instanceof IdempotencyProcessingException) {
                 throw e; // 위에서 발생한 IdempotencyProcessingException은 그대로 던짐
             } else {
                 // 실제 작업(operation) 중 발생한 예외는 그대로 던져서 ControllerAdvice 등에서 처리하도록 함
                 throw new RuntimeException("작업 처리 중 오류 발생: " + e.getMessage(), e);
             }
        } finally {
            // 6. 락 해제 (락을 성공적으로 획득한 경우에만)
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    // 사용자 정의 예외 클래스
    public static class IdempotencyProcessingException extends RuntimeException {
        public IdempotencyProcessingException(String message) {
            super(message);
        }

        public IdempotencyProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
} 