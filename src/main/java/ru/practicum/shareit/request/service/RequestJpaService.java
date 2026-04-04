package ru.practicum.shareit.request.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.util.InternalException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import ru.practicum.shareit.booking.Booking;
import ru.practicum.shareit.booking.dto.BookingSendDto;
import ru.practicum.shareit.common.enums.RequestStatus;
import ru.practicum.shareit.request.ItemRequest;
import ru.practicum.shareit.request.RequestMapper;
import ru.practicum.shareit.request.dto.ItemRequestReqDTO;
import ru.practicum.shareit.request.dto.ItemRequestSendDTO;
import ru.practicum.shareit.request.repository.RequestJpaRepository;
import ru.practicum.shareit.request.repository.RequestRepository;
import ru.practicum.shareit.user.User;
import ru.practicum.shareit.user.UserMapper;
import ru.practicum.shareit.user.service.UserJPAService;
import ru.practicum.shareit.user.service.UserService;

import javax.sql.rowset.serial.SerialException;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

@Slf4j
@Service("RequestJpaService")
@RequiredArgsConstructor
public class RequestJpaService implements RequestService {

    private final RequestJpaRepository requestJpaRepo;
    private final UserService userJPAService;

    private final RequestMapper requestMapper;
    private final UserMapper userMapper = new UserMapper();

    @Override
    @Transactional
    public ItemRequestSendDTO create(ItemRequestReqDTO requestDto, Long requestorId) {
        User user = findUserOrThrow(requestorId);

        ItemRequest request = requestMapper.toEntity(requestDto);
        request.setRequester(user);
        request.setStatus(RequestStatus.PENDING);
        request.setCreated(LocalDateTime.now());

        ItemRequest savedRequest = requestJpaRepo.save(request);
        log.debug("Запрос успешно сохранён в репозитории с ID: {}, пользователь ID: {}",
                savedRequest.getId(), requestorId);

        return toSendDto(request);
    }

    @Override
    @Transactional
    public ItemRequestSendDTO patchStatus(ItemRequestReqDTO requestDto, Long requestId, Long requestorId) {
        ItemRequest existingRequest = findActiveRequestByIdOrThrowInternal(requestId);

        validateRequestAccessOrThrow(existingRequest, requestorId);
        RequestStatus newStatus = requestDto.getStatus();

        if (newStatus.equals(RequestStatus.PENDING) || newStatus.equals(RequestStatus.RESPONDED)) {
            throw new IllegalArgumentException(
                    String.format("Пользователь пытается установить системный статус %s", newStatus.getName()));
        }

        existingRequest.setStatus(newStatus);
        return toSendDto(requestJpaRepo.save(existingRequest));
    }

    // внутренний метод, может устанавливать системные статусы Pending Responded
    @Override
    @Transactional
    public ItemRequest patchStatusInternal(ItemRequest request) {
        RequestStatus newStatus = request.getStatus();
        if (newStatus.equals(RequestStatus.COMPLETED) || newStatus.equals(RequestStatus.CANCELLED)) {
            throw new InternalException("Система пытается установить статус запроса" + newStatus.name());
        }
        return requestJpaRepo.save(request);
    }

    @Override
    @Transactional(readOnly = true)
    public ItemRequest findActiveRequestByIdOrThrowInternal(Long id) {
        return requestJpaRepo.findByIdWithActiveStatus(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Запрос с id: " + id + " не существует или имеет неактивный статус")
                );
    }

    @Override
    @Transactional(readOnly = true)
    public ItemRequest findRequestByIdOrThrowInternal(Long id) {
        return requestJpaRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Запрос с id: " + id + " не существует"));
    }

    // метод доступен всем
    @Override  // написать тест
    @Transactional(readOnly = true)
    public ItemRequestSendDTO getById(Long id) {
        return toSendDto(requestJpaRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Запрос с id: " + id + " не существует"))
        );
    }

    @Override // написать тест
    @Transactional(readOnly = true)
    public List<ItemRequestSendDTO> getAllByRequestorId(Long requestorId) {
        return toListSendDto(requestJpaRepo.findAllByRequesterId(requestorId));
    }

    // написать тест
    @Override  // может удалить только создатель
    @Transactional
    public void deleteById(Long requestId, Long requesterId) {
        ItemRequest existingRequest = findActiveRequestByIdOrThrowInternal(requestId);
        validateRequestAccessOrThrow(existingRequest, requesterId);

        requestJpaRepo.deleteById(requestId);
    }

    @Transactional(readOnly = true)
    private User findUserOrThrow(Long userId) {
        User user;
        if ((user = userJPAService.getByIdOrThrowInternal(userId)) == null) {
            throw new NoSuchElementException(
                    String.format("При создании запроса пользователь с ID:%s не обнаружен", userId));
        }
        return user;
    }

    @Transactional(readOnly = true)
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

    @Transactional(readOnly = true)
    private ItemRequestSendDTO toSendDto(ItemRequest request){
        ItemRequestSendDTO dto = requestMapper.toSendDto(request);
        dto.setRequester(userMapper.toSendDto(request.getRequester()));
        return dto;
    }

    @Transactional(readOnly = true)
    private List<ItemRequestSendDTO> toListSendDto (Collection<ItemRequest> requests) {
        return requests.stream()
                .map(this::toSendDto)
                .toList();
    }
}
