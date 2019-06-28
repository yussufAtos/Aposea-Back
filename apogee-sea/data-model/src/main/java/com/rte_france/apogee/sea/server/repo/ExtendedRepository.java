package com.rte_france.apogee.sea.server.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import java.io.Serializable;
import java.util.List;

@NoRepositoryBean
public interface ExtendedRepository<T, D extends Serializable> extends JpaRepository<T, D> {

    <S extends T> List<S> saveAll(Iterable<S> entities);
}


