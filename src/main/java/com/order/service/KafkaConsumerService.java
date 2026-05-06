package com.order.service;

import com.order.model.Orden;
import com.order.repository.OrdenRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class KafkaConsumerService {

    private static final Logger log = LoggerFactory.getLogger(KafkaConsumerService.class);

    @Autowired
    private OrdenRepository repository;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @KafkaListener(topics = "payment_received_events", groupId = "order-group")
    public void consumePaymentReceived(Map<String, Object> payload) {
        try {
            log.info("Received payment received event: {}", payload);
            Object ordenIdObj = payload.get("ordenId");
            String ordenId = ordenIdObj != null ? ordenIdObj.toString() : null;
            
            if (ordenId != null) {
                repository.findById(ordenId).ifPresent(orden -> {
                    log.info("Updating order {} status to PAGADO", ordenId);
                    orden.setStatus("PAGADO");
                    repository.save(orden);
                    
                    kafkaTemplate.send("order_status_changed_events", orden);
                    log.info("Emitted order_status_changed_events for order {}", ordenId);
                });
            }
        } catch (Exception e) {
            log.error("Error processing payment received event", e);
        }
    }
}
