# 📦 Shopify Integration Engine – Contexto del Sistema

## 1. 🎯 Propósito

El sistema **Shopify Integration Engine** es un backend diseñado como motor de integración entre:

* Shopify (e-commerce)
* Sistema B2B (órdenes, tracking, facturación)
* Estafeta (logística)
* Sistema de timbrado
* Servicios de notificación

Su objetivo principal es **orquestar flujos de negocio críticos**:

* Registro de órdenes
* Sincronización de entregas
* Facturación automática
* Actualización de productos
* Notificación operativa

---

## 2. 🌐 Problema que resuelve

El sistema elimina la necesidad de integraciones manuales entre plataformas, resolviendo:

* Desincronización entre Shopify y B2B
* Falta de trazabilidad en entregas
* Procesos manuales de facturación
* Actualización masiva de productos
* Falta de visibilidad operativa

---

## 3. 🧩 Sistemas Integrados

### 🛒 Shopify

* Órdenes
* Productos
* Webhooks
* GraphQL / REST APIs

### 🚚 Estafeta

* Generación de guías
* Tracking de envíos
* Cobertura

### 🏢 Sistema B2B

* Recepción de órdenes
* Seguimiento de entregas
* Facturación

### 📄 Facturación / Timbrado

* Generación de documentos fiscales

### 📧 Notificación (CRÍTICO)

No es un módulo aislado.
Es un componente **transversal** que participa en múltiples flujos:

* Tracking (resumen y errores)
* Shopify (sincronización)
* Facturación
* Jobs batch

Tipos de notificación:

* Confirmación
* Error
* Resumen operativo
* Facturación
* Actualización de productos

---

## 4. 🔄 Flujos de negocio principales

### 🔹 Shopify → B2B (Órdenes)

```
Shopify API / Webhook
        ↓
Transformación
        ↓
B2B Service
        ↓
Registro de orden
        ↓
Notificación
```

---

### 🔹 Tracking Estafeta → DB → B2B

```
Scheduler / API
        ↓
Estafeta API
        ↓
Mapeo estatus
        ↓
Actualización DB
        ↓
Facturación (si aplica)
        ↓
Notificación
```

---

### 🔹 Facturación automática

```
Entrega detectada
        ↓
B2B Facturación
        ↓
Notificación
```

---

### 🔹 Actualización de productos

```
API interna
        ↓
Shopify API (REST / GraphQL)
        ↓
Actualización
        ↓
Resumen
        ↓
Notificación
```

---

## 5. ⚙️ Modos de operación

El sistema soporta múltiples estrategias:

* Modo B2B (API-based)
* Modo DAO (DB-based)

---

## 6. 🧠 Naturaleza del sistema

Este sistema es:

* Integrador de sistemas
* Orquestador de procesos
* Híbrido (batch + tiempo real)
* Orientado a negocio (no CRUD)

---

## 7. 🚨 Características clave

* Idempotencia en tracking
* Manejo de errores robusto
* Procesamiento por lotes
* Seguridad (JWT + Webhooks)
* Notificación transversal

---

## 8. 🔥 Insight clave

Este sistema es un:

👉 **Motor de integración (Integration Engine)**

No es un backend tradicional.
