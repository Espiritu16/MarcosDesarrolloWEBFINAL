package com.marcosdeDesarrollo.demo.EstilosPE.web.controller;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.JwtResponse;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.LoginRequest;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.UsuarioRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Estado;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Usuario;
import com.marcosdeDesarrollo.demo.EstilosPE.web.security.JwtUtils;
import com.marcosdeDesarrollo.demo.EstilosPE.web.security.UserDetailsImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Endpoints para gestión de autenticación y login") // 👈 NUEVO
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final UsuarioRepository usuarioRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtUtils jwtUtils,UsuarioRepository usuarioRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtUtils = jwtUtils;
        this.usuarioRepository=usuarioRepository;
    }

    @Operation(summary = "Autenticar usuario", description = "Endpoint para login de usuarios") //
    @ApiResponses(value = { //
        @ApiResponse(responseCode = "200", description = "Login exitoso", 
                    content = @Content(schema = @Schema(implementation = JwtResponse.class))),
        @ApiResponse(responseCode = "401", description = "Credenciales inválidas"),
        @ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/signin")
    public ResponseEntity<Map<String, String>> authenticateUser(@RequestBody LoginRequest loginRequest) {

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.email(), loginRequest.password()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        Usuario usuario = usuarioRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (usuario.getEstado() == Estado.Inactivo) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Tu cuenta ha sido desactivada. Contacta al administrador."));
        }
        String rol = userDetails.getAuthorities().iterator().next().getAuthority();
        return ResponseEntity.ok(Map.of(
                "token", jwt,
                "nombreUsuario", userDetails.getUsername(),
                "rol", rol,
                "email", userDetails.getUsername(),
                "estado", usuario.getEstado().toString()
        ));
    }
}