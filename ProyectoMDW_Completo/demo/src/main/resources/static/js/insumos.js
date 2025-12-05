const API_INSUMOS = '/api/insumos';
const TOKEN_KEY = 'jwtToken';
const DEBOUNCE_DELAY = 300;

const INSUMO_PRESETS = [
    { nombre: 'Tela denim 10oz', descripcion: 'Denim azul para jeans, grosor medio', unidad: 'metros' },
    { nombre: 'Tela algodón pima', descripcion: 'Algodón pima suave para polos', unidad: 'metros' },
    { nombre: 'Tela drill 8oz', descripcion: 'Drill para pantalones de trabajo', unidad: 'metros' },
    { nombre: 'Botón metálico 14mm', descripcion: 'Botón niquelado para casacas', unidad: 'unidades' },
    { nombre: 'Cierre metálico 20cm', descripcion: 'Cierre reforzado para prendas', unidad: 'unidades' },
    { nombre: 'Hilo poliéster negro', descripcion: 'Cono de 5000y poliéster negro', unidad: 'unidades' },
    { nombre: 'Hilo poliéster blanco', descripcion: 'Cono de 5000y poliéster blanco', unidad: 'unidades' },
    { nombre: 'Elástico 3cm', descripcion: 'Elástico ancho para pretinas', unidad: 'metros' },
    { nombre: 'Etiqueta tejida marca', descripcion: 'Etiqueta tejida con logo', unidad: 'unidades' },
    { nombre: 'Bolsas de empaque', descripcion: 'Bolsas plásticas para entrega', unidad: 'paquetes' }
];

let insumos = [];
let debounceTimer = null;
let estadoTabSeleccionado = 'Activo';

document.addEventListener('DOMContentLoaded', () => {
    inicializarEventos();
    cargarKPIs();
    refrescarInsumos();
});

function inicializarEventos() {
    const buscador = document.getElementById('buscarInsumo');
    if (buscador) {
        buscador.addEventListener('input', () => debounce(refrescarInsumos, DEBOUNCE_DELAY));
    }

    ['filtroEstadoInsumo', 'filtroStockInsumo'].forEach(id => {
        const select = document.getElementById(id);
        if (select) {
            select.addEventListener('change', (e) => {
                if (id === 'filtroEstadoInsumo') {
                    estadoTabSeleccionado = e.target.value || '';
                    activarTabPorEstado(estadoTabSeleccionado);
                }
                refrescarInsumos();
            });
        }
    });

    const btnGuardar = document.getElementById('btnGuardarInsumo');
    if (btnGuardar) {
        btnGuardar.addEventListener('click', crearInsumo);
    }

    const btnActualizar = document.getElementById('btnActualizarInsumo');
    if (btnActualizar) {
        btnActualizar.addEventListener('click', actualizarInsumo);
    }

    const tabla = document.getElementById('tablaInsumosBody');
    if (tabla) {
        tabla.addEventListener('click', (event) => {
            const btn = event.target.closest('button');
            if (!btn) return;
            const id = btn.dataset.id;
            if (!id) return;

            if (btn.dataset.action === 'edit') {
                abrirModalEdicion(id);
            } else if (btn.dataset.action === 'delete') {
                eliminarInsumo(id);
            } else if (btn.dataset.action === 'reactivate') {
                reactivarInsumo(id);
            }
        });
    }

    const modalCrearEl = document.getElementById('modalInsumoCrear');
    if (modalCrearEl) {
        modalCrearEl.addEventListener('shown.bs.modal', () => {
            limpiarFormCrear();
            setPresetDefault('insumoNombreSelect', 'insumoNombreCustom', 'insumoDescripcion', 'insumoUnidad');
        });
    }

    setupNombreSelect('insumoNombreSelect', 'insumoNombreCustom', 'insumoDescripcion', 'insumoUnidad');
    setupNombreSelect('insumoNombreSelectEditar', 'insumoNombreEditar', 'insumoDescripcionEditar', 'insumoUnidadEditar', true);

    const tabsEstado = document.querySelectorAll('#insumoEstadoTabs .nav-link');
    if (tabsEstado.length) {
        tabsEstado.forEach(tab => {
            tab.addEventListener('click', () => {
                tabsEstado.forEach(t => t.classList.remove('active'));
                tab.classList.add('active');
                estadoTabSeleccionado = tab.dataset.estado || '';
                setSelectValue('filtroEstadoInsumo', estadoTabSeleccionado);
                refrescarInsumos();
            });
        });
        activarTabPorEstado(estadoTabSeleccionado);
        setSelectValue('filtroEstadoInsumo', estadoTabSeleccionado);
    }
}

function debounce(callback, delay) {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(callback, delay);
}

async function refrescarInsumos() {
    try {
        const query = construirQuery();
        const url = query ? `${API_INSUMOS}?${query}` : API_INSUMOS;
        const response = await fetchConToken(url);
        if (!response.ok) {
            throw new Error('No se pudieron cargar los insumos.');
        }
        insumos = await response.json();
        renderTabla();
    } catch (error) {
        console.error('Error al refrescar insumos:', error);
        renderTabla(true, error.message);
    }
}

function construirQuery() {
    const params = new URLSearchParams();
    const estado = document.getElementById('filtroEstadoInsumo');
    const stock = document.getElementById('filtroStockInsumo');
    const termino = document.getElementById('buscarInsumo');

    const estadoValor = estadoTabSeleccionado || (estado && estado.value);
    if (estadoValor) params.append('estado', estadoValor);
    if (stock && stock.value) params.append('stock', stock.value);
    if (termino && termino.value.trim() !== '') params.append('search', termino.value.trim());
    return params.toString();
}

function renderTabla(error = false, mensajeError = '') {
    const tbody = document.getElementById('tablaInsumosBody');
    if (!tbody) return;
    tbody.innerHTML = '';

    if (error) {
        const row = document.createElement('tr');
        row.innerHTML = `<td colspan="6" class="text-center text-danger py-3">${mensajeError || 'Error al cargar insumos.'}</td>`;
        tbody.appendChild(row);
        return;
    }

    if (!insumos.length) {
        const row = document.createElement('tr');
        row.innerHTML = `<td colspan="6" class="text-center text-muted py-3">No hay insumos registrados.</td>`;
        tbody.appendChild(row);
        return;
    }

    insumos.forEach((insumo) => {
        const row = document.createElement('tr');
        const inactivo = (insumo.estado || 'Activo') === 'Inactivo';
        const accionBtn = inactivo
            ? `<button class="btn btn-sm btn-outline-success btn-crear" data-action="reactivate" data-id="${insumo.idInsumo}">
                 <i class="bi bi-check2-circle"></i>
               </button>`
            : `<button class="btn btn-sm btn-outline-danger btn-eliminar" data-action="delete" data-id="${insumo.idInsumo}">
                 <i class="bi bi-trash"></i>
               </button>`;
        row.innerHTML = `
            <td class="fw-semibold">${insumo.idInsumo ?? '—'}</td>
            <td>
              <div class="fw-semibold">${insumo.nombre || 'Sin nombre'}</div>
              <div class="text-muted small">${insumo.descripcion || ''}</div>
            </td>
            <td>${insumo.unidadMedida || '—'}</td>
            <td>
              <span class="badge ${insumo.stockBajo ? 'bg-warning-subtle text-warning' : 'bg-success-subtle text-success'}">
                <i class="bi ${insumo.stockBajo ? 'bi-exclamation-triangle' : 'bi-check2-circle'} me-1"></i>
                ${insumo.stockActual ?? 0}
              </span>
            </td>
            <td>${badgeEstado(insumo.estado)}</td>
            <td class="d-flex gap-2">
              <button class="btn btn-sm btn-outline-secondary btn-editar" data-action="edit" data-id="${insumo.idInsumo}">
                <i class="bi bi-pencil"></i>
              </button>
              ${accionBtn}
            </td>
        `;
        tbody.appendChild(row);
    });
}

function badgeEstado(estado) {
    if (!estado || estado === 'Activo') {
        return '<span class="badge bg-success-subtle text-success">Activo</span>';
    }
    return '<span class="badge bg-secondary-subtle text-secondary">Inactivo</span>';
}

async function cargarKPIs() {
    try {
        const response = await fetchConToken(`${API_INSUMOS}/estadisticas`);
        if (!response.ok) throw new Error('No se pudieron cargar las métricas');
        const stats = await response.json();
        setTexto('kpiTotalInsumos', stats.totalInsumos ?? '—');
        setTexto('kpiActivos', stats.insumosActivos ?? '—');
        setTexto('kpiStockBajo', stats.stockBajo ?? '—');
        const inactivos = (stats.totalInsumos ?? 0) - (stats.insumosActivos ?? 0);
        setTexto('kpiInactivos', inactivos >= 0 ? inactivos : '—');
    } catch (error) {
        console.error('Error al cargar KPIs de insumos:', error);
        setTexto('kpiTotalInsumos', '—');
        setTexto('kpiActivos', '—');
        setTexto('kpiStockBajo', '—');
        setTexto('kpiInactivos', '—');
    }
}

function setTexto(id, valor) {
    const el = document.getElementById(id);
    if (el) el.textContent = valor;
}

function limpiarFormCrear() {
    const form = document.getElementById('formCrearInsumo');
    if (form) form.reset();
    setSelectValue('insumoEstado', 'Activo');
}

function setSelectValue(id, value) {
    const el = document.getElementById(id);
    if (el) el.value = value;
}

function activarTabPorEstado(estado) {
    const tabs = document.querySelectorAll('#insumoEstadoTabs .nav-link');
    if (!tabs.length) return;
    tabs.forEach(t => {
        const est = t.dataset.estado || '';
        if (est === (estado || '')) {
            t.classList.add('active');
        } else {
            t.classList.remove('active');
        }
    });
}

async function crearInsumo() {
    const payload = construirPayloadDesdeFormulario({
        nombreSelect: 'insumoNombreSelect',
        nombreCustom: 'insumoNombreCustom',
        descripcion: 'insumoDescripcion',
        unidadMedida: 'insumoUnidad',
        stockActual: 'insumoStock',
        estado: 'insumoEstado'
    });

    try {
        const response = await fetchConToken(API_INSUMOS, {
            method: 'POST',
            body: JSON.stringify(payload)
        });
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error || 'No se pudo registrar el insumo.');
        }
        mostrarMensaje('Insumo registrado correctamente.', 'success');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalInsumoCrear')).hide();
        await Promise.all([refrescarInsumos(), cargarKPIs()]);
    } catch (error) {
        console.error('Error al crear insumo:', error);
        mostrarMensaje(error.message || 'No se pudo registrar el insumo.', 'danger');
    }
}

async function abrirModalEdicion(id) {
    const insumo = insumos.find((i) => String(i.idInsumo) === String(id));
    if (!insumo) {
        mostrarMensaje('No se encontró el insumo seleccionado.', 'danger');
        return;
    }
    setInputValue('insumoIdEditar', insumo.idInsumo);
    setSelectValue('insumoNombreSelectEditar', esPreset(insumo.nombre) ? insumo.nombre : '__custom__');
    toggleCustomNombre('insumoNombreSelectEditar', 'insumoNombreEditar', 'insumoDescripcionEditar', 'insumoUnidadEditar', true, insumo);
    setInputValue('insumoStockEditar', insumo.stockActual);
    setSelectValue('insumoEstadoEditar', insumo.estado || 'Activo');
    bootstrap.Modal.getOrCreateInstance(document.getElementById('modalInsumoEditar')).show();
}

function setInputValue(id, value) {
    const el = document.getElementById(id);
    if (el !== null && el !== undefined) {
        el.value = value ?? '';
    }
}

async function actualizarInsumo() {
    const id = document.getElementById('insumoIdEditar')?.value;
    if (!id) {
        mostrarMensaje('No se pudo identificar el insumo a actualizar.', 'danger');
        return;
    }

    const payload = construirPayloadDesdeFormulario({
        nombreSelect: 'insumoNombreSelectEditar',
        nombreCustom: 'insumoNombreEditar',
        descripcion: 'insumoDescripcionEditar',
        unidadMedida: 'insumoUnidadEditar',
        stockActual: 'insumoStockEditar',
        estado: 'insumoEstadoEditar'
    });

    try {
        const response = await fetchConToken(`${API_INSUMOS}/${id}`, {
            method: 'PUT',
            body: JSON.stringify(payload)
        });
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error || 'No se pudo actualizar el insumo.');
        }
        mostrarMensaje('Insumo actualizado correctamente.', 'success');
        bootstrap.Modal.getOrCreateInstance(document.getElementById('modalInsumoEditar')).hide();
        await Promise.all([refrescarInsumos(), cargarKPIs()]);
    } catch (error) {
        console.error('Error al actualizar insumo:', error);
        mostrarMensaje(error.message || 'No se pudo actualizar el insumo.', 'danger');
    }
}

async function eliminarInsumo(id) {
    const confirma = confirm('¿Eliminar este insumo? Se marcará como inactivo y su stock quedará en 0.');
    if (!confirma) return;
    try {
        const response = await fetchConToken(`${API_INSUMOS}/${id}`, { method: 'DELETE' });
        if (!response.ok) {
            const data = await response.json().catch(() => ({}));
            throw new Error(data.error || 'No se pudo eliminar el insumo.');
        }
        mostrarMensaje('Insumo eliminado.', 'info');
        await Promise.all([refrescarInsumos(), cargarKPIs()]);
    } catch (error) {
        console.error('Error al eliminar insumo:', error);
        mostrarMensaje(error.message || 'No se pudo eliminar el insumo.', 'danger');
    }
}

async function reactivarInsumo(id) {
    try {
        const response = await fetchConToken(`${API_INSUMOS}/${id}`, {
            method: 'PUT',
            body: JSON.stringify({ estado: 'Activo' })
        });
        const data = await response.json();
        if (!response.ok) {
            throw new Error(data.error || 'No se pudo reactivar el insumo.');
        }
        mostrarMensaje('Insumo reactivado.', 'success');
        await Promise.all([refrescarInsumos(), cargarKPIs()]);
    } catch (error) {
        console.error('Error al reactivar insumo:', error);
        mostrarMensaje(error.message || 'No se pudo reactivar el insumo.', 'danger');
    }
}

function construirPayloadDesdeFormulario(campos) {
    const payload = {};
    payload.nombre = obtenerNombreSegunSelect(campos.nombreSelect, campos.nombreCustom);
    payload.descripcion = obtenerTexto(campos.descripcion);
    payload.unidadMedida = obtenerTexto(campos.unidadMedida);
    payload.stockActual = obtenerEntero(campos.stockActual);
    const estadoValor = obtenerTexto(campos.estado);
    payload.estado = estadoValor || 'Activo';
    return payload;
}

function obtenerTexto(id) {
    const el = document.getElementById(id);
    if (!el) return null;
    const value = (el.value || '').trim();
    return value === '' ? null : value;
}

function obtenerEntero(id) {
    const el = document.getElementById(id);
    if (!el || el.value === '') return null;
    const num = parseInt(el.value, 10);
    return Number.isFinite(num) ? num : null;
}

function obtenerNombreSegunSelect(selectId, customId) {
    const select = document.getElementById(selectId);
    const custom = document.getElementById(customId);
    if (!select) return obtenerTexto(customId);
    if (select.value === '__custom__') {
        return obtenerTexto(customId);
    }
    return select.value || obtenerTexto(customId);
}

function aplicarPresetNombre(inputNombreId, descId, unidadId, soloSiVacio = false) {
    const nombreInput = document.getElementById(inputNombreId);
    if (!nombreInput) return;
    const nombre = (nombreInput.value || '').trim();
    const preset = INSUMO_PRESETS.find(p => p.nombre.toLowerCase() === nombre.toLowerCase());
    const desc = document.getElementById(descId);
    const unidad = document.getElementById(unidadId);

    if (!preset) {
        toggleEditable(desc, unidad, true);
        return;
    }

    if (desc && (!soloSiVacio || desc.value.trim() === '')) {
        desc.value = preset.descripcion;
    }

    if (unidad && (!soloSiVacio || unidad.value.trim() === '')) {
        unidad.value = preset.unidad;
    }

    toggleEditable(desc, unidad, false);
}

function toggleEditable(descEl, unidadEl, editable) {
    if (descEl) {
        descEl.readOnly = !editable;
    }
    if (unidadEl) {
        unidadEl.readOnly = !editable;
    }
}

function setPresetDefault(selectId, customId, descId, unidadId) {
    const select = document.getElementById(selectId);
    if (!select || select.options.length === 0) return;
    const primera = INSUMO_PRESETS[0];
    select.value = primera ? primera.nombre : '';
    toggleCustomNombre(selectId, customId, descId, unidadId);
}

function setupNombreSelect(selectId, customInputId, descId, unidadId, soloSiVacio = false) {
    cargarOpcionesSelectNombre(selectId);
    const select = document.getElementById(selectId);
    if (!select) return;
    select.addEventListener('change', () => {
        toggleCustomNombre(selectId, customInputId, descId, unidadId, soloSiVacio);
    });
    toggleCustomNombre(selectId, customInputId, descId, unidadId, soloSiVacio);
}

function cargarOpcionesSelectNombre(selectId) {
    const select = document.getElementById(selectId);
    if (!select) return;
    select.innerHTML = '';
    const defaultOpt = document.createElement('option');
    defaultOpt.value = '';
    defaultOpt.textContent = 'Selecciona un insumo';
    select.appendChild(defaultOpt);

    INSUMO_PRESETS.forEach(preset => {
        const opt = document.createElement('option');
        opt.value = preset.nombre;
        opt.textContent = preset.nombre;
        select.appendChild(opt);
    });

    const customOpt = document.createElement('option');
    customOpt.value = '__custom__';
    customOpt.textContent = 'Otro (escribir)';
    select.appendChild(customOpt);
}

function toggleCustomNombre(selectId, customInputId, descId, unidadId, soloSiVacio = false, insumoExistente = null) {
    const select = document.getElementById(selectId);
    const input = document.getElementById(customInputId);
    const desc = document.getElementById(descId);
    const unidad = document.getElementById(unidadId);
    if (!select || !input) return;

    const valorSelect = select.value;
    if (valorSelect === '__custom__') {
        input.classList.remove('d-none');
        input.value = insumoExistente?.nombre || '';
        toggleEditable(desc, unidad, true);
        if (insumoExistente) {
            if (desc) desc.value = insumoExistente.descripcion || '';
            if (unidad) unidad.value = insumoExistente.unidadMedida || '';
        } else {
            if (desc) desc.value = '';
            if (unidad) unidad.value = '';
        }
        input.focus();
    } else if (valorSelect) {
        input.classList.add('d-none');
        input.value = valorSelect;
        aplicarPresetNombre(customInputId, descId, unidadId, soloSiVacio);
    } else {
        input.classList.add('d-none');
        input.value = '';
        if (desc) desc.value = '';
        if (unidad) unidad.value = '';
        toggleEditable(desc, unidad, true);
    }
}

function esPreset(nombre) {
    if (!nombre) return false;
    return INSUMO_PRESETS.some(p => p.nombre.toLowerCase() === nombre.toLowerCase());
}

function mostrarMensaje(mensaje, tipo = 'info') {
    const alert = document.createElement('div');
    alert.className = `alert alert-${tipo} position-fixed top-0 end-0 m-3 shadow`;
    alert.textContent = mensaje;
    document.body.appendChild(alert);
    setTimeout(() => alert.remove(), 2500);
}

async function fetchConToken(url, options = {}) {
    const token = sessionStorage.getItem(TOKEN_KEY);
    const headers = Object.assign({}, options.headers || {});
    const metodo = (options.method || 'GET').toUpperCase();
    if (metodo !== 'GET' && !headers['Content-Type']) {
        headers['Content-Type'] = 'application/json';
    }
    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }
    return fetch(url, { ...options, headers });
}
