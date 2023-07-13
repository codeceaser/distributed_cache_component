package com.example.services;

import com.example.dto.UserDTO;
import com.example.entities.User;

import java.util.Collection;
import java.util.Map;

public interface IUserService {
    Collection<UserDTO> findAll();

    Map<Long, UserDTO> findByLocation(String location);

    String printCaches();

    String clearCaches();

    Map<Long, UserDTO> findByDepartment(String department);
    Map<Long, UserDTO> findByLocationAndDepartment(String location, String department);

    UserDTO findById(Long id);

    UserDTO save(User user);

    UserDTO create(User user);

    void deleteById(Long id);
}
