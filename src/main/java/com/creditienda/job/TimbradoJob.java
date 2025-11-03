package com.creditienda.job;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.creditienda.model.timbrado.Cab;
import com.creditienda.model.timbrado.Det;
import com.creditienda.model.timbrado.Dir;
import com.creditienda.model.timbrado.Documento;
import com.creditienda.model.timbrado.Hdr;
import com.creditienda.model.timbrado.Infglo;
import com.creditienda.model.timbrado.Mon;
import com.creditienda.model.timbrado.Tex;
import com.creditienda.model.timbrado.TicketDoc;
import com.creditienda.model.timbrado.Tra;
import com.creditienda.service.TimbradoClient;
import com.creditienda.service.notificacion.NotificacionService;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@DisallowConcurrentExecution
public class TimbradoJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(TimbradoJob.class);

    private final TimbradoClient timbradoClient;

    @Autowired
    private NotificacionService notificacionService;

    @Value("${app.mail.notificacion.operacion}")
    private String correoNotificacion;

    public TimbradoJob(TimbradoClient timbradoClient) {
        this.timbradoClient = timbradoClient;
    }

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        logger.info("🚀 Ejecutando TimbradoJob...");

        try {
            Documento doc = generarDocumentoDummy();

            // Configurar ObjectMapper para ignorar campos nulos
            ObjectMapper mapper = new ObjectMapper();
            mapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

            // Serializar a JSON e imprimir
            String json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(doc);
            logger.info("📄 Documento generado (sin nulos):\n{}", json);

            // Enviar al PAC
            String respuesta1 = timbradoClient.timbrarJson(doc);
            logger.info("✅ Respuesta timbre: {}", respuesta1);

            // notificacionService.enviarFacturacion("El job de timbrado se ejecutó
            // correctamente.");

            // String respuesta2 = timbradoClient.timbrarXmlBase64(doc);
            // logger.info("✅ Respuesta integrador: {}", respuesta2);

        } catch (Exception e) {
            logger.error("❌ Error en TimbradoJob", e);
            throw new JobExecutionException(e);
        }

    }

    private Documento generarDocumentoDummy() {
        Documento doc = new Documento();

        // Cab
        Cab cab = new Cab();
        cab.setRfcEmisor("TRC661111721");
        cab.setRfcReceptor("XAXX010101000");
        cab.setTipoComprobante("I");
        cab.setVersionCfdi("4.0");
        doc.setCab(cab);

        // Hdr
        Hdr hdr = new Hdr();
        hdr.setIdFactura("03217117");
        hdr.setSerie("03217");
        hdr.setFolio("117");
        hdr.setFechaEmisionCfdi("2024-09-04 23:59:59");
        hdr.setFormaPago("04");
        hdr.setMetodoPago("PUE");
        hdr.setLugarExp("02300");
        hdr.setRegimenFisEmisor("624");
        hdr.setUsoCfdiReceptor("S01");
        hdr.setEjercicioFiscal("2024");
        hdr.setCentroCostos("AUTOBUS");
        hdr.setRegimenFisReceptor("616");
        hdr.setExportacion("01");
        doc.setHdr(hdr);

        // Mon
        Mon mon = new Mon();
        mon.setMoneda("MXN");
        mon.setTipoCambio(new BigDecimal("1.0"));
        mon.setSubtotal(new BigDecimal("3897.36"));
        mon.setTotal(new BigDecimal("4521.00"));
        mon.setTotalImpuestoTras(new BigDecimal("623.64"));
        mon.setPesoBruto(BigDecimal.ZERO);
        mon.setPesoNeto(BigDecimal.ZERO);
        doc.setMon(mon);

        // Dir
        Dir dir1 = new Dir();
        dir1.setCalificador("SE");
        dir1.setRfc("TRC661111721");
        dir1.setNombre("TURISMOS RAPIDOS CHIHUAHUA - ANAHUAC - CUAUHTEMOC");
        dir1.setCalle("PONIENTE 140");
        dir1.setNoExterior("859");
        dir1.setNoInterior("");
        dir1.setColonia("INDUSTRIAL VALLEJO");
        dir1.setMunicipio("Azcapotzalco");
        dir1.setEstado("Ciudad de México");
        dir1.setLocalidad("INDUSTRIAL VALLEJO");
        dir1.setPais("MEX");
        dir1.setCodigoPais("MEX");
        dir1.setCodigoPostal("02300");

        Dir dir2 = new Dir();
        dir2.setCalificador("BY");
        dir2.setRfc("XAXX010101000");
        dir2.setNombre("PUBLICO EN GENERAL");
        dir2.setCodigoPostal("02300");

        doc.setDir(List.of(dir1, dir2));

        // Tex
        Tex tex = new Tex();
        tex.setCalificador("DDOC");
        tex.setTexto("Venta fin de día 04/09/2024 03 - 1/1");
        doc.setTex(List.of(tex));

        // Det
        List<Det> detalles = new ArrayList<>();
        List<Tra> impuestosGlobales = new ArrayList<>();

        // Solo agrego uno como ejemplo, puedes repetir para todos si lo deseas
        Det det = new Det();
        det.setNoId("296707");
        det.setCantidad(new BigDecimal("1.00"));
        det.setUnidadMedida("E54");
        det.setDescripcion("VENTA DE PASAJE AL PUBLICO EN GENERAL");
        det.setValorUnitario(new BigDecimal("139.22"));
        det.setImporte(new BigDecimal("139.22"));
        det.setCveProdServ("78111802");
        det.setCveUnidadMedida("ACT");
        det.setNoInterno("ZOOE1600122579373");
        det.setPosicionOc(0);
        det.setObjetoImpuesto("02");

        Tra tra = new Tra();
        tra.setBase(new BigDecimal("139.22"));
        tra.setImpuesto("002");
        tra.setTipoFactor("Tasa");
        tra.setTasaCuota(new BigDecimal("0.16"));
        tra.setImporte(new BigDecimal("22.28"));

        det.setDit(List.of(tra));
        detalles.add(det);
        impuestosGlobales.add(tra);

        doc.setDet(detalles);
        doc.setTra(impuestosGlobales);

        // Infglo
        Infglo infglo = new Infglo();
        infglo.setPeriodicidad("01");
        infglo.setMeses("09");
        infglo.setAnio(2024);
        doc.setInfglo(infglo);

        // Ticket
        TicketDoc ticket = new TicketDoc();
        ticket.setIdTicket(296707);
        ticket.setFacturaGlobal(1);
        ticket.setFolio("ZOOE1600122579373");
        ticket.setImporteEnCFDI(1);
        doc.setTicket(List.of(ticket));

        return doc;
    }

}