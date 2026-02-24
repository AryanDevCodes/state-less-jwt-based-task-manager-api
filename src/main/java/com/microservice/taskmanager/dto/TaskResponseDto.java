package com.microservice.taskmanager.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class TaskResponseDto implements Serializable {
    Long taskId;
    String headLine;
    String description;
}
