package io.github.ilyalisov.storage.service;


import io.github.ilyalisov.storage.config.Page;
import io.github.ilyalisov.storage.config.StorageFile;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * StorageService interface.
 */
public interface StorageService {

    /**
     * Finds file by its name.
     *
     * @param fileName name of file
     * @return optional of file
     */
    Optional<StorageFile> find(
            String fileName
    );

    /**
     * Finds file by its name.
     *
     * @param fileName name of file
     * @param path     path to file
     * @return optional of file
     */
    Optional<StorageFile> find(
            String fileName,
            Path path
    );

    /**
     * Finds all files in folder.
     *
     * @param path path to folder
     * @param page pagination of results
     * @return list of files
     */
    List<StorageFile> findAll(
            Path path,
            Page page
    );

    /**
     * Checks if file with name exists.
     *
     * @param fileName name of file
     * @return true - if file exists, false - otherwise
     */
    boolean exists(
            String fileName
    );

    /**
     * Checks if file with name and path exists.
     *
     * @param fileName name of file
     * @param path     path to file
     * @return true - if file exists, false - otherwise
     */
    boolean exists(
            String fileName,
            Path path
    );

    /**
     * Saves file to storage.
     *
     * @param file file to be saved
     * @return relative path to file
     */
    Path save(
            StorageFile file
    );

    /**
     * Deletes file from storage.
     *
     * @param fileName name of file to be deleted
     */
    void delete(
            String fileName
    );

    /**
     * Deletes file with path from storage.
     *
     * @param fileName name of file to be deleted
     * @param path     path to file
     */
    void delete(
            String fileName,
            Path path
    );

    /**
     * Deletes all folder from storage.
     *
     * @param path path to folder to be deleted
     */
    void delete(
            Path path
    );

    /**
     * Creates file name from path and actual name.
     *
     * @param path     path to folder
     * @param fileName name of file
     * @return file name with path
     */
    default String fileName(
            final Path path,
            final String fileName
    ) {
        if (path == null) {
            return fileName;
        }
        if (fileName == null) {
            return path.toString();
        }
        return path + "/" + fileName;
    }

}
