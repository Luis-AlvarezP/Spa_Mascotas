package com.spamascotas.spa_mascotas_api.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    @GetMapping("/carnets/{filename:.+}")
    public ResponseEntity<Resource> serveCarnet(@PathVariable String filename) {
        return serveFile("carnets", filename);
    }

    @GetMapping("/fotos/{filename:.+}")
    public ResponseEntity<Resource> serveFoto(@PathVariable String filename) {
        return serveFile("fotos", filename);
    }

    @GetMapping("/productos/{filename:.+}")
    public ResponseEntity<Resource> serveProductoImagen(@PathVariable String filename) {
        return serveFile("productos", filename);
    }

    @GetMapping("/galeria/{filename:.+}")
    public ResponseEntity<Resource> serveGaleria(@PathVariable String filename) {
        return serveFile("galeria", filename);
    }

    private ResponseEntity<Resource> serveFile(String subdir, String filename) {
        Path filePath = Paths.get(uploadsDir, subdir, filename).toAbsolutePath();
        Resource resource = new FileSystemResource(filePath);

        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType;
        try {
            String contentType = Files.probeContentType(filePath);
            mediaType = contentType != null
                    ? MediaType.parseMediaType(contentType)
                    : MediaType.APPLICATION_OCTET_STREAM;
        } catch (IOException e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok().contentType(mediaType).body(resource);
    }
}
