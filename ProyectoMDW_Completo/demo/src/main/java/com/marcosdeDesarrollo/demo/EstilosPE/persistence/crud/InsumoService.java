package com.marcosdeDesarrollo.demo.EstilosPE.persistence.crud;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.ActualizarStockInsumoRequest;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.InsumoRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.InsumoResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.InsumosRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Estado;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Insumos;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class InsumoService {

    private static final int STOCK_UMBRAL_BAJO = 20;

    private final InsumosRepository insumosRepository;

    public InsumoService(InsumosRepository insumosRepository) {
        this.insumosRepository = insumosRepository;
    }

    @Transactional(readOnly = true)
    public List<InsumoResponseDto> listar(String estado, String stock, String search) {
        Specification<Insumos> spec = construirSpecification(estado, stock, search);
        return insumosRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "fechaCreacion"))
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public InsumoResponseDto obtenerPorId(Long id) {
        Insumos insumo = insumosRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El insumo indicado no existe"));
        return mapToResponse(insumo);
    }

    public InsumoResponseDto crear(InsumoRequestDto request) {
        validarRequestCreacion(request);
        Insumos insumo = new Insumos();
        aplicarDatos(insumo, request, false);
        Insumos guardado = insumosRepository.save(insumo);
        return mapToResponse(guardado);
    }

    public InsumoResponseDto actualizar(Long id, InsumoRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del insumo son obligatorios");
        }
        Insumos insumo = insumosRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El insumo indicado no existe"));
        aplicarDatos(insumo, request, true);
        return mapToResponse(insumosRepository.save(insumo));
    }

    public InsumoResponseDto actualizarStock(Long id, ActualizarStockInsumoRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de actualización de stock son obligatorios");
        }

        if (request.getAjuste() == null && request.getNuevoStock() == null) {
            throw new IllegalArgumentException("Debe indicar un ajuste o un nuevo valor de stock");
        }

        Insumos insumo = insumosRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El insumo indicado no existe"));

        int stockActual = insumo.getStockActual() != null ? insumo.getStockActual() : 0;

        if (request.getNuevoStock() != null) {
            if (request.getNuevoStock() < 0) {
                throw new IllegalArgumentException("El stock no puede ser negativo");
            }
            stockActual = request.getNuevoStock();
        }

        if (request.getAjuste() != null) {
            stockActual += request.getAjuste();
            if (stockActual < 0) {
                throw new IllegalArgumentException("El ajuste dejaría el stock en negativo");
            }
        }

        insumo.setStockActual(stockActual);
        Insumos actualizado = insumosRepository.save(insumo);
        return mapToResponse(actualizado);
    }

    public void eliminar(Long id) {
        Insumos insumo = insumosRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El insumo indicado no existe"));
        insumo.setEstado(Estado.Inactivo);
        insumo.setStockActual(0);
        insumosRepository.save(insumo);
    }

    @Transactional(readOnly = true)
    public long contarTotal() {
        return insumosRepository.count();
    }

    @Transactional(readOnly = true)
    public long contarActivos() {
        return insumosRepository.countByEstado(Estado.Activo);
    }

    @Transactional(readOnly = true)
    public long contarStockBajo() {
        return insumosRepository.countByStockActualLessThanEqual(STOCK_UMBRAL_BAJO);
    }

    private void aplicarDatos(Insumos insumo, InsumoRequestDto request, boolean esActualizacion) {
        if (!StringUtils.hasText(request.getNombre())) {
            if (!esActualizacion) {
                throw new IllegalArgumentException("El nombre del insumo es obligatorio");
            }
        } else {
            insumo.setNombre(request.getNombre().trim());
        }

        if (request.getDescripcion() != null) {
            insumo.setDescripcion(request.getDescripcion().trim());
        }

        if (request.getUnidadMedida() != null) {
            insumo.setUnidadMedida(request.getUnidadMedida().trim());
        }

        if (request.getStockActual() != null) {
            if (request.getStockActual() < 0) {
                throw new IllegalArgumentException("El stock no puede ser negativo");
            }
            insumo.setStockActual(request.getStockActual());
        } else if (!esActualizacion && insumo.getStockActual() == null) {
            insumo.setStockActual(0);
        }

        if (request.getEstado() != null) {
            insumo.setEstado(request.getEstado());
        } else if (!esActualizacion && insumo.getEstado() == null) {
            insumo.setEstado(Estado.Activo);
        }
    }

    private Specification<Insumos> construirSpecification(String estado, String stock, String search) {
        Specification<Insumos> spec = Specification.where(null);

        if (StringUtils.hasText(estado)) {
            Estado estadoEnum = parseEstado(estado);
            spec = spec.and((root, query, cb) -> cb.equal(root.get("estado"), estadoEnum));
        }

        if (StringUtils.hasText(stock)) {
            String stockLower = stock.trim().toLowerCase(Locale.ROOT);
            switch (stockLower) {
                case "bajo" -> spec = spec.and(
                        (root, query, cb) -> cb.lessThanOrEqualTo(root.get("stockActual"), STOCK_UMBRAL_BAJO));
                case "suficiente", "alto", "normal" ->
                    spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("stockActual"), STOCK_UMBRAL_BAJO));
                default -> {
                }
            }
        }

        if (StringUtils.hasText(search)) {
            String criterio = "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("nombre")), criterio),
                    cb.like(cb.lower(root.get("descripcion")), criterio),
                    cb.like(cb.lower(root.get("unidadMedida")), criterio)
            ));
        }

        return spec;
    }

    private Estado parseEstado(String estado) {
        String valor = estado.trim().toLowerCase(Locale.ROOT);
        return switch (valor) {
            case "activo" -> Estado.Activo;
            case "inactivo" -> Estado.Inactivo;
            default -> Estado.valueOf(estado.trim());
        };
    }

    private InsumoResponseDto mapToResponse(Insumos insumo) {
        InsumoResponseDto dto = new InsumoResponseDto();
        dto.setIdInsumo(insumo.getIdInsumo());
        dto.setNombre(insumo.getNombre());
        dto.setDescripcion(insumo.getDescripcion());
        dto.setStockActual(insumo.getStockActual());
        dto.setUnidadMedida(insumo.getUnidadMedida());
        dto.setEstado(insumo.getEstado());
        dto.setFechaCreacion(insumo.getFechaCreacion());
        int stock = insumo.getStockActual() != null ? insumo.getStockActual() : 0;
        dto.setStockBajo(stock <= STOCK_UMBRAL_BAJO);
        return dto;
    }

    private void validarRequestCreacion(InsumoRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos del insumo son obligatorios");
        }
        if (!StringUtils.hasText(request.getNombre())) {
            throw new IllegalArgumentException("El nombre del insumo es obligatorio");
        }
        if (request.getStockActual() != null && request.getStockActual() < 0) {
            throw new IllegalArgumentException("El stock no puede ser negativo");
        }
    }
}
