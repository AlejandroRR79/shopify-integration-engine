# 🏗️ Shopify Integration Engine – Arquitectura

---

## 1. 🧱 Estilo arquitectónico

* Arquitectura en capas (Layered)
* Orientada a servicios
* Integración mediante APIs REST
* Orquestación centralizada

---

## 2. 📂 Capas del sistema

---

### 🔹 Controller Layer

Responsable de exponer endpoints REST:

* Manejo de requests/responses
* Validación básica
* Entrada al sistema

---

### 🔹 Service Layer (CORE)

Aquí vive la lógica real del negocio.

Tipos de servicios:

* Orquestadores (coordinan flujos)
* Servicios de integración
* Servicios de negocio

Ejemplos:

* Tracking
* Shopify sync
* Facturación

---

### 🔹 DAO Layer

* Acceso a base de datos
* Uso de JDBC
* Queries dinámicos
* Manejo de históricos

---

### 🔹 Integration Layer

Clientes hacia sistemas externos:

* Shopify API
* Estafeta API
* B2B API

---

### 🔹 Config Layer

* Seguridad (JWT)
* HTTP Clients (RestTemplate)
* Propiedades

---

### 🔹 Cross-Cutting: Notificación ⚠️

La notificación NO es una capa.

Es un componente transversal:

```
Service → NotificacionService → Email
```

Se utiliza en:

* Tracking
* Shopify
* Facturación
* Jobs

---

## 3. 🔐 Seguridad

---

### 🔸 JWT

* Autenticación basada en token
* Filtro personalizado
* Protección de endpoints

---

### 🔸 Webhooks (HMAC)

* Validación de integridad
* Protección contra ataques

---

## 4. 🔄 Flujos principales

---

### 🔹 Tracking (flujo crítico)

```
Job / API
    ↓
TrackingOrchestrator
    ↓
DeliveryTrackingService / DAOService
    ↓
DeliveryCoreService
    ↓
DeliveryDAO
    ↓
Base de datos
    ↓
B2B API
    ↓
Notificación
```

---

### 🔹 Shopify (órdenes)

```
Controller / Webhook
    ↓
Shopify Service
    ↓
Transformación
    ↓
B2B Service
    ↓
Notificación
```

---

### 🔹 Productos (Bulk)

```
Controller
    ↓
Bulk Service
    ↓
Shopify API (GraphQL / REST)
    ↓
Procesamiento por chunks
    ↓
Resultado agregado
```

---

## 5. 🧠 Componentes clave

---

### 🔥 Orquestador

* Decide flujo (DAO vs B2B)
* Centraliza ejecución

---

### 🔥 Core Service

* Lógica transaccional
* Reglas de negocio
* Punto crítico del sistema

---

### 🔥 DAO

* Persistencia
* Historial de cambios

---

### 🔥 Notificación (DEBILIDAD ACTUAL)

Patrón actual:

```
Service → NotificacionService → Email
```

Problemas:

* Acoplado
* Sin asincronía
* Sin retry
* Sin trazabilidad completa

---

## 6. ⚠️ Problemas actuales

---

### ❌ 1. Servicios grandes (God classes)

* Mucha lógica en un solo servicio

---

### ❌ 2. Acoplamiento fuerte

* Dependencias directas entre servicios

---

### ❌ 3. Notificación acoplada

* No basada en eventos
* Difícil de escalar

---

### ❌ 4. Falta de resiliencia

* Sin retry formal
* Sin circuit breaker

---

### ❌ 5. DAO complejo

* SQL difícil de mantener

---

## 7. 🚀 Evolución recomendada

---

### 🔹 Introducir eventos (selectivo)

```
Service → Event → Handler
```

---

### 🔹 Desacoplar notificación

```
Service → Event → NotificationHandler
```

---

### 🔹 Agregar resiliencia

* Retry
* Circuit breaker

---

### 🔹 Mejorar observabilidad

* Logs estructurados
* Correlation ID
* Trazabilidad end-to-end

---

## 8. 🧠 Nivel actual

El sistema se encuentra en:

👉 Arquitectura de integración avanzada (mid-senior)

---

## 9. 🔥 Conclusión

Arquitectura sólida en base, pero requiere evolución hacia:

👉 Eventos + Observabilidad + Desacoplamiento

Especialmente en:

👉 NOTIFICACIÓN
