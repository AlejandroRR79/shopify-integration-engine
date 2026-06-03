package com.creditienda.service.notificacion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.creditienda.util.EmailUtil;

@Service
public class NotificacionService {

    private final EmailUtil emailUtil;

    @Value("${app.mail.notificacion.operacion}")
    private String correoNotificacion;

    @Value("${app.mail.error.operacion}")
    private String correoErrores;

    public NotificacionService(EmailUtil emailUtil) {
        this.emailUtil = emailUtil;
    }

    public void enviarConfirmacion(String mensaje) {
        emailUtil.enviar(correoNotificacion, "✅ Confirmación de operación", mensaje);
    }

    public void enviarError(String mensaje) {
        emailUtil.enviar(correoErrores, "❌ Error detectado", mensaje);
    }

    public void enviarFacturacion(String mensaje) {
        emailUtil.enviar(correoNotificacion, "📄 Flujo de facturación B2B", mensaje);
    }

    public void enviarResumen(String mensaje) {
        emailUtil.enviar(correoNotificacion, "📦 Resumen de sincronización Estafeta → B2B", mensaje);
    }

    public void enviarResumenProductos(String mensaje) {
        emailUtil.enviar(correoNotificacion, "📦 Resumen de actualización de productos Shopify", mensaje);
    }

}
