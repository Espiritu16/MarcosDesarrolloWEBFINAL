package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

import java.util.List;

public class ActualizarEstadoOrdenRequest {
    private String estado;
    private List<RecepcionDetalleRequest> recepciones;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public List<RecepcionDetalleRequest> getRecepciones() {
        return recepciones;
    }

    public void setRecepciones(List<RecepcionDetalleRequest> recepciones) {
        this.recepciones = recepciones;
    }
}
