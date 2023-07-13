package com.example.distributed_cache;

import com.example.conf.ApplicationConfiguration;
import com.example.dto.UserDTO;
import com.example.services.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.util.CollectionUtils;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ContextConfiguration(classes = {ApplicationConfiguration.class})
public class UserServiceTest {

    @Autowired
    UserService userService;

    @Test
    public void testGetUsersByLocation(){
        Collection<UserDTO> users = userService.findByLocation("Surat").values();
        assertTrue(!CollectionUtils.isEmpty(users));
    }
}
