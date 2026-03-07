package ru.practicum.market.domain.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

/**
 * Базовая сущность с идентификатором и реализацией equals/hashCode по id.
 */
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    Long id;

    /**
     * Сравнивает сущности по идентификатору.
     */
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BaseEntity other = (BaseEntity) obj;
        if (id == null || other.id == null) return false;
        return id.equals(other.id);
    }

    /**
     * Возвращает hash code на основе идентификатора.
     */
    @Override
    public final int hashCode() {
        if (id != null) return id.hashCode();
        return System.identityHashCode(this);
    }

    /**
     * Возвращает строковое представление сущности.
     */
    @Override
    public String toString() {
        return "id=" + id;
    }
}
