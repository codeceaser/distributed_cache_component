package com.example.dto;

import com.example.components.Cacheable;
import com.example.entities.User;
import lombok.*;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.springframework.data.elasticsearch.annotations.Document;

import java.io.Serializable;

@Data
@Getter
@Setter
@EqualsAndHashCode
@ToString
@Document(indexName = "indexed-user")
public class UserDTO implements Serializable, Cacheable<Long> {
    @QuerySqlField(index = true)
    private Long id;

    private String name;
    private String email;
    @QuerySqlField(index = true)
    private String location;

    @QuerySqlField(index = true)
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
