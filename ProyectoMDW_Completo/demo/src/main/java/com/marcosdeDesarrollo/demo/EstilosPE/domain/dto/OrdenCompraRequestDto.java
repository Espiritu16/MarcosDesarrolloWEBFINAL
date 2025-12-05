package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

import java.time.LocalDate;
import java.util.List;

public class OrdenCompraRequestDto {
    private Integer idOrden;
    private String proveedorNombre;
    private String proveedorContacto;
    private String proveedorTelefono;
    private String proveedorEmail;
    private LocalDate fecha;
    private String estado;
    private List<OrdenCompraDetalleRequestDto> detalles;

    public Integer getIdOrden() {
        return idOrden;
    }

    public void setIdOrden(Integer idOrden) {
        this.idOrden = idOrden;
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

    public List<OrdenCompraDetalleRequestDto> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<OrdenCompraDetalleRequestDto> detalles) {
        this.detalles = detalles;
    }
}
