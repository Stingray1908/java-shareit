package ru.practicum.shareit.item;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    @EqualsAndHashCode.Include
    private Long id;
    @EqualsAndHashCode.Include
    private Long ownerId;
    private String name;
    private String description;
    private Long requestId;
    private Boolean available;
}
