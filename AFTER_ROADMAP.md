# After Roadmap

Este documento recoge la hoja de ruta para llevar After desde el MVP actual hasta una aplicacion profesional, lanzable y solida como proyecto de portfolio backend/full stack.

La idea no es meter tecnologias por meterlas. La idea es construir un producto con decisiones razonables: seguro, testeado, desplegable, mantenible y preparado para crecer.

## Estado Actual

After ya tiene una base de MVP razonablemente solida:

- API REST con Spring Boot.
- Registro y login.
- JWT.
- Usuarios.
- Eventos.
- Hosts.
- Union y salida de eventos.
- Invitaciones y expulsiones.
- Control basico de capacidad.
- DTOs de entrada y salida.
- Validaciones.
- Unit tests de servicios.
- Integration tests HTTP con MockMvc.
- README profesional en ingles.

Esto ya permite presentar el proyecto como un MVP backend funcional. Aun asi, todavia hay una diferencia importante entre:

- MVP de portfolio.
- MVP beta con usuarios reales.
- Aplicacion oficial lanzada al publico.
- Producto profesional escalable tipo Airbnb.

Este roadmap separa esas etapas.

## Principio General

After deberia crecer como un monolito modular.

No hace falta empezar con microservicios, Kafka o Kubernetes. Eso seria demasiado pronto. Primero hay que conseguir que una unica aplicacion este bien hecha:

- Segura.
- Testeada.
- Observada.
- Desplegada.
- Documentada.
- Mantenible.
- Preparada para cambios.

Cuando el producto tenga usuarios, trafico y necesidades reales, entonces se puede escalar con herramientas mas avanzadas.

## Fase 1: Cerrar El MVP Tecnico

Esta fase es la mas importante. Es lo que separa el MVP actual de una beta backend realmente seria.

### 1. Seguridad

Ahora mismo la autenticacion funciona, pero esta implementada de forma manual en controllers y servicios. Para un proyecto profesional, la seguridad debe centralizarse.

Tareas:

- Implementar Spring Security.
- Crear un filtro JWT centralizado.
- Eliminar la validacion manual repetida en controllers.
- Proteger endpoints mediante reglas de seguridad.
- Anadir `@Valid` al login.
- Eliminar cualquier JWT secret por defecto en produccion.
- Configurar CORS correctamente.
- Anadir rate limiting para login, registro y acciones sensibles.
- Anadir refresh tokens.
- Anadir logout.
- Anadir revocacion de sesiones o tokens.
- Anadir recuperacion de contrasena.
- Anadir cambio de contrasena.
- Anadir verificacion de email.

Objetivo:

Que la seguridad no dependa de que cada controller recuerde validar el token manualmente.

### 2. Privacidad De Datos

Ahora el proyecto usa DTOs, pero aun hay que separar mejor que informacion ve cada tipo de usuario.

No todos los datos de un usuario deben ser publicos.

Tareas:

- Crear `PublicUserDTO`.
- Crear `PrivateUserDTO`.
- Crear DTOs especificos para host/admin cuando sea necesario.
- Evitar exponer email y telefono a cualquier usuario autenticado.
- Definir que datos ve:
  - Un visitante autenticado.
  - El propio usuario.
  - El host de un evento.
  - Un administrador.
- Ocultar la direccion exacta del evento hasta que el usuario este autorizado.

Objetivo:

Que la API no filtre informacion personal innecesaria.

### 3. Errores Uniformes

Ahora los errores funcionan, pero cada controller gestiona excepciones a mano. Eso genera respuestas diferentes y hace el codigo repetitivo.

Tareas:

- Crear un `GlobalExceptionHandler` con `@ControllerAdvice`.
- Crear un formato comun de error.
- Evitar devolver mensajes internos de excepciones.
- Usar siempre JSON para errores.
- Estandarizar codigos HTTP:
  - `400 Bad Request`
  - `401 Unauthorized`
  - `403 Forbidden`
  - `404 Not Found`
  - `409 Conflict`
  - `500 Internal Server Error`
- Anadir errores de validacion legibles.

Ejemplo de respuesta deseada:

```json
{
  "timestamp": "2026-06-22T16:30:00",
  "status": 400,
  "error": "BAD_REQUEST",
  "message": "Event capacity must be greater than 0",
  "path": "/api/v1/events"
}
```

Objetivo:

Que todos los errores de la API sean predecibles y profesionales.

### 4. Concurrencia En Plazas De Eventos

Este es uno de los riesgos mas importantes. Si dos usuarios intentan unirse al mismo evento al mismo tiempo, ambos podrian ver que queda una plaza y entrar.

Tareas:

- Anadir `@Transactional` donde corresponda.
- Evaluar bloqueo optimista con `@Version`.
- Evaluar bloqueo pesimista en consultas criticas.
- Crear tests de concurrencia.
- Garantizar que nunca se supera la capacidad real del evento.
- Garantizar que un usuario no entra dos veces aunque haya peticiones simultaneas.

Objetivo:

Que las plazas sean consistentes incluso con trafico real.

### 5. Modelo De Participacion

Ahora los asistentes estan modelados con una relacion `ManyToMany`. Para un MVP simple esta bien, pero para una app real se queda corto.

Lo profesional seria crear una entidad intermedia.

Nueva entidad propuesta:

```text
EventParticipant
```

Campos posibles:

- `id`
- `event`
- `user`
- `status`
- `createdAt`
- `updatedAt`
- `invitedBy`
- `cancelledAt`
- `kickedAt`

Estados posibles:

- `INVITED`
- `REQUESTED`
- `ACCEPTED`
- `REJECTED`
- `LEFT`
- `KICKED`

Tareas:

- Sustituir `ManyToMany` por entidad de participacion.
- Definir reglas de transicion entre estados.
- Evitar duplicados con una restriccion unica `(event_id, user_id)`.
- Adaptar join, leave, invite y kick al nuevo modelo.
- Crear tests para todos los estados.

Objetivo:

Preparar el dominio para flujos reales de asistencia.

### 6. Estados De Evento

Ahora un evento existe o no existe. Una aplicacion real necesita estados.

Estados posibles:

- `DRAFT`
- `PUBLISHED`
- `FULL`
- `CANCELLED`
- `FINISHED`

Tareas:

- Anadir estado al evento.
- Impedir unirse a eventos cancelados o finalizados.
- Impedir editar eventos finalizados.
- Permitir al host cancelar eventos.
- Definir que ocurre con los asistentes si se cancela un evento.
- Definir si un evento se marca como `FULL` automaticamente.

Objetivo:

Que el ciclo de vida del evento sea explicito.

### 7. Edicion Y Cancelacion De Eventos

Ahora el host puede crear y eliminar, pero falta gestionar el evento durante su vida.

Tareas:

- Endpoint para actualizar evento.
- Endpoint para cancelar evento.
- Permisos: solo host puede editar o cancelar.
- Validaciones:
  - No poner fecha en pasado.
  - No reducir capacidad por debajo de asistentes actuales.
  - No cambiar datos criticos si el evento ya esta muy cerca.
- Tests de permisos y validaciones.

Objetivo:

Que el host tenga control real sobre su evento sin romper reglas de negocio.

### 8. Paginacion Y Filtros

Los listados no deben devolver todo.

Tareas:

- Anadir `Pageable` a `GET /events`.
- Anadir paginacion a usuarios si se crea busqueda.
- Anadir ordenacion:
  - fecha ascendente.
  - fecha descendente.
  - recientes.
  - capacidad disponible.
- Mejorar filtros:
  - tipo.
  - estilo.
  - ciudad.
  - fecha.
  - rango de fechas.
  - capacidad disponible.

Objetivo:

Que la API pueda crecer sin cargar todos los datos en memoria.

### 9. Configuracion Y Perfiles

Ahora existe configuracion local, pero para un proyecto profesional hay que separar ambientes.

Tareas:

- Versionar una configuracion base segura.
- Crear perfiles:
  - `dev`
  - `test`
  - `prod`
- No versionar secretos reales.
- Documentar variables necesarias.
- Usar variables de entorno para produccion.
- Desactivar `spring.jpa.hibernate.ddl-auto=update` en produccion.
- Desactivar SQL visible en produccion.
- Desactivar `open-in-view`.
- Revisar `.gitignore` para no ignorar configuracion necesaria de ejemplo.

Objetivo:

Que cualquier persona pueda clonar el proyecto y arrancarlo sin adivinar configuracion.

### 10. Migraciones De Base De Datos

`ddl-auto=update` no es profesional para produccion.

Tareas:

- Anadir Flyway o Liquibase.
- Crear migracion inicial.
- Versionar cambios de esquema.
- Probar migraciones en tests.
- Documentar como se aplican.

Objetivo:

Que los cambios de base de datos sean controlados y reproducibles.

## Fase 2: Calidad Profesional Del Codigo

Esta fase convierte el proyecto en algo que se puede mantener.

### 1. Inyeccion Por Constructor

Actualmente hay `@Autowired` en campos. Funciona, pero no es la forma mas profesional.

Tareas:

- Sustituir inyeccion por campos por constructor injection.
- Usar `final` en dependencias.
- Mejorar testabilidad.

Objetivo:

Dependencias explicitas y clases mas limpias.

### 2. Aplicar SOLID De Forma Practica

SOLID no significa crear interfaces para todo. Significa separar responsabilidades cuando hay una razon real.

Aplicaciones concretas:

- Controllers solo coordinan HTTP.
- Services contienen casos de uso.
- Mappers transforman entidades y DTOs.
- Repositories solo acceden a datos.
- Seguridad no vive mezclada con logica de negocio.
- Validaciones de dominio se centralizan donde corresponde.

Tareas:

- Extraer mappers:
  - `UserMapper`
  - `EventMapper`
  - `ParticipantMapper`
- Separar casos de uso grandes.
- Evitar services enormes.
- Eliminar imports y comentarios muertos.
- Revisar nombres de clases y metodos.

Objetivo:

Que el codigo se pueda explicar facilmente en una entrevista.

### 3. Monolito Modular

Ahora el proyecto esta organizado por capas tecnicas:

```text
controller/
service/
repositories/
entity/
dto/
```

Esto esta bien para empezar. Pero cuando crezca, conviene organizar por modulos de negocio.

Estructura posible:

```text
auth/
users/
events/
participation/
notifications/
moderation/
shared/
```

Objetivo:

Que cada parte del dominio tenga sus propias clases y reglas.

### 4. Clean Architecture Con Moderacion

No hace falta aplicar Clean Architecture extrema, pero si conviene aprender sus ideas.

Ideas utiles:

- Separar dominio de infraestructura.
- No depender de Spring en cada decision de negocio.
- Tener casos de uso claros.
- Evitar que controllers sepan demasiado.
- Evitar que entidades JPA sean todo el modelo mental del dominio.

Aplicacion razonable para After:

- Mantener Spring Boot.
- Mantener JPA.
- Introducir casos de uso cuando el dominio crezca.
- Separar DTOs, mappers y entidades.
- Evitar sobrearquitectura.

Objetivo:

Tener arquitectura limpia sin convertir el proyecto en algo innecesariamente complejo.

## Fase 3: Tests Profesionales

Ya hay tests. Ahora hay que cubrir los riesgos que suelen romper productos reales.

### 1. Testcontainers

Los integration tests actuales usan HSQLDB. Eso esta bien para velocidad, pero produccion usa MySQL.

Tareas:

- Anadir Testcontainers.
- Levantar MySQL real en tests.
- Ejecutar migraciones en tests.
- Verificar queries y constraints reales.

Objetivo:

Que los tests se parezcan mas al entorno de produccion.

### 2. Tests De Concurrencia

Tareas:

- Simular varios usuarios uniendose al mismo evento.
- Verificar que no se supera capacidad.
- Verificar que no hay duplicados.
- Verificar consistencia con transacciones.

Objetivo:

Probar el problema mas delicado del dominio.

### 3. Tests De Seguridad

Tareas:

- Token expirado.
- Token malformado.
- Token ausente.
- Usuario intentando modificar datos de otro.
- Usuario intentando invitar sin ser host.
- Usuario intentando ver datos privados.
- Login con body invalido.
- Rate limiting si se implementa.

Objetivo:

Que la seguridad se pruebe como una parte central del producto.

### 4. Tests De Contrato API

Tareas:

- Verificar estructura exacta de respuestas.
- Verificar estructura exacta de errores.
- Mantener Swagger/OpenAPI alineado con la realidad.

Objetivo:

Que frontend y backend puedan evolucionar sin romperse.

### 5. Tests De Carga

Herramientas posibles:

- k6.
- JMeter.
- Gatling.

Flujos a probar:

- Login.
- Listado de eventos.
- Crear evento.
- Join event.
- Invite/kick.

Objetivo:

Saber donde empieza a sufrir la aplicacion.

### 6. Cobertura Y Calidad

Tareas:

- Anadir JaCoCo.
- Establecer minimo razonable de cobertura.
- Anadir Checkstyle o Spotless.
- Anadir SpotBugs.
- Anadir SonarCloud.

Objetivo:

Que la calidad no dependa solo de revisar a mano.

## Fase 4: Docker, CI/CD Y Pipelines

Esta fase es muy importante para empleo. Muchas ofertas piden Docker, CI/CD y pipelines.

### 1. Docker

Tareas:

- Crear `Dockerfile` multi-stage.
- Crear imagen ligera de la API.
- Configurar variables de entorno.
- No meter secretos en la imagen.
- Verificar arranque dentro del contenedor.

Objetivo:

Que After se pueda ejecutar igual en cualquier maquina.

### 2. Docker Compose

Tareas:

- Crear `docker-compose.yml`.
- Incluir API.
- Incluir MySQL.
- Incluir variables necesarias.
- Incluir health checks.
- Documentar comando de arranque.

Comando ideal:

```bash
docker compose up --build
```

Objetivo:

Que cualquier persona pueda levantar After con un solo comando.

### 3. CI Con GitHub Actions

Pipeline minimo:

- Checkout.
- Setup Java.
- Cache Maven.
- Compilar.
- Ejecutar tests.
- Publicar resultado.

Pipeline mejorado:

- Formato.
- Analisis estatico.
- Tests.
- Cobertura.
- Escaneo de dependencias.
- Build Docker.

Objetivo:

Que cada push y pull request se verifique automaticamente.

### 4. CD

CD significa despliegue automatizado o semi-automatizado.

Tareas:

- Desplegar automaticamente a staging.
- Desplegar a produccion con aprobacion manual.
- Mantener secretos en GitHub Secrets o AWS Secrets Manager.
- Construir y publicar imagen Docker.
- Ejecutar migraciones antes del deploy.

Objetivo:

Que publicar una nueva version no sea manual ni improvisado.

## Fase 5: AWS Y Despliegue Real

Para portfolio profesional, desplegar After en AWS seria muy valioso.

### Arquitectura AWS Inicial

Opcion sencilla:

- AWS App Runner para la API.
- RDS MySQL para base de datos.
- ECR para imagen Docker.
- Secrets Manager para secretos.
- CloudWatch para logs.

Opcion mas flexible:

- ECS Fargate.
- Application Load Balancer.
- RDS MySQL.
- ECR.
- Secrets Manager.
- CloudWatch.
- Route 53.
- ACM para HTTPS.

### Tareas AWS

- Crear entorno de staging.
- Crear base de datos RDS.
- Configurar red y seguridad.
- Configurar variables y secretos.
- Configurar logs.
- Configurar health checks.
- Configurar backups.
- Configurar dominio.
- Configurar HTTPS.

Objetivo:

Que After tenga una URL real y pueda demostrarse como aplicacion desplegada.

## Fase 6: Observabilidad Y Operaciones

Una app profesional no solo funciona: tambien se puede observar.

### 1. Spring Boot Actuator

Tareas:

- Anadir Actuator.
- Exponer health checks.
- Exponer info basica de la app.
- Proteger endpoints sensibles.

### 2. Logs

Tareas:

- Logs estructurados.
- Correlation ID por request.
- No loggear secretos.
- Registrar operaciones sensibles:
  - login fallido.
  - evento creado.
  - usuario invitado.
  - usuario expulsado.
  - evento cancelado.

### 3. Metricas Y Alertas

Tareas:

- Medir latencia.
- Medir errores 4xx y 5xx.
- Medir intentos de login.
- Medir uso de endpoints.
- Crear alertas para:
  - API caida.
  - base de datos caida.
  - muchos errores 500.
  - muchos login fallidos.

### 4. Backups

Tareas:

- Backups automaticos de base de datos.
- Politica de retencion.
- Prueba de restauracion.

Objetivo:

Poder operar la aplicacion sin ir a ciegas.

## Fase 7: Producto Real Lanzable

Hasta aqui se ha hablado sobre todo de backend. Para lanzar oficialmente, falta producto.

### 1. Frontend O App

Tareas:

- Landing o pantalla inicial.
- Registro y login.
- Perfil de usuario.
- Crear evento.
- Editar evento.
- Buscar eventos.
- Ver detalle de evento.
- Unirse a evento.
- Gestionar asistentes como host.
- Cancelar evento.
- Gestionar invitaciones.

### 2. Imagenes

Tareas:

- Foto de perfil.
- Fotos de eventos.
- Almacenamiento en S3.
- Validacion de tipo y tamano.
- Compresion o redimensionado.
- CDN si el trafico crece.

### 3. Notificaciones

Tipos de notificacion:

- Usuario invitado.
- Usuario aceptado.
- Evento cancelado.
- Cambio de datos del evento.
- Recordatorio antes del evento.

Canales:

- Email.
- Notificaciones internas.
- Push notifications si hay app mobile.

### 4. Moderacion

Tareas:

- Reportar usuario.
- Reportar evento.
- Bloquear usuario.
- Panel admin.
- Revisar eventos sospechosos.
- Eliminar contenido.
- Suspender usuarios.

### 5. Confianza Y Seguridad Del Usuario

Tareas:

- Verificacion de email.
- Verificacion de edad si aplica.
- Verificacion de identidad si el modelo lo requiere.
- Reglas claras sobre direccion exacta.
- Politicas anti-spam.
- Prevencion de abuso.

### 6. Legal

Antes de lanzar publicamente, esto es imprescindible.

Tareas:

- Terminos y condiciones.
- Politica de privacidad.
- Politica de cookies si hay frontend.
- Cumplimiento GDPR.
- Derecho de eliminacion de cuenta.
- Exportacion de datos del usuario.
- Retencion de datos.
- Responsabilidad sobre fiestas y espacios privados.
- Edad minima.
- Normativa local sobre eventos.
- Revisar necesidad de seguros o disclaimers.

Objetivo:

No lanzar una app de eventos privados sin marco legal minimo.

## Fase 8: Escalabilidad Avanzada

Esta fase no es necesaria al principio. Solo tiene sentido cuando hay usuarios reales y problemas reales.

### Redis

Usos posibles:

- Rate limiting.
- Cache de lecturas frecuentes.
- Sesiones o tokens revocados.
- Locks distribuidos si se necesita.

### Kafka

Kafka no deberia ser prioritario ahora mismo.

Tendria sentido si hay muchos eventos internos:

- Event created.
- User invited.
- User joined.
- Event cancelled.
- Notification requested.
- Audit event generated.

Antes de Kafka se puede empezar con:

- Eventos de dominio internos.
- Spring Application Events.
- Una cola mas simple.

Kafka seria util para demostrar conocimiento, pero no conviene meterlo si no resuelve un problema real.

### Kubernetes

Kubernetes no es necesario para After al principio.

Tiene sentido cuando:

- Hay multiples servicios.
- Hay trafico alto.
- Hay equipo DevOps.
- Se necesita escalado complejo.
- Se gestionan muchos despliegues.

Para empezar, App Runner o ECS Fargate es mucho mas razonable.

Conceptos que si conviene aprender:

- Pods.
- Deployments.
- Services.
- ConfigMaps.
- Secrets.
- Ingress.
- Health checks.
- Horizontal scaling.

## Fase 9: Gestion Profesional Del Proyecto

Esto ayuda mucho de cara a entrevistas.

### Jira O GitHub Projects

No hay que implementar Jira en el codigo. Hay que usarlo como herramienta de gestion.

Tareas:

- Crear backlog.
- Crear epics.
- Crear historias de usuario.
- Crear tareas tecnicas.
- Crear bugs.
- Definir prioridades.
- Mover tareas por estados:
  - Backlog.
  - Ready.
  - In progress.
  - Review.
  - Done.

### Agile

No se implementa Agile. Se trabaja con metodologia.

Practicas utiles:

- Iteraciones cortas.
- Objetivos por sprint.
- Revisiones frecuentes.
- Retrospectiva personal.
- Pull requests pequenos.
- Definition of Done.

### Documentacion Profesional

Tareas:

- README en ingles.
- Diagramas de arquitectura.
- Diagrama entidad-relacion.
- ADRs para decisiones importantes.
- Changelog.
- Guia de contribucion.
- Politica de seguridad.
- Documentacion de despliegue.
- Documentacion de variables de entorno.

Objetivo:

Que el proyecto parezca mantenido como un producto real, no solo como codigo.

## Orden Recomendado De Ejecucion

Este seria el orden mas razonable:

1. Spring Security con JWT centralizado.
2. DTOs publicos y privados para privacidad.
3. `ControllerAdvice` y errores uniformes.
4. Configuracion por perfiles y variables.
5. Flyway o Liquibase.
6. Restricciones e indices de base de datos.
7. Concurrencia en plazas.
8. Entidad `EventParticipant`.
9. Estados de evento y participacion.
10. Paginacion y filtros.
11. Edicion y cancelacion de eventos.
12. Testcontainers con MySQL.
13. Tests de seguridad y concurrencia.
14. Refactor con constructor injection, mappers y modulos.
15. Dockerfile.
16. Docker Compose.
17. GitHub Actions CI.
18. CD basico.
19. Despliegue en AWS.
20. Actuator, logs y metricas.
21. Backups y monitorizacion.
22. Frontend o app.
23. Notificaciones.
24. Moderacion.
25. Legal y privacidad.
26. Beta privada.
27. Optimizacion de rendimiento.
28. Redis si hace falta.
29. Kafka si hace falta.
30. Kubernetes solo si realmente aparece la necesidad.

## Prioridades Para Buscar Trabajo

Si el objetivo principal es empleabilidad, este orden da mucho valor:

1. Docker y Docker Compose.
2. GitHub Actions CI.
3. Spring Security.
4. Flyway o Liquibase.
5. Testcontainers.
6. AWS con RDS.
7. Actuator, logs y health checks.
8. Refactor modular y SOLID.
9. SonarCloud o analisis de calidad.
10. GitHub Projects o Jira para mostrar metodologia.

Con esto, After podria presentarse como un proyecto muy serio para posiciones junior o trainee backend/full stack.

## Cuando Considerar After Completado

### Portfolio Solido

After seria un portfolio muy solido cuando tenga:

- Seguridad centralizada.
- DTOs publicos y privados.
- Errores uniformes.
- Migraciones.
- Docker.
- CI.
- Tests con MySQL real.
- README y documentacion.
- Despliegue basico.

### Beta Privada

After estaria listo para beta privada cuando tenga:

- Todo lo anterior.
- Participaciones robustas.
- Estados de evento.
- Control de concurrencia.
- Privacidad de direccion.
- Notificaciones basicas.
- Moderacion minima.
- Logs y monitorizacion.
- Backups.

### Lanzamiento Publico

After podria lanzarse publicamente cuando tenga:

- Seguridad revisada.
- Legal revisado.
- Operabilidad minima.
- Moderacion.
- Politicas de privacidad.
- Frontend usable.
- Soporte a usuarios.
- Monitorizacion activa.
- Plan de respuesta a incidentes.

### Producto Escalable

After podria considerarse producto escalable cuando tenga:

- Trafico real.
- Datos reales.
- Observabilidad madura.
- Procesos de despliegue maduros.
- Optimizacion de consultas.
- Cache cuando haga falta.
- Procesamiento asincrono cuando haga falta.
- Infraestructura preparada para crecer.

## Conclusion

El siguiente paso no es anadir muchas features. El siguiente paso es hacer que lo que ya existe sea seguro, consistente y desplegable.

La mejor version de After para presentarte a trabajo no es la que usa mas tecnologias. Es la que demuestra que sabes construir software con criterio:

- Entiendes seguridad.
- Entiendes testing.
- Entiendes despliegue.
- Entiendes datos.
- Entiendes arquitectura.
- Entiendes producto.
- Sabes explicar por que cada decision existe.

Si completas las primeras fases de este roadmap, After dejara de ser solo un MVP y pasara a ser un proyecto backend/full stack realmente profesional.
