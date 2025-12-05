package com.marcosdeDesarrollo.demo.EstilosPE.web.controller;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.DashboardCategoriaDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.DashboardMonthlySerieDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.DashboardPaymentMethodDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.DashboardSummaryDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.DashboardVentaRecienteDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.service.DashboardService;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMINISTRADOR','VENDEDOR','CONTADOR')")
@Tag(name = "Dashboard", description = "Indicadores y métricas del panel principal")
public class DashboardController {

    private static final int MESES_SERIE = 6;
    private static final int TOP_CATEGORIAS = 5;

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "Resumen general del dashboard",
            description = "Devuelve los KPI principales del día y del mes (ventas, ingresos, ticket promedio y productos vendidos).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Resumen obtenido correctamente",
                content = @Content(schema = @Schema(implementation = DashboardSummaryDto.class)))
    })
    @GetMapping("/resumen")
    public DashboardSummaryDto resumen() {
        return dashboardService.obtenerResumen();
    }

    @Operation(summary = "Ventas registradas hoy",
            description = "Lista las ventas realizadas en la fecha actual ordenadas de más reciente a más antigua.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ventas obtenidas correctamente",
                content = @Content(schema = @Schema(implementation = DashboardVentaRecienteDto.class)))
    })
    @GetMapping("/ventas-recientes")
    public List<DashboardVentaRecienteDto> ventasRecientes() {
        return dashboardService.obtenerVentasDelDia();
    }

    @Operation(summary = "Serie de ventas por mes",
            description = "Devuelve la serie de ventas agregadas por mes para los últimos meses configurados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serie de ventas obtenida correctamente",
                content = @Content(schema = @Schema(implementation = DashboardMonthlySerieDto.class)))
    })
    @GetMapping("/ventas-mensuales")
    public List<DashboardMonthlySerieDto> ventasMensuales() {
        return dashboardService.obtenerVentasMensuales(MESES_SERIE);
    }

    @Operation(summary = "Serie de gastos por mes",
            description = "Devuelve la serie de gastos agregados por mes para los últimos meses configurados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serie de gastos obtenida correctamente",
                content = @Content(schema = @Schema(implementation = DashboardMonthlySerieDto.class)))
    })
    @GetMapping("/gastos-mensuales")
    public List<DashboardMonthlySerieDto> gastosMensuales() {
        return dashboardService.obtenerGastosMensuales(MESES_SERIE);
    }

    @Operation(summary = "Ventas por día del mes",
            description = "Devuelve la suma diaria de ventas para el mes indicado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serie diaria de ventas obtenida correctamente",
                content = @Content(schema = @Schema(implementation = DashboardMonthlySerieDto.class))),
        @ApiResponse(responseCode = "400", description = "Parámetros inválidos", content = @Content)
    })
    @GetMapping("/ventas-por-dia")
    public List<DashboardMonthlySerieDto> ventasPorDia(
            @Parameter(description = "Año consultado", example = "2024") @RequestParam int year,
            @Parameter(description = "Mes (1-12)", example = "5") @RequestParam int month) {
        return dashboardService.obtenerVentasPorDia(year, month);
    }

    @Operation(summary = "Gastos por día del mes",
            description = "Devuelve la suma diaria de gastos para el mes indicado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Serie diaria de gastos obtenida correctamente",
                content = @Content(schema = @Schema(implementation = DashboardMonthlySerieDto.class))),
        @ApiResponse(responseCode = "400", description = "Parámetros inválidos", content = @Content)
    })
    @GetMapping("/gastos-por-dia")
    public List<DashboardMonthlySerieDto> gastosPorDia(
            @Parameter(description = "Año consultado", example = "2024") @RequestParam int year,
            @Parameter(description = "Mes (1-12)", example = "5") @RequestParam int month) {
        return dashboardService.obtenerGastosPorDia(year, month);
    }

    @Operation(summary = "Top categorías más vendidas",
            description = "Retorna las categorías con mayor número de unidades vendidas en los últimos 30 días.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente",
                content = @Content(schema = @Schema(implementation = DashboardCategoriaDto.class)))
    })
    @GetMapping("/top-categorias")
    public List<DashboardCategoriaDto> topCategorias() {
        return dashboardService.obtenerTopCategorias(TOP_CATEGORIAS);
    }

    @Operation(summary = "Métodos de pago más utilizados",
            description = "Devuelve la cantidad de ventas por método de pago considerando los últimos meses configurados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado obtenido correctamente",
                content = @Content(schema = @Schema(implementation = DashboardPaymentMethodDto.class)))
    })
    @GetMapping("/metodos-pago")
    public List<DashboardPaymentMethodDto> metodosPago() {
        return dashboardService.obtenerMetodosPago(MESES_SERIE);
    }
}
