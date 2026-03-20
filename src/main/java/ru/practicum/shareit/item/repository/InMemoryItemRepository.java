package ru.practicum.shareit.item.repository;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import ru.practicum.shareit.item.Item;

import java.util.*;
import java.util.stream.Collectors;

@Repository("InMemoryItemRepository")
@Slf4j
@Data
public class InMemoryItemRepository implements ItemRepository {

    private long nextId = 1;
    private final Map<Long, Item> itemIdToItem = new HashMap<>();
    private final Map<Long, List<Long>> ownerIdToItemId = new HashMap<>();
    private final Map<Long, List<Long>> requestIdToItemId = new HashMap<>();

    @Override
    public Item create(Item item) {
        long itemId = nextId++;
        item.setId(itemId);

// Сохраняем связь владелец → ID вещи
        ownerIdToItemId.computeIfAbsent(item.getOwnerId(), k -> new ArrayList<>()).add(itemId);
// Сохраняем саму вещь
        itemIdToItem.put(itemId, item);

        log.info("Создана вещь с ID: {} для владельца: {}", itemId, item.getOwnerId());
        return item;
    }

    @Override
    public Item update(Long itemId, Long ownerId, Item item) {
        Item existingItem = getItemOrThrow(itemId);

// Проверяем, что владелец вещи совпадает с указанным
        if (!existingItem.getOwnerId().equals(ownerId)) {
            throw new NoSuchElementException(
                    String.format("Вещь с ID=%d принадлежит другому владельцу", itemId)
            );
        }

// Обновляем поля, если они не null
        updateItemFields(existingItem, item);

        log.info("Обновлена вещь с ID: {}", itemId);
        return existingItem;
    }

    @Override
    public Optional<Item> getById(long itemId) {
        return Optional.ofNullable(itemIdToItem.get(itemId));
    }

    @Override
    public List<Item> findByUserId(long userId) {
        List<Long> itemIds = ownerIdToItemId.getOrDefault(userId, Collections.emptyList());
        return itemIds.stream()
                .map(itemIdToItem::get)
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(Item::getId))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<Item> getItemsByRequestIds(Long requestId) {
        return itemIdToItem.values().stream()
                .filter(item -> Objects.equals(item.getRequestId(), requestId))
                .toList();
    }

    @Override
    public boolean deleteByItemAndOwnerIds(long itemId, long ownerId) {
// Проверяем существование вещи
        Item item = getItemOrThrow(itemId);

// Проверяем принадлежность вещи владельцу
        if (!item.getOwnerId().equals(ownerId)) {
            log.warn("Попытка удаления чужой вещи: вещь ID {}, владелец {}, запрашивающий {}",
                    itemId, item.getOwnerId(), ownerId);
            return false;
        }

// Выполняем удаление
        performItemDeletion(itemId, ownerId);
        log.info("Удалена вещь с ID: {} пользователя ID: {}", itemId, ownerId);
        return true;
    }

    @Override
    public List<Item> search(String text) {
        if (text == null || text.trim().isBlank()) {
            return new ArrayList<>();
        }

        String searchText = text.toLowerCase(Locale.ROOT);
        return itemIdToItem.values().stream()
                .filter(i -> i.getAvailable() == true)
                .filter(i -> matchesSearchCriteria(i, searchText))
                .toList();
    }

    // Вспомогательный метод для получения вещи или выброса исключения
    private Item getItemOrThrow(long itemId) {
        return itemIdToItem.get(itemId) != null
                ? itemIdToItem.get(itemId)
                : throwNoSuchElementException("Вещь с ID " + itemId + " не найдена");
    }

    // Вспомогательный метод для обновления полей вещи
    private void updateItemFields(Item existingItem, Item updatedItem) {
        if (updatedItem.getName() != null) {
            existingItem.setName(updatedItem.getName());
        }
        if (updatedItem.getDescription() != null) {
            existingItem.setDescription(updatedItem.getDescription());
        }
        if (updatedItem.getAvailable() != null) {
            existingItem.setAvailable(updatedItem.getAvailable());
        }
    }

    // Вспомогательный метод для выполнения удаления вещи
    private void performItemDeletion(long itemId, long ownerId) {
// Удаляем из основного хранилища
        itemIdToItem.remove(itemId);

// Удаляем ID вещи из списка владельца
        List<Long> userItems = ownerIdToItemId.get(ownerId);
        if (userItems != null) {
            userItems.remove(itemId);
// Если у владельца не осталось вещей, удаляем запись
            if (userItems.isEmpty()) {
                ownerIdToItemId.remove(ownerId);
            }
        }
    }

    // Вспомогательный метод для проверки соответствия вещи поисковому запросу
    private boolean matchesSearchCriteria(Item item, String searchText) {
        return item.getName().toLowerCase(Locale.ROOT).contains(searchText) ||
                item.getDescription().toLowerCase(Locale.ROOT).contains(searchText);
    }

    // Вспомогательный метод для выброса исключения
    private <T> T throwNoSuchElementException(String message) {
        throw new NoSuchElementException(message);
    }

    // Для тестов
    public void clear() {
        itemIdToItem.clear();
        ownerIdToItemId.clear();
        requestIdToItemId.clear();
        nextId = 1L;
    }
}
