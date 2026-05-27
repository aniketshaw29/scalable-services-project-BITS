package com.campuseventhub.resource.service;

import com.campuseventhub.resource.dto.ResourceResponse;
import com.campuseventhub.resource.entity.Resource;
import com.campuseventhub.resource.exception.FileSizeLimitExceededException;
import com.campuseventhub.resource.exception.ResourceNotFoundException;
import com.campuseventhub.resource.repository.ResourceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ResourceService {

    private final ResourceRepository resourceRepository;
    private final StorageService storageService;

    @Value("${resource.max-file-size-mb:10}")
    private long maxFileSizeMb;

    @Transactional
    public ResourceResponse uploadResource(Long eventId, String uploadedBy, String description,
                                           MultipartFile file) throws IOException {
        long maxBytes = maxFileSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new FileSizeLimitExceededException(maxFileSizeMb);
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        String storageKey = UUID.randomUUID() + "_" + originalName.replaceAll("[^a-zA-Z0-9._-]", "_");

        storageService.store(storageKey, file);

        Resource resource = Resource.builder()
                .eventId(eventId)
                .uploadedBy(uploadedBy)
                .fileName(originalName)
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .storageKey(storageKey)
                .description(description)
                .build();

        return toResponse(resourceRepository.save(resource));
    }

    public ResourceResponse getResourceById(Long id) {
        return resourceRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
    }

    public List<ResourceResponse> getResourcesByEvent(Long eventId) {
        return resourceRepository.findByEventId(eventId)
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    public byte[] downloadResource(Long id) throws IOException {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
        return storageService.load(resource.getStorageKey());
    }

    @Transactional
    public void deleteResource(Long id) throws IOException {
        Resource resource = resourceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Resource not found: " + id));
        storageService.delete(resource.getStorageKey());
        resourceRepository.delete(resource);
    }

    private ResourceResponse toResponse(Resource r) {
        return ResourceResponse.builder()
                .id(r.getId())
                .eventId(r.getEventId())
                .uploadedBy(r.getUploadedBy())
                .fileName(r.getFileName())
                .fileType(r.getFileType())
                .fileSize(r.getFileSize())
                .storageKey(r.getStorageKey())
                .description(r.getDescription())
                .uploadedAt(r.getUploadedAt())
                .build();
    }
}
