package com.example.annotations;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.lang.annotation.*;

@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface RefreshCache {

    @JsonProperty
    String[] cacheNames();

    @JsonProperty
    String isDelete() default "N";

}
