package ru.practicum.market.domain.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.relational.core.mapping.Table;

/**
 * Модель пользователя приложения.
 */
@Table(name = "users")
@Getter
@Setter
@ToString(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AppUser extends BaseEntity {
    String username;
    String password;
}
