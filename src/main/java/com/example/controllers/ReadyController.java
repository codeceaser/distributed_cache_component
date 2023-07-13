package com.example.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class ReadyController {

    public static final Logger LOGGER = LoggerFactory.getLogger(ReadyController.class);

    @GetMapping
    public ResponseEntity<String> ok() {
//        LOGGER.info("Okay End Point");
        return ResponseEntity.ok("Okay");
    }

}
