package com.ait.oss.service;

import com.ait.oss.config.StorageProperties;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/**
 * Writes objects beneath a local directory. The bundled nginx serves that
 * directory at {@code /uploads}, and Spring Boot also maps it as a static
 * resource location for development.
 */
@Slf4j
public class LocalStorageService implements StorageService {

    private final StorageProperties properties;

    public LocalStorageService(StorageProperties properties) {
        this.properties = properties;
        try {
            Files.createDirectories(root());
        } catch (IOException ex) {
            throw new IllegalStateException("cannot create storage root " + root(), ex);
        }
    }

    private Path root() {
        return Paths.get(properties.getLocal().getRoot()).toAbsolutePath().normalize();
    }

    @Override
    public String store(String key, InputStream stream, long size, String mimeType) {
        Path target = resolve(key);
        try {
            Files.createDirectories(target.getParent());
            Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("stored object {} ({} bytes)", key, size);
            return publicUrl(key);
        } catch (IOException ex) {
            throw new IllegalStateException("failed to store object " + key, ex);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolve(key));
        } catch (IOException ex) {
            log.warn("failed to delete object {}: {}", key, ex.getMessage());
        }
    }

    @Override
    public String publicUrl(String key) {
        String base = properties.getPublicBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + key;
    }

    @Override
    public String backend() {
        return "local:" + root();
    }

    /** Guards against {@code ../} traversal in the object key. */
    private Path resolve(String key) {
        Path resolved = root().resolve(key).normalize();
        if (!resolved.startsWith(root())) {
            throw new IllegalArgumentException("illegal object key: " + key);
        }
        return resolved;
    }
}
