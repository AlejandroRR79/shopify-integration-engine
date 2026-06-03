# 🚚 CU_SKYDROPX_001 – Generación de Guía con SkyDropX (Primario + Fallback Estafeta)

---

## 1. 🎯 Objetivo

Integrar SkyDropX como proveedor principal de paquetería para:

* Cotizar envíos al momento de crear la OC
* Seleccionar la mejor opción (precio + tiempo)
* Generar guía
* Obtener información de envío
* Manejar seguimiento vía webhook

En caso de falla:

👉 Utilizar Estafeta como **fallback**

---

## 2. 🧠 Contexto

* El sistema actual con Estafeta funciona al 100% (no se modifica)
* SkyDropX entra como proveedor principal
* Se usa el mismo payload base de Estafeta
* La ejecución es **on-demand (al crear OC)**
* SkyDropX maneja **webhooks para tracking**

---

## 3. ⚙️ Configuración (Environment Variables)

### 🔹 URLs

* `SKYDROPX_BASE_URL`

  * Ejemplo: `https://sb-pro.skydropx.com`

* `SKYDROPX_AUTH_URL`

  * Ejemplo: `https://sb-pro.skydropx.com/api/v1/oauth/token`

---

### 🔹 Credenciales

* `SKYDROPX_CLIENT_ID`
* `SKYDROPX_CLIENT_SECRET`

---

### 🔹 OAuth

* `SKYDROPX_GRANT_TYPE`

  * Valor: `client_credentials`

* `SKYDROPX_SCOPE`

  * Valor: `default.orders.create`

---

### 🔹 Configuración técnica adicional

* `SKYDROPX_TIMEOUT_MS`
* `SKYDROPX_TOKEN_REFRESH_BUFFER` (segundos antes de expirar)

---

### 🔹 Headers estándar

* `Authorization: Bearer {access_token}`
* `Content-Type: application/json`

---

## 4. 🔐 Autenticación (OAuth2)

SkyDropX utiliza autenticación OAuth2 con `client_credentials`.

---

### 🔹 Endpoint

POST
`{SKYDROPX_AUTH_URL}`

---

### 🔹 Request (x-www-form-urlencoded)

```text
grant_type=client_credentials
client_id={SKYDROPX_CLIENT_ID}
client_secret={SKYDROPX_CLIENT_SECRET}
scope=default.orders.create
redirect_uri=urn:ietf:wg:oauth:2.0:oob
```

---

### 🔹 Response

```json
{
  "access_token": "string",
  "token_type": "Bearer",
  "expires_in": 7200,
  "scope": "default.orders.create"
}
```

---

### 🔹 Consideraciones

* El token expira en ~7200 segundos
* Debe cachearse en memoria (NO solicitar token por request)
* Renovar antes de expiración
* Manejar fallback si falla autenticación

---

## 5. 🔄 Flujo general

```text
Creación de OC
        ↓
Generar payload (base Estafeta)
        ↓
SkyDropX - Cotización
        ↓
Obtener cotizaciones
        ↓
Evaluar (precio + días)
        ↓
Seleccionar mejor opción
        ↓
Crear shipment (guía)
        ↓
Obtener datos de guía
        ↓
Guardar en DB
        ↓
Esperar webhook (tracking)
```

---

## 6. 🚨 Flujo con fallback

```text
SkyDropX
   ↓
¿Cotización válida?
   ↓ NO
→ Fallback Estafeta

¿Shipment OK?
   ↓ NO
→ Fallback Estafeta

¿Guía OK?
   ↓ NO
→ Fallback Estafeta
```

---

## 7. 📡 Servicios SkyDropX

### 🔹 7.1 Cotización

POST
`{SKYDROPX_BASE_URL}/api/v1/quotations`

---

### 🔹 7.2 Consulta cotización

GET
`{SKYDROPX_BASE_URL}/api/v1/quotations/{quotationId}`

---

### 🔹 7.3 Crear envío

POST
`{SKYDROPX_BASE_URL}/api/v1/shipments`

---

### 🔹 7.4 Obtener guía

GET
`{SKYDROPX_BASE_URL}/api/v1/shipments/{shipmentId}`

---

### 🔹 7.5 Webhook (tracking)

POST
`/api/webhook/skydropx`

👉 Endpoint interno del sistema

---

## 8. 🧩 Reglas de negocio

### 🔹 Selección de guía

1. Prioridad: menor tiempo de entrega
2. Segundo criterio: menor precio

---

### 🔹 Fallback

Se activa cuando:

* No hay cotizaciones válidas
* Error en API SkyDropX
* Error al generar shipment
* No se obtiene guía

---

## 9. 🏗️ Diseño técnico

---

### 🔹 Componentes

```text
ShippingOrchestratorService
        ↓
SkyDropXService
        ↓
SkyDropXAuthService
        ↓
SkyDropXAdapter
        ↓
SkyDropXMapper
```

---

### 🔹 Responsabilidades

#### ShippingOrchestratorService

* Decide proveedor (SkyDropX / Estafeta)
* Maneja fallback
* Controla flujo

---

#### SkyDropXService

* Orquesta cotización + selección + shipment
* Aplica reglas de negocio
* Genera eventos

---

#### SkyDropXAuthService

* Obtiene token OAuth
* Cachea token
* Renueva token automáticamente

---

#### SkyDropXAdapter

* Invoca APIs externas
* Maneja headers
* Maneja errores HTTP

---

#### SkyDropXMapper

* Convierte payload Estafeta → SkyDropX
* Normaliza respuestas

---

## 10. 💾 Persistencia

Se debe guardar:

* proveedor (SKYDROPX / ESTAFETA)
* quotationId
* shipmentId
* precio
* días estimados
* trackingCode
* guía (ruta o URL)
* estado actual
* correlationId

---

## 11. 🔄 Webhook (Tracking)

```text
SkyDropX
    ↓
WebhookController
    ↓
Actualizar DB
    ↓
EVENT_LOG
    ↓
Notificación (si aplica)
```

---

### 🔹 Consideraciones webhook

* Debe ser idempotente
* Validar origen (seguridad)
* Manejar duplicados
* Actualizar estatus

---

## 12. 📢 Eventos

### 🔹 Eventos principales

* SKYDROPX_QUOTATION_REQUESTED
* SKYDROPX_QUOTATION_SELECTED
* SKYDROPX_SHIPMENT_CREATED
* SKYDROPX_LABEL_OBTAINED

---

### 🔴 Eventos de error

* SKYDROPX_ERROR
* SKYDROPX_FALLBACK_ESTAFETA

---

## 13. 🚨 Alertas

Alertas inmediatas en:

* Falla total SkyDropX
* Activación de fallback
* Error en webhook
* Error en generación de guía

---

## 14. 🔁 Manejo de errores

* Retry:

  * cotización
  * shipment

* No retry:

  * errores de validación

---

## 15. 🔗 Integración con sistema actual

* Estafeta se mantiene intacto
* Solo se invoca en fallback
* No se modifica tracking actual

---

## 16. 🧠 Consideraciones clave

* SkyDropX introduce asincronía (webhooks)
* Se debe manejar idempotencia en webhook
* Se debe usar correlationId
* Se debe cachear token OAuth

---

## 17. 🚀 Próximos pasos

Pendiente definir:

* Payload request (cotización)
* Payload response
* Mapeo Estafeta → SkyDropX
* Estructura webhook
* Estrategia fina de fallback

---
## 18. 💻 Implementación Técnica (Spring Boot)

### 🔹 Endpoint

POST `/api/secure/skydropx/cotizar`

Recibe:

* Payload original de Estafeta

---

### 🔹 Flujo interno

```text
Controller
    ↓
ShippingOrchestratorService
    ↓
SkyDropXService
    ↓
SkyDropXMapper (Estafeta → SkyDropX)
    ↓
SkyDropXAdapter (REST)
    ↓
SkyDropX API
```

---

### 🔹 Componentes implementados

* SkyDropXController
* ShippingOrchestratorService
* SkyDropXService
* SkyDropXMapper
* SkyDropXAdapter
* SkyDropXAuthService
* SkyDropXException

---

### 🔹 Autenticación

* OAuth2 (client_credentials)
* Token cacheado en memoria
* Renovación automática antes de expiración

---

### 🔹 Manejo de errores

SkyDropX responde con estructura:

```json
[
  {
    "code": 400,
    "description": "Error message"
  }
]
```

El sistema:

* captura error
* lo transforma a `SkyDropXException`
* lo regresa al cliente con HTTP 400

---

### 🔹 Consideraciones

* No se solicita token en cada request
* Se reutiliza token hasta expiración
* Mapper convierte payload de Estafeta automáticamente
* Sistema preparado para fallback a Estafeta


## 19. 🔄 Mapeo Estafeta → SkyDropX Quotations

### 🔹 Objetivo

Reutilizar el payload actual de Estafeta para generar automáticamente el payload requerido por SkyDropX Quotations.

---

### 🔹 Transformación principal

#### Payload origen (Estafeta)

```json
{
  "labelDefinition": {
    "itemDescription": {},
    "location": {}
  }
}


### 🔹 Reglas de transformación

| Estafeta | SkyDropX |
|---|---|
| `labelDefinition.location.origin.address.zipCode` | `quotation.address_from.postal_code` |
| `labelDefinition.location.origin.address.stateAbbName` | `quotation.address_from.area_level1` |
| `labelDefinition.location.origin.address.localityName` | `quotation.address_from.area_level2` |
| `labelDefinition.location.origin.address.townshipName` | `quotation.address_from.area_level3` |
| `labelDefinition.location.destination.homeAddress.address.zipCode` | `quotation.address_to.postal_code` |
| `labelDefinition.location.destination.homeAddress.address.stateAbbName` | `quotation.address_to.area_level1` |
| `labelDefinition.location.destination.homeAddress.address.localityName` | `quotation.address_to.area_level2` |
| `labelDefinition.location.destination.homeAddress.address.townshipName` | `quotation.address_to.area_level3` |
| `labelDefinition.itemDescription.length` | `quotation.parcels[0].length` |
| `labelDefinition.itemDescription.width` | `quotation.parcels[0].width` |
| `labelDefinition.itemDescription.height` | `quotation.parcels[0].height` |
| `labelDefinition.itemDescription.weight` | `quotation.parcels[0].weight` |


### 🔹 Consideraciones técnicas

* `country_code` se enviará inicialmente fijo como `"MX"`
* SkyDropX requiere valores numéricos en:
  * weight
  * height
  * length
  * width
* `requested_carriers` será configurado inicialmente de forma estática:
  * estafeta
  * fedex
  * dhl
  * paquetexpress


  ---

## 20. 📦 Clases a implementar

| Clase | Paquete | Responsabilidad |
|---|---|---|
| `SkyDropXController` | `com.creditienda.controller.skydropx` | Exponer endpoint `/api/secure/skydropx/cotizar` |
| `SkyDropXService` | `com.creditienda.service` | Orquestar flujo de cotización SkyDropX |
| `SkyDropXAuthService` | `com.creditienda.service` | Obtener y cachear token OAuth |
| `SkyDropXAdapter` | `com.creditienda.service` | Consumir APIs REST SkyDropX |
| `SkyDropXMapper` | `com.creditienda.service.mapper` | Transformar payload Estafeta → SkyDropX |
| `SkyDropXQuotationRequestDTO` | `com.creditienda.dto.skydropx` | Request quotations SkyDropX |
| `SkyDropXQuotationResponseDTO` | `com.creditienda.dto.skydropx` | Response quotations SkyDropX |
| `SkyDropXAddressDTO` | `com.creditienda.dto.skydropx` | Address quotations SkyDropX |
| `SkyDropXParcelDTO` | `com.creditienda.dto.skydropx` | Parcel quotations SkyDropX |
| `SkyDropXAuthResponseDTO` | `com.creditienda.dto.skydropx` | Response OAuth SkyDropX |
| `SkyDropXException` | `com.creditienda.exception` | Manejo de errores SkyDropX |
| `SkyDropXWebhookController` | `com.creditienda.controller.skydropx` | Recibir webhooks tracking SkyDropX |

---

### 🔹 Integración notificaciones

Inicialmente SkyDropX reutilizará:

```text
NotificacionService