package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

public class ActualizarStockInsumoRequest {
    private Integer ajuste;
    private Integer nuevoStock;
    private String motivo;

    public Integer getAjuste() {
        return ajuste;
    }

    public void setAjuste(Integer ajuste) {
        this.ajuste = ajuste;
    }

    public Integer getNuevoStock() {
        return nuevoStock;
    }

    public void setNuevoStock(Integer nuevoStock) {
        this.nuevoStock = nuevoStock;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
}
