package com.marcosdeDesarrollo.demo.EstilosPE.domain.dto;

import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Estado;

public class UsuarioRequestDto {
    private Integer id_rol;
    private String contrasena;
    private String nombreUsuario;
    private Estado estado;
    private String email;
    public Integer getId_rol() {
        return id_rol;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setId_rol(Integer id_rol) {
        this.id_rol = id_rol;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public Estado getEstado() {
        return estado;
    }

    public void setEstado(Estado estado) {
        this.estado = estado;
    }
}
