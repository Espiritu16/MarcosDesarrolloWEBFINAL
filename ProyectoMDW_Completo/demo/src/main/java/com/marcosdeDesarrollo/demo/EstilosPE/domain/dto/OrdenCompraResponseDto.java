package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class OrdenCompraResponseDto {
    private Integer idOrden;
    private LocalDate fecha;
    private String estado;
    private BigDecimal total;
    private String proveedorNombre;
    private String proveedorContacto;
    private String proveedorTelefono;
    private String proveedorEmail;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;
    private List<OrdenCompraDetalleResponseDto> detalles;

    public Integer getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(Integer idOrden) {
        this.idOrden = idOrden;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
    }

    public String getProveedorNombre() {
        return proveedorNombre;
    }

    public void setProveedorNombre(String proveedorNombre) {
        this.proveedorNombre = proveedorNombre;
    }

    public String getProveedorContacto() {
        return proveedorContacto;
    }

    public void setProveedorContacto(String proveedorContacto) {
        this.proveedorContacto = proveedorContacto;
    }

    public String getProveedorTelefono() {
        return proveedorTelefono;
    }

    public void setProveedorTelefono(String proveedorTelefono) {
        this.proveedorTelefono = proveedorTelefono;
    }

    public String getProveedorEmail() {
        return proveedorEmail;
    }

    public void setProveedorEmail(String proveedorEmail) {
        this.proveedorEmail = proveedorEmail;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public List<OrdenCompraDetalleResponseDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<OrdenCompraDetalleResponseDto> detalles) {
        this.detalles = detalles;
    }
}
