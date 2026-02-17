package com.microservice.taskmanager.service;

import com.microservice.taskmanager.dto.TaskRequestDto;
import com.microservice.taskmanager.dto.TaskResponseDto;
import com.microservice.taskmanager.entity.Task;
import com.microservice.taskmanager.entity.User;
import com.microservice.taskmanager.repositories.TaskRepository;
import com.microservice.taskmanager.repositories.UserRepository;
import com.microservice.taskmanager.service.mapper.TaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final TaskMapper taskMapper;
    private final UserRepository userRepository;

    @PreAuthorize("hasRole('USER')")
    public List<TaskResponseDto> getMyTask() {
        // retrieve current user
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return taskRepository.findByUser_Email(email)
                .stream().map(taskMapper::toResponseDto)
                .toList();
    }

    @PreAuthorize("hasRole('USER')")
    public TaskResponseDto createTask(TaskRequestDto dto) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        Task task = taskMapper.toEntity(dto);
        task.setUser(user);
        Task savedTask = taskRepository.save(task);
        return taskMapper.toResponseDto(savedTask);
    }

    @Transactional
    public void deleteTask(long id) {
        taskRepository.deleteByTaskId(id);
    }

    @Transactional
    @PreAuthorize("hasRole('USER')")
    public void deleteTaskByIdAndUser(Long id) {
        String currentUserEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found"));

        // Check if user is owner or admin
        boolean isOwner = task.getUser().getEmail().equals(currentUserEmail);
        boolean isAdmin = SecurityContextHolder.getContext().getAuthentication().getAuthorities()
                .stream().anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

        if (!isOwner && !isAdmin) {
            throw new RuntimeException("Unauthorized: You can only delete your own tasks");
        }

        taskRepository.deleteByTaskId(id);
    }

    public boolean isOwner(Long taskId, String email) {
        Task task = taskRepository.findById(taskId).orElse(null);
        return task != null && task.getUser().getEmail().equals(email);
    }
}
