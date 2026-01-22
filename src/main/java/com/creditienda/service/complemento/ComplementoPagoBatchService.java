package com.creditienda.service.complemento;

import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
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
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.microsoft.sqlserver.jdbc.SQLServerDataTable;

@Service
public class ComplementoPagoBatchService {

    private static final Logger log = LogManager.getLogger(ComplementoPagoBatchService.class);

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final int MAX_ROWS = 10_000;
    private static final int MAX_DETALLE = 100;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SuppressWarnings("unchecked")
    public Map<String, Object> procesarBatch(MultipartFile file) {

        long inicio = System.currentTimeMillis();

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("El archivo excede el tamaño máximo permitido (5MB)");
        }

        List<Map<String, Object>> errores = new ArrayList<>();
        List<Map<String, Object>> procesados = new ArrayList<>();
        List<Map<String, Object>> yaExistentes = new ArrayList<>();
        Map<String, Object> resumen = new HashMap<>();

        try (InputStream is = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);

            if (sheet.getLastRowNum() > MAX_ROWS) {
                throw new IllegalArgumentException("El archivo excede el máximo de filas permitido (" + MAX_ROWS + ")");
            }

            // ================= HEADERS =================
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("El archivo no contiene encabezados");
            }

            Map<String, Integer> headerIndex = new HashMap<>();
            for (Cell cell : headerRow) {
                headerIndex.put(cell.getStringCellValue().trim(), cell.getColumnIndex());
            }

            validarHeaders(headerIndex);

            // ================= TVP =================
            SQLServerDataTable pagosTable = new SQLServerDataTable();
            pagosTable.addColumnMetadata("FacturaPagada", java.sql.Types.NVARCHAR);
            pagosTable.addColumnMetadata("FechaPago", java.sql.Types.TIMESTAMP);
            pagosTable.addColumnMetadata("SerieFactura", java.sql.Types.NVARCHAR);
            pagosTable.addColumnMetadata("NumeroFactura", java.sql.Types.NVARCHAR);
            pagosTable.addColumnMetadata("UUIDExcel", java.sql.Types.NVARCHAR);
            pagosTable.addColumnMetadata("NumeroOrden", java.sql.Types.NVARCHAR);
            pagosTable.addColumnMetadata("Estatus", java.sql.Types.NVARCHAR);
            pagosTable.addColumnMetadata("FechaCarga", java.sql.Types.TIMESTAMP);
            pagosTable.addColumnMetadata("ErrorCarga", java.sql.Types.NVARCHAR);
            pagosTable.addColumnMetadata("IndicacionCarga", java.sql.Types.NVARCHAR);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null)
                    continue;

                pagosTable.addRow(
                        clean(getCellValueAsString(row, headerIndex.get("Factura Pagada"))),
                        getCellValueAsTimestamp(row, headerIndex.get("Fecha de Pago")),
                        clean(getCellValueAsString(row, headerIndex.get("Serie de factura"))),
                        clean(getCellValueAsString(row, headerIndex.get("Numero de factura"))),
                        clean(getCellValueAsString(row, headerIndex.get("UUID"))),
                        clean(getCellValueAsString(row, headerIndex.get("Numero de Orden"))),
                        clean(getCellValueAsString(row, headerIndex.get("Estatus"))),
                        getCellValueAsTimestamp(row, headerIndex.get("Fecha Carga")),
                        clean(getCellValueAsString(row, headerIndex.get("Error Carga"))),
                        clean(getCellValueAsString(row, headerIndex.get("Indicacion Carga"))));
            }

            JdbcTemplate localJdbc = new JdbcTemplate(jdbcTemplate.getDataSource());
            localJdbc.setQueryTimeout(120);

            SimpleJdbcCall call = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName("sp_insert_complemento_pago_batch");

            Map<String, Object> out = call.execute(
                    new MapSqlParameterSource().addValue("Pagos", pagosTable));

            // ================= RESULT SET 1 =================
            List<Map<String, Object>> rs1 = (List<Map<String, Object>>) out.get("#result-set-1");
            for (Map<String, Object> row : rs1) {
                String estado = (String) row.get("Estado");
                if ("Procesado correctamente".equals(estado)) {
                    procesados.add(row);
                } else if (estado != null && estado.startsWith("Error:")) {
                    if (errores.size() < MAX_DETALLE)
                        errores.add(row);
                } else if ("Ya existente en COMPLEMENTO_PAGO_STAGING".equals(estado)) {
                    if (yaExistentes.size() < MAX_DETALLE)
                        yaExistentes.add(row);
                }
            }

            // ================= RESULT SET 2 =================
            List<Map<String, Object>> rs2 = (List<Map<String, Object>>) out.get("#result-set-2");
            if (rs2 != null && !rs2.isEmpty()) {
                resumen = rs2.get(0);
            }

        } catch (Exception e) {
            throw new RuntimeException("Error al procesar archivo Excel", e);
        }

        long fin = System.currentTimeMillis();
        log.info("Batch '{}' procesado en {} ms", file.getOriginalFilename(), (fin - inicio));

        // ================= RESPUESTA =================
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("archivo", file.getOriginalFilename());
        resultado.put("totalRegistros",
                resumen.getOrDefault("totalRegistros",
                        procesados.size() + errores.size() + yaExistentes.size()));
        resultado.put("correctos", resumen.getOrDefault("correctos", procesados.size()));
        resultado.put("errores", resumen.getOrDefault("errores", errores.size()));
        resultado.put("yaExistentes", resumen.getOrDefault("yaExistentes", yaExistentes.size()));
        resultado.put("procesados", procesados);
        resultado.put("erroresDetalle", errores);
        resultado.put("yaExistentesDetalle", yaExistentes);

        return resultado;
    }

    // ================= VALIDACIONES =================
    private void validarHeaders(Map<String, Integer> headerIndex) {
        List<String> requeridos = List.of(
                "Factura Pagada",
                "Fecha de Pago",
                "Serie de factura",
                "Numero de factura",
                "UUID",
                "Numero de Orden",
                "Estatus");

        for (String h : requeridos) {
            if (!headerIndex.containsKey(h)) {
                throw new IllegalArgumentException("Falta columna obligatoria: " + h);
            }
        }
    }

    private String clean(String v) {
        return v == null ? null : v.trim();
    }

    // ================= CELDAS =================
    private String getCellValueAsString(Row row, int index) {
        Cell cell = row.getCell(index);
        if (cell == null)
            return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(cell.getDateCellValue())
                    : new BigDecimal(cell.getNumericCellValue()).toPlainString();
            case BOOLEAN -> Boolean.toString(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            default -> "";
        };
    }

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
                log.warn("No se pudo parsear fecha: {}", cell.getStringCellValue());
            }
        }
        return null;
    }
}
