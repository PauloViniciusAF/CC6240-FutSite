package com.futsite.service;

import com.futsite.dto.response.MatchTimerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redis service for match timer state, caching, and session management.
 * Demonstrates Redis (DB2) usage as a key-value / in-memory store.
 */
@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final String TIMER_PREFIX = "match:timer:";
    private static final String STANDINGS_CACHE_PREFIX = "standings:";

    // ====== MATCH TIMER ======

    public void startTimer(Long matchId, int totalSeconds) {
        String key = TIMER_PREFIX + matchId;
        redisTemplate.opsForHash().put(key, "status", "RUNNING");
        redisTemplate.opsForHash().put(key, "elapsed", 0);
        redisTemplate.opsForHash().put(key, "total", totalSeconds);
        redisTemplate.opsForHash().put(key, "lastTick", System.currentTimeMillis());
        redisTemplate.expire(key, 24, TimeUnit.HOURS);
    }

    public void pauseTimer(Long matchId) {
        String key = TIMER_PREFIX + matchId;
        updateElapsed(matchId);
        redisTemplate.opsForHash().put(key, "status", "PAUSED");
    }

    public void resumeTimer(Long matchId) {
        String key = TIMER_PREFIX + matchId;
        redisTemplate.opsForHash().put(key, "status", "RUNNING");
        redisTemplate.opsForHash().put(key, "lastTick", System.currentTimeMillis());
    }

    public void adjustTimer(Long matchId, int deltaSeconds) {
        String key = TIMER_PREFIX + matchId;
        updateElapsed(matchId);
        Integer total = getInt(key, "total");
        if (total != null) {
            int newTotal = Math.max(0, total + deltaSeconds);
            redisTemplate.opsForHash().put(key, "total", newTotal);
        }
    }

    public MatchTimerResponse getTimerState(Long matchId) {
        String key = TIMER_PREFIX + matchId;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(key))) {
            return null;
        }
        updateElapsed(matchId);
        String status = (String) redisTemplate.opsForHash().get(key, "status");
        Integer elapsed = getInt(key, "elapsed");
        Integer total = getInt(key, "total");

        return MatchTimerResponse.builder()
                .matchId(matchId)
                .status(status)
                .elapsedSeconds(elapsed != null ? elapsed : 0)
                .totalSeconds(total != null ? total : 0)
                .build();
    }

    public void stopTimer(Long matchId) {
        redisTemplate.delete(TIMER_PREFIX + matchId);
    }

    private void updateElapsed(Long matchId) {
        String key = TIMER_PREFIX + matchId;
        String status = (String) redisTemplate.opsForHash().get(key, "status");
        if ("RUNNING".equals(status)) {
            Long lastTick = getLong(key, "lastTick");
            Integer elapsed = getInt(key, "elapsed");
            if (lastTick != null && elapsed != null) {
                long now = System.currentTimeMillis();
                int additionalSeconds = (int) ((now - lastTick) / 1000);
                redisTemplate.opsForHash().put(key, "elapsed", elapsed + additionalSeconds);
                redisTemplate.opsForHash().put(key, "lastTick", now);
            }
        }
    }

    // ====== STANDINGS CACHE ======

    public void cacheStandings(Long championshipId, Object standings) {
        String key = STANDINGS_CACHE_PREFIX + championshipId;
        redisTemplate.opsForValue().set(key, standings, 5, TimeUnit.MINUTES);
    }

    public Object getCachedStandings(Long championshipId) {
        String key = STANDINGS_CACHE_PREFIX + championshipId;
        return redisTemplate.opsForValue().get(key);
    }

    public void invalidateStandingsCache(Long championshipId) {
        redisTemplate.delete(STANDINGS_CACHE_PREFIX + championshipId);
    }

    // ====== HELPERS ======

    private Integer getInt(String key, String field) {
        Object val = redisTemplate.opsForHash().get(key, field);
        if (val instanceof Integer) return (Integer) val;
        if (val instanceof Number) return ((Number) val).intValue();
        if (val instanceof String) return Integer.parseInt((String) val);
        return null;
    }

    private Long getLong(String key, String field) {
        Object val = redisTemplate.opsForHash().get(key, field);
        if (val instanceof Long) return (Long) val;
        if (val instanceof Number) return ((Number) val).longValue();
        if (val instanceof String) return Long.parseLong((String) val);
        return null;
    }
}
