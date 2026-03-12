package com.order.client;

import com.order.model.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "productservice")
public interface ProductoFeignClient {
    @GetMapping("/productos/{id}")
    ProductoDTO getProductoById(@PathVariable("id") String id);
}
