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

    @PostMapping
    public Orden create(@RequestBody Orden orden) {
        log.info("Iniciando creación de orden para el producto ID: {}", orden.getProductoId());
        
        ProductoDTO producto = productoClient.getProductoById(orden.getProductoId());
        
        if (producto != null && producto.getStock() >= orden.getCantidad()) {
            orden.setPrecioTotal(producto.getPrecio() * orden.getCantidad());
            orden.setStatus("PENDIENTE");
            orden.setFechaCreacion(LocalDateTime.now());
            Orden nuevaOrden = repository.save(orden);
            log.info("Orden creada exitosamente con ID: {}", nuevaOrden.getId());
            return nuevaOrden;
        }
        
        log.error("Fallo al crear orden: Producto no encontrado o stock insuficiente");
        throw new RuntimeException("Producto no encontrado o stock insuficiente");
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
