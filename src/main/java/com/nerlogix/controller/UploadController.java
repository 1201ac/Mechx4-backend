package com.nerlogix.controller;

import com.nerlogix.config.NerlogixProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.File;

@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final NerlogixProperties nerlogixProperties;

    public UploadController(NerlogixProperties nerlogixProperties) {
        this.nerlogixProperties = nerlogixProperties;
    }

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> getUploadedFile(@PathVariable String filename) {
        if (filename == null || filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid filename");
        }

        File uploadDir = new File(nerlogixProperties.getUploads().getDir());
        File targetFile = new File(uploadDir, filename);

        if (!targetFile.exists() || !targetFile.isFile()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "File not found: " + filename);
        }

        MediaType mediaType = MediaType.IMAGE_JPEG;
        String lowerName = filename.toLowerCase();
        if (lowerName.endsWith(".png")) {
            mediaType = MediaType.IMAGE_PNG;
        } else if (lowerName.endsWith(".webp")) {
            mediaType = MediaType.parseMediaType("image/webp");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Content-Type-Options", "nosniff");
        headers.add(HttpHeaders.CACHE_CONTROL, "private, max-age=86400");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(mediaType)
                .body(new FileSystemResource(targetFile));
    }
}
