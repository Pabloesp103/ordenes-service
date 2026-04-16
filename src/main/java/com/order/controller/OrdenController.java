package com.order.controller;

import com.order.client.ProductoFeignClient;
import com.order.model.Orden;
import com.order.model.ProductoDTO;
import com.order.repository.OrdenRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/ordenes")
public class OrdenController {

    @Autowired
    private OrdenRepository repository;

    @Autowired
    private ProductoFeignClient productoClient;

    @Autowired
    private org.springframework.kafka.core.KafkaTemplate<String, Object> kafkaTemplate;

    @PostMapping
    public Orden create(@RequestBody Orden orden, @RequestHeader(value = "X-Retry-Attempt", required = false) String isRetry) {
        log.info("Iniciando creación de orden para el producto ID: {}", orden.getProductoId());
        
        try {
            ProductoDTO producto = productoClient.getProductoById(orden.getProductoId());
            
            if (producto != null && producto.getStock() >= orden.getCantidad()) {
                orden.setPrecioTotal(producto.getPrecio() * orden.getCantidad());
                orden.setStatus("PENDIENTE");
                orden.setFechaCreacion(LocalDateTime.now());
                Orden nuevaOrden = repository.save(orden);
                log.info("Orden creada exitosamente con ID: {}", nuevaOrden.getId());
                return nuevaOrden;
            }
            
            throw new RuntimeException("Producto no encontrado o stock insuficiente");
        } catch (Exception e) {
            log.error("Error al crear orden. Validando reenvío. Error: {}", e.getMessage());

            if ("true".equals(isRetry)) {
                log.warn("El reintento falló de nuevo. NO se re-enviará a Kafka para evitar bucles.");
                throw new RuntimeException("Reintento fallido persistente. Deteniendo ciclo.", e);
            }
            
            log.info("Enviando a reintento (Kafka)...");
            java.util.Map<String, Object> payload = new java.util.HashMap<>();
            payload.put("data", orden);
            payload.put("sendEmail", new java.util.HashMap<String, String>() {{
                put("status", "PENDING");
                put("message", "Pendiente de reintento");
            }});
            payload.put("updateRetryJobs", new java.util.HashMap<String, String>() {{
                put("status", "PENDING");
                put("message", "Pendiente de reintento");
            }});
            
            kafkaTemplate.send("order_retry_jobs", payload);
            throw new RuntimeException("Error al crear orden. Enviado a cola de reintentos.", e);
        }
    }

    @GetMapping("/{id}")
    public Orden getById(@PathVariable String id) {
        log.info("Consultando orden ID: {}", id);
        return repository.findById(id).orElse(null);
    }

    @GetMapping("/usuario/{usuarioId}")
    public List<Orden> getByUsuario(@PathVariable String usuarioId) {
        log.info("Consultando órdenes del usuario ID: {}", usuarioId);
        return repository.findByUsuarioId(usuarioId);
    }

    @PutMapping("/{id}/status")
    public Orden updateStatus(@PathVariable String id, @RequestParam String status) {
        log.info("Actualizando estatus de orden ID: {} a {}", id, status);
        Orden orden = repository.findById(id).orElseThrow(() -> new RuntimeException("Orden no encontrada"));
        orden.setStatus(status);
        return repository.save(orden);
    }
}
