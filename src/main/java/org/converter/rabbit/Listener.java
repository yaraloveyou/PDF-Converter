package org.converter.rabbit;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.log4j.Log4j2;
import org.converter.entity.FileEntity;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
@Component
public class Listener {
    public static final String TASKS_QUEUE = "converter.tasks.queue";
    public static final String TASKS_EXCHANGE = "converter.tasks.exchange";

    @Value("${server.port}")
    @NonFinal Integer serverPort;
    @Autowired
    private ObjectMapper objectMapper;

    // Прием задачи
    @SneakyThrows
    @RabbitListener(
            bindings = @QueueBinding(
                    value = @Queue(value = TASKS_QUEUE, durable = Exchange.TRUE),
                    exchange = @Exchange(value = TASKS_EXCHANGE)
            )
    )
    public void handleMessage(String json) {
        Thread.sleep(1000);
        try {
            FileEntity fileEntity = objectMapper.readValue(json, FileEntity.class);
            log.info("on port: {}", serverPort);
            log.info("Received FileEntity: {}", fileEntity.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
