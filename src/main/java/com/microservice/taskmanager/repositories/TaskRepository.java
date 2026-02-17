package com.microservice.taskmanager.repositories;

import com.microservice.taskmanager.entity.Task;
import org.springframework.data.repository.Repository;

import java.util.List;

public interface TaskRepository extends Repository<Task, Long> {
    List<Task> findByUser_Email(String email);

    void deleteByTaskId( Long taskId );
}