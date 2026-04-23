# MinIO Storage

## Overview

MinIO is used as object storage for file uploads and presigned URL generation.
The Go client is initialized once at startup via a package-level singleton (`MinioClient`).

Source: [`minIO-storage/config.go`](../minIO-storage/config.go)

---

## Environment Variables

| Variable    | Purpose                              |
|-------------|--------------------------------------|
| `END_POINT`  | MinIO server hostname/URL            |
| `ACCESS_KEY` | MinIO access key (username)          |
| `SECRET_KEY` | MinIO secret key (password)          |
| `BUCKET`     | Default bucket name for file access  |

Set in deployment secrets or `.env`. Loaded in [`main.go:244-246`](../main.go#L244).

---

## Functions

### `InitMinIO(endpoint, accessKeyID, secretAccessKey string) error`
Initializes the global `MinioClient` singleton. Called once at server startup.
- SSL enabled, TLS verification on.
- Calls `log.Fatalln` on failure (process exits).

### `UploadFileStorage(bucketName, objectName string, reader io.Reader, objectSize int64, contentType string, ctx context.Context) error`
Uploads a file to MinIO.
- Auto-creates the bucket if it does not exist.
- Overwrites existing objects silently.
- Sets `Cache-Control: no-cache, no-store, must-revalidate`.
- Adds `x-amz-meta-uploaded-at` metadata (RFC3339 timestamp).

### `GetFileURL(objectFolder, file string, ctx context.Context) (string, error)`
Returns a presigned GET URL valid for **20 minutes**.
- Object path: `objectFolder/file`
- Bucket read from `os.Getenv("BUCKET")`.

### `DownloadFile(ctx context.Context, objectFolder, file string) error`
Fetches an object from MinIO. Currently a stub — content is not returned.

---

## Spring Boot Equivalent

### Dependency (`pom.xml`)
```xml
<dependency>
    <groupId>io.minio</groupId>
    <artifactId>minio</artifactId>
    <version>8.5.9</version>
</dependency>
```

### Client Configuration (`MinioConfig.java`)
```java
@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

### `application.yml`
```yaml
minio:
  endpoint: ${END_POINT}
  access-key: ${ACCESS_KEY}
  secret-key: ${SECRET_KEY}
  bucket: ${BUCKET}
```

### `GetFileURL` equivalent (`MinioStorageService.java`)
```java
@Service
public class MinioStorageService {

    @Value("${minio.bucket}")
    private String bucket;

    private final MinioClient minioClient;

    public MinioStorageService(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    public String getFileURL(String objectFolder, String file) throws Exception {
        String objectName = objectFolder + "/" + file;

        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucket)
                .object(objectName)
                .expiry(20, TimeUnit.MINUTES)
                .build()
        );
    }
}
```

### Go → Spring Boot mapping

| Go | Spring Boot |
|----|-------------|
| `os.Getenv("BUCKET")` | `@Value("${minio.bucket}")` |
| `path.Join(objectFolder, file)` | `objectFolder + "/" + file` |
| `PresignedGetObject(..., time.Minute*20, ...)` | `.expiry(20, TimeUnit.MINUTES)` |
| `presignedURL.String()` | return value is already a `String` |