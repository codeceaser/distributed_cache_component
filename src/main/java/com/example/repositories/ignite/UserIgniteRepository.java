package com.example.repositories.ignite;

import com.example.repositories.UserRepository;
import com.example.components.InvertedIndicesRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

@Repository
//@RepositoryConfig(cacheName = "indexed-user")
@ConditionalOnProperty(name = "caching.solution", havingValue = "indexed-ignite")
public interface UserIgniteRepository extends /*IgniteRepository<UserDTO, Long>,*/ UserRepository, InvertedIndicesRepository {


}
