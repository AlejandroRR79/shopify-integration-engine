package com.creditienda.controller.log;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private static final Logger log = LogManager.getLogger(LogController.class);
    private static final int MAX_LINES = 200; // últimas N líneas

    @GetMapping("/tail")
    public ResponseEntity<List<String>> tailLog() {
        log.info("➡️ Entrando a /api/logs/tail");

        Path logFile = Paths.get("logs/creditienda.log");
        if (!logFile.toFile().exists()) {
            log.warn("❌ No se encontró creditienda.log en {}", logFile.toAbsolutePath());
            return ResponseEntity.status(404)
                    .body(List.of("No se encontró creditienda.log en " + logFile.toAbsolutePath()));
        }

        try {
            List<String> tail = readLastLines(logFile.toString(), MAX_LINES, StandardCharsets.UTF_8);
            log.info("📄 Devolviendo últimas {} líneas del log", tail.size());
            return ResponseEntity.ok(tail);
        } catch (Exception eUtf8) {
            log.warn("⚠️ Error con UTF-8, intentando ISO-8859-1");
            try {
                List<String> tail = readLastLines(logFile.toString(), MAX_LINES, Charset.forName("ISO-8859-1"));
                log.info("📄 Devolviendo últimas {} líneas del log en ISO-8859-1", tail.size());
                return ResponseEntity.ok(tail);
            } catch (Exception eIso) {
                log.error("💥 Error al leer log en cualquier charset", eIso);
                return ResponseEntity.status(500)
                        .body(List.of("Error al leer log: " + eIso.getMessage()));
            }
        }
    }

    /**
     * Lee las últimas N líneas de un archivo usando RandomAccessFile.
     */
    private List<String> readLastLines(String filePath, int maxLines, Charset charset) throws IOException {
        List<String> result = new ArrayList<>();
        try (RandomAccessFile file = new RandomAccessFile(filePath, "r")) {
            long fileLength = file.length();
            long pos = fileLength - 1;
            int lineCount = 0;
            StringBuilder sb = new StringBuilder();

            while (pos >= 0 && lineCount < maxLines) {
                file.seek(pos);
                int ch = file.read();
                if (ch == '\n') {
                    if (sb.length() > 0) {
                        result.add(0, new String(sb.reverse().toString().getBytes(), charset));
                        sb.setLength(0);
                        lineCount++;
                    }
                } else {
                    sb.append((char) ch);
                }
                pos--;
            }
            if (sb.length() > 0 && lineCount < maxLines) {
                result.add(0, new String(sb.reverse().toString().getBytes(), charset));
            }
        }
        return result;
    }
}