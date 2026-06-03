package com.creditienda.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ShopifyOrderMapper {

    private static final Logger logger = LoggerFactory.getLogger(ShopifyOrderMapper.class);

    private static final Set<String> CAMPOS_LISTA_VACIA = Set.of(
            "line_item_groups", "discount_codes", "shipping_lines",
            "note_attributes", "fulfillments", "refunds", "returns");

    // ❌ Campos prohibidos en el nivel raíz
    private static final Set<String> CAMPOS_PROHIBIDOS_RAIZ = Set.of(
            "email_marketing_consent", "sms_marketing_consent",
            "middle_name", "phones", "multipass_identifier", "sales_line_item_group_id");

    // ❌ Campos prohibidos dentro de 'customer'
    private static final Set<String> CAMPOS_PROHIBIDOS_CUSTOMER = Set.of("customer_locale");

    // ✅ Subcampos válidos dentro de 'customer'
    private static final Set<String> CAMPOS_CUSTOMER_ESPERADOS = Set.of(
            "id", "email", "phone", "note", "verified_email",
            "multipass_identifier", "currency", "tax_exempt", "tax_exemptions",
            "admin_graphql_api_id", "default_address", "created_at",
            "updated_at", "first_name", "last_name", "state");

    public static Map<String, Object> transformar(Map<String, Object> orden) {
        Map<String, Object> registro = new LinkedHashMap<>();
        Set<String> camposEsperados = ShopifyPlantillaCampos.obtenerCamposEsperados();

        for (String key : camposEsperados) {
            if (CAMPOS_PROHIBIDOS_RAIZ.contains(key)) {
                continue; // ❌ excluir campo raíz
            }

            Object valor = orden.get(key);

            // 🔁 Derivaciones si el campo está ausente o null
            if (valor == null) {
                switch (key) {
                    case "current_subtotal_price":
                        valor = orden.get("subtotal_price");
                        break;
                    case "current_total_price":
                        valor = orden.get("total_price");
                        break;
                    case "current_total_tax":
                        valor = orden.get("total_tax");
                        break;
                    case "current_total_discounts":
                        valor = orden.get("total_discounts");
                        break;
                    case "current_subtotal_price_set":
                        valor = orden.get("subtotal_price_set");
                        break;
                    case "current_total_price_set":
                        valor = orden.get("total_price_set");
                        break;
                    case "current_total_tax_set":
                        valor = orden.get("total_tax_set");
                        break;
                    case "current_total_discounts_set":
                        valor = orden.get("total_discounts_set");
                        break;
                    case "current_shipping_price_set":
                        valor = orden.get("total_shipping_price_set");
                        break;
                    case "number":
                        valor = orden.get("order_number");
                        break;
                }
            }

            // 🧹 Normalizar listas vacías si vienen como null
            if (CAMPOS_LISTA_VACIA.contains(key) && !(valor instanceof List)) {
                valor = new ArrayList<>();
            }

            // 🧼 Filtrar subcampos de 'customer'
            if ("customer".equals(key)) {
                Map<String, Object> customerOriginal = (Map<String, Object>) orden.get("customer");
                Map<String, Object> customerFiltrado = new LinkedHashMap<>();

                for (String subKey : CAMPOS_CUSTOMER_ESPERADOS) {
                    if (!CAMPOS_PROHIBIDOS_CUSTOMER.contains(subKey)) {
                        customerFiltrado.put(subKey, customerOriginal != null ? customerOriginal.get(subKey) : null);
                    }
                }

                registro.put("customer", customerFiltrado);
                continue;
            }
            // 🔗 Ajuste: agregar sales_line_item_group_id dentro de cada line_item
            if (registro.containsKey("line_items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) registro.get("line_items");
                for (Map<String, Object> item : items) {
                    if (!item.containsKey("sales_line_item_group_id")) {
                        Object groupId = item.get("sales_line_item_group_id");
                        if (groupId == null) {
                            groupId = orden.get("sales_line_item_group_id"); // fallback desde orden
                        }
                        item.put("sales_line_item_group_id", groupId);
                    }
                }
            }

            registro.put(key, valor != null ? valor : null);
        }

        return registro;
    }
}