package com.marcosdeDesarrollo.demo.EstilosPE.web.controller;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.ActualizarEstadoOrdenRequest;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.OrdenCompraRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.OrdenCompraResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.crud.OrdenCompraService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ordenes-compra")
@CrossOrigin(origins = "*")
@Tag(name = "Órdenes de compra", description = "Gestión de órdenes de compra con recepción de insumos/productos")
@PreAuthorize("hasAnyAuthority('ADMINISTRADOR','VENDEDOR')")
public class OrdenCompraController {

    private final OrdenCompraService ordenCompraService;

    public OrdenCompraController(OrdenCompraService ordenCompraService) {
        this.ordenCompraService = ordenCompraService;
    }

    @Operation(summary = "Listar órdenes de compra")
    @GetMapping
    public List<OrdenCompraResponseDto> listar(@RequestParam(required = false) String estado) {
        return ordenCompraService.listar(estado);
    }

    @Operation(summary = "Obtener orden por ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Orden encontrada"),
        @ApiResponse(responseCode = "404", description = "No existe la orden")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(ordenCompraService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Crear orden de compra")
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody OrdenCompraRequestDto request) {
        try {
            OrdenCompraResponseDto creada = ordenCompraService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(creada);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo crear la orden: " + e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar orden de compra")
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Integer id, @RequestBody OrdenCompraRequestDto request) {
        try {
            return ResponseEntity.ok(ordenCompraService.actualizar(id, request));
        } catch (IllegalArgumentException e) {
            HttpStatus status = e.getMessage().toLowerCase().contains("no existe") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo actualizar la orden: " + e.getMessage()));
        }
    }

    @Operation(summary = "Cambiar estado de la orden (incluye confirmación de recepción)")
    @PostMapping("/{id}/estado")
    public ResponseEntity<?> actualizarEstado(@PathVariable Integer id, @RequestBody ActualizarEstadoOrdenRequest request) {
        try {
            return ResponseEntity.ok(ordenCompraService.actualizarEstado(id, request));
        } catch (IllegalArgumentException e) {
            HttpStatus status = e.getMessage().toLowerCase().contains("no existe") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo actualizar el estado: " + e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar orden de compra")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            ordenCompraService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }
}
