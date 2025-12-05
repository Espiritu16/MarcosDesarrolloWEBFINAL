package com.marcosdeDesarrollo.demo.EstilosPE.domain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Auditoria;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.TipoOperacion;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.AuditoriaRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.web.security.UserDetailsImpl;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private static final Logger logger = LoggerFactory.getLogger(AuditoriaService.class);

    private final AuditoriaRepository auditoriaRepository;
    private final ObjectMapper objectMapper;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, ObjectMapper objectMapper) {
        this.auditoriaRepository = auditoriaRepository;
        this.objectMapper = objectMapper;
    }

    public void registrarInsert(String tabla, Integer idRegistro, Object datosNuevos, String detalle) {
        registrar(tabla, idRegistro, TipoOperacion.INSERT, null, datosNuevos, detalle);
    }

    public void registrarUpdate(String tabla, Integer idRegistro, Object datosAnteriores, Object datosNuevos,
            String detalle) {
        registrar(tabla, idRegistro, TipoOperacion.UPDATE, datosAnteriores, datosNuevos, detalle);
    }

    private void registrar(String tabla,
            Integer idRegistro,
            TipoOperacion tipo,
            Object datosAnteriores,
            Object datosNuevos,
            String detalle) {
        try {
            Auditoria auditoria = new Auditoria();
            auditoria.setTablaAfectada(tabla);
            auditoria.setIdRegistro(idRegistro);
            auditoria.setTipoOperacion(tipo);
            auditoria.setDatosAnteriores(toJson(datosAnteriores));
            auditoria.setDatosNuevos(toJson(datosNuevos));
            auditoria.setDetallesCambio(detalle);
            auditoria.setIdUsuario(obtenerIdUsuarioActual());
            auditoriaRepository.save(auditoria);
        } catch (Exception ex) {
            logger.error("Error registrando auditoría para tabla {} id {}", tabla, idRegistro, ex);
        }
    }
    public void registrarDelete(String tabla, Integer idRegistro, Object datosAnteriores, String detalle) {
        registrar(tabla, idRegistro, TipoOperacion.DELETE, datosAnteriores, null, detalle);
    }
    private String toJson(Object data) {
        if (data == null) {
            return null;
        }
        if (data instanceof String str) {
            return str;
        }
        if (data instanceof Map<?, ?> mapa && mapa.isEmpty()) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            logger.warn("No se pudo serializar datos para auditoría", e);
            return null;
        }
    }

    private int obtenerIdUsuarioActual() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetailsImpl user) {
                    return user.getId();
                }
            }
        } catch (Exception ex) {
            logger.debug("No se pudo obtener el usuario autenticado para auditoría", ex);
        }
        return 0; // valor por defecto si no hay usuario autenticado
    }
}
