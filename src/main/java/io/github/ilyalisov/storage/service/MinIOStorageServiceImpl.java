package io.github.ilyalisov.storage.service;

import io.github.ilyalisov.storage.config.Page;
import io.github.ilyalisov.storage.config.StorageFile;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import lombok.SneakyThrows;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of StorageService based on MinIO.
 */
public class MinIOStorageServiceImpl implements StorageService {

    /**
     * MinIO client.
     */
    private final MinioClient client;

    /**
     * MinIO bucket.
     */
    private final String bucket;

    /**
     * Returns client.
     *
     * @return client
     */
    public MinioClient getClient() {
        return client;
    }

    /**
     * Creates an object.
     *
     * @param serverURL URL of server
     * @param accessKey access key for MinIO
     * @param secretKey secret key for MinIO
     * @param bucket    bucket to store files in
     */
    @SneakyThrows
    public MinIOStorageServiceImpl(
            final String serverURL,
            final String accessKey,
            final String secretKey,
            final String bucket
    ) {
        this.client = MinioClient.builder()
                .endpoint(serverURL)
                .credentials(
                        accessKey,
                        secretKey
                )
                .build();
        this.bucket = bucket;
        boolean found =
                client.bucketExists(BucketExistsArgs.builder()
                        .bucket(bucket)
                        .build());
        if (!found) {
            client.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build());
        }
    }

    @Override
    @SneakyThrows
    public Optional<StorageFile> find(
            final String fileName
    ) {
        try (GetObjectResponse result = client.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .build()
        )) {
            StatObjectResponse stat = client.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .build()
            );
            StorageFile file = new StorageFile(
                    fileName,
                    stat.contentType(),
                    new ByteArrayInputStream(result.readAllBytes())
            );
            return Optional.of(file);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    @SneakyThrows
    public Optional<StorageFile> find(
            final String fileName,
            final Path path
    ) {
        return find(fileName(path, fileName));
    }

    @Override
    @SneakyThrows
    public List<StorageFile> findAll(
            final Path path,
            final Page page
    ) {
        Iterable<Result<Item>> response = client.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(path.toString())
                        .recursive(true)
                        .build()
        );
        List<Item> items = new ArrayList<>();
        response.forEach(itemResult -> {
            try {
                items.add(itemResult.get());
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        List<Item> pageItem = items.subList(
                page.offset(),
                Math.min(
                        page.offset() + page.getPageSize(),
                        items.size()
                )
        );
        List<StorageFile> result = new ArrayList<>();
        for (Item item : pageItem) {
            InputStream objectStream = client.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(item.objectName())
                            .build()
            );
            String contentType = client.statObject(
                            StatObjectArgs.builder()
                                    .bucket(bucket)
                                    .object(item.objectName())
                                    .build()
                    )
                    .contentType();
            StorageFile storageFile = new StorageFile(
                    item.objectName(),
                    path,
                    contentType,
                    objectStream
            );
            result.add(storageFile);
            objectStream.close();
        }
        return result;
    }

    @Override
    public boolean exists(
            final String fileName
    ) {
        try {
            client.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileName)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean exists(
            final String fileName,
            final Path path
    ) {
        return exists(fileName(path, fileName));
    }

    @Override
    @SneakyThrows
    public Path save(
            final StorageFile file
    ) {
        client.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .stream(
                                file.getInputStream(),
                                file.getInputStream().available(),
                                -1
                        )
                        .object(fileName(file.getPath(), file.getFileName()))
                        .contentType(file.getContentType())
                        .build()
        );
        return Path.of(fileName(file.getPath(), file.getFileName()));
    }

    @Override
    @SneakyThrows
    public void delete(
            final String fileName
    ) {
        client.removeObject(
                RemoveObjectArgs.builder()
                        .bucket(bucket)
                        .object(fileName)
                        .build()
        );
    }

    @Override
    @SneakyThrows
    public void delete(
            final String fileName,
            final Path path
    ) {
        delete(fileName(path, fileName));
    }

    @Override
    @SneakyThrows
    public void delete(
            final Path path
    ) {
        Iterable<Result<Item>> response = client.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .prefix(path.toString())
                        .recursive(true)
                        .build()
        );
        for (Result<Item> item : response) {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(item.get().objectName())
                            .build()
            );
        }
    }

}
