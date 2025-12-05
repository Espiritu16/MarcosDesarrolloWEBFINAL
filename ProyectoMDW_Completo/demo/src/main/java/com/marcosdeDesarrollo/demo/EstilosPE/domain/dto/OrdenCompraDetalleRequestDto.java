package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

import java.math.BigDecimal;

public class OrdenCompraDetalleRequestDto {
    private Integer itemId;
    private String tipoItem; // PRODUCTO o INSUMO
    private Integer cantidad;
    private BigDecimal precioUnitario;
    private String nombrePersonalizado;
    private String descripcionPersonalizada;
    private String unidadMedidaPersonalizada;

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

    public String getNombrePersonalizado() {
        return nombrePersonalizado;
    }

    public void setNombrePersonalizado(String nombrePersonalizado) {
        this.nombrePersonalizado = nombrePersonalizado;
    }

    public String getDescripcionPersonalizada() {
        return descripcionPersonalizada;
    }

    public void setDescripcionPersonalizada(String descripcionPersonalizada) {
        this.descripcionPersonalizada = descripcionPersonalizada;
    }

    public String getUnidadMedidaPersonalizada() {
        return unidadMedidaPersonalizada;
    }

    public void setUnidadMedidaPersonalizada(String unidadMedidaPersonalizada) {
        this.unidadMedidaPersonalizada = unidadMedidaPersonalizada;
    }
}
