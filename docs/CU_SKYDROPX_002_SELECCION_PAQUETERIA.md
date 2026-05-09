# 🚚 CU_SKYDROPX_002 – Selección Inteligente de Paquetería

---

## 1. 🎯 Objetivo

Definir la estrategia de selección automática de paquetería a partir de las cotizaciones obtenidas desde SkyDropX.

El sistema deberá:

* Evaluar múltiples cotizaciones
* Aplicar reglas de negocio
* Seleccionar automáticamente la mejor opción
* Permitir parametrización futura sin modificar código

---

## 2. 🧠 Contexto

SkyDropX puede regresar múltiples cotizaciones para un mismo envío:

* Estafeta
* FedEx
* DHL
* Paquetexpress
* Otros carriers futuros

Cada carrier puede variar en:

* precio
* tiempo entrega
* cobertura
* SLA
* disponibilidad

La selección NO debe quedar hardcodeada permanentemente.

---

## 3. 🔄 Flujo general

```text
Payload Estafeta
        ↓
SkyDropX Quotations
        ↓
Obtener lista de rates
        ↓
Aplicar reglas negocio
        ↓
Seleccionar mejor opción
        ↓
Generar shipment

---

## 4. 🔒 Protección de Envíos (Protect Shipment)

### 🔹 Objetivo

Permitir proteger envíos utilizando el valor declarado del paquete después de seleccionar carrier y crear el shipment.

---

## 5. 🔄 Flujo protección

```text
SkyDropX Quotations
        ↓
Seleccionar mejor carrier
        ↓
Create Shipment
        ↓
¿isInsurance = true?
        ↓ YES
Protect Shipment
        ↓
Obtener Label