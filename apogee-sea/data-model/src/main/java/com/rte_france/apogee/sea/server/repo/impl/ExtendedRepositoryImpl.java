package com.rte_france.apogee.sea.server.repo.impl;

import com.rte_france.apogee.sea.server.repo.ExtendedRepository;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;

import javax.persistence.EntityManager;
import java.io.Serializable;

public class ExtendedRepositoryImpl<T, D extends Serializable>
        extends SimpleJpaRepository<T, D> implements ExtendedRepository<T, D> {

    public ExtendedRepositoryImpl(JpaEntityInformation<T, ?>
                                          entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
    }
}
