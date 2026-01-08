package ru.practicum.market.domain.model;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;

@Getter
@Setter
public abstract class BaseEntity {

    @Id
    Long id;

    @Override
    public final boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        BaseEntity other = (BaseEntity) obj;
        if (id == null || other.id == null) return false;
        return id.equals(other.id);
    }

    @Override
    public final int hashCode() {
        if (id != null) return id.hashCode();
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "id=" + id;
    }
}
