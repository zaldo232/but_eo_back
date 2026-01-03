package org.example.but_eo.component;

import lombok.RequiredArgsConstructor;
import org.example.but_eo.dto.RequestAutoMatch;
import org.example.but_eo.entity.Team;
import org.example.but_eo.service.TeamService;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MatchQueue {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ApplicationEventPublisher eventPublisher;
//    private static final String MATCH_QUEUE_KEY = "match_queue";

    private String getQueueKey(String sportType, String region) {
        // 키 이름: match_queue:SOCCER:Seoul 형식으로 분리
        return String.format("match_queue:%s:%s", sportType, region);
    }

    public void enqueue(RequestAutoMatch request) {
        String key  = getQueueKey(request.getSportType(), request.getRegion());

        redisTemplate.opsForList().rightPush(key, request);
        eventPublisher.publishEvent(new MatchQueueEvent(request.getSportType(), request.getRegion()));
    }

    public Optional<List<RequestAutoMatch>> tryMatch(String sportType, String region) {
        String key = getQueueKey(sportType, region);
        ListOperations<String, Object> ops = redisTemplate.opsForList();
        Long size = ops.size(key);

        if (size != null && size >= 2) {
            RequestAutoMatch a = (RequestAutoMatch) ops.leftPop(key);
            RequestAutoMatch b = (RequestAutoMatch) ops.leftPop(key);

            if (a != null && b != null) {
                return Optional.of(List.of(a, b));
            }

            if (a != null) ops.rightPush(key, a);
            if (b != null) ops.rightPush(key, b);
        }
        return Optional.empty();
    }
}
