package io.github.ilyalisov.storage.service;

import io.github.ilyalisov.storage.config.StorageFile;
import lombok.SneakyThrows;
import org.apache.http.entity.ContentType;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
public class MinIOStorageServiceImplTests {

    private static MinIOStorageServiceImpl storageService;

    private static ClassLoader classLoader;

    @Container
    public static GenericContainer minio = new GenericContainer(
            DockerImageName.parse("minio/minio")
    )
            .withEnv("MINIO_ROOT_USER", "rootUser")
            .withEnv("MINIO_ROOT_PASSWORD", "rootPassword")
            .withCommand("server /minio --console-address :9090")
            .withExposedPorts(9000, 9090);

    @BeforeAll
    static void init() {
        classLoader = MinIOStorageServiceImplTests.class.getClassLoader();
    }

    @BeforeEach
    void setup() {
        storageService = new MinIOStorageServiceImpl(
                "http://" + minio.getHost() + ":" + minio.getMappedPort(9000),
                "rootUser",
                "rootPassword",
                "bucket"
        );
    }

    @Test
    @SneakyThrows
    void saveWithPath() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            StorageFile file = new StorageFile(
                    "file1.txt",
                    Path.of("test", "innerFolder"),
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount
            );
            Path path = storageService.save(file);
            assertNotNull(path);
        }
    }

    @Test
    @SneakyThrows
    void saveWithoutPath() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            StorageFile file = new StorageFile(
                    "file1.txt",
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount
            );
            Path path = storageService.save(file);
            assertNotNull(path);
        }
    }

    @Test
    @SneakyThrows
    void findExistingFileWithoutPath() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            byte[] bytes = serviceAccount.readAllBytes();
            StorageFile file = new StorageFile(
                    "file1.txt",
                    ContentType.TEXT_PLAIN.getMimeType(),
                    new ByteArrayInputStream(bytes)
            );
            storageService.save(file);
            Optional<StorageFile> savedFile = storageService.find(
                    "file1.txt"
            );
            assertFalse(savedFile.isEmpty());
            assertEquals(file.getFileName(), savedFile.get()
                    .getFileName());
            assertEquals(file.getPath(), savedFile.get()
                    .getPath());
            assertEquals(file.getContentType(), savedFile.get()
                    .getContentType());
            assertArrayEquals(
                    bytes,
                    savedFile.get().getInputStream().readAllBytes()
            );
        }
    }

    @Test
    @SneakyThrows
    void findNotExistingFileWithoutPath() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            StorageFile file = new StorageFile(
                    "file1.txt",
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount
            );
            storageService.save(file);
            Optional<StorageFile> savedFile = storageService.find(
                    "notExisting.txt"
            );
            assertTrue(savedFile.isEmpty());
        }
    }

    @Test
    @SneakyThrows
    void findExistingFileWithPath() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            Path path = Path.of(
                    "folder",
                    UUID.randomUUID().toString()
            );
            byte[] bytes = serviceAccount.readAllBytes();
            StorageFile file = new StorageFile(
                    "file1.txt",
                    path,
                    ContentType.TEXT_PLAIN.getMimeType(),
                    new ByteArrayInputStream(bytes)
            );
            storageService.save(file);
            Optional<StorageFile> savedFile = storageService.find(
                    "file1.txt",
                    path
            );
            assertTrue(savedFile.isPresent());
            assertEquals(file.getFileName(), savedFile.get()
                    .getFileName());
            assertEquals(file.getPath(), savedFile.get()
                    .getPath());
            assertEquals(file.getContentType(), savedFile.get()
                    .getContentType());
            assertArrayEquals(
                    bytes,
                    savedFile.get().getInputStream().readAllBytes()
            );
        }
    }

    @Test
    @SneakyThrows
    void findNotExistingFileWithPath() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            Path path = Path.of(
                    "folder",
                    UUID.randomUUID().toString()
            );
            StorageFile file = new StorageFile(
                    "file1.txt",
                    path,
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount
            );
            storageService.save(file);
            Optional<StorageFile> savedFile = storageService.find(
                    "notExisting.txt",
                    path
            );
            assertTrue(savedFile.isEmpty());
        }
    }

    @Test
    @SneakyThrows
    void findAllInPath() {
        try (InputStream serviceAccount1 = classLoader.getResourceAsStream(
                "file1.txt"
        );
             InputStream serviceAccount2 = classLoader.getResourceAsStream(
                     "file2.txt"
             )) {
            Path path = Path.of(
                    "folder",
                    UUID.randomUUID().toString()
            );
            StorageFile file1 = new StorageFile(
                    "file1.txt",
                    path,
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount1
            );
            storageService.save(file1);
            StorageFile file2 = new StorageFile(
                    "file2.txt",
                    path,
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount2
            );
            storageService.save(file2);
            List<StorageFile> files = storageService.findAll(path);
            assertEquals(2, files.size());
        }
    }

    @Test
    @SneakyThrows
    void findAllInNotExistingPath() {
        Path path = Path.of("folder", UUID.randomUUID().toString());
        List<StorageFile> files = storageService.findAll(path);
        assertEquals(0, files.size());
    }

    @Test
    @SneakyThrows
    void fileWithoutPathExists() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            StorageFile file = new StorageFile(
                    "file1.txt",
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount
            );
            storageService.save(file);
            boolean exists = storageService.exists("file1.txt");
            assertTrue(exists);
        }
    }

    @Test
    @SneakyThrows
    void fileWithPathExists() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            Path path = Path.of(
                    "folder",
                    UUID.randomUUID().toString()
            );
            StorageFile file = new StorageFile(
                    "file1.txt",
                    path,
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount
            );
            storageService.save(file);
            boolean exists = storageService.exists(
                    "file1.txt",
                    path
            );
            assertTrue(exists);
        }
    }

    @Test
    @SneakyThrows
    void deleteExistingFileWithoutPath() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            StorageFile file = new StorageFile(
                    "file1.txt",
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount
            );
            storageService.save(file);
            storageService.delete(
                    "file1.txt"
            );
            boolean exists = storageService.exists("file1.txt");
            assertFalse(exists);
        }
    }

    @Test
    @SneakyThrows
    void deleteNotExistingFileWithoutPath() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            StorageFile file = new StorageFile(
                    "file1.txt",
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount
            );
            storageService.save(file);
            storageService.delete(
                    "notExisting.txt"
            );
            boolean exists = storageService.exists("notExisting.txt");
            assertFalse(exists);
        }
    }

    @Test
    @SneakyThrows
    void deleteExistingFileWithPath() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            Path path = Path.of(
                    "folder",
                    UUID.randomUUID().toString()
            );
            StorageFile file = new StorageFile(
                    "file1.txt",
                    path,
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount
            );
            storageService.save(file);
            storageService.delete(
                    "file1.txt",
                    path
            );
            boolean exists = storageService.exists(
                    "file1.txt",
                    path
            );
            assertFalse(exists);
        }
    }

    @Test
    @SneakyThrows
    void deleteNotExistingFileWithPath() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            Path path = Path.of(
                    "folder",
                    UUID.randomUUID().toString()
            );
            StorageFile file = new StorageFile(
                    "file1.txt",
                    path,
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount
            );
            storageService.save(file);
            storageService.delete(
                    "notExisting.txt",
                    path
            );
            boolean exists = storageService.exists(
                    "notExisting.txt",
                    path
            );
            assertFalse(exists);
        }
    }

    @Test
    @SneakyThrows
    void deleteExistingFolder() {
        try (InputStream serviceAccount = classLoader.getResourceAsStream(
                "file1.txt"
        )) {
            Path path = Path.of(
                    "folder",
                    UUID.randomUUID().toString()
            );
            StorageFile file = new StorageFile(
                    "file1.txt",
                    path,
                    ContentType.TEXT_PLAIN.getMimeType(),
                    serviceAccount
            );
            storageService.save(file);
            storageService.delete(
                    path
            );
            boolean exists = storageService.exists(
                    "file1.txt",
                    path
            );
            assertFalse(exists);
        }
    }

    @Test
    @SneakyThrows
    void deleteNotExistingFolder() {
        Path path = Path.of("folder", UUID.randomUUID().toString());
        storageService.delete(
                path
        );
        boolean exists = storageService.exists(
                "file1.txt",
                path
        );
        assertFalse(exists);
    }

}
