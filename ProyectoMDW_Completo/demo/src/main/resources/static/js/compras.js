const API_ORDENES = '/api/ordenes-compra';
const API_PRODUCTOS = '/api/productos';
const API_INSUMOS = '/api/insumos';
const API_PROVEEDORES = '/api/proveedores';
const TOKEN_KEY = 'jwtToken';

let ordenes = [];
let productos = [];
let insumos = [];
let detallesOrden = [];
let estadoTabActual = 'Pendiente';
let kpis = { pendientes: 0, enProceso: 0, recibidas: 0, canceladas: 0 };
let proveedores = [];

document.addEventListener('DOMContentLoaded', () => {
    activarTabPendientePorDefecto();
    cargarCatalogos();
    inicializarEventos();
    refrescarOrdenes();
    setFechaHoy();
});

async function cargarCatalogos() {
    try {
        productos = await fetchJsonConToken(API_PRODUCTOS);
        insumos = await fetchJsonConToken(API_INSUMOS);
        proveedores = await fetchJsonConToken(API_PROVEEDORES);
        poblarSelectItem();
        poblarSelectProveedores();
    } catch (e) {
        console.error('No se pudieron cargar catálogos', e);
    }
}

function inicializarEventos() {
    document.querySelectorAll('#ordenTabs .nav-link').forEach(btn => {
        btn.addEventListener('click', () => {
            estadoTabActual = btn.dataset.estado;
            document.querySelectorAll('#ordenTabs .nav-link').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            renderTabla();
        });
    });

    const btnNueva = document.getElementById('btnNuevaOrden');
    if (btnNueva) {
        btnNueva.addEventListener('click', () => abrirModalCrear());
    }

    document.getElementById('detalleTipo').addEventListener('change', () => {
        poblarSelectItem();
        mostrarInfoItem();
    });
    document.getElementById('detalleItem').addEventListener('change', () => {
        mostrarInfoItem();
    });
    document.getElementById('btnAgregarDetalle').addEventListener('click', agregarDetalle);
    document.getElementById('formOrdenCompra').addEventListener('submit', guardarOrden);
    document.getElementById('btnConfirmarRecepcion').addEventListener('click', confirmarRecepcion);
    const provSelect = document.getElementById('proveedorSelect');
    const provNombre = document.getElementById('proveedorNombre');
    if (provSelect) {
        provSelect.addEventListener('change', () => toggleProveedorCustom());
    }
    if (provNombre) {
        provNombre.addEventListener('input', () => {
            // mantener sincronizado el nombre si está en modo custom
        });
    }

    const buscarInput = document.getElementById('buscarOrden');
    if (buscarInput) {
        buscarInput.addEventListener('input', renderTabla);
    }

    const filtroInicio = document.getElementById('filtroFechaInicioCompra');
    const filtroFin = document.getElementById('filtroFechaFinCompra');
    [filtroInicio, filtroFin].forEach(el => el && el.addEventListener('change', renderTabla));

    const btnLimpiarFiltros = document.getElementById('btnLimpiarFiltrosCompras');
    if (btnLimpiarFiltros) {
        btnLimpiarFiltros.addEventListener('click', () => {
            if (filtroInicio) filtroInicio.value = '';
            if (filtroFin) filtroFin.value = '';
            renderTabla();
        });
    }

    const btnReporteCompras = document.getElementById('btnReporteCompras');
    if (btnReporteCompras) {
        btnReporteCompras.addEventListener('click', descargarReporteCompras);
    }

    const tablaBody = document.querySelector('#tablaCompras tbody');
    tablaBody.addEventListener('click', manejarAccionesTabla);
}

async function refrescarOrdenes() {
    try {
        ordenes = await fetchJsonConToken(API_ORDENES);
        calcularKpis();
        renderTabla();
    } catch (e) {
        console.error('Error al cargar órdenes', e);
    }
}

function renderTabla() {
    const tbody = document.querySelector('#tablaCompras tbody');
    if (!tbody) return;
    tbody.innerHTML = '';
    const termino = (document.getElementById('buscarOrden').value || '').toLowerCase();
    const inicioStr = document.getElementById('filtroFechaInicioCompra')?.value || '';
    const finStr = document.getElementById('filtroFechaFinCompra')?.value || '';
    const fechaInicio = inicioStr ? new Date(inicioStr) : null;
    const fechaFin = finStr ? new Date(finStr) : null;

    const filtradas = ordenes.filter(o => {
        const coincideEstado = !estadoTabActual || o.estado === estadoTabActual;
        const coincideProveedor = !termino || (o.proveedorNombre || '').toLowerCase().includes(termino);
        const fecha = parseFechaSeguro(o.fecha);
        const coincideFechaInicio = !fechaInicio || (fecha && fecha >= fechaInicio);
        const coincideFechaFin = !fechaFin || (fecha && fecha <= fechaFin);
        return coincideEstado && coincideProveedor && coincideFechaInicio && coincideFechaFin;
    });

    const resumen = document.getElementById('comprasResumen');
    if (resumen) {
        resumen.textContent = filtradas.length
            ? `${filtradas.length} orden(es) en ${estadoTabActual || 'todas'}`
            : 'Sin registros para mostrar';
    }

    if (!filtradas.length) {
        const row = document.createElement('tr');
        row.innerHTML = `<td colspan="7" class="text-center text-muted py-3">Sin órdenes en este estado.</td>`;
        tbody.appendChild(row);
        return;
    }

    filtradas.forEach(orden => {
        const row = document.createElement('tr');
        row.innerHTML = `
            <td>${orden.idOrden}</td>
            <td>
              <div class="fw-semibold">${orden.proveedorNombre || '—'}</div>
              <div class="text-muted small">${orden.proveedorContacto || ''}</div>
            </td>
            <td>${orden.fecha || '—'}</td>
            <td>S/ ${Number(orden.total || 0).toFixed(2)}</td>
            <td>${badgeEstado(orden.estado)}</td>
            <td>
              ${renderDetalleResumen(orden.detalles)}
            </td>
            <td class="d-flex gap-1 flex-wrap">
              <button class="btn btn-sm btn-outline-secondary btn-editar" data-id="${orden.idOrden}" title="Editar"><i class="bi bi-pencil"></i></button>
              ${accionesPorEstado(orden)}
            </td>
        `;
        tbody.appendChild(row);
    });
}

function renderDetalleResumen(detalles = []) {
    if (!detalles.length) return '<span class="text-muted">Sin ítems</span>';
    return `<div class="small text-muted">${detalles.map(d => `${d.nombreItem || 'Ítem'} (${d.tipoItem || '-'}) x${d.cantidad}`).join(' • ')}</div>`;
}

function badgeEstado(estado) {
    const map = {
        Pendiente: 'bg-warning text-dark',
        Aprobada: 'bg-info text-dark',
        EnEnvio: 'bg-primary',
        PorConfirmar: 'bg-secondary',
        Recibida: 'bg-success',
        Cancelada: 'bg-dark',
        Rechazada: 'bg-danger'
    };
    const clase = map[estado] || 'bg-light text-dark';
    return `<span class="badge ${clase}">${estado}</span>`;
}

function accionesPorEstado(orden) {
    const id = orden.idOrden;
    const estado = orden.estado;
    const acciones = [];
    const rol = (sessionStorage.getItem('userRole') || '').toUpperCase();
    const esAdmin = rol === 'ADMINISTRADOR';
    if (estado === 'Pendiente' && esAdmin) {
        acciones.push(btnAccion('btn-aprobar', id, 'Aprobar', 'bi-check-lg', 'btn-success'));
        acciones.push(btnAccion('btn-cancelar btn-eliminar', id, 'Cancelar', 'bi-x-circle', 'btn-outline-warning'));
        acciones.push(btnAccion('btn-rechazar btn-eliminar', id, 'Rechazar', 'bi-ban', 'btn-outline-danger'));
    }
    if (estado === 'Aprobada' && (esAdmin || rol === 'VENDEDOR')) {
        acciones.push(btnAccion('btn-enviar', id, 'Marcar en envío', 'bi-truck', 'btn-primary'));
        acciones.push(btnAccion('btn-cancelar btn-eliminar', id, 'Cancelar', 'bi-x-circle', 'btn-outline-warning'));
        acciones.push(btnAccion('btn-rechazar btn-eliminar', id, 'Rechazar', 'bi-ban', 'btn-outline-danger'));
    }
    if (estado === 'EnEnvio' && (esAdmin || rol === 'VENDEDOR')) {
        acciones.push(btnAccion('btn-porconfirmar', id, 'Listo para confirmar', 'bi-hourglass-split', 'btn-outline-primary'));
        acciones.push(btnAccion('btn-cancelar btn-eliminar', id, 'Cancelar', 'bi-x-circle', 'btn-outline-warning'));
        acciones.push(btnAccion('btn-rechazar btn-eliminar', id, 'Rechazar', 'bi-ban', 'btn-outline-danger'));
    }
    if (estado === 'PorConfirmar' && (esAdmin || rol === 'VENDEDOR')) {
        acciones.push(btnAccion('btn-confirmar', id, 'Confirmar recepción', 'bi-check2-circle', 'btn-outline-success'));
        acciones.push(btnAccion('btn-cancelar btn-eliminar', id, 'Cancelar', 'bi-x-circle', 'btn-outline-warning'));
        acciones.push(btnAccion('btn-rechazar btn-eliminar', id, 'Rechazar', 'bi-ban', 'btn-outline-danger'));
    }
    if (estado === 'Rechazada' && esAdmin) {
        acciones.push(btnAccion('btn-cancelar btn-eliminar', id, 'Cancelar', 'bi-x-circle', 'btn-outline-warning'));
    }
    if(estado==='Cancelada' && esAdmin){
        acciones.push(btnAccion('btn-rechazar btn-eliminar', id, 'Rechazar', 'bi-ban', 'btn-outline-danger'));
    }
    return acciones.join('');
}

function btnAccion(clases, id, title, icono, estilo = 'btn-outline-secondary') {
    return `<button class="btn btn-sm ${estilo} ${clases}" data-id="${id}" title="${title}"><i class="bi ${icono}"></i></button>`;
}

function manejarAccionesTabla(event) {
    const btn = event.target.closest('button');
    if (!btn) return;
    const id = btn.dataset.id;
    if (!id) return;

    if (btn.classList.contains('btn-editar')) {
        editarOrden(id);
    } else if (btn.classList.contains('btn-aprobar')) {
        cambiarEstado(id, 'Aprobada');
    } else if (btn.classList.contains('btn-enviar')) {
        cambiarEstado(id, 'EnEnvio');
    } else if (btn.classList.contains('btn-porconfirmar')) {
        cambiarEstado(id, 'PorConfirmar');
    } else if (btn.classList.contains('btn-confirmar')) {
        abrirModalRecepcion(id);
    } else if (btn.classList.contains('btn-cancelar')) {
        cambiarEstado(id, 'Cancelada');
    } else if (btn.classList.contains('btn-rechazar')) {
        cambiarEstado(id, 'Rechazada');
    }
}

function abrirModalCrear() {
    detallesOrden = [];
    document.getElementById('formOrdenCompra').reset();
    document.getElementById('ordenId').value = '';
    document.getElementById('ordenTotalLabel').textContent = 'S/ 0.00';
    document.getElementById('estadoOrden').value = 'Pendiente';
    const provSelect = document.getElementById('proveedorSelect');
    if (provSelect) {
        provSelect.value = '';
        toggleProveedorCustom();
    }
    renderDetalles();
}

function poblarSelectItem() {
    const tipo = document.getElementById('detalleTipo').value;
    const select = document.getElementById('detalleItem');
    select.innerHTML = '';
    const lista = tipo === 'PRODUCTO' ? productos : insumos;
    lista.forEach(item => {
        const opt = document.createElement('option');
        opt.value = tipo === 'PRODUCTO' ? item.idProducto : item.idInsumo;
        opt.textContent = item.nombreProducto || item.nombre;
        select.appendChild(opt);
    });
    mostrarInfoItem();
}

function agregarDetalle() {
    const tipo = document.getElementById('detalleTipo').value;
    const select = document.getElementById('detalleItem');
    const cantidad = parseInt(document.getElementById('detalleCantidad').value, 10) || 0;
    const precio = parseFloat(document.getElementById('detallePrecio').value) || 0;

    if (!select.value || cantidad <= 0 || precio <= 0) {
        alert('Completa tipo, ítem, cantidad y precio mayor a 0.');
        return;
    }
    const lista = tipo === 'PRODUCTO' ? productos : insumos;
    const item = lista.find(i => String(i.idProducto || i.idInsumo) === String(select.value));
    detallesOrden.push({
        tipoItem: tipo,
        itemId: parseInt(select.value, 10),
        nombreItem: item ? (item.nombreProducto || item.nombre) : 'Ítem',
        cantidad,
        precioUnitario: precio
    });
    renderDetalles();
}

function mostrarInfoItem() {
    const tipo = document.getElementById('detalleTipo').value;
    const select = document.getElementById('detalleItem');
    const nombreEl = document.getElementById('infoNombreItem');
    const descEl = document.getElementById('infoDescripcionItem');
    const extraEl = document.getElementById('infoExtraItem');

    const lista = tipo === 'PRODUCTO' ? productos : insumos;
    const item = lista.find(i => String(i.idProducto || i.idInsumo) === String(select.value));
    if (!item) {
        nombreEl.textContent = '—';
        descEl.textContent = 'Selecciona un ítem para ver sus datos.';
        extraEl.textContent = '';
        return;
    }
    nombreEl.textContent = item.nombreProducto || item.nombre || '—';
    descEl.textContent = item.descripcion || 'Sin descripción';
    if (tipo === 'PRODUCTO') {
        const talla = item.talla ? `Talla: ${item.talla}` : '';
        const color = item.color ? `Color: ${item.color}` : '';
        extraEl.textContent = [talla, color].filter(Boolean).join(' • ');
    } else {
        extraEl.textContent = item.unidadMedida ? `Unidad: ${item.unidadMedida}` : '';
    }
}

function renderDetalles() {
    const tbody = document.querySelector('#tablaDetalles tbody');
    tbody.innerHTML = '';
    if (!detallesOrden.length) {
        tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">Añade ítems a la orden.</td></tr>`;
    } else {
        detallesOrden.forEach((det, index) => {
            const subtotal = det.precioUnitario * det.cantidad;
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${det.tipoItem}</td>
                <td>${det.nombreItem}</td>
                <td class="text-end">${det.cantidad}</td>
                <td class="text-end">S/ ${det.precioUnitario.toFixed(2)}</td>
                <td class="text-end">S/ ${subtotal.toFixed(2)}</td>
                <td class="text-center">
                    <button class="btn btn-sm btn-outline-danger" data-index="${index}" onclick="eliminarDetalle(${index})"><i class="bi bi-trash"></i></button>
                </td>
            `;
            tbody.appendChild(row);
        });
    }
    const total = detallesOrden.reduce((acc, det) => acc + det.precioUnitario * det.cantidad, 0);
    document.getElementById('ordenTotalLabel').textContent = `S/ ${total.toFixed(2)}`;
}

function eliminarDetalle(index) {
    detallesOrden.splice(index, 1);
    renderDetalles();
}

async function guardarOrden(event) {
    event.preventDefault();
    if (!detallesOrden.length) {
        alert('Agrega al menos un ítem a la orden.');
        return;
    }

    const payload = {
        proveedorNombre: obtenerNombreProveedor(),
        proveedorContacto: document.getElementById('proveedorContacto').value,
        proveedorTelefono: document.getElementById('proveedorTelefono').value,
        proveedorEmail: document.getElementById('proveedorEmail').value,
        fecha: document.getElementById('fecha').value,
        detalles: detallesOrden.map(d => ({
            tipoItem: d.tipoItem,
            itemId: d.itemId,
            cantidad: d.cantidad,
            precioUnitario: d.precioUnitario
        }))
    };

    const id = document.getElementById('ordenId').value;
    const url = id ? `${API_ORDENES}/${id}` : API_ORDENES;
    const method = id ? 'PUT' : 'POST';

    try {
        await fetchJsonConToken(url, { method, body: JSON.stringify(payload) });
        bootstrap.Modal.getOrCreateInstance(document.getElementById('ordenModal')).hide();
        await refrescarOrdenes();
    } catch (e) {
        alert(e.message || 'No se pudo guardar la orden');
    }
}

async function editarOrden(id) {
    try {
        const orden = await fetchJsonConToken(`${API_ORDENES}/${id}`);
        document.getElementById('ordenId').value = orden.idOrden;
        const provSelect = document.getElementById('proveedorSelect');
        const provNombre = document.getElementById('proveedorNombre');
        const encontrado = proveedores.find(p => (p.nombreProveedor || '').toLowerCase() === (orden.proveedorNombre || '').toLowerCase());
        if (provSelect) {
            provSelect.value = encontrado ? encontrado.nombreProveedor : '__custom__';
            toggleProveedorCustom();
        }
        if (provNombre) {
            provNombre.value = orden.proveedorNombre || '';
        }
        document.getElementById('proveedorContacto').value = orden.proveedorContacto || '';
        document.getElementById('proveedorTelefono').value = orden.proveedorTelefono || '';
        document.getElementById('proveedorEmail').value = orden.proveedorEmail || '';
        document.getElementById('fecha').value = orden.fecha || '';
        detallesOrden = (orden.detalles || []).map(d => ({
            tipoItem: d.tipoItem,
            itemId: d.itemId,
            nombreItem: d.nombreItem,
            cantidad: d.cantidad,
            precioUnitario: parseFloat(d.precioUnitario || 0)
        }));
        renderDetalles();
        bootstrap.Modal.getOrCreateInstance(document.getElementById('ordenModal')).show();
    } catch (e) {
        alert('No se pudo cargar la orden para editar.');
    }
}

async function cambiarEstado(id, nuevoEstado, recepciones = null) {
    const payload = { estado: nuevoEstado };
    if (recepciones) payload.recepciones = recepciones;
    if (!confirm(`¿Confirmas cambiar el estado a ${nuevoEstado}?`)) return;
    try {
        await fetchJsonConToken(`${API_ORDENES}/${id}/estado`, {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        await refrescarOrdenes();
    } catch (e) {
        alert(e.message || 'No se pudo actualizar el estado');
    }
}

async function abrirModalRecepcion(id) {
    try {
        const orden = await fetchJsonConToken(`${API_ORDENES}/${id}`);
        document.getElementById('recepcionOrdenId').value = orden.idOrden;
        const tbody = document.querySelector('#tablaRecepcion tbody');
        tbody.innerHTML = '';
        (orden.detalles || []).forEach(det => {
            const row = document.createElement('tr');
            row.innerHTML = `
                <td>${det.nombreItem || 'Ítem'}</td>
                <td>${det.tipoItem || '-'}</td>
                <td class="text-end">${det.cantidad}</td>
                <td class="text-end">
                    <input type="number" class="form-control form-control-sm text-end" min="0" value="${det.cantidad}" data-id="${det.idDetalle}">
                </td>
            `;
            tbody.appendChild(row);
        });
        bootstrap.Modal.getOrCreateInstance(document.getElementById('recepcionModal')).show();
    } catch (e) {
        alert('No se pudo cargar la orden para confirmar recepción.');
    }
}

async function confirmarRecepcion() {
    const ordenId = document.getElementById('recepcionOrdenId').value;
    const inputs = document.querySelectorAll('#tablaRecepcion input[data-id]');
    const recepciones = Array.from(inputs).map(inp => ({
        idDetalle: parseInt(inp.dataset.id, 10),
        cantidadRecibida: parseInt(inp.value, 10) || 0
    }));
    if (!confirm('¿Confirmas la recepción de los ítems seleccionados?')) return;
    await cambiarEstado(ordenId, 'Recibida', recepciones);
    bootstrap.Modal.getOrCreateInstance(document.getElementById('recepcionModal')).hide();
}

async function fetchJsonConToken(url, options = {}) {
    const token = sessionStorage.getItem(TOKEN_KEY);
    const headers = Object.assign({}, options.headers || {});
    headers['Content-Type'] = 'application/json';
    if (token) headers['Authorization'] = `Bearer ${token}`;
    const response = await fetch(url, { ...options, headers });
    if (!response.ok) {
        let error = 'Error en la solicitud';
        try {
            const body = await response.json();
            error = body.error || error;
        } catch (_) {
            // ignore
        }
        throw new Error(error);
    }
    if (response.status === 204) return null;
    return response.json();
}

function setFechaHoy() {
    const hoy = new Date();
    const yyyy = hoy.getFullYear();
    const mm = String(hoy.getMonth() + 1).padStart(2, '0');
    const dd = String(hoy.getDate()).padStart(2, '0');
    const fechaStr = `${yyyy}-${mm}-${dd}`;
    const fechaInput = document.getElementById('fecha');
    if (fechaInput) fechaInput.value = fechaStr;
}

function generarCsvCompras(datos) {
    const encabezados = ['ID', 'Proveedor', 'Fecha', 'Estado', 'Total', 'Contacto', 'Teléfono', 'Email'];
    const filas = datos.map(o => [
        o.idOrden ?? '',
        (o.proveedorNombre || '').replace(/,/g, ' '),
        o.fecha ?? '',
        o.estado ?? '',
        Number(o.total || 0).toFixed(2),
        (o.proveedorContacto || '').replace(/,/g, ' '),
        o.proveedorTelefono || '',
        o.proveedorEmail || ''
    ]);
    return [encabezados, ...filas].map(f => f.join(',')).join('\n');
}

function descargarReporteCompras() {
    const hoy = new Date();
    const mes = hoy.getMonth();
    const anio = hoy.getFullYear();
    const enMes = ordenes.filter(o => {
        const f = parseFechaSeguro(o.fecha);
        return f && f.getMonth() === mes && f.getFullYear() === anio;
    });
    if (!enMes.length) {
        alert('No hay órdenes registradas en el mes actual.');
        return;
    }
    const csv = generarCsvCompras(enMes);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const enlace = document.createElement('a');
    enlace.href = url;
    enlace.download = `reporte_compras_${hoy.toISOString().slice(0, 10)}.csv`;
    enlace.click();
    URL.revokeObjectURL(url);
}

function poblarSelectProveedores() {
    const select = document.getElementById('proveedorSelect');
    if (!select) return;
    select.innerHTML = '';
    const placeholder = document.createElement('option');
    placeholder.value = '';
    placeholder.textContent = 'Selecciona proveedor';
    select.appendChild(placeholder);

    proveedores.forEach(p => {
        const opt = document.createElement('option');
        opt.value = p.nombreProveedor;
        opt.textContent = p.nombreProveedor;
        opt.dataset.contacto = p.contacto || '';
        opt.dataset.telefono = p.telefono || '';
        opt.dataset.email = p.email || '';
        select.appendChild(opt);
    });

    const customOpt = document.createElement('option');
    customOpt.value = '__custom__';
    customOpt.textContent = 'Otro (escribir)';
    select.appendChild(customOpt);
}

function toggleProveedorCustom() {
    const select = document.getElementById('proveedorSelect');
    const inputNombre = document.getElementById('proveedorNombre');
    const contacto = document.getElementById('proveedorContacto');
    const telefono = document.getElementById('proveedorTelefono');
    const email = document.getElementById('proveedorEmail');
    if (!select || !inputNombre) return;

    if (select.value === '__custom__') {
        inputNombre.classList.remove('d-none');
        inputNombre.value = '';
        if (contacto) contacto.value = '';
        if (telefono) telefono.value = '';
        if (email) email.value = '';
        toggleCamposProveedorEdicion(true);
    } else if (select.value) {
        inputNombre.classList.add('d-none');
        const opt = select.selectedOptions[0];
        inputNombre.value = opt ? opt.value : '';
        if (contacto && opt) contacto.value = opt.dataset.contacto || '';
        if (telefono && opt) telefono.value = opt.dataset.telefono || '';
        if (email && opt) email.value = opt.dataset.email || '';
        toggleCamposProveedorEdicion(false);
    } else {
        inputNombre.classList.add('d-none');
        inputNombre.value = '';
        if (contacto) contacto.value = '';
        if (telefono) telefono.value = '';
        if (email) email.value = '';
        toggleCamposProveedorEdicion(true);
    }
}

function obtenerNombreProveedor() {
    const select = document.getElementById('proveedorSelect');
    const inputNombre = document.getElementById('proveedorNombre');
    if (!select) return inputNombre ? inputNombre.value : '';
    if (select.value === '__custom__') {
        return inputNombre ? inputNombre.value : '';
    }
    return select.value || (inputNombre ? inputNombre.value : '');
}

function toggleCamposProveedorEdicion(habilitar) {
    const campos = [
        document.getElementById('proveedorContacto'),
        document.getElementById('proveedorTelefono'),
        document.getElementById('proveedorEmail')
    ];
    campos.forEach(campo => {
        if (!campo) return;
        campo.readOnly = !habilitar;
        campo.classList.toggle('bg-light', !habilitar);
    });
}

function calcularKpis() {
    const estadoCounts = ordenes.reduce((acc, o) => {
        acc[o.estado] = (acc[o.estado] || 0) + 1;
        return acc;
    }, {});
    kpis.pendientes = estadoCounts['Pendiente'] || 0;
    kpis.enProceso = (estadoCounts['Aprobada'] || 0) + (estadoCounts['EnEnvio'] || 0) + (estadoCounts['PorConfirmar'] || 0);
    kpis.recibidas = estadoCounts['Recibida'] || 0;
    kpis.canceladas = (estadoCounts['Cancelada'] || 0) + (estadoCounts['Rechazada'] || 0);
    setTexto('kpiPendientes', kpis.pendientes);
    setTexto('kpiEnProceso', kpis.enProceso);
    setTexto('kpiRecibidas', kpis.recibidas);
    setTexto('kpiCanceladas', kpis.canceladas);
}

function activarTabPendientePorDefecto() {
    const pendienteTab = document.querySelector('#ordenTabs .nav-link[data-estado="Pendiente"]');
    if (pendienteTab) {
        document.querySelectorAll('#ordenTabs .nav-link').forEach(b => b.classList.remove('active'));
        pendienteTab.classList.add('active');
    }
    estadoTabActual = 'Pendiente';
}

function setTexto(id, valor) {
    const el = document.getElementById(id);
    if (el) {
        el.textContent = valor;
    }
}

function parseFechaSeguro(valor) {
    if (!valor) return null;
    const fecha = new Date(valor);
    return Number.isNaN(fecha.getTime()) ? null : fecha;
}
