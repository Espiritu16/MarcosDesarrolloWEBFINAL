/**
 * auth.js - Guardia de Seguridad para Páginas Protegidas
 * Versión 5.0 - Nombres de roles unificados con el backend (sin "ROLE_").
 */
document.addEventListener('DOMContentLoaded', function () {

    // --- SECCIÓN 1: VERIFICACIÓN Y CARGA DE DATOS DE SESIÓN ---
    const token = sessionStorage.getItem('jwtToken');
    const userName = sessionStorage.getItem('userName');
    const userRole = sessionStorage.getItem('userRole');
    const userEmail = sessionStorage.getItem('userEmail');
    const userEstado = sessionStorage.getItem('userEstado');
      if (!token) {
        alert('Acceso denegado. Por favor, inicie sesión.');
        window.location.href = '/'; //Redirige al login si no hay un token para verificar
        return;
    }
    if (userEstado === 'Inactivo') {
        alert('Tu cuenta ha sido desactivada. Contacta al administrador para más información.');
         // Limpiar sesión
         sessionStorage.removeItem('jwtToken');
         sessionStorage.removeItem('userName');
         sessionStorage.removeItem('userRole');
         sessionStorage.removeItem('userEmail');
         sessionStorage.removeItem('userEstado');
         window.location.href = '/';
            return;
    }
    const roleHomes = {
        'Administrador': '/administrador/',
        'Vendedor': '/vendedor/',
        'Contador': '/contador/'
    };

    const destinoRol = roleHomes[userRole];
    if (destinoRol) {
        const actual = window.location.pathname;
        if (!actual.startsWith(destinoRol)) {
            window.location.href = destinoRol;
            return;
        }
    }

    // --- SECCIÓN 2: PERSONALIZAR LA INTERFAZ DE USUARIO (UI) ---
    const profileNameElement = document.getElementById('user-name');
    const profileRoleElement = document.getElementById('user-role');
    const profileEmailElement = document.querySelector('.user-email');

    if (userName && profileNameElement) {
        profileNameElement.textContent = userName;
    }
    if (userRole && profileRoleElement) {
        // El rol ya viene sin el prefijo, así que no necesitamos reemplazar nada.
        profileRoleElement.textContent = userRole;
    }
    if (userEmail && profileEmailElement) {
        profileEmailElement.textContent = userEmail;
        profileEmailElement.title = userEmail;
    }

    // --- SECCIÓN 3: LÓGICA DE PERMISOS POR ROL ---
    console.log(`Guardia 'auth.js' activado. Rol detectado: ${userRole}`);

    // ================== CAMBIO CLAVE ==================
    // Las claves del objeto de permisos ahora coinciden con lo que envía el backend.
    const permissions = {
        'Vendedor': {
            canCreate: true,
            canEdit: true,
            canDelete: false,
            pageExceptions: {
            '/vendedor/insumos': { canCreate: true, canEdit: true, canDelete: false },
            'ventas_insumos.html': { canCreate: true, canEdit: true, canDelete: false },
            'insumos':{ canCreate: true, canEdit: true, canDelete: false },
            '/vendedor/productos':{ canCreate: true, canEdit: true, canDelete: false },
            'ventas_productos.html':{ canCreate: true, canEdit: true, canDelete: false },
            'productos':{ canCreate: true, canEdit: true, canDelete: false },
            '/vendedor/compras':{canCreate:false,canEdit:false,canDelete:false,disableActions:true},
            'compras_vendedor.html':{canCreate:false,canEdit:false,canDelete:false,disableActions:true},
            'compras':{canCreate:false,canEdit:false,canDelete:false,disableActions:true}
            }
        },
        'Contador': {
            canCreate: true, // Puede registrar gastos
            canEdit: true,   // Puede editar gastos
            canDelete: false,
            pageExceptions: {
                '/contador/ventas': { canCreate: false, canEdit: false, canDelete: false, disableActions: true },
                'ventasContador.html': { canCreate: false, canEdit: false, canDelete: false, disableActions: true },
                'ventas': { canCreate: false, canEdit: false, canDelete: false, disableActions: true },
                '/contador/gastos': {canCreate:true,canEdit:true,canDelete: false},
                'gastosContador.html': {canCreate:true,canEdit:true,canDelete: false},
                'gastos': {canCreate:true,canEdit:true,canDelete: false}
            }
        },
        'Administrador': {
            canCreate: true,
            canEdit: true,
            canDelete: true
        }
    };

    const currentUserPermissions = permissions[userRole] || {};
    const normalizedPath = window.location.pathname.replace(/\/+$/, '') || '/';
    const pageSegment = normalizedPath.split('/').pop();
    const keysToCheck = [normalizedPath, pageSegment];

    let finalPermissions = { ...currentUserPermissions };
    if (currentUserPermissions.pageExceptions) {
        keysToCheck.forEach((key) => {
            if (currentUserPermissions.pageExceptions[key]) {
                finalPermissions = { ...finalPermissions, ...currentUserPermissions.pageExceptions[key] };
            }
        });
    }

    const disableActions = finalPermissions.disableActions === true;

    function disableControl(btn) {
        if (!btn) return;
        btn.classList.add('disabled', 'opacity-50', 'cursor-not-allowed');
        btn.setAttribute('aria-disabled', 'true');
        btn.tabIndex = -1;
        if (!btn.title) {
            btn.title = 'Acción no disponible para su rol';
        }
        const suppressEvent = (event) => {
            event.preventDefault();
            event.stopImmediatePropagation();
            event.stopPropagation();
            if (event.type === 'click' || event.key === 'Enter' || event.key === ' ' || event.type === 'keydown') {
                alert('No tienes permisos para realizar esta acción.');
            }
        };
        btn.addEventListener('click', suppressEvent, true);
        btn.addEventListener('keydown', suppressEvent, true);
        btn.addEventListener('pointerdown', suppressEvent, true);
        btn.addEventListener('touchstart', suppressEvent, true);
        btn.style.pointerEvents = 'none';
    }

    function disableActionButtons() {
        const selectors = [
            '.btn-editar',
            '.btn-eliminar',
            '.btn-crear',
            '[data-bs-target="#ventaModal"]',
            'button[data-action]',
            'a[data-action]'
        ];
        document.querySelectorAll(selectors.join(',')).forEach(disableControl);
    }

    if (finalPermissions.canCreate === false) {
        document.querySelectorAll('.btn-crear, #btnNuevaOrden, #btnNuevoGasto, #btnNuevoInsumo, #btnNuevoProducto, #btnAgregarRol, #btnNuevaVenta')
            .forEach(btn => {
                if (!btn) return;
                if (disableActions) {
                    disableControl(btn);
                } else {
                    btn.style.display = 'none';
                }
            });
    }

    if (!disableActions) {
        const procesarBotones = () => {
            if (finalPermissions.canEdit === false) {
                document.querySelectorAll('.btn-editar').forEach(btn => {
                    if (btn) btn.style.display = 'none';
                });
            }
            if (finalPermissions.canDelete === false) {
                document.querySelectorAll('.btn-eliminar').forEach(btn => {
                    if (btn) btn.style.display = 'none';
                });
            }
            if (finalPermissions.canEdit === false && finalPermissions.canDelete === false) {
                document.querySelectorAll('.col-acciones, td.col-acciones').forEach(col => {
                    col.style.display = 'none';
                });
            }
        };

        // Procesar botones existentes
        procesarBotones();

        // Observar cambios en la tabla para procesar botones nuevos
        const observer = new MutationObserver(procesarBotones);
        const tableBody = document.querySelector('tbody');
        if (tableBody) {
            observer.observe(tableBody, { childList: true, subtree: true });
        }
    }

    if (disableActions) {
        disableActionButtons();
        const observer = new MutationObserver(disableActionButtons);
        document.querySelectorAll('tbody').forEach(tbody => {
            observer.observe(tbody, { childList: true, subtree: true });
        });
    }
});

//Saludo al usuario
document.addEventListener("DOMContentLoaded", () => {
    const userName = sessionStorage.getItem("userName");
    const welcomeElement = document.getElementById("welcome-user-name");
    if (userName && welcomeElement) {
        welcomeElement.textContent = userName;
    }
});
(function setUpFetchInterceptor(){
    const originalFetch=window.fetch; window.fetch = function (...args) {
    let [url, options = {}] = args;
    const token = sessionStorage.getItem('jwtToken');
    // Solo añde el token si existe y la petición se envió al backend
    const isBackendRequest =typeof url === 'string' &&
    (url.startsWith('/') || url.includes(window.location.hostname));
    if (token && isBackendRequest) {
       // Asegurarse de que existe el objeto headers
       options.headers = options.headers || {};
       // Si headers es un objeto Headers, convertirlo a objeto plano
       if (options.headers instanceof Headers) {
           const headersObj = {};
           options.headers.forEach((value, key) => {
              headersObj[key] = value;
           });
           options.headers = headersObj;
       }
       // Añadir el token JWT SOLO si no existe ya
       if (!options.headers['Authorization'] && !options.headers['authorization']) {
           options.headers['Authorization'] = `Bearer ${token}`;
           console.log(`Token JWT añadido a la petición: ${url}`);
       }
    }
    // Ejecuta la petición original
    return originalFetch(url, options).then(response => {
        // Manejar respuestas de error de autenticación
        if (response.status === 401 || response.status === 403) {
              response.json().then(data => {
              const mensaje = data.error || 'Su sesión ha expirado. Por favor, inicie sesión nuevamente.';
              // Limpia sessionStorage
              sessionStorage.removeItem('jwtToken');
              sessionStorage.removeItem('userName');
              sessionStorage.removeItem('userRole');
              sessionStorage.removeItem('userEmail');
              sessionStorage.removeItem('userEstado');
              alert(mensaje);
              window.location.href = '/';
              }).catch(() => {
              // Si no se puede leer el JSON, usar mensaje genérico
              sessionStorage.clear();
              alert('Su sesión ha expirado. Por favor, inicie sesión nuevamente.');
              window.location.href = '/';
              });
              // Retornar una promesa rechazada para detener el procesamiento
              return Promise.reject(new Error('Token expirado o inválido'));
        }
          return response;
        })
       .catch(error => {
         console.error('Error en la petición:', error);
         throw error;
       });
    };
    console.log(' Interceptor de fetch creado. Todas las peticiones incluirán automáticamente el JWT token.');
})();
