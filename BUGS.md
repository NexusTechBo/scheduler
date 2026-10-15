# Reporte de QA exploratorio — Scheduler (ClinicFlow)

**Fecha:** 2026-10-12  
**Entorno:** frontend Angular 19 en `http://localhost:4200` (proxy a `/api` y `/ws`), backend Spring Boot en `http://localhost:8080` (levantado, PostgreSQL con los datos semilla de `DataSeeder`).  
**Cobertura:** roles ADMINISTRATOR, DOCTOR, ASSISTANT y PATIENT (clínica 1 "Downtown"); ADMINISTRATOR y PATIENT de la clínica 2 "Uptown" para las pruebas entre tenants. Viewports de 1440×900 y 375×812. Navegador headless (Edge vía `playwright-core`) y `curl` contra la API.  
**Flujos probados:** selección de clínica, login, registro de clínica, dashboard, tablero (incluye WebSocket), calendario, listado de citas, detalle, confirmar y cancelar, reserva (wizard), pacientes, personal, especialidades, disponibilidad y bloqueos, perfil, asistente IA.

Severidad: **crítica** (caída del servicio, fuga de datos entre tenants o corrupción), **alta** (falla de seguridad o de regla de negocio central), **media** (resultado incorrecto, validación faltante o error mal manejado), **baja** (pulido, textos, consistencia).

---

## Backend

### B1. Bucle infinito al generar slots: una sola disponibilidad mal formada tumba el backend
- **Severidad:** crítica
- **Endpoint:** `POST /api/doctorAvailability` y luego `GET /api/doctorAvailability/doctor/{id}/availables` (también lo usan el wizard de reserva y la vista previa de disponibilidad)
- **Pasos:**
  1. Como DOCTOR, `POST /api/doctorAvailability` con `{"dayOfWeek":"MONDAY","startTime":"09:00","endTime":"12:00","slotDurationMinutes":0}` (o un valor negativo). El controlador no tiene `@Valid`, así que `@Min(5)` no se aplica.
  2. Otra variante sin valores inválidos: un bloque que termina cerca de la medianoche, por ejemplo `startTime 23:00`, `endTime 23:59`, `slotDurationMinutes 30`.
  3. Cualquier usuario consulta los slots de ese doctor para un lunes.
- **Esperado:** 400 en el paso 1, y la generación de slots siempre termina.
- **Obtenido (según el código):** `createGeneratedSlots` hace `while (current.plusMinutes(duration) <= end)`. Con `duration = 0` nunca avanza. Con el bloque de 23:00, `LocalTime.plusMinutes` da la vuelta a `00:00`, que sigue siendo `< end`. En ambos casos el bucle es infinito y llena un `ArrayList` hasta el OutOfMemory, con el hilo del request bloqueado. **No lo ejecuté a propósito** para no tumbar el entorno compartido. Sí verifiqué que el endpoint no valida: un body sin `dayOfWeek` devuelve `409 "The operation conflicts with existing data"` en lugar de 400.
- **Evidencia:** `DoctorAvailabilityController.java:29` (`@RequestBody` sin `@Valid`), `DoctorAvailabilityServiceImpl.java:117` (bucle `while`).

### B2. La cuenta global se comparte entre clínicas: una clínica puede modificar los datos de una persona en otra clínica
- **Severidad:** crítica
- **Endpoints:** `POST /api/patients`, `POST /api/personal`, `POST /api/clinics`, `PUT /api/patients/update/{id}`, `PUT /api/personal/update/...`
- **Pasos:**
  1. Como ASSISTANT de Downtown: `POST /api/patients` con `{"name":"Otro Nombre","email":"otro@test.com","ci":"123123113","password":"zzzzzzzz1"}`. El CI pertenece a James Wilson, paciente de Uptown.
  2. Respuesta `200 {"id":4,"name":"James Wilson","email":"james.wilson@email.com",...}`: se ignoraron el nombre, el email y la contraseña enviados, y se vinculó la cuenta de otra clínica.
  3. `PUT /api/patients/update/4` con `name:"James Wilson (editado por Downtown)"`.
  4. James inicia sesión en Uptown: `GET /api/auth/me` devuelve `"name":"James Wilson (editado por Downtown)"`.
- **Esperado:** cada clínica no puede alterar los datos que otra clínica ve. Si el CI ya existe, se debe pedir confirmación o rechazar, y nunca descartar en silencio los datos enviados.
- **Obtenido:** cualquier clínica que conozca el CI de una persona puede cambiar su nombre, email o teléfono (y con el email, su usuario de login) en todas las clínicas. Ver también B6.
- **Evidencia:** `PatientServiceImpl.java:47`, `PersonalServiceImpl.java:58` y `ClinicServiceImpl.java:50` (`accountRepository.findByCi(...)`, que reutiliza la cuenta en `public.accounts`). Restauré el nombre original después de la prueba.

### B3. Registrar una clínica con un CI existente convierte a otra persona en administradora, y el registrante no puede entrar
- **Severidad:** alta
- **Endpoint:** `POST /api/clinics` (público)
- **Pasos:** `POST /api/clinics` con `{"name":"QA Clinica CI","phoneNumber":"77777777","adminName":"Hacker","adminEmail":"hacker@test.com","adminPassword":"hackerpass1","adminCi":"123123122"}`. El CI es de la Dra. Ana García.
- **Esperado:** 409, o al menos crear la cuenta con los datos enviados.
- **Obtenido:** `201 {"id":4,...,"adminPersonalId":1,"adminEmail":"ana.garcia@clinic.com"}`. Ana queda como administradora de la clínica nueva y la respuesta pública **filtra su email**. Las credenciales enviadas se descartan y el registrante recibe 401 al iniciar sesión (ver F4).
- **Evidencia:** `ClinicServiceImpl.java:50-61`.

### B4. `GET /api/account/{ci}` expone datos personales de cualquier clínica
- **Severidad:** alta
- **Endpoint:** `GET /api/account/{ci}`
- **Pasos:** con el token de un PACIENTE de Uptown, `GET /api/account/123123121`.
- **Esperado:** 403 (o un endpoint limitado al tenant y al rol), y 404 si no existe.
- **Obtenido:** `200 {"id":1,"ci":"123123121","name":"Downtown Admin","email":"admin.downtown@clinic.com","phoneNumber":null}`. Cualquier usuario autenticado de cualquier clínica y rol puede enumerar cuentas por CI. Con un CI inexistente responde `200` con body vacío.
- **Evidencia:** `AccountController.java:23` (sin `@PreAuthorize`), `AccountServiceImpl.java:20` (`orElse(null)`).

### B5. IDOR: un paciente puede ver los doctores asignados a cualquier otro paciente
- **Severidad:** alta
- **Endpoint:** `GET /api/patients/{patientId}/doctors`
- **Pasos:** con el token de John (paciente id 1), `GET /api/patients/2/doctors`.
- **Esperado:** 403 cuando `patientId` no es el del usuario autenticado.
- **Obtenido:** `200 [{"id":2,"name":"Dr. Ana García",...}]`, que son los doctores de María López.
- **Evidencia:** `PatientController.java:74-77` (permite PATIENT y no compara con `auth.getName()`).

### B6. Una cuenta que es paciente y personal en la misma clínica siempre entra como personal, aunque ese personal esté desactivado
- **Severidad:** alta
- **Endpoint:** `POST /api/auth/login`
- **Pasos:**
  1. Como ADMIN: `POST /api/personal` con `ci:"123123126"` (CI de John Smith, paciente) y `roleId:3`. Responde 200 y crea al personal id 8 reutilizando la cuenta de John.
  2. John inicia sesión: `GET /api/auth/me` devuelve `"role":"ASSISTANT"`.
  3. `DELETE /api/personal/8` (desactivar) y John vuelve a iniciar sesión: sigue entrando como `ASSISTANT`.
- **Esperado:** rechazar que un paciente sea personal de la misma clínica (o permitir elegir el rol), e ignorar los perfiles inactivos.
- **Obtenido:** `AuthServiceImpl` da prioridad a `personal` sobre `patient` y no mira `active`. John perdió el acceso como paciente y ganó permisos de asistente.
- **Evidencia:** `AuthServiceImpl.java:34-41`. **Ver la nota de datos al final:** este estado quedó en la base de datos.

### B7. Los usuarios desactivados siguen pudiendo iniciar sesión y usar la API
- **Severidad:** alta
- **Endpoint:** `POST /api/auth/login` y todos los demás
- **Pasos:** `DELETE /api/patients/3` (204) e iniciar sesión con `qa.pac1@test.com`: devuelve token 200. Lo mismo con el doctor desactivado `qa.doc2@test.com`: `GET /api/auth/me` responde 200 con rol DOCTOR.
- **Esperado:** 401/403 para cuentas inactivas, y que los tokens ya emitidos dejen de funcionar.
- **Obtenido:** desactivar no tiene ningún efecto en el acceso.
- **Evidencia:** `AuthServiceImpl.java:29-48` (no comprueba `active`) y `JwtAuthFilter.java` (no hay revocación).

### B8. La reserva ignora los bloqueos parciales de agenda
- **Severidad:** alta
- **Endpoint:** `POST /api/appointments`
- **Pasos:**
  1. Como DOCTOR Ana: `POST /api/scheduleException` con `{"date":"2026-10-26","startTime":"08:00","endTime":"09:00","isFullDayBlock":false}`.
  2. `GET .../availables?date=2026-10-26` ya no ofrece 08:00, lo cual es correcto.
  3. Como PACIENTE: `POST /api/appointments` con `startTime:"2026-10-26T08:00:00"`.
- **Esperado:** 409 `That schedule is blocked`.
- **Obtenido:** `201`, cita id 5 creada dentro del bloqueo.
- **Evidencia:** `AppointmentServiceImpl.java:57`. `isBlocked` solo consulta `existsByDoctorIdAndDateAndIsFullDayBlockTrue`.

### B9. Se pueden reservar citas en el pasado, y los slots de hoy incluyen horarios ya pasados
- **Severidad:** alta
- **Endpoints:** `POST /api/appointments`, `GET /api/doctorAvailability/doctor/{id}/availables`
- **Pasos:**
  1. `POST /api/appointments` con `startTime:"2026-10-05T08:00:00"` (hoy es 12-oct) responde `201`.
  2. A las 17:02, `GET .../availables?date=2026-10-12` devuelve slots desde las 08:00.
- **Esperado:** 400/409 para fechas u horas pasadas, y el listado de slots solo con horarios futuros. El frontend incluso traduce un mensaje `in the past` que el backend nunca envía.
- **Obtenido:** ambas cosas se aceptan. Desde la UI se reservó la cita id 7 a las 08:00 de hoy, ya pasadas (ver F2).
- **Evidencia:** `AppointmentServiceImpl.java:53-74` y `DoctorAvailabilityServiceImpl.getDoctorAvailableSlots` (no hay ninguna comparación con `now()`).

### B10. Bloques de disponibilidad solapados: slots duplicados y slots ofrecidos que en realidad están ocupados
- **Severidad:** media
- **Endpoint:** `POST /api/doctorAvailability`, `GET .../availables`
- **Pasos:**
  1. Ana ya tiene un bloque MONDAY de 08:00 a 12:00. Agregar MONDAY 09:00-12:00/30 y MONDAY 10:00-11:00/20. Ambos responden 200 sin validar el solapamiento.
  2. `GET .../availables?date=2026-10-19` devuelve `09:00` dos veces, `10:00` tres veces, etc.
  3. Reservar las 10:00 (de 10:00 a 10:30). El slot `10:20` se sigue ofreciendo, porque el filtro solo compara la hora de inicio exacta. Al reservarlo responde `409 That slot is already taken`.
- **Esperado:** rechazar los bloques solapados (o fusionarlos) y quitar de la oferta los slots que se solapan con citas existentes.
- **Evidencia:** `DoctorAvailabilityServiceImpl.java:38-55` (sin chequeo de solapamiento) y `:90-94` (`bookedStartTimes.contains(slot.startTime())`). En el frontend esto genera NG0955 (ver F3).

### B11. `POST /api/scheduleException` sin validación: rangos invertidos, bloqueos parciales sin horas y fechas pasadas
- **Severidad:** media
- **Endpoint:** `POST /api/scheduleException`
- **Pasos y resultados:**
  - `{"date":"2026-10-27","startTime":"12:00","endTime":"09:00","isFullDayBlock":false}` responde `200`. El rango invertido se guarda y no bloquea nada.
  - `{"date":"2026-10-27","isFullDayBlock":false}` responde `200`. Un bloqueo parcial sin horas **rompe la pantalla de disponibilidad** (ver F1).
  - `{"isFullDayBlock":true}` (sin fecha) responde `409 The operation conflicts with existing data` en lugar de 400.
  - `{"date":"2020-01-01","isFullDayBlock":true}` responde `200`.
- **Esperado:** 400 con errores por campo.
- **Evidencia:** `ScheduleExceptionController.java:28` (sin `@Valid`), `ScheduleExceptionServiceImpl.java:28-34` (sin reglas de negocio).

### B12. Un bloqueo de día completo sobre un día con citas activas se acepta en silencio
- **Severidad:** media
- **Endpoint:** `POST /api/scheduleException`
- **Pasos:** el día 2026-10-19 tenía 2 citas PENDING de Ana. `POST /api/scheduleException` con `{"date":"2026-10-19","isFullDayBlock":true}` responde `200`.
- **Esperado:** advertir o rechazar, o cancelar y notificar las citas afectadas.
- **Obtenido:** las citas siguen PENDING y se pueden confirmar en un día que el doctor bloqueó.

### B13. Errores 500 con entradas inválidas fáciles de provocar
- **Severidad:** media
- **Endpoints y resultados:**
  - `POST /api/auth/login` con `clinicId: 999` (clínica inexistente): **500**. El schema `clinic_999` no existe y la consulta falla (`AuthServiceImpl.java:31`). Debería responder 401 o 404. Es fácil de provocar desde la UI si la clínica guardada en localStorage ya no existe (F5).
  - `GET /api/appointments/calendar?month=13&year=2026` (o `month=0`): **500** (`DateTimeException` en `AppointmentServiceImpl.java:116`). Debería responder 400.
  - `GET /api/appointments?sort=foo`: **500** (propiedad de orden inválida). Debería responder 400.

### B14. `PatientRequest.email` sin validación: se guardan emails inválidos y un email nulo da 409
- **Severidad:** media
- **Endpoint:** `PUT /api/patients/update/{id}` y `PUT /api/patients/update`
- **Pasos:** `PUT /api/patients/update/1` con `{"name":"John Smith","email":"not-an-email",...}` responde `200` y guarda `"email":"not-an-email"`. Sin `email` responde `409` genérico (la restricción NOT NULL de la base de datos).
- **Esperado:** 400 (`@NotBlank @Email`, igual que `PersonalRequest`).
- **Evidencia:** `dto/patient/PatientRequest.java:8`. Restauré el email original.

### B15. Las violaciones de unicidad y las referencias inexistentes devuelven un 409 genérico sin detalle
- **Severidad:** media
- **Endpoints:** `POST /api/patients`, `PUT /api/patients/update`, `POST /api/appointments`
- **Pasos y resultados:**
  - Crear o editar un paciente con un teléfono que ya usa otro paciente devuelve `409 The operation conflicts with existing data`. No se dice qué campo, y el frontend muestra "vuelve a intentarlo" (F5).
  - `POST /api/appointments` con `patientId: 999` (como ASSISTANT) devuelve `409` genérico. Debería ser `404 Patient not found`, porque `getReferenceById` falla recién en la FK.
  - `doctorId: 999` o el id de un asistente devuelve `409 That Schedule is out of journey`. Debería ser 404, o 400 "no es doctor".
- **Evidencia:** `AppointmentServiceImpl.java:63-64`, `PatientServiceImpl.java:46-67` (solo verifica el email).

### B16. Doctores y pacientes inactivos se pueden reservar, y `/personal/doctors` los devuelve
- **Severidad:** media
- **Endpoints:** `POST /api/appointments`, `GET /api/personal/doctors`
- **Pasos:**
  1. Desactivar al paciente 3. Como ASSISTANT, `POST /api/appointments` con `patientId:3` responde `201` (cita id 6).
  2. Como PACIENTE, `GET /api/personal/doctors` sin `isActive` incluye al doctor inactivo id 7 (`"active":false`).
- **Esperado:** no permitir reservas con doctores o pacientes inactivos, y que los pacientes solo vean doctores activos por defecto.

### B17. Asistente IA: sin memoria de conversación y sin herramientas para reservar o listar citas
- **Severidad:** media
- **Endpoint:** `POST /api/chat/patient`
- **Pasos (misma sesión, con cookies):**
  1. "Mi color favorito es el verde. Responde solo OK." responde "OK".
  2. "¿Cuál es mi color favorito?" responde "NOSE".
  3. "¿Cuáles son mis próximas citas?" responde "no cuento con una función disponible para consultar tus citas".
  4. "Agenda una cita con la Dra. Ana García el 20 de octubre a las 09:00" responde "no tengo una función disponible para registrar la cita".
- **Esperado:** según la UI ("Agenda tu cita conversando"), el asistente recuerda el contexto y puede reservar.
- **Obtenido:** se pasa `ChatMemory.CONVERSATION_ID`, pero no se registra ningún `MessageChatMemoryAdvisor`, así que no hay memoria. `FlowScheduleTool` solo expone 3 herramientas de lectura. Además usa `HttpSession` en una API `STATELESS`.
- **Evidencia:** `ChatServiceImpl.java:16-31`, `tool/FlowScheduleTool.java`.

### B18. Un asistente (o cualquier miembro del personal) puede recibir pacientes asignados, y un doctor ve la lista de pacientes de otro doctor
- **Severidad:** baja
- **Endpoints:** `POST /api/personal/patients/assign`, `GET /api/personal/{doctorId}/patients`
- **Pasos:**
  - Como ASSISTANT, `assign {"doctorId":5,"patientId":1}`, donde 5 es Maria Ramos (asistente), responde `204`. `GET /api/personal/5/patients` devuelve a John. Lo revertí con `/patients/remove`.
  - Como DOCTOR Ana, `GET /api/personal/3/patients` (Dr. Carlos) responde `200`.
- **Esperado:** validar que `doctorId` sea un DOCTOR, y que un doctor solo vea a sus propios pacientes.

### B19. Validaciones débiles en el registro de clínica y en especialidades
- **Severidad:** baja
- **Pasos y resultados:**
  - `POST /api/clinics` con `name:""` y `phoneNumber:""` responde `201` y crea la clínica id 3 sin nombre, que aparece vacía en `/clinic-options`. Causa: `@NotNull` en lugar de `@NotBlank` (`ClinicRequest.java:9-12`).
  - `POST /api/specialties {"name":"general medicine"}` responde `201`, aunque ya existe "General Medicine": el chequeo distingue mayúsculas (`SpecialtyServiceImpl.java:30`). Las dos aparecen como filtros distintos en la reserva.
  - Se puede crear un DOCTOR con `specialtyId:1` ("None"), que se muestra como especialidad "None".

### B20. Códigos de estado y mensajes inconsistentes
- **Severidad:** baja
- **Detalle:**
  - Las creaciones responden `200` en lugar de `201`: `POST /api/patients` (`PatientController.java:41`), `POST /api/personal` (`PersonalController.java:59`), `POST /api/doctorAvailability` (`DoctorAvailabilityController.java:30`), `POST /api/scheduleException` (`ScheduleExceptionController.java:29`).
  - Nombre de clínica duplicado responde `400`. Lo coherente con el resto de la API sería `409`.
  - Mensajes con errores: `the start time must be after the end time` (dice lo contrario de lo que valida, `DoctorAvailabilityServiceImpl.java:42`), `Personal not fount`, `is already exists`, `That Schedule is out of journey`.
  - `GET /api/doctorAvailability/doctor/{doctorId}/availables` con rol DOCTOR ignora el `doctorId` de la ruta y devuelve los slots propios. Por ejemplo, Carlos pide los de Ana y recibe `"doctorId":3` (`DoctorAvailabilityController.java:53`).

### B21. Ni los pacientes ni el administrador pueden cancelar citas
- **Severidad:** baja
- **Endpoint:** `PATCH /api/appointments/{id}/cancel`
- **Pasos:** como PACIENTE, cancelar su propia cita 1 responde `403 Access denied`. Como ADMIN, confirmar responde 403.
- **Esperado:** que el paciente pueda cancelar sus citas (como mínimo las PENDING). Hoy el paciente no tiene ninguna forma de anular una solicitud.
- **Evidencia:** `AppointmentController.java:83,90`.

### B22. El logging registra datos sensibles
- **Severidad:** baja
- **Archivo:** `application.yaml`: `show-sql: true` y `org.hibernate.orm.jdbc.bind: TRACE`. Así quedan en el log los valores de cada parámetro SQL: emails, teléfonos, CI y hashes de contraseña. Debería ir solo en un perfil de desarrollo.

---

## Frontend

### F1. La pantalla "Mi disponibilidad" se rompe con un bloqueo parcial sin horas
- **Severidad:** alta
- **Pantalla:** `/availability` · DOCTOR · 1440 y 375
- **Pasos:** existe un bloqueo con `isFullDayBlock:false` y `startTime/endTime` en null (el backend lo acepta, B11). Entrar a `/availability`.
- **Esperado:** el bloqueo se muestra (por ejemplo "Horario no definido") y el resto de la página funciona.
- **Obtenido:** `ERROR TypeError: Cannot read properties of null (reading 'includes') at AvailabilityPageComponent.formatTime`. La lista de bloqueos queda cortada en ese ítem y la "Vista previa" de slots queda vacía, porque el render se aborta.
- **Evidencia:** consola; captura `avail_full.png` en el scratchpad. Código: `availability-page.component.html:113` (`formatTime(exception.startTime!)` con aserción no nula) y `core/utils/calendar.util.ts` (`formatTime` no tolera null). Limpié los datos de prueba después.

### F2. El wizard de reserva ofrece y permite reservar horarios de hoy que ya pasaron
- **Severidad:** media
- **Pantalla:** `/appointments/new` · PACIENTE (también staff) · 1440
- **Pasos:** a las 17:04 elegir la Dra. Ana García, dejar la fecha de hoy (12-oct), elegir 08:00 y pulsar "Solicitar cita".
- **Esperado:** solo slots futuros. Los de hoy anteriores a la hora actual se ocultan o se deshabilitan.
- **Obtenido:** se listan desde las 08:00 y la cita se crea (id 7, "Lun, 12 oct 08:00 – 08:30, Pendiente"). La causa raíz está en el backend (B9), pero el frontend tampoco filtra.
- **Evidencia:** captura `book_today.png`. Código: `booking-page.component.ts:130-140` (mapea `availableSlots` sin comparar con la hora actual). Lo mismo pasa en la vista previa de `availability-resource.service.ts`.

### F3. Slots duplicados: warning NG0955 y varios botones "seleccionados" a la vez
- **Severidad:** media
- **Pantalla:** `/appointments/new` y la vista previa de `/availability` · cualquier rol
- **Pasos:** el doctor tiene bloques solapados (B10). Abrir la reserva y elegir al doctor.
- **Esperado:** cada horario aparece una sola vez.
- **Obtenido:** se ven `09:00, 09:00, 10:00, 10:00, 10:00…` y la consola muestra `NG0955: The provided track expression resulted in duplicated keys… key "09:00" at index "2" and "3"…`. Al elegir un horario duplicado, ambos botones quedan con `aria-pressed=true`, y elegir 10:20 (que se solapa con 10:00) termina en un 409 al enviar.
- **Evidencia:** consola; captura `book_today.png`. Código: `booking-page.component.html:234` (`track slot.startTime`), `availability-page.component.html:161` (`track slot`). Corregir: deduplicar por `startTime` en el cliente (además de B10).

### F4. Registro de clínica: si la clínica se crea pero el login automático falla, el usuario queda en un callejón sin salida
- **Severidad:** media
- **Pantalla:** `/register` · anónimo · 1440
- **Pasos:** completar el formulario con un CI de admin que ya existe (por ejemplo `123123123`), un email y una contraseña nuevos, y enviar.
- **Esperado:** si la clínica se creó, informarlo y llevar al login de esa clínica con un mensaje claro.
- **Obtenido:** la clínica se crea (id 6, que queda guardada en `localStorage.scheduler.clinic`), pero el usuario sigue en `/register` con el toast "Correo o contraseña incorrectos" en un formulario que no tiene contraseña de login. Si reintenta, recibe "Ya existe un registro con esos datos".
- **Evidencia:** captura `register_existing_ci.png`; red: `POST /api/clinics 201` y luego `POST /api/auth/login 401`. Código: `register-form.component.ts:44-60` (el `switchMap` al login trata ambos errores igual). La causa raíz está en el backend (B3).

### F5. Mensajes de error engañosos con 409/500, y la clínica guardada no se valida
- **Severidad:** media
- **Pantallas:** `/profile`, diálogos de paciente, `/login`
- **Pasos y resultados:**
  - En "Mi perfil" (paciente) poner el teléfono de otro paciente y guardar: el toast dice "Conflicto con el estado actual, vuelve a intentarlo", aunque reintentar nunca va a funcionar. No dice que el teléfono ya está en uso.
  - Con `localStorage.scheduler.clinic` apuntando a una clínica inexistente (por ejemplo, borrada), iniciar sesión da "No se pudo iniciar sesión" (por un 500 del backend, B13). No se sugiere cambiar de clínica, y la pantalla de login muestra el nombre guardado sin comprobar que la clínica exista en `GET /api/clinics`.
- **Evidencia:** captura `login_stale_clinic.png`. Código: `core/http/api-error.ts:34` (fallback 409).

### F6. En mobile (375 px) las tablas recortan las columnas Estado y Acciones, y las pestañas de filtro quedan cortadas
- **Severidad:** media
- **Pantallas:** `/appointments`, `/staff`, `/patients` · 375×812 · todos los roles
- **Pasos:** abrir cualquiera de esas pantallas en 375 px.
- **Esperado:** en mobile, mostrar tarjetas o priorizar columnas, de modo que el estado y las acciones (Confirmar o Cancelar) se vean sin desplazar.
- **Obtenido:** la tabla se desborda dentro del panel. la columna "ESTADO" queda cortada (en algunas capturas solo se lee "ES"), las acciones quedan fuera de la vista, y no hay ninguna pista de que se puede desplazar en horizontal. La pestaña "Canceladas" también queda cortada ("C…"). La página no tiene scroll horizontal global.
- **Evidencia:** capturas `shot_ASSISTANT_m__appointments.png`, `shot_ADMIN_m__staff.png`, `m_drawer_esc.png`.

### F7. El asistente IA muestra el markdown sin renderizar y sugiere acciones que no puede hacer
- **Severidad:** baja
- **Pantalla:** `/assistant` · PACIENTE · 375 y 1440
- **Pasos:** pulsar la sugerencia "¿Qué especialidades tienen?".
- **Obtenido:** la respuesta aparece con `* Medicina General` y `**negritas**` en crudo. Las sugerencias "Quiero agendar una cita" y "¿Cuáles son mis próximas citas?" llevan a respuestas como "no cuento con una función disponible" (B17). "Limpiar" solo borra la conversación en el cliente.
- **Evidencia:** captura `m_chat.png`. Código: `assistant-page.component.ts:35` (sugerencias) y la plantilla, que interpola el texto plano.

### F8. Los días del calendario anuncian "1 citas" a los lectores de pantalla
- **Severidad:** baja
- **Pantalla:** `/calendar` · todos
- **Obtenido:** `aria-label="Lunes, 5 de octubre de 2026, 1 citas"`. El texto visible sí usa singular y plural.
- **Evidencia:** `shared/ui/calendar-day-cell/calendar-day-cell.component.ts:33`.

### F9. Los patrones de traducción de errores no coinciden con los mensajes reales del backend
- **Severidad:** baja
- **Archivo:** `core/http/api-error.ts:13,22`
- **Detalle:** `/end time must be after/` nunca coincide, porque el backend envía `the start time must be after the end time`. `/in the past/` y `/partial block needs/` corresponden a mensajes que el backend nunca emite. Esos errores caen en el genérico "Revisa los datos ingresados".

### F10. Validación de CI inconsistente entre formularios
- **Severidad:** baja
- **Pantallas:** diálogo "Nuevo miembro" (`/staff`) y diálogo "Nuevo paciente" (`/patients`)
- **Detalle:** el personal exige solo dígitos (`staff-form-dialog.component.ts:45`, `Validators.pattern(/^\d+$/)`), mientras que el paciente acepta cualquier texto (`patient-form-dialog.component.ts:35`). El backend no valida el formato en ninguno de los dos.

### F11. La sesión inválida o expirada redirige al login sin ningún aviso
- **Severidad:** baja
- **Pantalla:** cualquiera del shell
- **Pasos:** alterar `localStorage.scheduler.token` (firma inválida) y navegar a `/appointments`.
- **Obtenido:** el 401 provoca `logout()` y la redirección a `/login`, pero no se muestra "Tu sesión expiró". El usuario no sabe por qué salió. El mensaje existe en `api-error.ts` pero no se usa en `authInterceptor` (`core/http/api.interceptors.ts`).

---

## Lo que funcionó bien (cubierto y sin defectos)
- Guards por rol: cada ruta no permitida redirige al inicio del rol (verificado en 11 rutas × 4 roles × 2 viewports). Los guards de invitado (`/login`, `/register`, `/clinic-options`) funcionan con sesión activa.
- Login: validación de campos vacíos, mensaje correcto ante una contraseña incorrecta, labels asociados a todos los inputs.
- Registro: validación de los 6 campos y rollback del schema y la clínica cuando falla el alta del admin por email duplicado.
- Tablero: la actualización en tiempo real por WebSocket funciona. Una reserva hecha por API se reflejó en el tablero del asistente en menos de 3 s. La suscripción a otro tenant está protegida por `JwtChannelInterceptor`.
- Confirmar o cancelar desde el listado, con diálogo de confirmación y toast. El doble clic en "Solicitar cita" crea una sola cita. Cancelar una cita ya cancelada devuelve un 409 bien traducido.
- Diálogos: Esc cierra, el foco queda dentro del diálogo y vuelve al disparador. El menú móvil abre y cierra con Esc y devuelve el foco.
- Permisos entre doctores y pacientes en `GET/PATCH /appointments/{id}` (403). El aislamiento de citas entre tenants funciona: `GET /appointments/1` desde Uptown responde 404.
- Sin scroll horizontal de página en 1440 ni en 375 en ninguna ruta. El calendario y la navegación por meses funcionan.

## No probado o limitaciones
- El bucle infinito de B1 se confirmó solo leyendo el código, para no tumbar el backend compartido.
- No se probaron el drag & drop del tablero (CDK) ni la expiración real del token a las 24 h. El contraste WCAG no se midió de forma sistemática.
- Twilio/WhatsApp no está configurado (credenciales de ejemplo en `application.yaml`).

## Nota sobre los datos de prueba creados
Durante el QA se crearon y luego desactivaron o cancelaron estos registros. **No existen endpoints para borrar**, así que siguen en la base de datos:
- Clínicas: id 3 (nombre vacío), id 4 "QA Clinica CI" (admin = Ana García, por B3), id 6 "QA Clinica UI …".
- Downtown: paciente 3 "QA Paciente" (inactivo), paciente 4 (vínculo de James Wilson, inactivo), personal 6 "QA Doctor" y 7 "QA Doc None" (inactivos), especialidad 5 "general medicine", citas 1-8 (todas CANCELLED).
- Ya eliminé los bloques de disponibilidad (9 y 10) y los bloqueos (1, 2, 3, 5 y 6) de prueba.
- **Efecto pendiente de B6:** John Smith (`john.smith@email.com`, clínica 1) ahora inicia sesión como **ASSISTANT**, porque existe `clinic_1.personal` id 8 con su cuenta (aunque está inactivo). Para restaurar el estado semilla: `DELETE FROM clinic_1.personal WHERE id = 8;`
