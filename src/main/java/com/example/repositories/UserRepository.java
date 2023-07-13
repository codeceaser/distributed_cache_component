package com.example.repositories;

import com.example.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface UserRepository extends JpaRepository<User, Long> {

    Collection<User> findByLocationAndIdNotIn(String location, Collection<Long> ids);
    Collection<User> findByDepartmentAndIdNotIn(String department, Collection<Long> ids);
    Collection<User> findByLocationAndDepartmentAndIdNotIn(String location, String department, Collection<Long> ids);

}
