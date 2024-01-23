package org.converter.rabbit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.log4j.Log4j2;
import org.converter.entity.FileEntity;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitMessagingTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Component
public class Sender {

    RabbitTemplate rabbitMessagingTemplate;

    ObjectMapper objectMapper = new ObjectMapper();

    public void send(FileEntity fileEntity) {
        try {
            objectMapper.registerModule(new JavaTimeModule());
            String json = objectMapper.writeValueAsString(fileEntity);
            rabbitMessagingTemplate.convertAndSend(Listener.TASKS_EXCHANGE, null, json);
        } catch (JsonProcessingException e) {
            e.printStackTrace(); // Обработка ошибки сериализации в JSON
        }
    }
}
