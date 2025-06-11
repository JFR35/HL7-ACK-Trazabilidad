Gestión y Auditoría de Mensajes Clínicos HL7v2 (MLLP)

Implementación de un servicio de integración robusto en Spring Boot para la recepción, procesamiento y auditoría de mensajes clínicos en formato HL7 v2.x a través del protocolo MLLP (Minimum Lower Layer Protocol).

Esta solución simula la interacción bidireccional entre sistemas hospitalarios, donde:

Se reciben mensajes HL7v2 (ej., desde Mirth Connect u otros sistemas de información hospitalaria).
Se procesan los mensajes, determinando su validez y estado (Éxito AA, Error AE, Rechazo AR) basándose en reglas de negocio iniciales.
Se envía un mensaje de confirmación ACK estandarizado al sistema de origen, reflejando el resultado del procesamiento.
Se almacena una copia completa del mensaje recibido, junto con su estado y detalles de error, en una base de datos para auditoría y trazabilidad.
Esta implementación garantiza la fiabilidad en la comunicación de datos legados y sienta las bases para flujos de interoperabilidad más complejos.
