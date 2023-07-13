package com.example.repositories.elastic;

import com.example.dto.UserDTO;
import com.example.repositories.UserRepository;
import com.example.repositories.api.InvertedIndicesRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;

@Repository
@ConditionalOnProperty(name = "caching.solution", havingValue = "elastic")
public interface UserElasticRepository extends ElasticsearchRepository<UserDTO, String>, UserRepository, InvertedIndicesRepository {

//    Collection<UserDTO> findByBand(String band);
    Collection<UserDTO> findByLocation(String location);
    Collection<UserDTO> findByDepartment(String department);
    Collection<UserDTO> findByLocationAndDepartment(String location, String department);
//    Collection<UserDTO> findByLocationAndBand(String location, String band);
//    Collection<UserDTO> findByDepartmentAndBand(String department, String band);
}
