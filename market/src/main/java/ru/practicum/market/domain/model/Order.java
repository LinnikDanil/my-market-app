package ru.practicum.market.domain.model;

import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

/**
 * Модель заказа пользователя.
 */
@Table(name = "orders")
@Getter
@Setter
@ToString(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order extends BaseEntity {

    @Column("total_sum")
    long totalSum;

    @Column("created_at")
    LocalDateTime createdAt;

    @Column("user_id")
    long userId;

    /**
     * Создаёт заказ с пользователем и итоговой суммой.
     *
     * @param userId   идентификатор пользователя
     * @param totalSum итоговая сумма заказа
     */
    public Order(long userId, long totalSum) {
        this.userId = userId;
        this.totalSum = totalSum;
    }
}
