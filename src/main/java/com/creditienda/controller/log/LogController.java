package com.creditienda.controller.log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

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
                        result.add(0, new String(sb.reverse().toString().getBytes(charset), charset));
                        sb.setLength(0);
                        lineCount++;
                    }
                } else {
                    sb.append((char) ch);
                }
                pos--;
            }
            if (sb.length() > 0 && lineCount < maxLines) {
                result.add(0, new String(sb.reverse().toString().getBytes(charset), charset));
            }
        }
        return result;
    }

    @GetMapping("/download")
    public void downloadLog(HttpServletResponse response) throws IOException {

        log.info("⬇️ Entrando a /api/logs/download");

        Path logFile = Paths.get("logs/creditienda.log");
        File file = logFile.toFile();

        if (!file.exists()) {
            log.warn("❌ No se encontró creditienda.log en {}", logFile.toAbsolutePath());
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("No se encontró creditienda.log");
            return;
        }

        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + file.getName());
        response.setContentLengthLong(file.length());

        try (InputStream in = new FileInputStream(file);
                OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        log.info("✅ Log descargado correctamente");
    }

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> listLogs() throws IOException {
        log.info("Entrando a /api/logs/list");

        Path logsDir = Paths.get("logs");
        if (!Files.isDirectory(logsDir)) {
            return ResponseEntity.ok(List.of());
        }

        try (Stream<Path> paths = Files.list(logsDir)) {
            List<Map<String, Object>> files = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("creditienda"))
                    .map(path -> {
                        try {
                            LocalDateTime lastModified = LocalDateTime.ofInstant(
                                    Files.getLastModifiedTime(path).toInstant(),
                                    ZoneId.systemDefault());

                            return Map.<String, Object>of(
                                    "filename", path.getFileName().toString(),
                                    "sizeBytes", Files.size(path),
                                    "lastModified", lastModified.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                        } catch (IOException e) {
                            throw new IllegalStateException("Error al leer metadata de " + path.getFileName(), e);
                        }
                    })
                    .sorted(Comparator.comparing(
                            entry -> (String) entry.get("lastModified"),
                            Comparator.reverseOrder()))
                    .toList();

            return ResponseEntity.ok(files);
        }
    }

    @GetMapping("/download/{filename}")
    public void downloadLogFile(
            @PathVariable String filename,
            HttpServletResponse response) throws IOException {

        log.info("Entrando a /api/logs/download/{}", filename);

        Path logsDir = Paths.get("logs").toAbsolutePath().normalize();
        Path logFile = logsDir.resolve(filename).normalize();

        if (!logFile.startsWith(logsDir)) {
            log.warn("Intento de path traversal en descarga de log: {}", filename);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Nombre de archivo invalido");
            return;
        }

        File file = logFile.toFile();
        if (!file.exists() || !file.isFile()) {
            log.warn("No se encontro archivo de log en {}", logFile);
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("No se encontro el archivo de log");
            return;
        }

        if (!filename.startsWith("creditienda")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Nombre de archivo no permitido");
            return;
        }

        if (!filename.endsWith(".log") && !filename.endsWith(".log.gz")) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Extension de archivo no soportada");
            return;
        }

        String downloadFilename = filename.endsWith(".log.gz")
                ? filename.substring(0, filename.length() - 3)
                : filename;

        response.setContentType("text/plain; charset=UTF-8");
        response.setHeader("Content-Disposition",
                "attachment; filename=" + downloadFilename);

        if (filename.endsWith(".log")) {
            response.setContentLengthLong(file.length());
        }

        try (InputStream in = openLogInputStream(file, filename);
                OutputStream out = response.getOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        log.info("Log descargado correctamente: {}", filename);
    }

    private InputStream openLogInputStream(File file, String filename) throws IOException {
        if (filename.endsWith(".log.gz")) {
            InputStream in = new FileInputStream(file);
            try {
                return new GZIPInputStream(in);
            } catch (IOException e) {
                in.close();
                throw e;
            }
        }
        return new FileInputStream(file);
    }

}
