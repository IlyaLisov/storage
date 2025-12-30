# Storage

[![mvn](https://github.com/ilyalisov/storage/actions/workflows/maven-build.yml/badge.svg)](https://github.com/ilyalisov/storage/actions/workflows/maven-build.yml)
[![codecov](https://codecov.io/gh/IlyaLisov/storage/graph/badge.svg?token=OJR6TFQ2qr)](https://codecov.io/gh/IlyaLisov/storage)
[![Hits-of-Code](https://hitsofcode.com/github/ilyalisov/storage?branch=master)](https://hitsofcode.com/github/ilyalisov/storage/view?branch=master)
[![Lines-of-Code](https://tokei.rs/b1/github/ilyalisov/storage)](https://github.com/ilyalisov/storage)

A comprehensive Java storage library for fast and convenient storing and accessing data in your Java applications. Currently supports MinIO and Firebase storage services with a unified API.

## Table of Contents

- [Features](#features)
- [Quick Start](#quick-start)
  - [Installation](#installation)
- [Core Components](#core-components)
  - [Storage Service Interface](#storage-service-interface)
  - [Storage File](#storage-file)
  - [Page Pagination](#page-pagination)
- [Supported Providers](#supported-providers)
  - [MinIO Storage Service](#minio-storage-service)
  - [Firebase Storage Service](#firebase-storage-service)
- [Usage Examples](#usage-examples)
  - [Instantiate a Service](#instantiate-a-service)
  - [Save File](#save-file)
  - [Delete File](#delete-file)
  - [Check If File Exists](#check-if-file-exists)
  - [Get File](#get-file)
- [License](#license)
- [Contributing](#contributing)

## Features

- **Unified Storage API** - Consistent interface for multiple storage providers
- **Multi-Provider Support** - Currently supports MinIO and Firebase with extensible architecture
- **File Management** - Complete file operations: save, delete, find, list
- **Pagination Support** - Built-in pagination for listing files in directories
- **Flexible Path Handling** - Support for file operations with and without path prefixes
- **Stream-Based Operations** - Efficient memory usage with InputStream-based file handling
- **Type Safety** - Strongly typed file metadata and operations

## Quick Start

Get your file storage operations up and running quickly with a unified API that works across multiple storage providers.

### Installation

**Maven:**

Add to your `pom.xml`:

```xml
<dependency>
    <groupId>io.github.ilyalisov</groupId>
    <artifactId>storage</artifactId>
    <version>0.1.0</version>
</dependency>
```

**Gradle:**

Add to your `build.gradle` or `build.gradle.kts`:

```kotlin
implementation("io.github.ilyalisov:storage:0.1.0")
```

## Core Components

### Storage Service Interface

The foundation for all storage operations with a unified API:

```java
public interface StorageService {
    Path save(StorageFile file);
    Path save(StorageFile file, Path path);
    void delete(String fileName);
    void delete(String fileName, Path path);
    void delete(Path path);
    boolean exists(String fileName);
    boolean exists(String fileName, Path path);
    Optional<StorageFile> find(String fileName);
    Optional<StorageFile> find(String fileName, Path path);
    List<StorageFile> findAll(Path path, Page page);
}
```

### Storage File

Represents a file with metadata and content:

```java
public class StorageFile {
    private String fileName;
    private String contentType;
    private InputStream inputStream;
    private long size;
    // ... getters, setters, and validation
}
```

### Page Pagination

Simple pagination support for listing files:

```java
public class Page {
    private int pageNumber;
    private int pageSize;
    // ... getters, setters, and validation
}
```

## Supported Providers

### MinIO Storage Service

MinIO implementation for high-performance object storage:

```java
StorageService storageService = new MinIOStorageServiceImpl(
    "http://localhost:9000",  // host
    "rootUser",               // username
    "rootPassword",           // password
    "bucket"                  // bucket name
);
```

### Firebase Storage Service

Firebase Cloud Storage implementation:

```java
InputStream credentials = new ByteArrayInputStream(
    System.getenv("FIREBASE_SECRET").getBytes()
);
StorageService storageService = new FirebaseStorageServiceImpl(
    credentials,              // Firebase service account credentials
    "firebase-bucket"        // Firebase storage bucket
);
```

## Usage Examples

### Instantiate a Service

You need to create `MinIOStorageService` object and pass login data for MinIO to the constructor.

```java
public class Main {
    public static void main(String[] args) {
        String host = "http://localhost:9000";
        String rootUser = "rootUser";
        String rootPassword = "rootPassword";
        String bucket = "bucket";

        StorageService storageService = new MinIOStorageServiceImpl(
                host,
                rootUser,
                rootPassword,
                bucket
        );
    }
}
```

And for Firebase it is a bit different.

```java
public class Main {
    public static void main(String[] args) {
        InputStream inputStream = new ByteArrayInputStream(
                System.getenv("FIREBASE_SECRET").getBytes()
        );
        String bucket = System.getenv("FIREBASE_BUCKET");

        StorageService storageService = new FirebaseStorageServiceImpl(
                inputStream,
                bucket
        );
    }
}
```

After, you can call available methods and use library.

### Save File

To save file just call method save. It will return path to saved file.

```java
public class Main {
    public static void main(String[] args) {
        StorageFile file = new StorageFile(
                "fileName",
                "text/plain",
                new ByteArrayInputStream("...")
        );

        Path path = storageService.save(file);
    }
}
```

### Delete File

You can delete file by its name, name and path, and you can delete entire folder by path.

```java
public class Main {
    public static void main(String[] args) {
        storageService.delete("file.txt");
        storageService.delete("file.txt", Path.of("folder"));
        storageService.delete(Path.of("folder"));
    }
}
```

### Check If File Exists

You can check whether file exists or not.

```java
public class Main {
    public static void main(String[] args) {
        boolean file1 = storageService.exists("file.txt");
        boolean file2 = storageService.exists("file.txt", Path.of("folder"));
    }
}
```

### Get File

You can get file from storage by calling corresponding methods.

```java
public class Main {
    public static void main(String[] args) {
        Optional<StorageFile> file = storageService.find("file.txt");
        Optional<StorageFile> file2 = storageService.find("file.txt", Path.of("folder"));
        List<StorageFile> files = storageService.findAll(
                Path.of("folder"),
                new Page(0, 10)
        );
    }
}
```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

We welcome contributions! Please feel free to submit issues and enhancement requests.

To contribute, make a fork and open a pull request. You can find issues [here](https://github.com/ilyalisov/storage/issues).

Make sure you follow the project's code style.
