# Storage

[![Lines-of-Code](https://tokei.rs/b1/github/ilyalisov/storage)](https://github.com/ilyalisov/storage)
[![Hits-of-Code](https://hitsofcode.com/github/ilyalisov/storage?branch=master)](https://hitsofcode.com/github/ilyalisov/storage/view?branch=master)
[![mvn](https://github.com/ilyalisov/storage/actions/workflows/maven-build.yml/badge.svg)](https://github.com/ilyalisov/storage/actions/workflows/maven-build.yml)

[![codecov](https://codecov.io/gh/IlyaLisov/storage/graph/badge.svg?token=OJR6TFQ2qr)](https://codecov.io/gh/IlyaLisov/storage)

This repository is an open-source Java library for fast and convenient storing
and accessing data in your Java applications.

Currently, we support MinIO and Firebase.

## Content:

* [How to use](#how-to-use)
    * [Instantiate a service](#instantiate-a-service)
    * [Save file](#save-file)
    * [Delete file](#delete-file)
    * [If file exists](#check-if-file-exists)
    * [Get file](#get-file)
* [How to contribute](#how-to-contribute)

## How to use

At first, you need to install this library.

With Maven add dependency to your `pom.xml`.

```xml
<dependency>
    <groupId>io.github.ilyalisov</groupId>
    <artifactId>storage</artifactId>
    <version>0.1.0</version>
</dependency>
```

This library provides simple and convenient usage.

### Instantiate a service

You need to create `MinIOStorageService` object and pass login data for MinIO
to the constructor.

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

### Save file

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

### Delete file

You can delete file by its name, name and path, and you can delete entire folder
by path.

```java
public class Main {
    public static void main(String[] args) {
        storageService.delete("file.txt");
        storageService.delete("file.txt", Path.of("folder"));
        storageService.delete(Path.of("folder"));
    }
}
```

### Check if file exists

You can check whether file exists or not.

```java
public class Main {
    public static void main(String[] args) {
        boolean file1 = storageService.exists("file.txt");
        boolean file2 = storageService.delete("file.txt", Path.of("folder"));
    }
}
```

### Get file

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

## How to contribute

See active issues at [issues page](https://github.com/ilyalisov/storage/issues)
