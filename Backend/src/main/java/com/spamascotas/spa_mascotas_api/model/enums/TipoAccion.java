package com.spamascotas.spa_mascotas_api.model.enums;

public enum TipoAccion {
    // Autenticación
    LOGIN_EXITOSO           ("Inicio de sesión exitoso"),
    LOGIN_FALLIDO           ("Inicio de sesión fallido"),
    CUENTA_BLOQUEADA        ("Cuenta bloqueada"),
    REGISTRO                ("Registro de cuenta"),
    ACTIVACION_CUENTA       ("Activación de cuenta"),
    LOGOUT                  ("Cierre de sesión"),
    CAMBIO_PASSWORD         ("Cambio de contraseña"),
    RESETEO_PASSWORD        ("Restablecimiento de contraseña"),
    // Personal / Admin
    CREAR_STAFF             ("Creación de personal"),
    MODIFICAR_USUARIO       ("Modificación de usuario"),
    ACTIVAR_USUARIO         ("Activación de usuario"),
    DESACTIVAR_USUARIO      ("Desactivación de usuario"),
    ACCESO_MODULO           ("Acceso a módulo"),
    // Inventario
    MOVIMIENTO_INVENTARIO   ("Movimiento de inventario"),
    // Ventas
    VENTA_REALIZADA         ("Venta realizada"),
    VENTA_SERVICIO          ("Venta de servicio");

    private final String label;

    TipoAccion(String label) { this.label = label; }

    public String getLabel() { return label; }
}
