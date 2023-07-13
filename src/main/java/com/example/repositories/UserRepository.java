package com.example.repositories;

import com.example.dto.UserDTO;

import java.util.Collection;

public interface UserRepository {
    Collection<UserDTO> findByLocation(String location);
    Collection<UserDTO> findByDepartment(String department);
    Collection<UserDTO> findByLocationAndDepartment(String location, String department);

    /*Collection<UserDTO> findByLocationAndIdNotIn(String location, Collection<Long> ids);
    Collection<UserDTO> findByDepartmentAndIdNotIn(String department, Collection<Long> ids);
    Collection<UserDTO> findByLocationAndDepartmentAndIdNotIn(String location, String department, Collection<Long> ids);*/
}
