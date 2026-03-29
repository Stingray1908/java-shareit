/*package ru.practicum.shareit.request.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.request.ItemRequest;

import java.time.LocalDateTime;
import java.util.*;

@Repository("InMemoryRequestRepository")
@Slf4j
public class InMemoryRequestRepository implements RequestRepository {

    private Long nextId = 1L;
    private final Map<Long, List<ItemRequest>> userIdToRequests = new HashMap<>();
    private final Map<Long, Long> requestIdToUserId = new HashMap<>();

    @Override
    public ItemRequest save(ItemRequest request) {
        Long requestId = nextId++;
        request.setId(requestId);
        request.setCreated(LocalDateTime.now());

        Long userId = request.getRequester();

        requestIdToUserId.put(requestId, userId);
        userIdToRequests.computeIfAbsent(userId, k -> new ArrayList<>()).add(request);

        log.debug("Создан запрос с ID: {} для пользователя: {}", requestId, userId);
        return request;
    }

    public ItemRequest patchStatus(ItemRequest patching) {
        return findByRequestId(patching.getId())
                .map(itemRequest -> {
                    itemRequest.setStatus(patching.getStatus());
                    return itemRequest;
                })
                .orElseThrow(() -> new NoSuchElementException(
                        "ItemRequest с id:" + patching.getId() + " не найден"
                ));
    }

    @Override
    public Optional<ItemRequest> findByRequestId(Long requestId) {
        Long userId = requestIdToUserId.get(requestId);
        if (userId == null) {
            return Optional.empty();
        }

        List<ItemRequest> userRequests = userIdToRequests.get(userId);
        if (userRequests == null) {
            return Optional.empty();
        }

        return userRequests.stream()
                .filter(r -> r.getId().equals(requestId))
                .findFirst();
    }

    @Override
    public List<ItemRequest> findByRequestorId(Long userId) {
        return userIdToRequests.getOrDefault(userId, Collections.emptyList());
    }


    @Override
    public List<ItemRequest> findAll() {
        return userIdToRequests.values().stream()
                .flatMap(List::stream)
                .toList();
    }

    @Override
    public void deleteById(Long requestId) {
        Long userId = requestIdToUserId.remove(requestId);
        if (userId != null) {
            List<ItemRequest> userRequests = userIdToRequests.get(userId);
            if (userRequests != null) {
                userRequests.removeIf(r -> r.getId().equals(requestId));
                if (userRequests.isEmpty()) {
                    userIdToRequests.remove(userId);
                }
            }
        }
        log.info("Удален запрос с ID: {}", requestId);
    }

    // для тестов
    public void clear() {
        requestIdToUserId.clear();
        userIdToRequests.clear();
        nextId = 1L;
    }
}*/
