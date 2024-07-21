package io.github.ilyalisov.storage.service;

import io.github.ilyalisov.storage.config.Page;
import io.github.ilyalisov.storage.config.StorageFile;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class S3StorageServiceImplTests {

    @Mock
    private S3Client s3Client;

    private S3StorageServiceImpl s3StorageService;

    private final String bucketName = "test-bucket";

    @BeforeEach
    @SneakyThrows
    void setup() {
        MockitoAnnotations.openMocks(this);
        s3StorageService = new S3StorageServiceImpl(
                "access-key",
                "secret-key",
                "us-west-2",
                "http://localhost:4566",
                bucketName
        );
        Field clientField = S3StorageServiceImpl.class.getDeclaredField("client");
        clientField.setAccessible(true);
        clientField.set(s3StorageService, s3Client);
    }

    @Test
    void find() throws Exception {
        String fileName = "test.txt";
        byte[] content = "Hello, World!".getBytes();
        ResponseInputStream<GetObjectResponse> contentStream = new ResponseInputStream<>(
                GetObjectResponse.builder().build(),
                new ByteArrayInputStream(content)
        );
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(contentStream);
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentType("text/plain").build());
        Optional<StorageFile> result = s3StorageService.find(fileName);
        assertTrue(result.isPresent());
        assertEquals(fileName, result.get().getFileName());
        assertEquals("text/plain", result.get().getContentType());
        assertArrayEquals(content, result.get().getInputStream().readAllBytes());
    }

    @Test
    void findAll() {
        Path path = Path.of("some/path");
        S3Object s3Object1 = S3Object.builder().key("some/path/file1.txt").build();
        S3Object s3Object2 = S3Object.builder().key("some/path/file2.txt").build();
        ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
                .contents(Arrays.asList(s3Object1, s3Object2))
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);
        when(s3Client.getObject(any(GetObjectRequest.class)))
                .thenReturn(new ResponseInputStream<>(
                                GetObjectResponse.builder().build(),
                                new ByteArrayInputStream("test".getBytes())
                        )
                );
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().contentType("text/plain").build());
        List<StorageFile> result = s3StorageService.findAll(
                path,
                new Page(1, 10)
        );
        assertEquals(2, result.size());
        assertEquals("some/path/file1.txt", result.get(0).getPath() + "/" + result.get(0).getFileName());
        assertEquals("text/plain", result.get(0).getContentType());
        assertEquals("some/path/file2.txt", result.get(1).getPath() + "/" + result.get(1).getFileName());
        assertEquals("text/plain", result.get(1).getContentType());
    }

    @Test
    void save() {
        String fileName = "test.txt";
        byte[] content = "Hello, World!".getBytes();
        StorageFile storageFile = new StorageFile(fileName, "text/plain", new ByteArrayInputStream(content));
        Path result = s3StorageService.save(storageFile);
        assertEquals(Path.of(fileName), result);
        ArgumentCaptor<PutObjectRequest> requestCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(requestCaptor.capture(), any(RequestBody.class));
        PutObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals(bucketName, capturedRequest.bucket());
        assertEquals(fileName, capturedRequest.key());
        assertEquals("text/plain", capturedRequest.contentType());
    }

    @Test
    void delete() {
        String fileName = "test.txt";
        s3StorageService.delete(fileName);
        ArgumentCaptor<DeleteObjectRequest> requestCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(requestCaptor.capture());
        DeleteObjectRequest capturedRequest = requestCaptor.getValue();
        assertEquals(bucketName, capturedRequest.bucket());
        assertEquals(fileName, capturedRequest.key());
    }

    @Test
    void exists() {
        String fileName = "test.txt";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());
        boolean result = s3StorageService.exists(fileName);
        assertTrue(result);
    }

    @Test
    void doesNotExists() {
        String fileName = "test.txt";
        when(s3Client.headObject(any(HeadObjectRequest.class)))
                .thenThrow(NoSuchKeyException.builder().build());
        boolean result = s3StorageService.exists(fileName);
        assertFalse(result);
    }

    @Test
    void deleteWithPath() {
        String fileName = "test.txt";
        Path path = Path.of("some/path");
        s3StorageService.delete(fileName, path);
        verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
    }

    @Test
    void deletePath() {
        Path path = Path.of("some/path");
        S3Object s3Object1 = S3Object.builder().key("some/path/file1.txt").build();
        S3Object s3Object2 = S3Object.builder().key("some/path/file2.txt").build();
        ListObjectsV2Response listResponse = ListObjectsV2Response.builder()
                .contents(Arrays.asList(s3Object1, s3Object2))
                .build();
        when(s3Client.listObjectsV2(any(ListObjectsV2Request.class))).thenReturn(listResponse);
        s3StorageService.delete(path);
        verify(s3Client, times(2)).deleteObject(any(DeleteObjectRequest.class));
    }

}
