package com.microservice.taskmanager.service;

import com.microservice.taskmanager.dto.TaskResponseDto;
import com.microservice.taskmanager.entity.Task;
import com.microservice.taskmanager.repositories.TaskRepository;
import com.microservice.taskmanager.service.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository  taskRepository;
    private final TaskMapper taskMapper;

    @PreAuthorize("hasRole('USER')")
    public List<TaskResponseDto> getMyTask(){
        //retrieve current user
        String email  = SecurityContextHolder.getContext().getAuthentication().getName();
        return taskRepository.findByUser_Email(email)
                .stream().map(taskMapper::toResponseDto)
                .toList();
    }

    @PreAuthorize("hasRole('ADMIN') or #email == authentication.name")
    public void deleteTask(Long id, String email){
        taskRepository.deleteByTaskId(id);
    }
}
