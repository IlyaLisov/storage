package io.github.ilyalisov.storage.service;

import com.google.api.gax.paging.Page;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.StorageClient;
import io.github.ilyalisov.storage.config.StorageFile;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of StorageService based on Firebase.
 */
public class FirebaseStorageServiceImpl implements StorageService {

    /**
     * Firebase Bucket.
     */
    private final Bucket bucket;

    /**
     * Returns bucket.
     *
     * @return bucket
     */
    public Bucket getBucket() {
        return bucket;
    }

    /**
     * Creates an object.
     *
     * @param credentials input stream with Firebase credentials from JSON file
     * @param bucket      Firebase bucket name
     */
    @SneakyThrows
    public FirebaseStorageServiceImpl(
            final InputStream credentials,
            final String bucket
    ) {
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(
                        GoogleCredentials.fromStream(
                                credentials
                        )
                )
                .build();
        FirebaseApp.initializeApp(options);
        this.bucket = StorageClient.getInstance()
                .bucket(bucket);
    }

    @Override
    public Optional<StorageFile> find(
            final String fileName
    ) {
        Blob result = bucket.get(fileName);
        if (result == null) {
            return Optional.empty();
        }
        StorageFile file = new StorageFile(
                fileName,
                result.getContentType(),
                new ByteArrayInputStream(result.getContent())
        );
        return Optional.of(file);
    }

    @Override
    public Optional<StorageFile> find(
            final String fileName,
            final Path path
    ) {
        Blob result = bucket.get(path.toString() + "/" + fileName);
        if (result == null) {
            return Optional.empty();
        }
        StorageFile file = new StorageFile(
                fileName,
                path,
                result.getContentType(),
                new ByteArrayInputStream(result.getContent())
        );
        return Optional.of(file);
    }

    @Override
    public List<StorageFile> findAll(
            final Path path,
            final io.github.ilyalisov.storage.config.Page page
    ) {
        Page<Blob> results = bucket.list(
                Storage.BlobListOption.prefix(path.toString() + "/")
        );
        List<Blob> pageBlobs = results.streamAll()
                .toList();
        List<Blob> blobs = pageBlobs
                .subList(
                        page.offset(),
                        Math.min(
                                page.offset() + page.getPageSize(),
                                pageBlobs.size()
                        )
                );
        return blobs.stream()
                .map(
                        (result) -> find(result.getName())
                                .get()
                )
                .collect(Collectors.toList());
    }

    @Override
    public boolean exists(
            final String fileName
    ) {
        return bucket.get(fileName) != null;
    }

    @Override
    public boolean exists(
            final String fileName,
            final Path path
    ) {
        return bucket.get(path.toString() + "/" + fileName) != null;
    }

    @Override
    @SneakyThrows
    public Path save(
            final StorageFile file
    ) {
        String path = file.getPath() != null ? file.getPath() + "/" : "";
        bucket.create(
                path + file.getFileName(),
                file.getInputStream().readAllBytes(),
                file.getContentType()
        );
        return Path.of(path, file.getFileName());
    }

    @Override
    public void delete(
            final String fileName
    ) {
        Blob file = bucket.get(fileName);
        if (file != null) {
            file.delete();
        }
    }

    @Override
    public void delete(
            final String fileName,
            final Path path
    ) {
        Blob file = bucket.get(path.toString() + "/" + fileName);
        if (file != null) {
            file.delete();
        }
    }

    @Override
    public void delete(
            final Path path
    ) {
        Page<Blob> blobs = bucket.list(
                Storage.BlobListOption.prefix(path + "/")
        );
        blobs.streamAll().forEach(Blob::delete);
    }

}
