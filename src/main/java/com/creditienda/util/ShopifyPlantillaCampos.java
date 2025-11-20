package com.creditienda.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

public class ShopifyPlantillaCampos {

    public static Set<String> obtenerCamposEsperados() {
        return new LinkedHashSet<>(Arrays.asList(
                // Identificadores y estado
                "id", "admin_graphql_api_id", "app_id", "confirmation_number", "confirmed",
                "created_at", "processed_at", "updated_at", "financial_status", "fulfillment_status",
                "order_number", "name", "currency", "presentment_currency", "source_name",
                "source_identifier", "merchant_business_entity_id", "order_status_url", "token",
                "note", "tags", "test",

                // Totales
                "subtotal_price", "total_price", "total_tax", "total_weight", "total_discounts",
                "total_tip_received", "total_outstanding",

                // Sets de precios
                "subtotal_price_set", "total_price_set", "total_tax_set", "total_discounts_set",
                "current_subtotal_price_set", "current_total_price_set", "current_total_tax_set",
                "current_total_discounts_set", "current_shipping_price_set", "total_shipping_price_set",
                "total_cash_rounding_payment_adjustment_set", "total_cash_rounding_refund_adjustment_set",
                "total_line_items_price", "total_line_items_price_set", "current_total_duties_set",
                "current_total_additional_fees_set", "original_total_duties_set", "original_total_additional_fees_set",

                // Cliente y direcciones
                "customer", "billing_address", "shipping_address",

                // Productos y líneas
                "line_items", "line_item_groups", "shipping_lines", "discount_codes",
                "discount_applications", "fulfillments", "refunds", "returns", "note_attributes",
                "tax_lines", "duties_included", "tax_exempt", "taxes_included", "estimated_taxes",
                "payment_gateway_names", "payment_terms",

                // Extras y metadata
                "browser_ip", "client_details", "checkout_id", "checkout_token", "contact_email",
                "email", "phone", "user_id", "device_id", "landing_site", "landing_site_ref",
                "location_id", "referring_site", "source_url", "po_number", "reference",
                "sales_line_item_group_id", "multipass_identifier", "customer_locale",
                "merchant_of_record_app_id", "closed_at", "cancelled_at", "cancel_reason",
                "buyer_accepts_marketing",

                // Derivados
                "current_subtotal_price", "current_total_price", "current_total_tax",
                "current_total_discounts", "number", "cart_token"));
    }
}