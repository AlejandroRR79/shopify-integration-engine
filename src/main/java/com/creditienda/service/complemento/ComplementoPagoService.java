package com.creditienda.service.complemento;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ComplementoPagoService {

    private static final Logger log = LogManager.getLogger(ComplementoPagoService.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public Map<String, List<Map<String, Object>>> procesarConDetalle(MultipartFile file) {
        List<Map<String, Object>> errores = new ArrayList<>();
        List<Map<String, Object>> procesados = new ArrayList<>();

        try (InputStream is = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = 0;

            Row headerRow = sheet.getRow(0);
            Map<String, Integer> headerIndex = new HashMap<>();
            for (Cell cell : headerRow) {
                headerIndex.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }
            log.info("Iniciando");

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;
                rowCount++;

                String facturaPagada = getCellValueAsString(row, headerIndex.get("Factura Pagada"));
                Timestamp fechaPago = getCellValueAsTimestamp(row, headerIndex.get("Fecha de Pago"));
                String serieFactura = getCellValueAsString(row, headerIndex.get("Serie de factura"));
                String numeroFactura = getCellValueAsString(row, headerIndex.get("Numero de factura"));
                String uuidExcel = getCellValueAsString(row, headerIndex.get("UUID"));
                String numeroOrden = getCellValueAsString(row, headerIndex.get("Numero de Orden"));
                String estatus = getCellValueAsString(row, headerIndex.get("Estatus"));
                Timestamp fechaCarga = getCellValueAsTimestamp(row, headerIndex.get("Fecha Carga"));
                String errorCarga = getCellValueAsString(row, headerIndex.get("Error Carga"));
                String indicacionCarga = getCellValueAsString(row, headerIndex.get("Indicacion Carga"));

                // log.info("Fila {} -> Orden={}, UUID={}, FacturaPagada={}", rowCount,
                // numeroOrden, uuidExcel,facturaPagada);

                if (facturaPagada == null || !facturaPagada.trim().equalsIgnoreCase("SI")) {
                    errores.add(Map.of(
                            "fila", rowCount,
                            "numeroOrden", numeroOrden,
                            "uuidExcel", uuidExcel,
                            "mensajeError", "Factura Pagada no es 'SI', se descarta"));
                    continue;
                }

                if (uuidExcel == null || uuidExcel.trim().isEmpty()) {
                    errores.add(Map.of(
                            "fila", rowCount,
                            "numeroOrden", numeroOrden,
                            "mensajeError", "UUID vacío en Excel, no se envía a BD"));
                    continue;
                }

                // 🚨 Invocar SP con SimpleJdbcCall y declarar parámetro de salida
                SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                        .withProcedureName("sp_insert_complemento_pago_staging")
                        .declareParameters(
                                new SqlParameter("FacturaPagada", Types.NVARCHAR),
                                new SqlParameter("FechaPago", Types.TIMESTAMP),
                                new SqlParameter("SerieFactura", Types.NVARCHAR),
                                new SqlParameter("NumeroFactura", Types.NVARCHAR),
                                new SqlParameter("UUIDExcel", Types.NVARCHAR),
                                new SqlParameter("NumeroOrden", Types.NVARCHAR),
                                new SqlParameter("Estatus", Types.NVARCHAR),
                                new SqlParameter("FechaCarga", Types.TIMESTAMP),
                                new SqlParameter("ErrorCarga", Types.NVARCHAR),
                                new SqlParameter("IndicacionCarga", Types.NVARCHAR),
                                new SqlOutParameter("MensajeError", Types.NVARCHAR) // 👈 salida
                        );

                Map<String, Object> out = call.execute(
                        new MapSqlParameterSource()
                                .addValue("FacturaPagada", facturaPagada)
                                .addValue("FechaPago", fechaPago)
                                .addValue("SerieFactura", serieFactura)
                                .addValue("NumeroFactura", numeroFactura)
                                .addValue("UUIDExcel", uuidExcel)
                                .addValue("NumeroOrden", numeroOrden)
                                .addValue("Estatus", estatus)
                                .addValue("FechaCarga", fechaCarga)
                                .addValue("ErrorCarga", errorCarga)
                                .addValue("IndicacionCarga", indicacionCarga));

                String mensajeError = (String) out.get("MensajeError");

                if (mensajeError != null && !mensajeError.isEmpty()) {
                    errores.add(Map.of(
                            "fila", rowCount,
                            "numeroOrden", numeroOrden,
                            "uuidExcel", uuidExcel,
                            "mensajeError", mensajeError));
                } else {
                    procesados.add(Map.of(
                            "fila", rowCount,
                            "numeroOrden", numeroOrden,
                            "uuidExcel", uuidExcel,
                            "mensaje", "Procesado correctamente"));
                }
            }

            log.info("Procesados {} registros desde Excel", rowCount);
            log.info("Correctos: {}, Errores: {}", procesados.size(), errores.size());

        } catch (Exception e) {
            log.error("Error al procesar archivo Excel", e);
            throw new RuntimeException("Error al procesar archivo Excel", e);
        }

        Map<String, List<Map<String, Object>>> resultado = new HashMap<>();
        resultado.put("procesados", procesados);
        resultado.put("errores", errores);
        return resultado;
    }

    // 🔹 Métodos auxiliares para leer celdas como String
    private String getCellValueAsString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null)
            return "";
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                            .format(cell.getDateCellValue());
                } else {
                    return new BigDecimal(cell.getNumericCellValue()).toPlainString();
                }
            case BOOLEAN:
                return Boolean.toString(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }

    // 🔹 Métodos auxiliares para leer celdas como Timestamp
    private Timestamp getCellValueAsTimestamp(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null)
            return null;

        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            return new Timestamp(cell.getDateCellValue().getTime());
        }

        if (cell.getCellType() == CellType.STRING) {
            try {
                Date parsed = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                        .parse(cell.getStringCellValue());
                return new Timestamp(parsed.getTime());
            } catch (Exception e) {
                log.warn("No se pudo parsear fecha en celda: {}", cell.getStringCellValue());
                return null;
            }
        }
        return null;
    }
}