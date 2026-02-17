package com.microservice.taskmanager.repositories;

import com.microservice.taskmanager.entity.Task;
import org.springframework.data.repository.Repository;

public interface TaskRepository extends Repository<Task, Long> {
}