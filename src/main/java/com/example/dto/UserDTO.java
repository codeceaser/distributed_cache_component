package com.example.dto;

import com.example.components.Cacheable;
import com.example.entities.User;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

@Getter
@Setter
@EqualsAndHashCode
@ToString
@Document(indexName = "elastic-user")
public class UserDTO implements Serializable, Cacheable<Long> {
    private Long id;

    private String name;
    private String email;
    private String location;
    private String department;

    public UserDTO(){}

    public UserDTO(User user) {
        this.id = user.getId();
        this.name = user.getName();
        this.email = user.getEmail();
        this.location = user.getLocation();
        this.department = user.getDepartment();
    }

}
