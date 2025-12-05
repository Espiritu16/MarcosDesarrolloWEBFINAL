package com.marcosdeDesarrollo.demo.EstilosPE.persistence.crud;

import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.ActualizarEstadoOrdenRequest;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.OrdenCompraDetalleRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.OrdenCompraDetalleResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.OrdenCompraRequestDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.OrdenCompraResponseDto;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.dto.RecepcionDetalleRequest;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.InsumosRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.CategoriaRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.OrdenCompraRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.ProductoRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.ProveedorRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.domain.repository.UsuarioRepository;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.DetalleOrden;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.EstadoOrdenCompra;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Insumos;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Ordenes_Compra;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Producto;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Proveedores;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Categoria;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.TipoItemOrden;
import com.marcosdeDesarrollo.demo.EstilosPE.persistence.entity.Usuario;
import com.marcosdeDesarrollo.demo.EstilosPE.web.security.UserDetailsImpl;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class OrdenCompraService {

    private final OrdenCompraRepository ordenCompraRepository;
    private final ProveedorRepository proveedorRepository;
    private final CategoriaRepository categoriaRepository;
    private final ProductoRepository productoRepository;
    private final InsumosRepository insumosRepository;
    private final UsuarioRepository usuarioRepository;

    public OrdenCompraService(OrdenCompraRepository ordenCompraRepository,
            ProveedorRepository proveedorRepository,
            CategoriaRepository categoriaRepository,
            ProductoRepository productoRepository,
            InsumosRepository insumosRepository,
            UsuarioRepository usuarioRepository) {
        this.ordenCompraRepository = ordenCompraRepository;
        this.proveedorRepository = proveedorRepository;
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
        this.insumosRepository = insumosRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<OrdenCompraResponseDto> listar(String estado) {
        List<Ordenes_Compra> ordenes = StringUtils.hasText(estado)
                ? ordenCompraRepository.findByEstado(parseEstado(estado))
                : ordenCompraRepository.findAll();
        return ordenes.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrdenCompraResponseDto obtenerPorId(Integer id) {
        Ordenes_Compra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La orden indicada no existe"));
        return mapToResponse(orden);
    }

    public OrdenCompraResponseDto crear(OrdenCompraRequestDto request) {
        validarRequest(request);

        Ordenes_Compra orden = new Ordenes_Compra();
        orden.setFecha(request.getFecha() != null ? request.getFecha() : LocalDate.now());
        orden.setEstado(parseEstado(Optional.ofNullable(request.getEstado()).orElse("Pendiente")));
        orden.setUsuario(obtenerUsuarioActual().orElseThrow(() -> new IllegalStateException("No se pudo identificar al usuario autenticado")));

        Proveedores proveedor = obtenerORegistrarProveedor(request);
        orden.setProveedor(proveedor);

        List<DetalleOrden> detalles = construirDetalles(request.getDetalles(), orden);
        orden.setDetalles(detalles);
        orden.setTotal(calcularTotal(detalles));

        Ordenes_Compra guardada = ordenCompraRepository.save(orden);
        return mapToResponse(guardada);
    }

    public OrdenCompraResponseDto actualizar(Integer id, OrdenCompraRequestDto request) {
        validarRequest(request);
        Ordenes_Compra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La orden indicada no existe"));

        if (orden.getEstado() == EstadoOrdenCompra.Recibida || orden.getEstado() == EstadoOrdenCompra.Cancelada || orden.getEstado() == EstadoOrdenCompra.Rechazada) {
            throw new IllegalArgumentException("No se puede modificar una orden finalizada o cancelada");
        }

        orden.setFecha(request.getFecha() != null ? request.getFecha() : orden.getFecha());
        // Mantener estado actual si no se proporciona uno nuevo (en UI se crea como Pendiente)
        if (StringUtils.hasText(request.getEstado())) {
            validarCambioEstadoPrivilegio(request.getEstado());
            orden.setEstado(parseEstado(request.getEstado()));
        }

        Proveedores proveedor = obtenerORegistrarProveedor(request);
        orden.setProveedor(proveedor);

        orden.getDetalles().clear();
        List<DetalleOrden> nuevosDetalles = construirDetalles(request.getDetalles(), orden);
        orden.getDetalles().addAll(nuevosDetalles);
        orden.setTotal(calcularTotal(nuevosDetalles));

        Ordenes_Compra guardada = ordenCompraRepository.save(orden);
        return mapToResponse(guardada);
    }

    public OrdenCompraResponseDto actualizarEstado(Integer id, ActualizarEstadoOrdenRequest request) {
        if (request == null || !StringUtils.hasText(request.getEstado())) {
            throw new IllegalArgumentException("El estado es obligatorio");
        }
        validarCambioEstadoPrivilegio(request.getEstado());
        Ordenes_Compra orden = ordenCompraRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("La orden indicada no existe"));

        EstadoOrdenCompra nuevoEstado = parseEstado(request.getEstado());
        EstadoOrdenCompra estadoAnterior = orden.getEstado();

        if (estadoAnterior == EstadoOrdenCompra.Recibida && nuevoEstado == EstadoOrdenCompra.Recibida) {
            throw new IllegalArgumentException("La orden ya fue recibida");
        }

        orden.setEstado(nuevoEstado);

        if (nuevoEstado == EstadoOrdenCompra.Recibida) {
            aplicarRecepcion(orden, request.getRecepciones());
        }

        Ordenes_Compra guardada = ordenCompraRepository.save(orden);
        return mapToResponse(guardada);
    }

    public void eliminar(Integer id) {
        if (!ordenCompraRepository.existsById(id)) {
            throw new IllegalArgumentException("La orden indicada no existe");
        }
        ordenCompraRepository.deleteById(id);
    }

    private List<DetalleOrden> construirDetalles(List<OrdenCompraDetalleRequestDto> detallesRequest, Ordenes_Compra orden) {
        if (detallesRequest == null || detallesRequest.isEmpty()) {
            throw new IllegalArgumentException("Debe registrar al menos un ítem en la orden");
        }

        List<DetalleOrden> detalles = new ArrayList<>();
        for (OrdenCompraDetalleRequestDto detalleDto : detallesRequest) {
            if (detalleDto.getCantidad() == null || detalleDto.getCantidad() <= 0) {
                throw new IllegalArgumentException("La cantidad debe ser mayor a cero");
            }
            if (detalleDto.getPrecioUnitario() == null || detalleDto.getPrecioUnitario().compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("El precio unitario debe ser mayor a cero");
            }
            TipoItemOrden tipo = parseTipo(detalleDto.getTipoItem());
            DetalleOrden detalle = new DetalleOrden();
            detalle.setOrden(orden);
            detalle.setCantidad(detalleDto.getCantidad());
            detalle.setPrecioUnitario(detalleDto.getPrecioUnitario());
            detalle.setTipoItem(tipo);

            if (tipo == TipoItemOrden.PRODUCTO) {
                if (detalleDto.getItemId() == null) {
                    throw new IllegalArgumentException("Debe seleccionar un producto existente");
                }
                Producto producto = productoRepository.findById(detalleDto.getItemId().longValue())
                        .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
                detalle.setProducto(producto);
                detalle.setInsumo(null);
            } else {
                if (detalleDto.getItemId() == null) {
                    throw new IllegalArgumentException("Debe seleccionar un insumo existente");
                }
                Insumos insumo = insumosRepository.findById(detalleDto.getItemId().longValue())
                        .orElseThrow(() -> new IllegalArgumentException("Insumo no encontrado"));
                detalle.setInsumo(insumo);
                detalle.setProducto(null);
            }
            detalles.add(detalle);
        }
        return detalles;
    }

    private BigDecimal calcularTotal(List<DetalleOrden> detalles) {
        return detalles.stream()
                .map(d -> d.getPrecioUnitario().multiply(BigDecimal.valueOf(d.getCantidad())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void aplicarRecepcion(Ordenes_Compra orden, List<RecepcionDetalleRequest> recepciones) {
        Map<Integer, Integer> cantidadesRecibidas = new HashMap<>();
        Map<Integer, RecepcionDetalleRequest> recepcionPorDetalle = new HashMap<>();
        if (recepciones != null) {
            recepciones.forEach(r -> {
                if (r.getIdDetalle() != null && r.getCantidadRecibida() != null && r.getCantidadRecibida() >= 0) {
                    cantidadesRecibidas.put(r.getIdDetalle(), r.getCantidadRecibida());
                    recepcionPorDetalle.put(r.getIdDetalle(), r);
                }
            });
        }

        for (DetalleOrden detalle : orden.getDetalles()) {
            int recibido = cantidadesRecibidas.getOrDefault(detalle.getIdDetalle(), detalle.getCantidad());
            if (recibido < 0) {
                throw new IllegalArgumentException("La cantidad recibida no puede ser negativa");
            }
            if (detalle.getTipoItem() == TipoItemOrden.INSUMO) {
                Insumos insumo = detalle.getInsumo();
                if (insumo == null) {
                    throw new IllegalArgumentException("El detalle no tiene insumo asociado");
                }
                int stockActual = Optional.ofNullable(insumo.getStockActual()).orElse(0);
                insumo.setStockActual(stockActual + recibido);
                insumosRepository.save(insumo);
            } else {
                Producto producto = detalle.getProducto();
                if (producto == null) {
                    throw new IllegalArgumentException("El detalle no tiene producto asociado");
                }
                int stockActual = Optional.ofNullable(producto.getStockActual()).orElse(0);
                producto.setStockActual(stockActual + recibido);
                productoRepository.save(producto);
            }
        }
    }

    private Proveedores obtenerORegistrarProveedor(OrdenCompraRequestDto request) {
        if (!StringUtils.hasText(request.getProveedorNombre())) {
            throw new IllegalArgumentException("El nombre del proveedor es obligatorio");
        }
        return proveedorRepository.findByNombreProveedorIgnoreCase(request.getProveedorNombre().trim())
                .map(proveedorExistente -> {
                    if (StringUtils.hasText(request.getProveedorContacto())) {
                        proveedorExistente.setContacto(request.getProveedorContacto().trim());
                    }
                    if (StringUtils.hasText(request.getProveedorTelefono())) {
                        proveedorExistente.setTelefono(request.getProveedorTelefono().trim());
                    }
                    if (StringUtils.hasText(request.getProveedorEmail())) {
                        proveedorExistente.setEmail(request.getProveedorEmail().trim());
                    }
                    return proveedorRepository.save(proveedorExistente);
                })
                .orElseGet(() -> {
                    Proveedores nuevo = new Proveedores();
                    nuevo.setNombreProveedor(request.getProveedorNombre().trim());
                    nuevo.setContacto(request.getProveedorContacto());
                    nuevo.setTelefono(request.getProveedorTelefono());
                    nuevo.setEmail(request.getProveedorEmail());
                    return proveedorRepository.save(nuevo);
                });
    }

    private EstadoOrdenCompra parseEstado(String estado) {
        String valor = estado.trim().toLowerCase();
        return switch (valor) {
            case "pendiente" -> EstadoOrdenCompra.Pendiente;
            case "aprobada" -> EstadoOrdenCompra.Aprobada;
            case "enenvio", "en_envio", "en envio" -> EstadoOrdenCompra.EnEnvio;
            case "porconfirmar", "por_confirmar" -> EstadoOrdenCompra.PorConfirmar;
            case "recibida" -> EstadoOrdenCompra.Recibida;
            case "cancelada" -> EstadoOrdenCompra.Cancelada;
            case "rechazada" -> EstadoOrdenCompra.Rechazada;
            default -> throw new IllegalArgumentException("Estado de orden no válido: " + estado);
        };
    }

    private TipoItemOrden parseTipo(String tipo) {
        if (!StringUtils.hasText(tipo)) {
            throw new IllegalArgumentException("El tipo de ítem es obligatorio");
        }
        String valor = tipo.trim().toUpperCase();
        return switch (valor) {
            case "PRODUCTO" -> TipoItemOrden.PRODUCTO;
            case "INSUMO" -> TipoItemOrden.INSUMO;
            default -> throw new IllegalArgumentException("Tipo de ítem no válido: " + tipo);
        };
    }

    private void validarCambioEstadoPrivilegio(String estadoSolicitado) {
        EstadoOrdenCompra destino = parseEstado(estadoSolicitado);
        if (destino == EstadoOrdenCompra.Aprobada
                || destino == EstadoOrdenCompra.Cancelada
                || destino == EstadoOrdenCompra.Rechazada) {
            if (!esAdminActual()) {
                throw new IllegalArgumentException("Solo el administrador puede realizar esta acción");
            }
        }
    }

    private boolean esAdminActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> "ADMINISTRADOR".equalsIgnoreCase(auth.getAuthority()));
    }

    private OrdenCompraResponseDto mapToResponse(Ordenes_Compra orden) {
        OrdenCompraResponseDto dto = new OrdenCompraResponseDto();
        dto.setIdOrden(orden.getIdOrden());
        dto.setFecha(orden.getFecha());
        dto.setEstado(orden.getEstado() != null ? orden.getEstado().name() : null);
        dto.setTotal(orden.getTotal());
        if (orden.getProveedor() != null) {
            dto.setProveedorNombre(orden.getProveedor().getNombreProveedor());
            dto.setProveedorContacto(orden.getProveedor().getContacto());
            dto.setProveedorTelefono(orden.getProveedor().getTelefono());
            dto.setProveedorEmail(orden.getProveedor().getEmail());
        }
        dto.setFechaCreacion(orden.getFechaCreacion());
        dto.setFechaActualizacion(orden.getFechaActualizacion());
        dto.setDetalles(mapDetalles(orden.getDetalles()));
        return dto;
    }

    private List<OrdenCompraDetalleResponseDto> mapDetalles(List<DetalleOrden> detalles) {
        if (detalles == null) {
            return List.of();
        }
        return detalles.stream().map(detalle -> {
            OrdenCompraDetalleResponseDto dto = new OrdenCompraDetalleResponseDto();
            dto.setIdDetalle(detalle.getIdDetalle());
            dto.setCantidad(detalle.getCantidad());
            dto.setPrecioUnitario(detalle.getPrecioUnitario());
            dto.setSubtotal(detalle.getSubtotal());
            dto.setTipoItem(detalle.getTipoItem() != null ? detalle.getTipoItem().name() : null);
            if (detalle.getTipoItem() == TipoItemOrden.PRODUCTO && detalle.getProducto() != null) {
                dto.setItemId(detalle.getProducto().getIdProducto().intValue());
                dto.setNombreItem(detalle.getProducto().getNombreProducto());
            } else if (detalle.getTipoItem() == TipoItemOrden.INSUMO && detalle.getInsumo() != null) {
                dto.setItemId(detalle.getInsumo().getIdInsumo().intValue());
                dto.setNombreItem(detalle.getInsumo().getNombre());
            }
            return dto;
        }).collect(Collectors.toList());
    }

    private void validarRequest(OrdenCompraRequestDto request) {
        if (request == null) {
            throw new IllegalArgumentException("Los datos de la orden son obligatorios");
        }
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new IllegalArgumentException("Debe registrar al menos un ítem en la orden");
        }
    }

    private Optional<Usuario> obtenerUsuarioActual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof UserDetailsImpl userDetails) {
            return usuarioRepository.findById(userDetails.getId());
        }

        String username = authentication.getName();
        return usuarioRepository.findByEmail(username);
    }
}
