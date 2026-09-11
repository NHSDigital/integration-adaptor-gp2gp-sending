package uk.nhs.adaptors.gp2gp.common.task;

import java.util.Arrays;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class TaskDefinitionFactory {
    private final ObjectMapper objectMapper;

    public TaskDefinition getTaskDefinition(String taskType, String body)  {
        return Arrays.stream(TaskType.values())
            .filter(type -> type.getTaskName().equals(taskType))
            .map(type -> unmarshallTask(type, body))
            .findFirst()
            .orElseThrow(() -> {
                LOGGER.error("No task definition class found for task type '{}'", taskType);
                return new TaskHandlerException("No task definition class for task type '" + taskType + "'");
            });
    }

    private TaskDefinition unmarshallTask(TaskType taskType, String body) {
        try {
            return objectMapper.readValue(body, taskType.getClassOfTaskDefinition());
        } catch (JsonProcessingException e) {
            LOGGER.error("Failed to deserialise task of type '{}'. Error: {}", taskType.getTaskName(), e.getMessage(), e);
            throw new TaskHandlerException("Unable to unmarshall task definition", e);
        }
    }
}
