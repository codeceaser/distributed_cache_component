package com.example.entities;

import com.example.components.Cacheable;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import javax.persistence.*;
import java.io.Serializable;

@Entity
@Getter
@Setter
@EqualsAndHashCode
@ToString
public class User implements Serializable, Cacheable<Long> {

    public static Long userCount = 10L;

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "user_id_seq_gen")
//    @SequenceGenerator(name = "user_id_seq_gen", sequenceName = "user_id_seq", allocationSize = 50)
    private Long id;

    private String name;
    private String email;
    private String location;
    private String department;

    public User() {
    }

    public User(String name, String email, String location, String department) {
        this.name = name;
        this.email = email;
        this.location = location;
        this.department = department;
    }

    // Getters and setters for all fields
    // ...
}
