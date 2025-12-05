package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

public class RecepcionDetalleRequest {
    private Integer idDetalle;
    private Integer cantidadRecibida;

    public Integer getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(Integer idDetalle) {
        this.idDetalle = idDetalle;
    }

    public Integer getCantidadRecibida() {
        return cantidadRecibida;
    }

    public void setCantidadRecibida(Integer cantidadRecibida) {
        this.cantidadRecibida = cantidadRecibida;
    }
}
