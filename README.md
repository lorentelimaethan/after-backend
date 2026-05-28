# After

After es una API REST para gestionar eventos privados en espacios personales: casas, pisos, chalets, fincas u otros lugares donde un anfitrion puede organizar una fiesta y permitir que otros usuarios se apunten.

La idea del MVP es validar el flujo principal de una plataforma tipo "Airbnb de fiestas": un usuario se registra, crea un evento como host, otros usuarios pueden unirse, salir, ser invitados o ser expulsados por el anfitrion.

## Estado del proyecto

MVP backend en desarrollo.

El proyecto actualmente incluye:

- Registro y login de usuarios.
- Autenticacion mediante JWT.
- Gestion de perfiles de usuario.
- Creacion y consulta de eventos.
- Filtros por tipo de evento y estilo musical.
- Union y salida de usuarios en eventos.
- Invitacion y expulsion de usuarios por parte del host.
- Control de capacidad del evento.
- Validaciones con DTOs.
- Respuestas publicas mediante DTOs.
- Documentacion OpenAPI/Swagger.

## Stack tecnico

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- MySQL
- JWT con JJWT
- BCrypt para passwords
- Jakarta Validation
- Lombok
- Springdoc OpenAPI / Swagger UI
- Maven

## Modelo de dominio

El MVP se organiza alrededor de tres conceptos principales:

- `UserAccess`: credenciales de acceso, username, password cifrado y relacion con usuario.
- `Users`: perfil publico del usuario.
- `Events`: evento creado por un host, con datos de fiesta, direccion, capacidad y asistentes.

Un evento tiene:

- Nombre.
- Descripcion.
- Fecha y hora.
- Capacidad.
- Tipo de evento.
- Estilo musical.
- Host.
- Lista de usuarios asistentes.
- Direccion asociada.

## Funcionalidades principales

### Autenticacion

Los usuarios pueden registrarse y hacer login.

Al hacer login, la API devuelve un JWT que debe enviarse en las peticiones protegidas:

```http
Authorization: Bearer <token>
```

### Usuarios

Un usuario autenticado puede:

- Consultar un perfil.
- Actualizar su propio perfil.
- Actualizar su display name.

La API impide que un usuario modifique el perfil de otro.

### Eventos

Un usuario autenticado puede:

- Listar eventos.
- Filtrar eventos por tipo y estilo musical.
- Consultar un evento por id.
- Crear un evento como host.
- Unirse a un evento.
- Salir de un evento.

El host de un evento puede:

- Invitar usuarios.
- Expulsar usuarios.
- Eliminar el evento.

Reglas principales implementadas:

- El host no puede unirse a su propio evento como asistente.
- El host no puede salir de su propio evento.
- El host no puede ser expulsado.
- No se puede superar la capacidad del evento.
- Un usuario no puede unirse dos veces al mismo evento.
- Solo el host puede invitar, expulsar o eliminar el evento.

## DTOs

El proyecto evita exponer entidades directamente en las operaciones principales de la API.

DTOs actuales:

- `RegisterDTO`
- `LoginDTO`
- `UpdateUserDTO`
- `UpdateDisplayNameDTO`
- `CreateEventDTO`
- `AddressDTO`
- `UserResponseDTO`
- `EventResponseDTO`

Esto permite separar el contrato publico de la API del modelo interno de persistencia.

## Endpoints principales

### Auth

| Metodo | Endpoint | Descripcion |
| --- | --- | --- |
| POST | `/token/auth/register` | Registrar usuario |
| POST | `/token/auth/login` | Login y generacion de JWT |

### Users

| Metodo | Endpoint | Descripcion |
| --- | --- | --- |
| GET | `/users/{id}` | Obtener usuario por id |
| PUT | `/users/{id}` | Actualizar perfil propio |
| PATCH | `/users/{id}/display-name` | Actualizar display name |

### Events

| Metodo | Endpoint | Descripcion |
| --- | --- | --- |
| GET | `/events` | Listar eventos |
| GET | `/events?type=AFTER` | Filtrar por tipo |
| GET | `/events?style=TECHNO` | Filtrar por estilo musical |
| GET | `/events?type=AFTER&style=TECHNO` | Filtrar por tipo y estilo |
| GET | `/events/{id}` | Obtener evento por id |
| POST | `/events` | Crear evento |
| PATCH | `/events/{id}/join` | Unirse a evento |
| PATCH | `/events/{id}/leave` | Salir de evento |
| PATCH | `/events/{eventId}/invite/user/{userId}` | Invitar usuario |
| DELETE | `/events/{eventId}/kick/user/{userId}` | Expulsar usuario |
| DELETE | `/events/{id}` | Eliminar evento |

## Valores permitidos

### EventType

```text
AFTER
PREVIA
HOUSE_PARTY
CHILL
CLUB
POOL_PARTY
```

### MusicStyle

```text
REGGAETON
TECHNO
TEKNO
HOUSE
POP
MIXED
```

## Ejemplos de uso

### Registro

```http
POST /token/auth/register
Content-Type: application/json
```

```json
{
  "username": "ethanlo",
  "password": "password123"
}
```

### Login

```http
POST /token/auth/login
Content-Type: application/json
```

```json
{
  "username": "ethanlo",
  "password": "password123"
}
```

Respuesta:

```text
eyJhbGciOiJIUzI1NiJ9...
```

### Crear evento

```http
POST /events
Authorization: Bearer <token>
Content-Type: application/json
```

```json
{
  "name": "Techno Underground Barcelona",
  "eventType": "AFTER",
  "musicStyle": "TECHNO",
  "description": "Underground techno party in Barcelona",
  "dateTime": "2026-08-15T23:00:00",
  "capacity": 150,
  "address": {
    "street": "Carrer Marina",
    "streetNum": "25",
    "postalCode": "08005",
    "additionalInfo": "Industrial warehouse near the beach",
    "city": "Barcelona",
    "province": "Catalonia"
  }
}
```

Respuesta:

```json
{
  "id": 6,
  "name": "Techno Underground Barcelona",
  "description": "Underground techno party in Barcelona",
  "dateTime": "2026-08-15T23:00:00",
  "capacity": 150,
  "eventType": "AFTER",
  "musicStyle": "TECHNO",
  "hostDisplayName": "ethanlo",
  "usersCount": 0
}
```

## Instalacion y ejecucion

### Requisitos

- Java 21
- Maven Wrapper incluido en el proyecto
- MySQL en local
- Base de datos `after`

### Crear base de datos

```sql
CREATE DATABASE after;
```

### Configuracion

La configuracion principal esta en:

```text
src/main/resources/application.properties
```

Valores actuales por defecto:

```properties
server.port=8081
spring.datasource.url=jdbc:mysql://localhost:3306/after
spring.datasource.username=root
spring.datasource.password=
spring.jpa.hibernate.ddl-auto=update
```

El secreto JWT se lee desde la variable de entorno `JWT_SECRET`.

Si no existe, se usa un valor de desarrollo en Base64:

```properties
jwt.secret=${JWT_SECRET:ZGV2LXNlY3JldC1kZXYtc2VjcmV0LWRldi1zZWNyZXQ=}
```

Para un entorno real, se debe definir un secreto propio:

```bash
export JWT_SECRET=<base64-secret>
```

### Ejecutar la aplicacion

```bash
./mvnw spring-boot:run
```

La API queda disponible en:

```text
http://localhost:8081
```

### Ejecutar tests

```bash
./mvnw test
```

Actualmente existe una prueba basica de arranque de contexto. La siguiente fase del proyecto es ampliar la cobertura con tests de flujo reales.

## Swagger

Con la aplicacion arrancada, la documentacion Swagger UI esta disponible en:

```text
http://localhost:8081/swagger-ui/index.html
```

## Arquitectura actual

El proyecto sigue una estructura clasica de Spring Boot:

```text
controller/
service/
repositories/
entity/
dto/
exceptions/
enums/
utils/
```

Responsabilidades:

- `controller`: expone endpoints REST y valida autorizacion basica por token.
- `service`: contiene la logica de negocio.
- `repositories`: acceso a base de datos mediante Spring Data JPA.
- `entity`: modelo persistido.
- `dto`: contratos de entrada y salida de la API.
- `utils`: utilidades como generacion y validacion de JWT.
- `exceptions`: excepciones de dominio/API.

## Decisiones tecnicas

- Passwords cifrados con BCrypt.
- JWT con expiracion de una hora.
- DTOs para separar API publica de entidades JPA.
- Enums guardados como texto para evitar problemas si cambia el orden.
- Validaciones en DTOs para controlar datos de entrada.
- El host se guarda separado de la lista de asistentes.

## Limitaciones actuales

Este proyecto es un MVP. Algunas partes estan preparadas para evolucionar:

- La seguridad todavia no usa Spring Security completo con filtros JWT.
- Los errores no estan centralizados con un `ControllerAdvice`.
- Los tests de flujo aun estan pendientes.
- No hay frontend conectado todavia.
- No hay roles avanzados ni estados de evento.
- La visibilidad de asistentes/direccion puede evolucionar segun reglas de privacidad.

## Roadmap cercano

- Tests de flujo con registro, login, creacion de evento y operaciones de asistentes.
- Refactor de seguridad hacia Spring Security.
- Manejo global de errores.
- Mejoras de documentacion Swagger.
- Posible frontend web o mobile para consumir la API.
- Estados de evento: abierto, lleno, cancelado, finalizado.
- Politicas de privacidad para direccion y asistentes.

## Autor

Proyecto desarrollado como MVP de aprendizaje y portfolio backend/full stack.

