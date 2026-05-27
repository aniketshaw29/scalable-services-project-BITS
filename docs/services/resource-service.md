# Resource Service

**Port:** 4081
**Database:** `resource_db`
**Role:** Upload, store, and serve files (slides, posters, notes) associated with events.

---

## Overview

Resource Service provides file storage for campus events. Organisers and students can upload files up to 10MB per upload. Files are stored on the local filesystem (or a mounted volume in Docker) using a `StorageService` abstraction that can be swapped for S3 in production without changing the controller or service layer.

Each file is assigned a unique `storageKey` (`UUID + _ + sanitized filename`) to prevent collisions. File metadata (name, type, size, uploader, event) is stored in PostgreSQL; the actual bytes live on disk.

---

## Domain Model

### Entity: `Resource`

| Field | Type | Notes |
|-------|------|-------|
| `id` | Long | Auto-generated |
| `eventId` | Long | Event this file belongs to (optional) |
| `uploadedBy` | String | User identifier |
| `fileName` | String | Original filename |
| `fileType` | String | MIME type |
| `fileSize` | Long | Bytes |
| `storageKey` | String | **Unique** — `<UUID>_<sanitized-name>` |
| `description` | String | Max 500 chars, optional |
| `uploadedAt` | LocalDateTime | |

---

## API Endpoints

### Upload File
```
POST /api/resources/upload
Content-Type: multipart/form-data

Form fields:
  eventId     (optional)
  uploadedBy  (required)
  description (optional)
  file        (required, max 10MB)

201 Created:
{
  "id": 1,
  "fileName": "slides.pdf",
  "fileType": "application/pdf",
  "fileSize": 1048576,
  "storageKey": "3fa85f64-5717-4562-b3fc-2c963f66af12_slides.pdf",
  "uploadedAt": "..."
}
413 Payload Too Large: file exceeds 10MB limit
```

### Get Resource Metadata
```
GET /api/resources/{id}

200 OK: { ...resource metadata }
404 Not Found
```

### List Resources for Event
```
GET /api/resources/event/{eventId}

200 OK: [ { ...resource }, ... ]
```

### Download File
```
GET /api/resources/{id}/download

200 OK: <binary file data>
Content-Type: <original file MIME type>
Content-Disposition: attachment; filename="slides.pdf"
404 Not Found
```

### Delete Resource
```
DELETE /api/resources/{id}

204 No Content (file removed from disk + DB row deleted)
404 Not Found
```

---

## Storage Abstraction

`StorageService` wraps all filesystem operations. To swap to S3, replace this bean:

```java
@Service
public class StorageService {
    private final Path uploadDir;

    public StorageService(@Value("${resource.upload-dir:uploads}") String dir) throws IOException {
        this.uploadDir = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    public void store(String storageKey, MultipartFile file) throws IOException { ... }
    public byte[] load(String storageKey) throws IOException { ... }
    public void delete(String storageKey) throws IOException { ... }
}
```

---

## File Size Limit

The service enforces a **10MB limit** at both the Spring multipart layer and in application code.
Files exceeding the limit return `413 Payload Too Large`:

```json
{ "status": 413, "error": "Payload Too Large", "message": "File too large. Maximum size is 10MB." }
```

---

## Configuration

```yaml
server:
  port: 4081
spring:
  datasource:
    url: ${DB_URL:jdbc:postgresql://localhost:5432/resource_db}
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

resource:
  upload-dir: ${UPLOAD_DIR:uploads}
  max-file-size-mb: 10

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URL:http://localhost:4070/eureka/}
```

---

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_URL` | `jdbc:postgresql://localhost:5432/resource_db` | JDBC URL |
| `DB_USERNAME` | `postgres` | |
| `DB_PASSWORD` | `postgres` | |
| `UPLOAD_DIR` | `uploads` | Filesystem path for uploaded files |
| `EUREKA_URL` | `http://localhost:4070/eureka/` | |

In Docker Compose, `UPLOAD_DIR` is set to `/app/uploads` which is backed by the `resource-uploads` named volume.

---

## Tests

11 automated integration tests covering:
- Upload file (success)
- Get metadata by ID (success and 404)
- List by event
- Download file (correct content, headers)
- File size limit (>10MB returns 413)
- Delete resource
- 404 after deletion

Tests write to `${java.io.tmpdir}/campus-eventhub-test-uploads` to avoid polluting the working directory.

Run: `cd resource-service && mvn test`

---

## Technology

- Spring Boot 3.2.5, Spring Data JPA, PostgreSQL
- Spring `MultipartFile` for upload handling
- Local filesystem storage (StorageService abstraction — S3-swappable)
- No RabbitMQ, no Feign clients
