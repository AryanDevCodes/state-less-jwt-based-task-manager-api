package com.microservice.taskmanager.controller;

import com.microservice.taskmanager.dto.TaskRequestDto;
import com.microservice.taskmanager.dto.TaskResponseDto;
import com.microservice.taskmanager.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<TaskResponseDto>> getAllTasks() {
        return ResponseEntity.ok(taskService.getMyTask());
    }

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<TaskResponseDto> createTask(@RequestBody TaskRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(request));
    }

    @DeleteMapping("/{id}")
    // Only the owner of the task or an Admin can delete it
    @PreAuthorize("hasRole('ADMIN') or @taskService.isOwner(#id, authentication.name)")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}