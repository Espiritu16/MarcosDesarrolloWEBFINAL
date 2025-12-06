const ROLES_API = '/api/roles';
const PERMISOS_API = '/api/permisos';
const TOKEN_KEY = 'jwtToken';
const USUARIOS_API='/api/usuario';
let roles = [];
let usuarios = [];
let permisosCatalogo = [];
let rolEnEdicion = null;
let filtroRol = 'all';

const tablaBody = document.querySelector('#tablaRoles tbody');
const tablaUsuarios=document.querySelector('#tablaUsuarios tbody');
const tablaUsuariosBody = document.querySelector('#tablaUsuarios tbody');
const contenedorTablaRoles = document.getElementById('contenedorTablaRoles');
const contenedorTablaUsuarios = document.getElementById('contenedorTablaUsuarios');
const infoRegistros = document.getElementById('infoRegistros');
const formRol = document.getElementById('formRol');
const rolModalElement = document.getElementById('rolModal');
const rolModal = rolModalElement ? new bootstrap.Modal(rolModalElement) : null;
const modalTitle = document.getElementById('modalLabel');
const inputNombre = document.getElementById('nombreRol');
const inputDescripcion = document.getElementById('descripcionRol');
const inputRolId = document.getElementById('rolId');
const inputNombreUsuario=document.getElementById('nombreUsuario');
const inputCorreo=document.getElementById('correo');
const inputContrasena=document.getElementById('contrasena');
const estadoB=document.getElementById('estadoB');
const checklistPermisos = document.getElementById('permisosChecklist');
const formUsuario = document.getElementById('formUsuario');
const usuarioModalElement = document.getElementById('usuarioModal');
const usuarioModal = usuarioModalElement ? new bootstrap.Modal(usuarioModalElement) : null;
const usuarioModalLabel = document.getElementById('usuarioModalLabel');
const rol2 = document.getElementById('rol2');
function obtenerToken() {
  return sessionStorage.getItem(TOKEN_KEY);
}

async function fetchConToken(url, options = {}) {
  const headers = Object.assign({}, options.headers || {});
  headers['Content-Type'] = 'application/json';
  const token = obtenerToken();
  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }
  return fetch(url, { ...options, headers });
}

async function cargarPermisos() {
  try {
    const response = await fetchConToken(PERMISOS_API, { method: 'GET' });
    if (!response.ok) {
      throw new Error('No se pudieron cargar los permisos');
    }
    permisosCatalogo = await response.json();
    renderPermisosChecklist();
  } catch (error) {
    console.error('❌ Error al cargar permisos:', error);
    mostrarMensaje(error.message || 'No se pudieron cargar los permisos', 'danger');
    permisosCatalogo = [];
    renderPermisosChecklist();
  }
}


function formatoFecha(fechaIso) {
  if (!fechaIso) {
    return '—';
  }
  try {
    const fecha = new Date(fechaIso);
    return fecha.toLocaleString('es-PE', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
  } catch (e) {
    return fechaIso;
  }
}

function mostrarMensaje(mensaje, tipo = 'info') {
  const toast = document.createElement('div');
  toast.className = `alert alert-${tipo}`;
  toast.textContent = mensaje;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 2500);
}

function renderRoles() {
  tablaBody.innerHTML = '';

  if (!roles.length) {
    const row = document.createElement('tr');
    row.innerHTML = `<td colspan="8" class="text-center text-secondary py-3">No hay roles registrados</td>`;
    tablaBody.appendChild(row);
    infoRegistros.textContent = 'Sin registros';
    return;
  }

  roles.forEach((rol) => {
    const row = document.createElement('tr');
    row.innerHTML = `
      <td class="fw-semibold">${rol.nombre}</td>
      <td>${rol.descripcion || '—'}</td>
      <td class="text-center">${rol.usuariosAsignados || 0}</td>
      <td>${formatoFecha(rol.fechaCreacion)}</td>
      <td>${formatoFecha(rol.fechaActualizacion)}</td>
      <td>${rol.actualizadoPor || '—'}</td>
      <td class="d-flex gap-2">
        <button type="button" class="btn btn-sm btn-outline-secondary" data-accion="editar">
          <i class="bi bi-pencil"></i> Editar
        </button>
        <button type="button" class="btn btn-sm btn-outline-danger" data-accion="eliminar">
          <i class="bi bi-trash"></i> Eliminar
        </button>
      </td>
    `;

    row.querySelector('[data-accion="editar"]').addEventListener('click', () => abrirModalEdicion(rol));
    row.querySelector('[data-accion="eliminar"]').addEventListener('click', () => eliminarRol(rol.id, rol.nombre));

    tablaBody.appendChild(row);
  });

  infoRegistros.textContent = `Mostrando ${roles.length} rol${roles.length === 1 ? '' : 'es'}`;
}
function configurarTabsRol() {
  const tabs = document.querySelectorAll('#rolesTabs .nav-link');

  if (!tabs.length) {
    console.warn('No se encontraron tabs');
    return;
  }
  tabs.forEach((tab) => {
    tab.addEventListener('click', () => {
      const rolFiltro = tab.dataset.rolTab || 'all';

      // Actualizar tabs visualmente
      tabs.forEach(t => t.classList.remove('active'));
      tab.classList.add('active');

      filtroRol = rolFiltro;

      // Mostrar/ocultar tablas
      if (rolFiltro === 'all') {
        contenedorTablaRoles.classList.remove('d-none');
        contenedorTablaUsuarios.classList.add('d-none');
        renderRoles();
      } else {
        contenedorTablaRoles.classList.add('d-none');
        contenedorTablaUsuarios.classList.remove('d-none');
        renderUsuariosPorRol(rolFiltro);
      }
    });
  });
}
function renderUsuariosPorRol(rolFiltro) {
  tablaUsuariosBody.innerHTML = '';
  // Filtrar usuarios por rol
  const usuariosFiltrados = usuarios.filter(usuario => {
    const rolNombre = (usuario.rol || '').toLowerCase();
    switch (rolFiltro) {
      case 'administrador':
        return rolNombre.includes('admin');
      case 'vendedor':
        return rolNombre.includes('vendedor');
      case 'contador':
        return rolNombre.includes('contador');
      default:
        return true;
    }
  });

  if (!usuariosFiltrados.length) {
    const row = document.createElement('tr');
    row.innerHTML = `<td colspan="8" class="text-center text-secondary py-3">No hay usuarios con este rol</td>`;
    tablaUsuariosBody.appendChild(row);
    infoRegistros.textContent = 'Sin usuarios';
    return;
  }

  usuariosFiltrados.forEach((usuario) => {
    const row = document.createElement('tr');
    row.innerHTML = `
      <td class="fw-semibold">${usuario.nombreUsuario || '—'}</td>
      <td>${usuario.email || '—'}</td>
      <td><span class="badge bg-primary">${usuario.rol || '—'}</span></td>
      <td><span class="badge ${usuario.estado === 'Activo' ? 'bg-success' : 'bg-secondary'}">${usuario.estado || '—'}</span></td>
      <td>${formatoFecha(usuario.fechaCreacion)}</td>
      <td>${formatoFecha(usuario.fechaActualizacion)}</td>
      <td>${usuario.actualizadoPor || '—'}</td>
      <td class="d-flex gap-2">
        <button type="button" class="btn btn-sm btn-outline-secondary" data-accion="editar-usuario">
          <i class="bi bi-pencil"></i> Editar
        </button>
        <button type="button" class="btn btn-sm btn-outline-${usuario.estado === 'Activo' ? 'warning' : 'success'}" data-accion="cambiar-estado">
          <i class="bi bi-${usuario.estado === 'Activo' ? 'x-circle' : 'check-circle'}"></i>
          ${usuario.estado === 'Activo' ? 'Desactivar' : 'Activar'}
        </button>
      </td>
    `;

    row.querySelector('[data-accion="editar-usuario"]').addEventListener('click', () => abrirModalEdicionUsuario(usuario));
    row.querySelector('[data-accion="cambiar-estado"]').addEventListener('click', () => cambiarEstadoUsuario(usuario.id, usuario.estado));

    tablaUsuariosBody.appendChild(row);
  });
  const rolNombre = rolFiltro.charAt(0).toUpperCase() + rolFiltro.slice(1) + 'es';
  infoRegistros.textContent = `Mostrando ${usuariosFiltrados.length} usuario${usuariosFiltrados.length === 1 ? '' : 's'} - ${rolNombre}`;
  actualizarKpisRoles();
}
async function cargarRoles() {
  try {
    const response = await fetchConToken(ROLES_API, { method: 'GET' });
    if (!response.ok) {
      throw new Error('No se pudieron cargar los roles');
    }
    roles = await response.json();
    renderRoles();
  } catch (error) {
    console.error('❌ Error al cargar roles:', error);
    mostrarMensaje(error.message, 'danger');
  }
}
async function cargarUsuarios(){
    try{
        const response=await fetchConToken(USUARIOS_API,{method:'GET'});
        if (!response.ok) {
          throw new Error('No se pudieron cargar los usuarios');
        }
        usuarios=await response.json();
    } catch(error){
        console.error(' Error al cargar usuarios:', error);
        mostrarMensaje(error.message, 'danger');
    }
}
function abrirModalCreacion() {
  rolEnEdicion = null;
  formRol.reset();
  inputNombre.value='';
  inputRolId.value = '';
  modalTitle.textContent = 'Crear rol';
  if (inputNombre) {
    inputNombre.selectedIndex = 0;
  }
  renderPermisosChecklist(obtenerPermisosPorRolDefecto(inputNombre ? inputNombre.value : null));
  if (rolModal) {
    rolModal.show();
  }
}

function abrirModalEdicion(rol) {
  rolEnEdicion = rol;
  inputRolId.value = rol.id;
  inputNombre.value=rol.nombre;
  inputDescripcion.value = rol.descripcion || '';
  modalTitle.textContent = 'Editar rol';
  const seleccionados = (rol.permisos || []).map((permiso) => permiso.id);
  renderPermisosChecklist(seleccionados);
  if (rolModal) {
    rolModal.show();
  }
}
async function eliminarRol(id, nombreRol) {
  const mensaje = `⚠️ ADVERTENCIA\n\n` +
                  `Estás a punto de eliminar permanentemente el rol "${nombreRol}".\n\n` +
                  `Esta acción NO se puede deshacer.\n\n` +
                  `¿Estás seguro de continuar?`;

  if (!confirm(mensaje)) {
    return;
  }

  try {
    const response = await fetchConToken(`${ROLES_API}/${id}`, {
      method: 'DELETE'
    });

    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error || 'Error al eliminar el rol');
    }

    const result = await response.json();
    mostrarMensaje(result.mensaje || 'Rol eliminado correctamente', 'success');
    await cargarRoles(); // Recargar la tabla

  } catch (error) {
    console.error('❌ Error al eliminar rol:', error);
    mostrarMensaje(error.message, 'danger');
  }
}
async function guardarRol(event) {
  event.preventDefault();

  const payload = {
    nombre: inputNombre.value,
    descripcion: inputDescripcion.value || null,
    permisosIds: obtenerPermisosSeleccionados(),
  };

  const rolId = inputRolId.value;
  const url = rolId ? `${ROLES_API}/${rolId}` : ROLES_API;
  const method = rolId ? 'PUT' : 'POST';

  try {
    const response = await fetchConToken(url, {
      method,
      body: JSON.stringify(payload),
    });

    if (!response.ok) {
      const errorBody = await response.json().catch(() => ({}));
      throw new Error(errorBody.error || 'No se pudo guardar el rol');
    }

    const mensaje = rolId ? 'Rol actualizado correctamente.' : 'Rol creado correctamente.';
    mostrarMensaje(mensaje, 'success');

    if (rolModal) {
      rolModal.hide();
    }
    formRol.reset();
    rolEnEdicion = null;
    await cargarRoles();
  } catch (error) {
    console.error('❌ Error al guardar rol:', error);
    mostrarMensaje(error.message, 'danger');
  }
}

function inicializarEventos() {
  const btnAgregar = document.getElementById('btnAgregarRol');
  if (btnAgregar) {
    btnAgregar.addEventListener('click', abrirModalCreacion);
  }
  if (formUsuario) {
        formUsuario.addEventListener('submit', crearUsuarios);
  }
  const btnCrearUsuarios = document.getElementById('btncrearUsuarios');
      if (btnCrearUsuarios && usuarioModalElement) {
          usuarioModalElement.addEventListener('show.bs.modal', () => {
              formUsuario.reset();
              document.getElementById('usuarioId').value = '';
              document.getElementById('usuarioModalLabel').textContent = 'Crear usuario';
          });
     }
  if (formRol) {
    formRol.addEventListener('submit', guardarRol);
  }

  if (inputNombre) {
    inputNombre.addEventListener('change', () => {
      if (rolEnEdicion) {
        return;
      }
      const seleccion = obtenerPermisosPorRolDefecto(inputNombre.value);
      renderPermisosChecklist(seleccion);
    });
  }
  configurarTabsRol();
}

document.addEventListener('DOMContentLoaded', async () => {
  inicializarEventos();
  await cargarPermisos();
  await cargarRoles();
  await mostrarRoles();
  await cargarUsuarios();
});
function actualizarTabsRolUI() {
  const tabs = document.querySelectorAll('#rolesTabs .nav-link');
  tabs.forEach((tab) => {
    const rolFiltro = tab.dataset.rolTab || 'all';
    if (rolFiltro === filtroRol) {
      tab.classList.add('active');
    } else {
      tab.classList.remove('active');
    }
  });
}
async function mostrarRoles(){
    try{
        const response=await fetch(`${ROLES_API}`,{
            method:'GET',
            headers: {
                 'Authorization': `Bearer ${sessionStorage.getItem(TOKEN_KEY)}`,
                 'Content-Type': 'application/json'
            }
        })
        if (!response.ok) {
              const error = await response.json();
              throw new Error(error.error || 'Error al eliminar el rol');
        }
        const respuesta=await response.json();
        rol2.innerHTML = '';
        const opcionDefault = document.createElement('option');
        opcionDefault.value = '';
        opcionDefault.textContent = 'Selecciona un rol';
        opcionDefault.selected = true;
        opcionDefault.disabled = true;
        rol2.appendChild(opcionDefault);
        roles.forEach(rol=>{
        const option = document.createElement('option');
            option.value = rol.id;
            option.textContent = rol.nombre;
            option.dataset.descripcion = rol.descripcion || '';
            rol2.appendChild(option);
        })
    }catch(error){
        console.error('Error al mostrar roles:', error);
    }
}
function coincideConFiltro(nombreRol) {
  if (filtroRol === 'all') {
    return true;
  }

  const nombreNormalizado = (nombreRol || '').trim().toLowerCase();

  switch (filtroRol) {
    case 'administrador':
      return nombreNormalizado.includes('admin');
    case 'vendedor':
      return nombreNormalizado.includes('vendedor');
    case 'contador':
      return nombreNormalizado.includes('contador');
    default:
      return true;
  }
}

function renderPermisosChecklist(seleccionados = []) {
  if (!checklistPermisos) {
    return;
  }

  checklistPermisos.innerHTML = '';

  if (!permisosCatalogo.length) {
    const aviso = document.createElement('div');
    aviso.className = 'text-secondary small';
    aviso.textContent = 'Sin registros de permisos';
    checklistPermisos.appendChild(aviso);
    return;
  }

  const seleccionSet = new Set(seleccionados || []);

  const listaOrdenada = [...permisosCatalogo].sort((a, b) => {
    const nombreA = (a.nombre || '').toLowerCase();
    const nombreB = (b.nombre || '').toLowerCase();
    return nombreA.localeCompare(nombreB);
  });

  listaOrdenada.forEach((permiso) => {
    const col = document.createElement('div');
    col.className = 'col';

    const wrapper = document.createElement('div');
    wrapper.className = 'form-check';

    const input = document.createElement('input');
    input.className = 'form-check-input';
    input.type = 'checkbox';
    input.id = `permiso-${permiso.id}`;
    input.value = permiso.id;
    input.name = 'permisoRol';
    if (seleccionSet.has(permiso.id)) {
      input.checked = true;
    }

    const label = document.createElement('label');
    label.className = 'form-check-label';
    label.setAttribute('for', `permiso-${permiso.id}`);
    label.textContent = permiso.nombre;

    wrapper.appendChild(input);
    wrapper.appendChild(label);

    if (permiso.descripcion) {
      const hint = document.createElement('div');
      hint.className = 'form-text';
      hint.textContent = permiso.descripcion;
      wrapper.appendChild(hint);
    }

    col.appendChild(wrapper);
    checklistPermisos.appendChild(col);
  });
}

function obtenerPermisosSeleccionados() {
  if (!checklistPermisos) {
    return [];
  }
  return Array.from(checklistPermisos.querySelectorAll('input[name="permisoRol"]:checked')).map((input) => parseInt(input.value, 10));
}

function obtenerPermisosPorRolDefecto(nombreRol) {
  if (!nombreRol) {
    return [];
  }
  const nombre = nombreRol.trim().toUpperCase();
  let objetivos;
  switch (nombre) {
    case 'ADMINISTRADOR':
      return permisosCatalogo.map((permiso) => permiso.id);
    case 'CONTADOR':
      objetivos = ['ver'];
      break;
    case 'VENDEDOR':
      objetivos = ['crear', 'editar', 'eliminar', 'ver'];
      break;
    default:
      return [];
  }
  const objetivosSet = new Set(objetivos.map((p) => p.toLowerCase()));
  return permisosCatalogo
    .filter((permiso) => objetivosSet.has((permiso.nombre || '').toLowerCase()))
    .map((permiso) => permiso.id);
}

function construirDropdownPermisos(row, rol) {
  const lista = row.querySelector('.permis-lista');
  if (!lista) {
    return;
  }

  lista.innerHTML = '';

  if (!rol.permisos || rol.permisos.length === 0) {
    const li = document.createElement('li');
    const empty = document.createElement('span');
    empty.className = 'dropdown-item text-secondary';
    empty.textContent = 'Sin permisos asignados';
    li.appendChild(empty);
    lista.appendChild(li);
    return;
  }

  rol.permisos.forEach((permiso) => {
    const li = document.createElement('li');
    const item = document.createElement('span');
    item.className = 'dropdown-item';
    const nombre = permiso && permiso.nombre ? permiso.nombre : permiso;
    item.textContent = nombre;
    if (permiso && permiso.descripcion) {
      item.title = permiso.descripcion;
    }
    li.appendChild(item);
    lista.appendChild(li);
  });
}
async function crearUsuarios(event){
    event.preventDefault();
    const usuarioId = document.getElementById('usuarioId').value;
     const payload = {
       nombreUsuario: inputNombreUsuario.value,
       estado: estadoB.value || null,
       email: inputCorreo.value,
       id_rol: parseInt(rol2.value),
       contrasena:inputContrasena.value
     };
    const url = usuarioId ? `${USUARIOS_API}/${usuarioId}` : USUARIOS_API;
    const method = usuarioId ? 'PUT' : 'POST';

     try {
       const response = await fetchConToken(url, {
         method,
         body: JSON.stringify(payload),
       });

       if (!response.ok) {
         const errorBody = await response.json().catch(() => ({}));
         throw new Error(errorBody.error || 'No se pudo crear el usuario');
       }

       const mensaje = usuarioId ? 'Usuario actualizado correctamente.' : 'Rol creado correctamente.';
       mostrarMensaje(mensaje, 'success');

       if (usuarioModal) {
         usuarioModal.hide();
       }
       formUsuario.reset();
       await cargarUsuarios();
        if (filtroRol !== 'all') {
           renderUsuariosPorRol(filtroRol);
        }
     } catch (error) {
       console.error(' Error al crear usuario:', error);
       mostrarMensaje(error.message, 'danger');
     }
}
function formatearUsuarios(lista) {
  if (!lista || !lista.length) {
    return '—';
  }
  const max = 3;
  if (lista.length <= max) {
    return lista.join(', ');
  }
  return `${lista.slice(0, max).join(', ')} y ${lista.length - max} más`;
}

function actualizarKpisRoles() {
  const totalRoles = roles.length;
  const totalUsuariosActivos = roles.reduce((sum, rol) => sum + (rol.usuariosActivos || 0), 0);
  const totalUsuariosInactivos = roles.reduce((sum, rol) => sum + (rol.usuariosInactivos || 0), 0);
  const permisosUnicos = new Set();
  roles.forEach((rol) => {
    if (rol.permisos) {
      rol.permisos.forEach((permiso) => {
        const nombre = permiso && permiso.nombre ? permiso.nombre : permiso;
        if (nombre) {
          permisosUnicos.add(nombre.toUpperCase());
        }
      });
    }
  });

  const totalPermisos = permisosUnicos.size;

  const totalRolesEl = document.getElementById('kpiTotalRoles');
  const usuariosActivosEl = document.getElementById('kpiUsuariosActivos');
  const usuariosInactivosEl = document.getElementById('kpiUsuariosInactivos');
  const permisosUnicosEl = document.getElementById('kpiPermisosUnicos');

  if (totalRolesEl) totalRolesEl.textContent = totalRoles;
  if (usuariosActivosEl) usuariosActivosEl.textContent = totalUsuariosActivos;
  if (usuariosInactivosEl) usuariosInactivosEl.textContent = totalUsuariosInactivos;
  if (permisosUnicosEl) permisosUnicosEl.textContent = totalPermisos;
}
