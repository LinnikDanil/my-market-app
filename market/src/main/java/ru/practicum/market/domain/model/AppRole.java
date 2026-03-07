package ru.practicum.market.domain.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Модель роли пользователя.
 */
@Table(name = "roles")
@Getter
@Setter
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppRole extends BaseEntity {
    String name;
}
