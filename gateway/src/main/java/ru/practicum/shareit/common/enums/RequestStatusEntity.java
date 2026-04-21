/*package ru.practicum.shareit.common.enums;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "request_statuses")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RequestStatusEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "code", unique = true, nullable = false)
    private Integer code;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    // Конструктор из enum
    public RequestStatusEntity(RequestStatus status) {
        this.code = status.getId();
        this.name = status.getName();
    }

    // Вспомогательный метод для конвертации в enum
    public RequestStatus toEnum() {
        return RequestStatus.fromId(this.code);
    }
}*/
