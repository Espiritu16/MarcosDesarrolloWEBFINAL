package com.marcosdeDesarrollo.demo.EstilosPE.web.controller;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.ActualizarStockInsumoRequest;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.InsumoRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.InsumoResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.crud.InsumoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/insumos")
@CrossOrigin(origins = "*")
@Tag(name = "Insumos", description = "Endpoints para la gestión de insumos")
@PreAuthorize("hasAnyAuthority('ADMINISTRADOR','VENDEDOR')")
public class InsumoController {

    private final InsumoService insumoService;

    public InsumoController(InsumoService insumoService) {
        this.insumoService = insumoService;
    }

    @Operation(summary = "Listar insumos", description = "Devuelve los insumos registrados con filtros opcionales")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente")
    })
    @GetMapping
    public ResponseEntity<List<InsumoResponseDto>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String stock,
            @RequestParam(required = false, name = "search") String terminoBusqueda) {
        return ResponseEntity.ok(insumoService.listar(estado, stock, terminoBusqueda));
    }

    @Operation(summary = "Consultar insumo por ID", description = "Obtiene el detalle de un insumo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Insumo encontrado",
            content = @Content(schema = @Schema(implementation = InsumoResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Insumo no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(
            @Parameter(description = "ID del insumo", required = true, example = "1")
            @PathVariable Long id) {
        try {
            return ResponseEntity.ok(insumoService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Registrar un nuevo insumo", description = "Crea un insumo en el sistema")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Insumo creado",
            content = @Content(schema = @Schema(implementation = InsumoResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody InsumoRequestDto request) {
        try {
            InsumoResponseDto creado = insumoService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(creado);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo registrar el insumo: " + e.getMessage()));
        }
    }

    @Operation(summary = "Modificar datos de un insumo", description = "Actualiza la información de un insumo existente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Insumo actualizado",
            content = @Content(schema = @Schema(implementation = InsumoResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Insumo no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @Parameter(description = "ID del insumo a actualizar", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody InsumoRequestDto request) {
        try {
            InsumoResponseDto actualizado = insumoService.actualizar(id, request);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            HttpStatus status = e.getMessage().toLowerCase().contains("no existe") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "No se pudo actualizar el insumo: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @Operation(summary = "Actualizar stock de un insumo", description = "Permite ajustar la cantidad disponible de un insumo")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Stock actualizado",
            content = @Content(schema = @Schema(implementation = InsumoResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Insumo no encontrado")
    })
    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> actualizarStock(
            @Parameter(description = "ID del insumo", required = true, example = "1")
            @PathVariable Long id,
            @RequestBody ActualizarStockInsumoRequest request) {
        try {
            InsumoResponseDto actualizado = insumoService.actualizarStock(id, request);
            return ResponseEntity.ok(actualizado);
        } catch (IllegalArgumentException e) {
            HttpStatus status = e.getMessage().toLowerCase().contains("no existe") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "No se pudo actualizar el stock: " + e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar insumo", description = "Elimina lógicamente un insumo obsoleto")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Insumo eliminado"),
        @ApiResponse(responseCode = "404", description = "Insumo no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(
            @Parameter(description = "ID del insumo a eliminar", required = true, example = "1")
            @PathVariable Long id) {
        try {
            insumoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Estadísticas de insumos", description = "Totales rápidos para tarjetas de KPI")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estadísticas calculadas")
    })
    @GetMapping("/estadisticas")
    public Map<String, Long> estadisticas() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalInsumos", insumoService.contarTotal());
        stats.put("insumosActivos", insumoService.contarActivos());
        stats.put("stockBajo", insumoService.contarStockBajo());
        return stats;
    }
}
