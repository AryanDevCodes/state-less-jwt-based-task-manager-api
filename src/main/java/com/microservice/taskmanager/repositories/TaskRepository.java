package com.microservice.taskmanager.repositories;

import com.microservice.taskmanager.entity.Task;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface TaskRepository extends JpaRepository<Task, Long> {
    Page<Task> findByUser_Email(String email, Pageable pageable);

    void deleteByTaskId(Long taskId);
}