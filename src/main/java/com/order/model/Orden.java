package com.order.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "ordenes")
public class Orden {
    @Id
    private String id;
    private String usuarioId;
    private String productoId;
    private Integer cantidad;
    private Double precioTotal;
    private String status; // PENDIENTE, PAGADO, CANCELADO
    private LocalDateTime fechaCreacion;
}
