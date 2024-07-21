package io.github.ilyalisov.storage.service;

import io.github.ilyalisov.storage.config.Page;
import io.github.ilyalisov.storage.config.StorageFile;
import lombok.Getter;
import lombok.SneakyThrows;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of StorageService based on S3.
 */
public class S3StorageServiceImpl implements StorageService {

    /**
     * S3 bucket.
     */
    private final String bucket;

    /**
     * S3 client.
     */
    @Getter
    private final S3Client client;

    /**
     * Creates an object.
     *
     * @param accessKey access key for S3
     * @param secretKey secret key for S3
     * @param region    region of S3
     * @param url       URL of S3
     * @param bucket    name of the bucket
     */
    public S3StorageServiceImpl(
            final String accessKey,
            final String secretKey,
            final String region,
            final String url,
            final String bucket
    ) {
        this.bucket = bucket;
        this.client = S3Client.builder()
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(
                                        accessKey,
                                        secretKey
                                )
                        )
                )
                .endpointOverride(URI.create(url))
                .region(Region.of(region))
                .build();
        try {
            CreateBucketRequest build = CreateBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            client.createBucket(build);
        } catch (Exception ignored) {
        }
    }

    @Override
    public Optional<StorageFile> find(
            final String fileName
    ) {
        try {
            var getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();
            InputStream objectStream = client.getObject(getObjectRequest);
            var request = HeadObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();
            var response = client.headObject(request);
            StorageFile file = new StorageFile(
                    fileName,
                    response.contentType(),
                    new ByteArrayInputStream(objectStream.readAllBytes())
            );
            return Optional.of(file);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<StorageFile> find(
            final String fileName,
            final Path path
    ) {
        return find(fileName(path, fileName));
    }

    @Override
    public List<StorageFile> findAll(
            final Path path,
            final Page page
    ) {
        var request = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(path.toString())
                .build();
        var response = client.listObjectsV2(request);
        List<S3Object> items = response.contents();
        List<S3Object> pageItems = items.subList(
                page.offset(),
                Math.min(page.offset() + page.getPageSize(), items.size())
        );
        List<StorageFile> result = new ArrayList<>();
        for (S3Object item : pageItems) {
            InputStream objectStream = client.getObject(
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(item.key())
                            .build()
            );
            var headObjectResponse = client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(item.key())
                            .build()
            );
            StorageFile storageFile = new StorageFile(
                    item.key().substring(path.toString().length()),
                    path,
                    headObjectResponse.contentType(),
                    objectStream
            );
            result.add(storageFile);
        }
        return result;
    }

    @Override
    public boolean exists(
            final String fileName
    ) {
        try {
            client.headObject(
                    HeadObjectRequest.builder()
                            .bucket(bucket)
                            .key(fileName)
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
        var request = PutObjectRequest.builder()
                .bucket(bucket)
                .key(fileName(file.getPath(), file.getFileName()))
                .contentType(file.getContentType())
                .build();
        client.putObject(
                request,
                RequestBody.fromBytes(file.getInputStream().readAllBytes())
        );
        return Path.of(fileName(file.getPath(), file.getFileName()));
    }

    @Override
    public void delete(
            final String fileName
    ) {
        var request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(fileName)
                .build();
        client.deleteObject(request);
    }

    @Override
    public void delete(
            final String fileName,
            final Path path
    ) {
        delete(fileName(path, fileName));
    }

    @Override
    public void delete(
            final Path path
    ) {
        var listRequest = ListObjectsV2Request.builder()
                .bucket(bucket)
                .prefix(path.toString())
                .build();
        var response = client.listObjectsV2(listRequest);
        for (S3Object item : response.contents()) {
            var request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(item.key())
                    .build();
            client.deleteObject(request);
        }
    }

}
