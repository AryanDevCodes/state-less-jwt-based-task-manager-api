package com.microservice.taskmanager.dto;


import java.io.Serializable;


public class TaskRequestDto implements Serializable {
    Long taskId;
    String headLine;
    String description;
}