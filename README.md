## Descripción General

Implementación de un servicio de integración clínico robusto en Spring Boot, orientado a la recepción, validación, procesamiento y auditoría de mensajes en formato HL7 v2.x utilizando el protocolo MLLP (Minimum Lower Layer Protocol).

La solución simula la interacción bidireccional entre sistemas hospitalarios (HIS, LIS, RIS, etc.) o motores de integración como Mirth Connect, cubriendo tanto la recepción como la trazabilidad completa de los mensajes clínicos.

Se complementa con una capa de auditoría y trazabilidad implementada en Oracle mediante PL/SQL, utilizando procedimientos almacenados, funciones, vistas y triggers.
---

## Funcionalidades Principales
- Recepción de Mensajes HL7v2
- Escucha activa en un puerto TCP con protocolo MLLP.
- Compatible con múltiples tipos de mensajes: ADT, ORM, ORU, entre otros.

## Procesamiento del Mensaje
Validación estructural básica de los segmentos y campos del mensaje HL7v2.

Aplicación de reglas de negocio para determinar el estado del mensaje:

- AA (Application Accept): Procesamiento exitoso.

- AE (Application Error): Error durante el procesamiento.

- AR (Application Reject): Mensaje rechazado.

📤 Generación de Mensaje ACK
Respuesta ACK estándar enviada al sistema origen.

El contenido del ACK refleja el resultado del procesamiento del mensaje recibido.

🗄️ Auditoría y Persistencia (PL/SQL + Oracle)
Almacenamiento de cada mensaje HL7 recibido con trazabilidad completa.

Registro de:

Contenido bruto (messageRaw),

Origen (sender), destino (receiver),

Tipo de mensaje (messageType),

Estado (ackStatus: AA, AE, AR),

Errores (ackError) si existen.

Implementación de triggers, vistas y procedimientos almacenados para:

Registro automático de errores en tabla de log (hl7_error_log).

Generación de vistas para monitoreo en tiempo real.

Posibilidad de extracción y análisis de datos vía funciones PL/SQL.

Base de datos Oracle para entornos reales, y H2 en memoria para pruebas locales y desarrollo ágil (MVP).



---

## Componentes Técnicos

- **Spring Boot** (API REST, configuración, beans)
- **Base de Datos**: JPA/Hibernate + Oracle para PL/SQL.
- **Parser HL7**: `HAPI HL7 v2` para análisis y construcción de mensajes HL7
- **Servicio ACK Builder**: Generador de ACK dinámico
- **Auditoría**: Entity Auditor + Logging persistente

---

## Casos de Uso

| Flujo                             | Descripción                                                                 |
|----------------------------------|-----------------------------------------------------------------------------|
| Ingreso desde Mirth              | Mirth envía un mensaje HL7v2 ADT^A01 al servidor                             |
| Validación y procesamiento       | El mensaje es parseado y validado según reglas definidas                    |
| Respuesta ACK                    | Se construye y retorna un mensaje ACK (AA/AE/AR) al origen                  |
| Auditoría persistente            | El mensaje original, el ACK y el estado se almacenan para trazabilidad      |

---

## Beneficios

- ✅ Asegura la **trazabilidad completa** de los mensajes clínicos.
- ✅ Compatible con **sistemas legados** basados en HL7v2.
- ✅ Fácil integración con **plataformas de interoperabilidad** como Mirth Connect.
- ✅ Base para construir **flujos más complejos** (transformación a FHIR, enrutamiento, etc.)

---

## Futuras Extensiones

- 🔄 Reintentos automáticos en caso de fallos.
- 🔁 Transformación HL7v2 → FHIR.
- 📊 Panel web para visualización de auditoría.
- 🔐 Validaciones avanzadas de negocio (por tipo de mensaje, campos obligatorios, etc.).

---
