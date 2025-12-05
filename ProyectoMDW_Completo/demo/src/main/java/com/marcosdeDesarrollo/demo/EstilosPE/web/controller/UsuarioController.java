package com.marcosdeDesarrollo.demo.EstilosPE.web.controller;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.RolRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.UsuarioRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.UsuarioResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.service.UsuarioService;
import io.swagger.v3.oas.annotations.Parameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/usuario")
@CrossOrigin(origins = "*")
@PreAuthorize("hasAnyRole('ADMINISTRADOR')")
public class UsuarioController {
    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }
    @GetMapping
    public List<UsuarioResponseDto> listar(){return usuarioService.listarTodos();}
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody UsuarioRequestDto usuario){
        try{
            UsuarioResponseDto nuevo= usuarioService.crear(usuario);
            return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        }catch(IllegalArgumentException e){
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Integer id,
            @RequestBody UsuarioRequestDto usuario) {
        try {
            return usuarioService.actualizar(id, usuario)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
