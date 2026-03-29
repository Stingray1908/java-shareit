package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.RequestMapper;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.request.repository.RequestJpaRepository;
import ru.practicum.shareit.request.repository.RequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.service.UserJPAService;
import ru.practicum.shareit.user.service.UserService;

import javax.sql.rowset.serial.SerialException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Slf4j
@Service("RequestJpaService")
@RequiredArgsConstructor
public class RequestJpaService implements RequestService{

    private final RequestJpaRepository requestJpaRepo;
    private final UserService userJPAService;
    private final RequestMapper requestMapper = new RequestMapper();

    @Override
    public ItemRequestSendDTO create(ItemRequestReqDTO requestDto, Long requestorId) {
        User user = findUserOrThrow(requestorId);

        ItemRequest request = requestMapper.toEntity(requestDto);
        request.setRequester(user);
        request.setStatus(RequestStatus.PENDING);
        request.setCreated(LocalDateTime.now());

        ItemRequest savedRequest = requestJpaRepo.save(request);
        log.debug("Запрос успешно сохранён в репозитории с ID: {}, пользователь ID: {}",
                savedRequest.getId(), requestorId);

        return requestMapper.toSendDto(savedRequest);
    }

    @Override
    public ItemRequestSendDTO patchStatus(ItemRequestReqDTO requestDto, Long requestId, Long requestorId) {

        ItemRequest existingRequest = findActiveRequestByIdOrThrowInternal(requestId);

        validateRequestAccessOrThrow(existingRequest, requestId);
        RequestStatus newStatus = requestDto.getStatus();

        if (newStatus.equals(RequestStatus.PENDING) || newStatus.equals(RequestStatus.RESPONDED)) {
            throw new IllegalArgumentException(
                    String.format("Пользователь пытается установить системный статус %s", newStatus.getName()));
        }

        existingRequest.setStatus(newStatus);
        ItemRequest updatedRequest = requestJpaRepo.save(existingRequest);

        ItemRequestSendDTO result = requestMapper.toSendDto(updatedRequest);
        log.info("Статус запроса ID: {} успешно обновлён. Ответ клиенту: {}", requestId, result);
        return result;
    }

    // внутренний метод, может устанавливать системные статусы Pending Responded
    @Override
    public ItemRequest patchStatusInternal(ItemRequest request) {
        return null;
    }

    @Override
    public ItemRequest findActiveRequestByIdOrThrowInternal(Long id) {
        return requestJpaRepo.findByIdWithActiveStatus(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Запрос с id: " + id + " не существует или имеет неактивный статус"));
    }

    // метод доступен всем
    @Override  // написать тест
    public ItemRequestSendDTO getById(Long id) {
        return requestMapper.toSendDto(
                requestJpaRepo.findById(id)
                        .orElseThrow(() -> new NoSuchElementException("Запрос с id: " + id + " не существует"))
        );
    }

    @Override // написать тест
    public List<ItemRequestSendDTO> getAllByRequestorId(Long requestorId) {
        return requestJpaRepo.findAllByRequesterId(requestorId).stream()
                .map(requestMapper::toSendDto)
                .toList();
    }

    // написать тест
    @Override  // может удалтиь только создатель
    public void deleteById(Long requestId, Long requesterId) {

        ItemRequest existingRequest = findActiveRequestByIdOrThrowInternal(requestId);
        validateRequestAccessOrThrow(existingRequest, requesterId);

        requestJpaRepo.deleteById(requestId);
    }

    private User findUserOrThrow(Long userId) {
        User user;
        if ((user = userJPAService.getByIdInternal(userId)) == null) {
            throw new NoSuchElementException(
                    String.format("При создании запроса пользователь с ID:%s не обнаружен", userId));
        }
        return user;
    }

    private void validateRequestAccessOrThrow(ItemRequest existingRequest, Long requestId) {
        Long actualRequesterId = existingRequest.getRequester().getId();

        if (!Objects.equals(actualRequesterId, requestId)) {
            String message = String.format(
                    ("Попытка несанкционированного доступа: пользователь ID=%d " +
                            "пытается изменить запрос, принадлежащий пользователю ID=%d"),
                    requestId, actualRequesterId);
            throw new SecurityException(message);
        }
    }
}
