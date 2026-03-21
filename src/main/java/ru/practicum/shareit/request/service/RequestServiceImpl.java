package ru.practicum.shareit.request.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.RequestMapper;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.request.repository.RequestRepository;
import ru.practicum.shareit.user.service.UserService;

import java.util.List;
import java.util.NoSuchElementException;

@Service
@Slf4j
public class RequestServiceImpl implements RequestService {

    private final RequestRepository requestRepository;
    private final UserService userService;
    private final RequestMapper requestMapper;

    public RequestServiceImpl(@Qualifier("InMemoryRequestRepository") RequestRepository requestRepository,
                              UserService userService,
                              RequestMapper requestMapper) {
        this.requestRepository = requestRepository;
        this.userService = userService;
        this.requestMapper = requestMapper;
    }

    @Override
    public ItemRequestSendDTO create(ItemRequestReqDTO requestDto, Long requestorId) {
        validateUserExists(requestorId);

        ItemRequest request = requestMapper.toEntity(requestDto);
        request.setRequestor(requestorId);
        request.setStatus(RequestStatus.PENDING);

        ItemRequest savedRequest = requestRepository.save(request);
        log.debug("Запрос успешно сохранён в репозитории с ID: {}, пользователь ID: {}",
                savedRequest.getId(), requestorId);

        return requestMapper.toSendDto(savedRequest);
    }

    @Override
    public ItemRequestSendDTO patchStatus(ItemRequestReqDTO requestDto, Long requestId, Long requestorId) {
        RequestStatus newStatus = requestDto.getStatus();
        validateStatusChange(newStatus);

        ItemRequest existingRequest = getByIdForInternal(requestId);
        validateRequestOwnership(existingRequest, requestorId);
        validateCurrentStatus(existingRequest.getStatus(), requestId);

        existingRequest.setStatus(newStatus);
        ItemRequest updatedRequest = requestRepository.patchStatus(existingRequest);

        ItemRequestSendDTO result = requestMapper.toSendDto(updatedRequest);
        log.info("Статус запроса ID: {} успешно обновлён. Ответ клиенту: {}", requestId, result);
        return result;
    }

    @Override
    public ItemRequest patchStatusInternal(ItemRequest request) {
        return requestRepository.patchStatus(request);
    }

    @Override
    public ItemRequestSendDTO getByIdExternal(Long requestId) {
        ItemRequest request = getByIdForInternal(requestId);
        ItemRequestSendDTO result = requestMapper.toSendDto(request);
        log.info("Запрос по ID: {} успешно получен. Ответ клиенту: {}", requestId, result);
        return result;
    }

    @Override
    public List<ItemRequestSendDTO> getAllByRequestorId(Long requestorId) {
        validateUserExistsInternal(requestorId);
        List<ItemRequest> requests = requestRepository.findByRequestorId(requestorId);
        return mapToSendDtoList(requests);
    }

    @Override
    public List<ItemRequestSendDTO> getAllRequests() {
        List<ItemRequest> allRequests = requestRepository.findAll();
        List<ItemRequestSendDTO> results = mapToSendDtoList(allRequests);
        log.info("Получено {} запросов из системы", results.size());
        return results;
    }

    @Override
    public void delete(Long requestId, Long requestorId) {
        ItemRequest existingRequest = getByIdForInternal(requestId);
        validateRequestOwnership(existingRequest, requestorId);

        requestRepository.deleteById(requestId);
        log.info("Запрос ID: {} пользователя ID: {} успешно удалён", requestId, requestorId);
    }

    @Override
    public ItemRequest getByIdForInternal(Long requestId) {
        return requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new NoSuchElementException(
                        String.format("Запрос с ID=%d не найден", requestId)));
    }

    // Вспомогательные методы для валидации

    private void validateUserExists(Long userId) {
        if (userService.getById(userId) == null) {
            throw new NoSuchElementException(
                    String.format("При создании запроса пользователь с ID:%s не обнаружен", userId));
        }
    }

    private void validateUserExistsInternal(Long userId) {
        if (userService.getByIdInternal(userId) == null) {
            throw new NoSuchElementException(
                    String.format("Пользователь с ID:%d не зарегистрирован в системе", userId));
        }
    }

    private void validateRequestOwnership(ItemRequest request, Long requestorId) {
        log.debug("Проверка принадлежности запроса ID: {} пользователю ID: {}", request.getId(), requestorId);
        if (!request.getRequestor().equals(requestorId)) {
            throw new IllegalArgumentException("Запрос не принадлежит указанному пользователю");
        }
    }

    private void validateStatusChange(RequestStatus newStatus) {
        if (newStatus == RequestStatus.PENDING || newStatus == RequestStatus.RESPONDED) {
            throw new IllegalArgumentException("Пользователь не может ставить статус " + newStatus.getName());
        }
    }

    private void validateCurrentStatus(RequestStatus currentStatus, Long requestId) {
        if (currentStatus == RequestStatus.CANCELLED || currentStatus == RequestStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    String.format("Нельзя менять статус запросу ID:%d, потому что его статус %s",
                            requestId, currentStatus.getName()));
        }
    }

    private List<ItemRequestSendDTO> mapToSendDtoList(List<ItemRequest> list) {
        return list.stream()
                .map(requestMapper::toSendDto)
                .toList();
    }
}
