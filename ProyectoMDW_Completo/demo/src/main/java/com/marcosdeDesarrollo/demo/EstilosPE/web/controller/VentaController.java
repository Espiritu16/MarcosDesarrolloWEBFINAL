package com.marcosdeDesarrollo.demo.EstilosPE.web.controller;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.VentaRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.VentaResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.VentasKpiDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.service.VentaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/ventas")
@CrossOrigin(origins = "*")
@Tag(name = "Ventas", description = "Gestión y consulta de ventas")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','VENDEDOR','CONTADOR')")
public class VentaController {

    private final VentaService ventaService;

    public VentaController(VentaService ventaService) {
        this.ventaService = ventaService;
    }

    @Operation(summary = "Listar ventas con filtros", description = "Devuelve la lista de ventas aplicando filtros opcionales por estado, método de pago, tipo de comprobante, rango de fechas y búsqueda por texto.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente",
                content = @Content(schema = @Schema(implementation = VentaResponseDto.class)))
    })
    @GetMapping
    public List<VentaResponseDto> listarVentas(
            @Parameter(description = "Estado de la venta", example = "Pagada")
            @RequestParam(required = false) String estado,
            @Parameter(description = "Método de pago utilizado", example = "Tarjeta")
            @RequestParam(required = false) String metodoPago,
            @Parameter(description = "Tipo de comprobante emitido", example = "Factura")
            @RequestParam(required = false) String tipoComprobante,
            @Parameter(description = "Fecha de inicio (yyyy-MM-dd)", example = "2024-05-01")
            @RequestParam(required = false) String fechaInicio,
            @Parameter(description = "Fecha de fin (yyyy-MM-dd)", example = "2024-05-31")
            @RequestParam(required = false) String fechaFin,
            @Parameter(description = "Búsqueda por cliente, referencia u otros campos", example = "Torres")
            @RequestParam(required = false, name = "search") String terminoBusqueda) {

        LocalDate inicio = parseFecha(fechaInicio);
        LocalDate fin = parseFecha(fechaFin);

        return ventaService.listarVentas(estado, metodoPago, tipoComprobante, inicio, fin, terminoBusqueda);
    }

    @Operation(summary = "Obtener una venta", description = "Recupera la información detallada de una venta por su ID.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Venta encontrada",
                content = @Content(schema = @Schema(implementation = VentaResponseDto.class))),
        @ApiResponse(responseCode = "404", description = "La venta no existe", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<VentaResponseDto> obtenerVenta(@PathVariable Integer id) {
        try {
            return ResponseEntity.ok(ventaService.obtenerVenta(id));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.notFound().build();
        }
    }

    @Operation(summary = "Registrar una nueva venta", description = "Crea una venta con sus detalles y ajusta el stock correspondiente.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Venta creada correctamente",
                content = @Content(schema = @Schema(implementation = VentaResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos en la solicitud", content = @Content)
    })
    @PostMapping
    public ResponseEntity<?> crearVenta(@RequestBody VentaRequestDto request) {
        try {
            VentaResponseDto venta = ventaService.crearVenta(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(venta);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Actualizar una venta", description = "Modifica la información de una venta existente y recalcula los totales y el stock si es necesario.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Venta actualizada correctamente",
                content = @Content(schema = @Schema(implementation = VentaResponseDto.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o estado inconsistente", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarVenta(@PathVariable Integer id, @RequestBody VentaRequestDto request) {
        try {
            VentaResponseDto venta = ventaService.actualizarVenta(id, request);
            return ResponseEntity.ok(venta);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.badRequest().body(error);
        }
    }

    @Operation(summary = "Eliminar una venta", description = "Elimina la venta indicada y revierte los movimientos de stock asociados.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Venta eliminada correctamente"),
        @ApiResponse(responseCode = "404", description = "La venta no existe", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarVenta(@PathVariable Integer id) {
        try {
            ventaService.eliminarVenta(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException ex) {
            Map<String, String> error = new HashMap<>();
            error.put("error", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }

    @Operation(summary = "KPI de ventas", description = "Devuelve indicadores clave como ventas del día, ingresos del mes, ticket promedio y productos vendidos.")
    @ApiResponse(responseCode = "200", description = "KPI obtenidos correctamente",
            content = @Content(schema = @Schema(implementation = VentasKpiDto.class)))
    @GetMapping("/kpis")
    public VentasKpiDto obtenerKpis() {
        return ventaService.obtenerKpis();
    }

    @Operation(summary = "Ventas del mes", description = "Lista las ventas registradas en el mes en curso.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente",
                content = @Content(schema = @Schema(implementation = VentaResponseDto.class)))
    })
    @GetMapping("/reporte-mensual")
    public List<VentaResponseDto> obtenerReporteMensual() {
        return ventaService.obtenerVentasDelMes();
    }

    private LocalDate parseFecha(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }
}
