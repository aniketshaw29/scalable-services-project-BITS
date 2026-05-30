package com.campuseventhub.resource.controller;

import com.campuseventhub.resource.dto.ResourceResponse;
import com.campuseventhub.resource.service.ResourceService;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
@Validated
public class ResourceController {

    private final ResourceService resourceService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ResourceResponse> upload(
            @RequestParam @NotNull(message = "eventId is required")
            @Positive(message = "eventId must be a positive number") Long eventId,
            @RequestParam(required = false) String uploadedBy,
            @RequestParam(required = false) String description,
            @RequestParam("file") @NotNull(message = "file is required") MultipartFile file)
            throws IOException {
        return ResponseEntity.status(201).body(
                resourceService.uploadResource(eventId, uploadedBy, description, file));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResourceResponse> getById(
            @PathVariable @Positive(message = "id must be a positive number") Long id) {
        return ResponseEntity.ok(resourceService.getResourceById(id));
    }

    @GetMapping("/event/{eventId}")
    public ResponseEntity<List<ResourceResponse>> getByEvent(
            @PathVariable @Positive(message = "eventId must be a positive number") Long eventId) {
        return ResponseEntity.ok(resourceService.getResourcesByEvent(eventId));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(
            @PathVariable @Positive(message = "id must be a positive number") Long id)
            throws IOException {
        ResourceResponse meta = resourceService.getResourceById(id);
        byte[] data = resourceService.downloadResource(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        meta.getFileType() != null ? meta.getFileType() : MediaType.APPLICATION_OCTET_STREAM_VALUE))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.getFileName() + "\"")
                .body(data);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable @Positive(message = "id must be a positive number") Long id)
            throws IOException {
        resourceService.deleteResource(id);
        return ResponseEntity.noContent().build();
    }
}
