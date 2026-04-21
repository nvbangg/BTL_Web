package com.nvbangg.fashonshop.service;

import com.nvbangg.fashonshop.common.dto.ErrorDetail;
import com.nvbangg.fashonshop.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class ImageService {

    private static final String INVALID_REQUEST_MESSAGE = "Dữ liệu không hợp lệ";
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");

    @Value("${app.upload.product-images-dir:src/main/resources/static/images}")
    private String productImagesDir;

    @Value("${app.upload.max-image-size-bytes:5242880}")
    private long maxImageSizeBytes;

    public String uploadProductImage(MultipartFile file) {
        validateFile(file);

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.trim().isEmpty()) {
            throw invalid("file", "Tên tệp không hợp lệ");
        }
        
        // Extract plain filename out of possible absolute path from IE/Edge
        String fileName = java.nio.file.Paths.get(originalFilename).getFileName().toString();

        Path uploadDir = Paths.get(productImagesDir).toAbsolutePath().normalize();
        Path targetPath = uploadDir.resolve(fileName).normalize();
        if (!targetPath.startsWith(uploadDir)) {
            throw invalid("file", "Tên tệp không hợp lệ");
        }

        try (java.io.InputStream inputStream = file.getInputStream()) {
            Files.createDirectories(uploadDir);
            Files.copy(inputStream, targetPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new IllegalStateException("Không thể lưu ảnh: " + ex.getMessage(), ex);
        }

        return fileName;
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw invalid("file", "Vui lòng chọn ảnh");
        }

        if (file.getSize() > maxImageSizeBytes) {
            throw invalid("file", "Tệp quá lớn (tối đa 5MB)");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw invalid("file", "Định dạng ảnh không hỗ trợ");
        }

        resolveExtension(file.getOriginalFilename());
    }

    private String resolveExtension(String originalFileName) {
        String fileName = originalFileName == null ? "" : originalFileName.trim();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw invalid("file", "Định dạng ảnh không hỗ trợ");
        }

        String extension = fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw invalid("file", "Định dạng ảnh không hỗ trợ");
        }

        return extension;
    }

    private BadRequestException invalid(String field, String message) {
        return new BadRequestException(INVALID_REQUEST_MESSAGE, List.of(new ErrorDetail(field, message)));
    }
}
