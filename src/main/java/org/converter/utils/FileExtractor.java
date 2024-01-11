package org.converter.utils;

import org.springframework.web.multipart.MultipartFile;

public class FileExtractor {
    public static String extractFormat(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename != null && originalFilename.contains(".")) {
            String[] parts = originalFilename.split("\\.");
            return parts[parts.length - 1].toLowerCase();
        }

        return null;
    }

    public static String extractName(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();

        if (originalFilename != null && originalFilename.contains(".")) {
            String[] parts = originalFilename.split("\\.");
            return parts[0];
        }

        return originalFilename;
    }
}
