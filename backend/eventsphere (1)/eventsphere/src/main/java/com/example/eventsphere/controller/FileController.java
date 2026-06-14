package com.example.eventsphere.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;

@RestController
@Slf4j
public class FileController {

    @GetMapping("/generated-cards/**")
    public ResponseEntity<Resource> serveCard(HttpServletRequest request) {
        String path = request.getRequestURI().replace("/generated-cards/", "");
        File file = new File("./generated-cards/" + path);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        String contentType = path.endsWith(".pdf") ? "application/pdf" : "image/png";

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    @GetMapping("/uploads/**")
    public ResponseEntity<Resource> serveUpload(HttpServletRequest request) {
        String path = request.getRequestURI().replace("/uploads/", "");
        File file = new File("./uploads/" + path);

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        // Detect correct content type from file extension
        String contentType = detectContentType(file);

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    @GetMapping("/api/files/qr/{invitationId}")
    public ResponseEntity<Resource> serveQr(@PathVariable String invitationId) {
        File file = new File("./generated-cards/qrcodes/" + invitationId + ".png");

        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + file.getName() + "\"")
                .body(resource);
    }

    private String detectContentType(File file) {
        String name = file.getName().toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) return "image/jpeg";
        if (name.endsWith(".png"))  return "image/png";
        if (name.endsWith(".gif"))  return "image/gif";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".pdf"))  return "application/pdf";
        // fallback — let the browser figure it out from the file itself
        try {
            String probed = Files.probeContentType(file.toPath());
            return probed != null ? probed : "application/octet-stream";
        } catch (Exception e) {
            return "application/octet-stream";
        }
    }
}