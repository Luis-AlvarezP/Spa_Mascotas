# SpaMascotas 🐾

Sistema de gestión web/móvil (PWA) para un spa de grooming de mascotas. Permite administrar citas, fichas de grooming, inventario, ventas, clientes y mascotas con distintos roles de usuario.

## Stack Tecnológico

| Capa       | Tecnología                        |
|------------|-----------------------------------|
| Backend    | Spring Boot 4.0.6 · Java 17       |
| Frontend   | Angular 21 · TypeScript (strict)  |
| Base datos | PostgreSQL                        |
| Seguridad  | JWT · BCrypt · 2FA TOTP · OAuth2  |

## Estado de Módulos

| Módulo                   | Estado         |
|--------------------------|----------------|
| Autenticación y seguridad | ✅ Completo   |
| Administración y auditoría | ✅ Completo  |
| Perfil de usuario        | ✅ Completo    |
| Agenda / Citas           | 🚧 Placeholder |
| Grooming                 | 🚧 Placeholder |
| Clientes y Mascotas      | 🚧 Parcial     |
| Inventario y Ventas      | 🚧 Placeholder |
| Notificaciones           | 📋 Planificado |

## Equipo de Desarrollo

- Luis Gael Alvarez Portugal

---

## Instalación y Ejecución

### 1. Clonar el repositorio

```bash
git clone https://github.com/Luis-AlvarezP/Spa_Mascotas.git
cd Spa_Mascotas
```

### 2. Configurar el Backend

El archivo `application.properties` contiene credenciales sensibles y **no está incluido en el repositorio**.
Debes crearlo a partir de la plantilla:

```bash
cd Backend/src/main/resources
cp application.properties.template application.properties
```

Luego abre `application.properties` y reemplaza los siguientes valores con los tuyos:

| Propiedad | Descripción |
|-----------|-------------|
| `spring.datasource.password` | Contraseña de PostgreSQL |
| `jwt.secret` | Clave base64 segura (mín. 32 bytes) |
| `spring.security.oauth2.client.registration.google.client-id` | Client ID de Google Cloud Console |
| `spring.security.oauth2.client.registration.google.client-secret` | Client Secret de Google Cloud Console |
| `spring.mail.username` | Tu correo Gmail |
| `spring.mail.password` | App Password de Gmail (no tu contraseña real) |

> **Cómo generar una App Password de Gmail:** Google Account → Seguridad → Verificación en dos pasos → Contraseñas de aplicaciones

### 3. Preparar la base de datos

Ejecuta el schema SQL en PostgreSQL (base de datos: `spa_mascotas`):

```bash
psql -U postgres -d spa_mascotas -f Backend/src/main/resources/db/schema.sql
```

### 4. Ejecutar el Backend

```bash
cd Backend
./mvnw spring-boot:run
# API disponible en http://localhost:8080
```

### 5. Ejecutar el Frontend

```bash
cd Frontend
npm install
ng serve
# App disponible en http://localhost:4200
```

---

## Roles del Sistema

| Rol        | Descripción |
|------------|-------------|
| ADMIN      | Acceso total; único en el sistema |
| RECEPCION  | Agenda, clientes, inventario |
| GROOMER    | Fichas de grooming, agenda propia |
| CLIENTE    | Mis citas, mis mascotas, catálogo |

El primer ADMIN se carga automáticamente al iniciar la aplicación (ver `DataInitializer`).
