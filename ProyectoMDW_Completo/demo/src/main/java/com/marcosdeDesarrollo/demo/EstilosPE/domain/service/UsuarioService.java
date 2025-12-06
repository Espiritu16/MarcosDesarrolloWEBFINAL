package com.marcosdeDesarrollo.demo.EstilosPE.domain.service;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.UsuarioRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.UsuarioResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.AuditoriaRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.RolRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.UsuarioRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Auditoria;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Estado;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Rol;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Usuario;
import com.marcosdeDesarrollo.demo.EstilosPE.web.security.UserDetailsImpl;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UsuarioService {
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final AuditoriaService auditoriaService;
    private final AuditoriaRepository auditoriaRepository;
    public UsuarioService(UsuarioRepository usuarioRepository,
                          RolRepository rolRepository,
                          AuditoriaService auditoriaService,
                          AuditoriaRepository auditoriaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.auditoriaService = auditoriaService;
        this.auditoriaRepository = auditoriaRepository;
    }
    @Transactional(readOnly = true)
    public List<UsuarioResponseDto> listarTodos() {
        return usuarioRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    public UsuarioResponseDto crear(UsuarioRequestDto request) {
        validarEmailUnico(request.getEmail(), null);

        Rol rol = rolRepository.findById(request.getId_rol())
                .orElseThrow(() -> new IllegalArgumentException("El rol especificado no existe"));

        Usuario usuario = new Usuario();
        usuario.setEmail(request.getEmail());
        usuario.setNombreUsuario(request.getNombreUsuario());
        usuario.setContrasena(request.getContrasena());
        usuario.setEstado(request.getEstado());
        usuario.setRol(rol);

        Usuario guardado = usuarioRepository.save(usuario);

        // 🔥 Registrar auditoría
        auditoriaService.registrarInsert("usuarios", guardado.getId(),
                construirDatosAuditoria(guardado), "Creación de usuario");

        return mapToResponse(guardado);
    }
    public Optional<UsuarioResponseDto> actualizar(Integer id, UsuarioRequestDto request) {
        return usuarioRepository.findById(id).map(usuario -> {
            validarEmailUnico(request.getEmail(), id);
            Map<String, Object> datosAnteriores = construirDatosAuditoria(usuario);
            Rol rol = rolRepository.findById(request.getId_rol())
                    .orElseThrow(() -> new IllegalArgumentException("El rol especificado no existe"));
            usuario.setEmail(request.getEmail());
            usuario.setNombreUsuario(request.getNombreUsuario());
            // Solo actualizar contraseña si viene en el request
            if (request.getContrasena() != null && !request.getContrasena().isBlank()) {
                usuario.setContrasena(request.getContrasena());
            }
            usuario.setEstado(request.getEstado());
            usuario.setRol(rol);
            Usuario actualizado = usuarioRepository.save(usuario);
            auditoriaService.registrarUpdate("usuarios", actualizado.getId(),
                    datosAnteriores, construirDatosAuditoria(actualizado),
                    "Actualización de usuario");
            return mapToResponse(actualizado);
        });
    }
    private void validarEmailUnico(String email, Integer id) {
        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("El email del usuario es obligatorio");
        }

        boolean existe = (id == null)
                ? usuarioRepository.existsByEmail(email)
                : usuarioRepository.existsByEmailAndIdNot(email, id);

        if (existe) {
            throw new IllegalArgumentException("Ya existe un usuario con ese email");
        }
    }
    private UsuarioResponseDto mapToResponse(Usuario usuario) {
        UsuarioResponseDto dto = new UsuarioResponseDto();
        dto.setId(usuario.getId());
        dto.setEmail(usuario.getEmail());
        dto.setNombreUsuario(usuario.getNombreUsuario());
        dto.setRol(usuario.getRol() != null ? usuario.getRol().getNombre() : null);
        dto.setEstado(usuario.getEstado());
        dto.setFechaCreacion(usuario.getFechaCreacion());
        dto.setFechaActualizacion(usuario.getFechaActualizacion());
        Auditoria auditoriaUsuario = obtenerAuditoriaReciente(usuario.getId());
        dto.setActualizadoPor(obtenerNombreUsuario(auditoriaUsuario));

        return dto;
    }

    // 🔥 MÉTODO CORREGIDO
    private Auditoria obtenerAuditoriaReciente(Integer usuarioId) {
        return auditoriaRepository
                .findTopByTablaAfectadaAndIdRegistroOrderByFechaDesc("usuarios", usuarioId)  // 👈 "usuarios", no "roles"
                .orElse(null);
    }
    private String obtenerNombreUsuario(Auditoria auditoria) {
        if (auditoria == null) {
            return "Sistema";
        }
        Integer idUsuario = auditoria.getIdUsuario();
        if (idUsuario == null || idUsuario == 0) {
            return "Sistema";
        }
        return usuarioRepository.findById(idUsuario)
                .map(Usuario::getNombreUsuario)
                .orElse("Usuario " + idUsuario);
    }

    // 🔥 MÉTODO NUEVO
    private Map<String, Object> construirDatosAuditoria(Usuario usuario) {
        Map<String, Object> datos = new LinkedHashMap<>();
        datos.put("id", usuario.getId());
        datos.put("email", usuario.getEmail());
        datos.put("nombreUsuario", usuario.getNombreUsuario());
        datos.put("estado", usuario.getEstado());
        datos.put("rol", usuario.getRol() != null ? usuario.getRol().getNombre() : null);
        datos.put("fechaCreacion", usuario.getFechaCreacion());
        // NO incluir contraseña por seguridad
        return datos;
    }
    public Optional<UsuarioResponseDto> eliminar(Integer id) {
        return usuarioRepository.findById(id).map(usuario -> {
            // Validar que no sea el usuario actual
            Integer idUsuarioActual = obtenerIdUsuarioActual();
            if (usuario.getId().equals(idUsuarioActual)) {
                throw new IllegalArgumentException("No puedes eliminar tu propio usuario");
            }
            // Validar que no esté ya inactivo
            if (usuario.getEstado() == Estado.Inactivo) {
                throw new IllegalArgumentException("El usuario ya está inactivo");
            }
            // Guardar estado anterior para auditoría
            Map<String, Object> datosAnteriores = construirDatosAuditoria(usuario);
            // Cambiar estado a Inactivo
            usuario.setEstado(Estado.Inactivo);

            Usuario actualizado = usuarioRepository.save(usuario);

            // Registrar en auditoría
            auditoriaService.registrarUpdate("usuarios", actualizado.getId(),
                    datosAnteriores, construirDatosAuditoria(actualizado),
                    "Eliminación lógica de usuario");

            return mapToResponse(actualizado);
        });
    }
    public Optional<UsuarioResponseDto> reactivar (Integer id){
        return usuarioRepository.findById(id).map(usuario -> {
            // Validar que no sea el usuario actual
            Integer idUsuarioActual = obtenerIdUsuarioActual();
            if (usuario.getEstado() == Estado.Activo) {
                throw new IllegalArgumentException("El usuario ya está activo");
            }
            // Guardar estado anterior para auditoría
            Map<String, Object> datosAnteriores = construirDatosAuditoria(usuario);
            // Cambiar estado a Inactivo
            usuario.setEstado(Estado.Activo);
            Usuario actualizado = usuarioRepository.save(usuario);
            // Registrar en auditoría
            auditoriaService.registrarUpdate("usuarios", actualizado.getId(),
                    datosAnteriores, construirDatosAuditoria(actualizado),
                    "Reactivación lógica de usuario");
            return mapToResponse(actualizado);
        });
    }
    private Integer obtenerIdUsuarioActual() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof UserDetailsImpl user) {
                    return user.getId();
                }
            }
        } catch (Exception ex){
            throw new RuntimeException("Error al obtener el id actual", ex);
        }
        return 0;
    }
}
