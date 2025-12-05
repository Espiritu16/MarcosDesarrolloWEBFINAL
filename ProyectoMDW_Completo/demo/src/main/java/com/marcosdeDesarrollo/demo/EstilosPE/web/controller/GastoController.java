package com.marcosdeDesarrollo.demo.EstilosPE.web.controller;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.GastoRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.GastoResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.GastoUpdateRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.crud.GastoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
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
@RequestMapping("/api/gastos")
@CrossOrigin(origins = "*")
@Tag(name = "Gastos", description = "Endpoints para la administración de gastos generales")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','CONTADOR')")
public class GastoController {

    private final GastoService gastoService;

    public GastoController(GastoService gastoService) {
        this.gastoService = gastoService;
    }

    @Operation(summary = "Listar todos los gastos", description = "Retorna la lista completa de gastos ordenados por fecha (más recientes primero)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista obtenida exitosamente")
    })
    @GetMapping
    public ResponseEntity<List<GastoResponseDto>> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) LocalDate fechaInicio,
            @RequestParam(required = false) LocalDate fechaFin,
            @RequestParam(required = false) String terminoBusqueda) {
        return ResponseEntity.ok(gastoService.listar(estado, tipo, fechaInicio, fechaFin, terminoBusqueda));
    }

    @Operation(summary = "Registrar un nuevo gasto", description = "Crea un nuevo gasto asociado al usuario autenticado")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Gasto creado exitosamente",
            content = @Content(schema = @Schema(implementation = GastoResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos en la solicitud")
    })
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody GastoRequestDto request) {
        try {
            GastoResponseDto creado = gastoService.crear(request);
            return ResponseEntity.created(URI.create("/api/gastos/" + creado.getId())).body(creado);
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error interno al crear el gasto: " + e.getMessage()));
        }
    }

    @Operation(summary = "Obtener gasto por ID", description = "Devuelve el detalle de un gasto en particular")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Gasto encontrado",
            content = @Content(schema = @Schema(implementation = GastoResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "Gasto no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<?> obtenerPorId(
            @Parameter(description = "ID del gasto", required = true, example = "1")
            @PathVariable Integer id) {
        try {
            return ResponseEntity.ok(gastoService.obtenerPorId(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Actualizar un gasto existente", description = "Permite actualizar los campos de un gasto, incluyendo su estado")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Gasto actualizado correctamente",
            content = @Content(schema = @Schema(implementation = GastoResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
        @ApiResponse(responseCode = "404", description = "Gasto no encontrado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @Parameter(description = "ID del gasto", required = true, example = "1")
            @PathVariable Integer id,
            @RequestBody GastoUpdateRequestDto request) {
        try {
            return ResponseEntity.ok(gastoService.actualizar(id, request));
        } catch (IllegalArgumentException e) {
            HttpStatus status = e.getMessage().toLowerCase().contains("no existe") ? HttpStatus.NOT_FOUND : HttpStatus.BAD_REQUEST;
            return ResponseEntity.status(status).body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(summary = "Eliminar un gasto", description = "Elimina permanentemente un gasto por su ID")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Gasto eliminado"),
        @ApiResponse(responseCode = "404", description = "Gasto no encontrado")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(
            @Parameter(description = "ID del gasto", required = true, example = "1")
            @PathVariable Integer id) {
        try {
            gastoService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
}
