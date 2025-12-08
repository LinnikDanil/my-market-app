package ru.practicum.market.domain.model;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.Hibernate;

@MappedSuperclass
@Getter
@Setter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || Hibernate.getClass(this) != Hibernate.getClass(obj)) return false;
        BaseEntity other = (BaseEntity) obj;
        if (id == null || other.id == null) return false;
        return id.equals(other.id);
    }

    @Override
    public int hashCode() {
        if (id != null) return id.hashCode();
        return System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "id=" + id;
    }
}
