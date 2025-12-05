package com.marcosdeDesarrollo.demo.EstilosPE.web.controller;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.RolRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.RolResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.crud.RolService;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/roles")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMINISTRADOR')")
@Tag(name = "Roles", description = "Endpoints para gestión de roles de usuario") // 👈 NUEVO
public class RolController {

    private final RolService rolService;

    public RolController(RolService rolService) {
        this.rolService = rolService;
    }

    @Operation(
        summary = "Listar todos los roles", 
        description = "Obtiene la lista completa de roles disponibles en el sistema"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Lista de roles obtenida exitosamente"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping
    public List<RolResponseDto> listar() {
        return rolService.listarTodos();
    }

    @Operation(
        summary = "Obtener rol por ID", 
        description = "Busca y retorna un rol específico mediante su ID"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rol encontrado exitosamente"),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<RolResponseDto> obtenerPorId(
            @Parameter(description = "ID del rol a buscar", required = true, example = "1")
            @PathVariable Integer id) {
        return rolService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "Crear nuevo rol", 
        description = "Crea un nuevo rol en el sistema. Solo disponible para administradores"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Rol creado exitosamente",
                    content = @Content(schema = @Schema(implementation = RolResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos en la solicitud"),
        @ApiResponse(responseCode = "401", description = "No autorizado"),
        @ApiResponse(responseCode = "403", description = "Permisos insuficientes")
    })
    @PostMapping
    public ResponseEntity<?> crear(
            @Parameter(description = "Datos del rol a crear", required = true)
            @RequestBody RolRequestDto request) {
        try {
            RolResponseDto nuevo = rolService.crear(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @Operation(
        summary = "Actualizar rol existente", 
        description = "Actualiza la información de un rol específico. No permite cambiar roles del sistema base"
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Rol actualizado exitosamente",
                    content = @Content(schema = @Schema(implementation = RolResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o rol del sistema"),
        @ApiResponse(responseCode = "404", description = "Rol no encontrado"),
        @ApiResponse(responseCode = "401", description = "No autorizado")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @Parameter(description = "ID del rol a actualizar", required = true, example = "1")
            @PathVariable Integer id,

            @Parameter(description = "Nuevos datos del rol", required = true)
            @RequestBody RolRequestDto request) {
        try {
            return rolService.actualizar(id, request)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Integer id) {
        try {
            rolService.eliminar(id);
            return ResponseEntity.ok(Map.of("mensaje", "Rol eliminado correctamente"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error al eliminar el rol"));
        }
    }
}
