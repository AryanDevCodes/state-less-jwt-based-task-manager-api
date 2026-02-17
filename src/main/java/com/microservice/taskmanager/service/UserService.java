package com.microservice.taskmanager.service;

import com.microservice.taskmanager.dto.RegisterRequestDTO;
import com.microservice.taskmanager.entity.User;
import com.microservice.taskmanager.repositories.UserRepository;
import com.microservice.taskmanager.service.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public User createUser( RegisterRequestDTO dto ){
        if ( dto == null ){
            return null;
        }
        User user = userMapper.toEntity(dto);
        return userRepository.save(user);
    }

    public List<User> findAll(){
        return userRepository.findAll();
    }

    public User findById( Long id ){
        return  userRepository.findById( id ).get();
    }

    public void delete( Long id ){
        userRepository.deleteById( id );
    }
}
