-- ============================================================
-- SpaMascotas — Schema SQL completo
-- Ejecutar ANTES del primer arranque del backend
-- Compatible con entidades JPA de todos los módulos
-- ============================================================

-- ── Extensiones ─────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── Secuencias ──────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS roles_id_seq             START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS usuarios_id_seq          START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS usuario_roles_id_seq     START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS empleados_id_seq         START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS clientes_id_seq          START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS mascotas_id_seq          START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS horarios_id_seq          START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS servicios_id_seq         START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS servicios_precios_id_seq START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS citas_id_seq             START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS cita_detalles_id_seq     START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS categoria_id_seq         START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS productos_id_seq         START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS es_de_catego_id_seq      START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS movimientos_inv_id_seq   START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS fichas_id_seq            START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS ficha_detalles_id_seq    START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS ventas_id_seq            START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS venta_items_id_seq       START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS venta_servicios_id_seq   START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS pedido_id_seq            START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS galeria_id_seq           START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS recomendaciones_id_seq   START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS bloqueos_id_seq          START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS vacunas_id_seq           START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS mascota_vacunas_id_seq   START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS restricciones_id_seq     START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS cli_prefs_id_seq         START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS calificaciones_id_seq    START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS notificaciones_id_seq    START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS cat_temp_id_seq          START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS metodos_pago_id_seq      START 1 INCREMENT 1;
CREATE SEQUENCE IF NOT EXISTS audit_logs_id_seq        START 1 INCREMENT 1;

-- ── AUTENTICACIÓN ────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS roles (
    id     BIGINT PRIMARY KEY DEFAULT nextval('roles_id_seq'),
    nombre VARCHAR(20) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS usuarios (
    id                   BIGINT    PRIMARY KEY DEFAULT nextval('usuarios_id_seq'),
    correo               VARCHAR(255) UNIQUE NOT NULL,
    password_hash        VARCHAR(255),
    nombre_usuario       VARCHAR(50) UNIQUE,
    estado               VARCHAR(20)  NOT NULL DEFAULT 'PENDIENTE',
    token_activacion     TEXT,
    token_activacion_exp TIMESTAMP,
    reset_token          TEXT,
    reset_token_exp      TIMESTAMP,
    intentos_fallidos    INTEGER      NOT NULL DEFAULT 0,
    bloqueado_hasta      TIMESTAMP,
    totp_secret          TEXT,
    totp_habilitado      BOOLEAN      NOT NULL DEFAULT FALSE,
    google_id            TEXT,
    fecha_creacion       TIMESTAMP    NOT NULL DEFAULT NOW(),
    ultima_actividad     TIMESTAMP
);

CREATE TABLE IF NOT EXISTS usuario_roles (
    id          BIGINT PRIMARY KEY DEFAULT nextval('usuario_roles_id_seq'),
    usuario_id  BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
    rol_id      BIGINT NOT NULL REFERENCES roles(id),
    asignado_el TIMESTAMP DEFAULT NOW(),
    UNIQUE (usuario_id, rol_id)
);

-- ── PERSONAL ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS empleados (
    id         BIGINT  PRIMARY KEY DEFAULT nextval('empleados_id_seq'),
    usuario_id BIGINT  UNIQUE REFERENCES usuarios(id),
    nombre     TEXT    NOT NULL,
    activo     BOOLEAN DEFAULT TRUE,
    puesto     TEXT    NOT NULL,
    ci         TEXT,
    telefono   TEXT
);

CREATE TABLE IF NOT EXISTS horarios_trabajo (
    id               BIGINT  PRIMARY KEY DEFAULT nextval('horarios_id_seq'),
    empleado_id      BIGINT  REFERENCES empleados(id),
    dia_semana       TEXT    NOT NULL,
    hora_inicio      TIME    NOT NULL,
    hora_fin         TIME    NOT NULL,
    inicio_almuerzo  TIME,
    fin_almuerzo     TIME,
    vigente_desde    DATE    NOT NULL,
    vigente_hasta    DATE,
    capacidad_maxima INTEGER NOT NULL DEFAULT 8,
    UNIQUE (empleado_id, dia_semana, vigente_desde)
);

CREATE TABLE IF NOT EXISTS bloqueos_agenda (
    id           BIGINT    PRIMARY KEY DEFAULT nextval('bloqueos_id_seq'),
    titulo       TEXT      NOT NULL,
    tipo         TEXT,
    fecha_inicio TIMESTAMP NOT NULL,
    fecha_fin    TIMESTAMP NOT NULL,
    empleado_id  BIGINT    REFERENCES empleados(id),
    descripcion  TEXT,
    creado_por   BIGINT    REFERENCES usuarios(id)
);

-- ── CLIENTES ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS clientes (
    id         BIGINT PRIMARY KEY DEFAULT nextval('clientes_id_seq'),
    usuario_id BIGINT UNIQUE REFERENCES usuarios(id),
    nombre     TEXT NOT NULL,
    ci         TEXT,
    telefono   TEXT,
    direccion  TEXT
);

CREATE TABLE IF NOT EXISTS cliente_preferencias (
    id         BIGINT PRIMARY KEY DEFAULT nextval('cli_prefs_id_seq'),
    cliente_id BIGINT REFERENCES clientes(id),
    nombre     TEXT NOT NULL,
    valor      TEXT,
    UNIQUE (cliente_id, nombre)
);

-- ── MASCOTAS ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS cat_temperamentos (
    id           BIGINT PRIMARY KEY DEFAULT nextval('cat_temp_id_seq'),
    nombre       TEXT UNIQUE NOT NULL,
    color_alerta TEXT
);

CREATE TABLE IF NOT EXISTS mascotas (
    id                       BIGINT PRIMARY KEY DEFAULT nextval('mascotas_id_seq'),
    nombre                   TEXT   NOT NULL,
    especie                  TEXT,
    raza                     TEXT,
    tamaño                   TEXT,
    fecha_nacimiento         DATE,
    alergias                 TEXT,
    url_foto_mascota         TEXT,
    url_carnet               TEXT,
    temperamento_esperado_id BIGINT REFERENCES cat_temperamentos(id),
    cliente_id               BIGINT REFERENCES clientes(id)
);

CREATE TABLE IF NOT EXISTS restricciones_mascota (
    id          BIGINT PRIMARY KEY DEFAULT nextval('restricciones_id_seq'),
    mascota_id  BIGINT REFERENCES mascotas(id),
    tipo        TEXT,
    descripcion TEXT,
    gravedad    TEXT
);

-- ── AGENDA ───────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS servicios (
    id               BIGINT   PRIMARY KEY DEFAULT nextval('servicios_id_seq'),
    nombre           TEXT     NOT NULL,
    descripcion      TEXT,
    duracion_minutos INTEGER  NOT NULL,
    precio_base      NUMERIC,
    activo           BOOLEAN  DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS servicios_precios (
    id             BIGINT  PRIMARY KEY DEFAULT nextval('servicios_precios_id_seq'),
    servicio_id    BIGINT  NOT NULL REFERENCES servicios(id),
    tamaño_mascota TEXT    NOT NULL,
    precio         NUMERIC NOT NULL,
    UNIQUE (servicio_id, tamaño_mascota)
);

CREATE TABLE IF NOT EXISTS citas (
    id                BIGINT    PRIMARY KEY DEFAULT nextval('citas_id_seq'),
    cliente_id        BIGINT    REFERENCES clientes(id),
    mascota_id        BIGINT    REFERENCES mascotas(id),
    empleado_id       BIGINT    REFERENCES empleados(id),
    fecha_hora_inicio TIMESTAMP NOT NULL,
    fecha_hora_fin    TIMESTAMP NOT NULL,
    estado            TEXT      DEFAULT 'solicitada'
);

CREATE TABLE IF NOT EXISTS cita_detalles (
    id           BIGINT  PRIMARY KEY DEFAULT nextval('cita_detalles_id_seq'),
    cita_id      BIGINT  REFERENCES citas(id),
    servicio_id  BIGINT  REFERENCES servicios(id),
    completado   BOOLEAN DEFAULT FALSE,
    precio_final NUMERIC
);

-- ── GROOMING ─────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS fichas_ingreso (
    id                      BIGINT PRIMARY KEY DEFAULT nextval('fichas_id_seq'),
    cita_id                 BIGINT REFERENCES citas(id),
    temperamento_id         BIGINT REFERENCES cat_temperamentos(id),
    observaciones_generales TEXT
);

CREATE TABLE IF NOT EXISTS ficha_detalles (
    id                 BIGINT PRIMARY KEY DEFAULT nextval('ficha_detalles_id_seq'),
    ficha_id           BIGINT REFERENCES fichas_ingreso(id),
    descripcion_estado TEXT
);

CREATE TABLE IF NOT EXISTS galeria_mascotas (
    id          BIGINT    PRIMARY KEY DEFAULT nextval('galeria_id_seq'),
    cita_id     BIGINT    REFERENCES citas(id),
    url_image   TEXT      NOT NULL,
    momento     TEXT,
    fecha_carga TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS recomendaciones_post_servicio (
    id                    BIGINT PRIMARY KEY DEFAULT nextval('recomendaciones_id_seq'),
    cita_id               BIGINT REFERENCES citas(id),
    recomendacion         TEXT,
    proxima_cita_sugerida DATE
);

CREATE TABLE IF NOT EXISTS calificaciones_cita (
    id         BIGINT    PRIMARY KEY DEFAULT nextval('calificaciones_id_seq'),
    cita_id    BIGINT    REFERENCES citas(id),
    puntuacion INTEGER,
    comentario TEXT,
    fecha      TIMESTAMP DEFAULT NOW()
);

-- ── INVENTARIO / CATÁLOGO ────────────────────────────────────

CREATE TABLE IF NOT EXISTS categoria (
    id            BIGINT PRIMARY KEY DEFAULT nextval('categoria_id_seq'),
    nom_categoria TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS productos (
    id                BIGINT  PRIMARY KEY DEFAULT nextval('productos_id_seq'),
    nombre            TEXT    NOT NULL,
    sku               TEXT    UNIQUE,
    stock_actual      INTEGER DEFAULT 0,
    precio_venta      NUMERIC,
    fecha_vencimiento DATE,
    lote              TEXT
);

CREATE TABLE IF NOT EXISTS es_de_catego (
    id           BIGINT PRIMARY KEY DEFAULT nextval('es_de_catego_id_seq'),
    producto_id  BIGINT NOT NULL REFERENCES productos(id),
    categoria_id BIGINT NOT NULL REFERENCES categoria(id)
);

CREATE TABLE IF NOT EXISTS movimientos_inventario (
    id                 BIGINT    PRIMARY KEY DEFAULT nextval('movimientos_inv_id_seq'),
    empleado_id        BIGINT    REFERENCES empleados(id),
    tipo_movimiento_id TEXT,
    cantidad           INTEGER   NOT NULL,
    producto_id        BIGINT    NOT NULL REFERENCES productos(id),
    notas              TEXT,
    fecha              TIMESTAMP DEFAULT NOW()
);

-- ── VENTAS ───────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS metodos_pago (
    id     BIGINT PRIMARY KEY DEFAULT nextval('metodos_pago_id_seq'),
    nombre TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS ventas (
    id             BIGINT    PRIMARY KEY DEFAULT nextval('ventas_id_seq'),
    cliente_id     BIGINT    REFERENCES clientes(id),
    vendedor_id    BIGINT    REFERENCES empleados(id),
    metodo_pago_id BIGINT    REFERENCES metodos_pago(id),
    fecha_venta    TIMESTAMP DEFAULT NOW(),
    subtotal       NUMERIC,
    descuento      NUMERIC   DEFAULT 0,
    total_final    NUMERIC   NOT NULL
);

CREATE TABLE IF NOT EXISTS venta_items (
    id                        BIGINT  PRIMARY KEY DEFAULT nextval('venta_items_id_seq'),
    venta_id                  BIGINT  REFERENCES ventas(id),
    producto_id               BIGINT  REFERENCES productos(id),
    cantidad                  INTEGER NOT NULL,
    precio_unitario_historico NUMERIC
);

CREATE TABLE IF NOT EXISTS pedido (
    id           BIGINT PRIMARY KEY DEFAULT nextval('pedido_id_seq'),
    venta_items  BIGINT REFERENCES venta_items(id),
    estado       TEXT,
    "fechaEnvio"   DATE,
    "fechaEntrega" DATE
);

CREATE TABLE IF NOT EXISTS venta_servicios (
    id              BIGINT  PRIMARY KEY DEFAULT nextval('venta_servicios_id_seq'),
    venta_id        BIGINT  REFERENCES ventas(id),
    cita_detalle_id BIGINT  REFERENCES cita_detalles(id),
    precio_cobrado  NUMERIC
);

-- ── NOTIFICACIONES ───────────────────────────────────────────

CREATE TABLE IF NOT EXISTS notificaciones_programadas (
    id                 BIGINT    PRIMARY KEY DEFAULT nextval('notificaciones_id_seq'),
    tipo               TEXT      NOT NULL,
    canal              TEXT      NOT NULL,
    cliente_destino_id BIGINT    REFERENCES clientes(id),
    programado_para    TIMESTAMP NOT NULL,
    estado             TEXT      DEFAULT 'PENDIENTE'
);

-- ── AUDITORÍA ────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS audit_logs (
    id             BIGINT       PRIMARY KEY DEFAULT nextval('audit_logs_id_seq'),
    accion         VARCHAR(50)  NOT NULL,
    correo_usuario VARCHAR(255),
    rol            VARCHAR(20),
    ip             VARCHAR(60),
    user_agent     TEXT,
    detalles       TEXT,
    exitoso        BOOLEAN      NOT NULL DEFAULT TRUE,
    timestamp      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_correo    ON audit_logs (correo_usuario);
CREATE INDEX IF NOT EXISTS idx_audit_accion    ON audit_logs (accion);

-- ── DATOS INICIALES ──────────────────────────────────────────

-- Migración horarios
ALTER TABLE IF EXISTS horarios_trabajo
    ADD COLUMN IF NOT EXISTS capacidad_maxima INTEGER NOT NULL DEFAULT 8;

-- Migración mascotas
ALTER TABLE IF EXISTS mascotas ADD COLUMN IF NOT EXISTS especie TEXT;
ALTER TABLE IF EXISTS mascotas ADD COLUMN IF NOT EXISTS fecha_nacimiento DATE;
ALTER TABLE IF EXISTS mascotas ADD COLUMN IF NOT EXISTS alergias TEXT;
ALTER TABLE IF EXISTS mascotas ADD COLUMN IF NOT EXISTS url_carnet TEXT;
ALTER TABLE IF EXISTS mascotas ADD COLUMN IF NOT EXISTS url_foto_mascota TEXT;

INSERT INTO roles (nombre) VALUES
    ('ADMIN'), ('RECEPCION'), ('GROOMER'), ('CLIENTE')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO metodos_pago (nombre) VALUES
    ('Efectivo'), ('Tarjeta'), ('Transferencia'), ('QR')
ON CONFLICT DO NOTHING;

