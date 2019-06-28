package com.rte_france.apogee.sea.server.model.dao.user;

import com.rte_france.apogee.sea.server.model.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findOneByUsername(String username);

    @Query(value = "SELECT u FROM User u WHERE u.defaultUsertype.name =:name OR u.actualUsertype.name =:name")
    List<User> findUsersByDefaultUsertypeOrActualUsertype(@Param("name") String name);
}
