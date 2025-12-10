package com.creditienda.service.complemento;

import java.io.InputStream;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ComplementoPagoService {

    private static final Logger log = LogManager.getLogger(ComplementoPagoService.class);

    public void procesar(MultipartFile file) {
        try (InputStream is = file.getInputStream();
                Workbook workbook = WorkbookFactory.create(is)) {

            Sheet sheet = workbook.getSheetAt(0);
            int rowCount = 0;

            for (Row row : sheet) {
                List<String> cells = new ArrayList<>();
                for (Cell cell : row) {
                    switch (cell.getCellType()) {
                        case STRING:
                            cells.add(cell.getStringCellValue());
                            break;
                        case NUMERIC:
                            if (DateUtil.isCellDateFormatted(cell)) {
                                Date date = cell.getDateCellValue();
                                String formatted = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(date);
                                cells.add(formatted);
                            } else {
                                BigDecimal bd = new BigDecimal(cell.getNumericCellValue());
                                cells.add(bd.toPlainString());
                            }
                            break;
                        case BOOLEAN:
                            cells.add(Boolean.toString(cell.getBooleanCellValue()));
                            break;
                        case FORMULA:
                            cells.add(cell.getCellFormula());
                            break;
                        default:
                            cells.add("");
                    }
                }
                rowCount++;
                // 🔑 Loguea cada fila completa
                log.info("Fila {}: {}", rowCount, cells);
            }

            log.info("Procesados {} registros desde Excel", rowCount);

        } catch (Exception e) {
            log.error("Error al procesar archivo Excel", e);
            throw new RuntimeException("Error al procesar archivo Excel", e);
        }
    }

    public void procesarSeleccionados(List<Map<String, Object>> registros) {
        int idx = 0;
        for (Map<String, Object> r : registros) {
            idx++;
            log.info("Registro seleccionado {}: {}", idx, r);
        }
        log.info("Procesados {} registros seleccionados", registros.size());
    }

    public void procesarSeleccionados(List<Map<String, Object>> registros) {
        // Aquí recibes los registros seleccionados ya normalizados desde el frontend
        registros.forEach(r -> {
            System.out.println("Registro seleccionado: " + r);
            // Guardar en BD o procesar según tu lógica
        });
    }

    private String obtenerValor(Cell cell) {
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> DateUtil.isCellDateFormatted(cell)
                    ? cell.getDateCellValue().toString()
                    : String.valueOf(cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK -> "";
            default -> "[tipo desconocido]";
        };
    }

    public void procesarSeleccionados(List<Map<String, Object>> registros) {
        for (int i = 0; i < registros.size(); i++) {
            Map<String, Object> fila = registros.get(i);
            log.info("Procesando fila seleccionada {}: {}", i + 1, fila);
            // Aquí implementas la lógica de negocio: guardar en BD, validar, etc.
        }
    }

}