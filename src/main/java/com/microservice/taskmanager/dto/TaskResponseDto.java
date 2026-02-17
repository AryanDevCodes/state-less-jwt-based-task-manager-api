package com.microservice.taskmanager.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class TaskResponseDto implements Serializable {
    String headLine;
    String description;
}
