package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

import java.math.BigDecimal;

public class OrdenCompraDetalleResponseDto {
    private Integer idDetalle;
    private Integer itemId;
    private String tipoItem;
    private String nombreItem;
    private String descripcionPersonalizada;
    private String unidadPersonalizada;
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private BigDecimal subtotal;

    public Integer getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(Integer idDetalle) {
        this.idDetalle = idDetalle;
    }

    public Integer getItemId() {
        return itemId;
    }

    public void setItemId(Integer itemId) {
        this.itemId = itemId;
    }

    public String getTipoItem() {
        return tipoItem;
    }

    public void setTipoItem(String tipoItem) {
        this.tipoItem = tipoItem;
    }

    public String getNombreItem() {
        return nombreItem;
    }

    public void setNombreItem(String nombreItem) {
        this.nombreItem = nombreItem;
    }

    public String getDescripcionPersonalizada() {
        return descripcionPersonalizada;
    }

    public void setDescripcionPersonalizada(String descripcionPersonalizada) {
        this.descripcionPersonalizada = descripcionPersonalizada;
    }

    public String getUnidadPersonalizada() {
        return unidadPersonalizada;
    }

    public void setUnidadPersonalizada(String unidadPersonalizada) {
        this.unidadPersonalizada = unidadPersonalizada;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(BigDecimal precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public BigDecimal getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(BigDecimal subtotal) {
        this.subtotal = subtotal;
    }
}
