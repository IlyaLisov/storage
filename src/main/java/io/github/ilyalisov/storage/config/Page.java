package io.github.ilyalisov.storage.config;

import lombok.Getter;

/**
 * Pagination object.
 */
@Getter
public class Page {

    /**
     * Number of page.
     */
    private final int page;

    /**
     * Size of page.
     */
    private final int pageSize;

    /**
     * Creates an object.
     *
     * @param page     page number from 1
     * @param pageSize size of page
     */
    public Page(
            final int page,
            final int pageSize
    ) {
        if (page <= 0 || pageSize < 0) {
            throw new IllegalArgumentException(
                    "Page and page size must be positive number."
            );
        }
        this.page = page;
        this.pageSize = pageSize;
    }

    /**
     * Returns offset of page.
     *
     * @return offset
     */
    public int offset() {
        return (page - 1) * pageSize;
    }

}
