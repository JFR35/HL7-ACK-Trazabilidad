# Gestión y Auditoría de Mensajes Clínicos HL7v2 (MLLP)

## Descripción General

Implementación de un **servicio de integración robusto en Spring Boot** para la **recepción, procesamiento y auditoría de mensajes clínicos** en formato **HL7 v2.x** utilizando el protocolo **MLLP (Minimum Lower Layer Protocol)**.

Esta solución simula la **interacción bidireccional entre sistemas hospitalarios**, como pueden ser HIS, LIS o motores de integración como **Mirth Connect**.

---

## Funcionalidades Principales

- 📥 **Recepción de Mensajes HL7v2**
  - Escucha activa en un puerto TCP usando protocolo MLLP.
  - Compatible con mensajes ADT, ORM, ORU, entre otros.

- ⚙️ **Procesamiento del Mensaje**
  - Validación estructural básica del mensaje HL7v2.
  - Aplicación de reglas de negocio para decidir el estado del mensaje:
    - `AA` (Application Accept) → Éxito
    - `AE` (Application Error) → Error de procesamiento
    - `AR` (Application Reject) → Mensaje rechazado

- 📤 **Generación de Mensaje ACK**
  - Respuesta estándar HL7 ACK al sistema origen.
  - Contenido refleja el resultado del procesamiento.

- 🗄️ **Auditoría y Almacenamiento**
  - Persistencia de cada mensaje HL7 recibido.
  - Estado (`AA`, `AE`, `AR`), timestamp y detalle de errores si existen.
  - Base de datos relacional (ej. PostgreSQL o MySQL).

---

## Componentes Técnicos

- **Spring Boot** (API REST, configuración, beans)
- **Netty/TCP Server** o integración con librerías MLLP específicas
- **Base de Datos**: JPA/Hibernate + PostgreSQL/MySQL
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
- ✅ Base para construir **flujos más complejos** (ej. transformación a FHIR, enrutamiento, etc.)

---

## Futuras Extensiones

- 🔄 Reintentos automáticos en caso de fallos.
- 🔁 Transformación HL7v2 → FHIR.
- 📊 Panel web para visualización de auditoría.
- 🔐 Validaciones avanzadas de negocio (por tipo de mensaje, campos obligatorios, etc.).

---
