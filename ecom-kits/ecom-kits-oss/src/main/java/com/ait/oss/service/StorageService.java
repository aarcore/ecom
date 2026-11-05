package com.ait.oss.service;

import java.io.InputStream;

/**
 * Minimal object storage abstraction: enough for product images, banners and
 * avatars without dragging in a cloud SDK contract.
 */
public interface StorageService {

    /**
     * Stores the stream and returns the public URL clients should use.
     *
     * @param key      object key relative to the bucket/root, e.g. {@code product/2026/11/uuid.png}
     * @param stream   payload
     * @param size     payload length in bytes
     * @param mimeType content type
     */
    String store(String key, InputStream stream, long size, String mimeType);

    /** Deletes an object; missing objects are ignored. */
    void delete(String key);

    /** @return public URL for an already stored key. */
    String publicUrl(String key);

    /** Human readable backend name, surfaced on the admin settings page. */
    String backend();
}
