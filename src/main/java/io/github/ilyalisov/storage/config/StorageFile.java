package io.github.ilyalisov.storage.config;

import lombok.Getter;
import lombok.Setter;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.Optional;

/**
 * File class.
 */
@Getter
@Setter
public class StorageFile {

    /**
     * Path to store file in.
     */
    private Path path;

    /**
     * Name of file. Must ends with extension.
     */
    private String fileName;

    /**
     * Content type of file.
     */
    private String contentType;

    /**
     * InputStream with file data.
     */
    private InputStream inputStream;

    /**
     * Creates an object.
     *
     * @param fileName    name of file
     * @param contentType content type
     * @param inputStream input stream with file data
     */
    public StorageFile(
            final String fileName,
            final String contentType,
            final InputStream inputStream
    ) {
        this(
                fileName,
                null,
                contentType,
                inputStream
        );
    }

    /**
     * Creates an object.
     *
     * @param path        path to file
     * @param fileName    name of file
     * @param contentType content type
     * @param inputStream input stream with file data
     */
    public StorageFile(
            final String fileName,
            final Path path,
            final String contentType,
            final InputStream inputStream
    ) {
        if (!containsExtension(fileName)) {
            throw new IllegalArgumentException(
                    "Filename must contain extension."
            );
        }
        this.path = path;
        this.fileName = fileName;
        updatePath(fileName);
        this.contentType = contentType;
        this.inputStream = inputStream;
    }

    /**
     * Checks if file name contains extension.
     *
     * @param fileName name of file
     * @return true - if contains, false - otherwise
     */
    private boolean containsExtension(
            final String fileName
    ) {
        return Optional.of(fileName)
                .filter(f -> f.contains("."))
                .map(f -> f.substring(fileName.lastIndexOf(".") + 1))
                .isPresent();
    }

    private void updatePath(
            final String fileName
    ) {
        Path newPath;
        if (this.path == null) {
            newPath = Path.of("", fileName);
        } else {
            newPath = Path.of(path.toString(), fileName);
        }
        this.path = newPath.getParent();
        this.fileName = newPath.getFileName().toString();

    }

}
