package com.group3.cafeteria_system.service;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.Locale;
import java.util.UUID;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ImageStorageService {

    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;
    private static final long MAX_IMAGE_PIXELS = 12_000_000;

    private final Path uploadRoot;

    public ImageStorageService(@Value("${app.upload-dir:uploads}") String uploadDirectory) {
        this.uploadRoot = Path.of(uploadDirectory).toAbsolutePath().normalize();
    }

    public Path getUploadRoot() {
        return uploadRoot;
    }

    public String storeMenuItemImage(MultipartFile image) throws IOException {
        return store(image, "menu");
    }

    public String storeProfilePicture(MultipartFile image) throws IOException {
        return store(image, "profiles");
    }

    public void delete(String imageUrl) throws IOException {
        if (imageUrl == null || !imageUrl.startsWith("/uploads/")) {
            return;
        }

        Path imagePath = uploadRoot.resolve(imageUrl.substring("/uploads/".length()))
                .normalize();
        if (!imagePath.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid stored image path.");
        }
        Files.deleteIfExists(imagePath);
    }

    private String store(MultipartFile image, String folder) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Choose an image to upload.");
        }
        if (image.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Images must be 5 MB or smaller.");
        }

        String extension = validateImage(image);
        Path directory = uploadRoot.resolve(folder).normalize();
        if (!directory.startsWith(uploadRoot)) {
            throw new IllegalArgumentException("Invalid image storage location.");
        }
        Files.createDirectories(directory);

        String filename = UUID.randomUUID() + "." + extension;
        Path destination = directory.resolve(filename);
        try (var input = image.getInputStream()) {
            Files.copy(input, destination);
        } catch (IOException exception) {
            Files.deleteIfExists(destination);
            throw exception;
        }
        return "/uploads/" + folder + "/" + filename;
    }

    private String validateImage(MultipartFile image) throws IOException {
        try (var source = image.getInputStream();
             ImageInputStream input = ImageIO.createImageInputStream(source)) {
            if (input == null) {
                throw new IllegalArgumentException("The selected file is not a supported image.");
            }

            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw new IllegalArgumentException("Use a JPEG, PNG, or GIF image.");
            }

            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String extension = switch (format) {
                    case "jpg", "jpeg" -> "jpg";
                    case "png" -> "png";
                    case "gif" -> "gif";
                    default -> throw new IllegalArgumentException("Use a JPEG, PNG, or GIF image.");
                };

                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
                    throw new IllegalArgumentException("Image dimensions must not exceed 12 megapixels.");
                }

                BufferedImage decoded = reader.read(0);
                if (decoded == null) {
                    throw new IllegalArgumentException("The selected file is not a valid image.");
                }
                decoded.flush();
                return extension;
            } finally {
                reader.dispose();
            }
        }
    }
}
