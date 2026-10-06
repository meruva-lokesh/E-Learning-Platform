package com.examly.springapp.controller;

import com.examly.springapp.exception.InvalidRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Course image upload. The admin picks a picture in the browser, this endpoint saves it in the uploads folder and
 * answers with its public URL. That URL is then stored in the existing Course.courseImageUrl field, so the Course
 * model and the course endpoints do not change.
 *
 * <p>Safety: the file type is detected from the first bytes of the file (the name and Content-Type sent by the
 * browser are never trusted), the size is limited, and the stored name is a random UUID chosen by the server.
 *
 * @author Team Lead
 */
@RestController
@RequestMapping("/api/upload")
@Tag(name = "Uploads", description = "Course image upload")
public class UploadController {
    private static final Logger log = LoggerFactory.getLogger(UploadController.class);
    private static final long MAX_BYTES = 5L * 1024 * 1024;   // 5 MB

    private final Path uploadDir;

    public UploadController(@Value("${app.upload.dir:uploads}") String dir) throws IOException {
        this.uploadDir = Paths.get(dir).toAbsolutePath().normalize();
        Files.createDirectories(this.uploadDir);
    }

    @Operation(summary = "Upload a course image", description = "ADMIN only. PNG, JPG, GIF or WEBP, up to 5 MB. Returns the public URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Image saved, {\"url\": \"...\"} returned"),
            @ApiResponse(responseCode = "400", description = "Empty file, wrong type or too large"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid token"),
            @ApiResponse(responseCode = "403", description = "Not an admin")
    })
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestException("Please choose an image file");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new InvalidRequestException("The image is too large. The limit is 5 MB");
        }
        String extension = detectExtension(file);
        if (extension == null) {
            throw new InvalidRequestException("Only PNG, JPG, GIF or WEBP images are allowed");
        }
        String storedName = UUID.randomUUID() + "." + extension;
        Path target = uploadDir.resolve(storedName).normalize();
        if (!target.startsWith(uploadDir)) {
            throw new InvalidRequestException("Invalid file name");
        }
        try (InputStream in = file.getInputStream()) {
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("Could not save an uploaded image", e);
            throw new UncheckedIOException("Could not save the image", e);
        }
        String url = ServletUriComponentsBuilder.fromCurrentContextPath().path("/uploads/").path(storedName).toUriString();
        log.info("Course image saved as {}", storedName);
        return ResponseEntity.ok(Map.of("url", url));
    }

    /** Looks at the first bytes of the file and returns png, jpg, gif or webp; null when it is none of these. */
    private String detectExtension(MultipartFile file) {
        byte[] h = new byte[12];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.readNBytes(h, 0, h.length);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read the image", e);
        }
        if (read < 12) {
            return null;
        }
        if ((h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G') {
            return "png";
        }
        if ((h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8 && (h[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (h[0] == 'G' && h[1] == 'I' && h[2] == 'F' && h[3] == '8') {
            return "gif";
        }
        if (h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F' && h[8] == 'W' && h[9] == 'E' && h[10] == 'B' && h[11] == 'P') {
            return "webp";
        }
        return null;
    }
}
