/*
 * @ (#) MediaController.java    1.0    03/04/2025
 * Copyright (c) 2025 IUH. All rights reserved.
 */
package vn.edu.iuh.fit.olachatbackend.controllers;/*
 * @description:
 * @author: Bao Thong
 * @date: 03/04/2025
 * @version: 1.0
 */

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import vn.edu.iuh.fit.olachatbackend.dtos.responses.FileResponse;
import vn.edu.iuh.fit.olachatbackend.dtos.responses.MessageResponse;
import vn.edu.iuh.fit.olachatbackend.dtos.responses.UploadFilesResponse;
import vn.edu.iuh.fit.olachatbackend.entities.File;
import vn.edu.iuh.fit.olachatbackend.exceptions.NotFoundException;
import vn.edu.iuh.fit.olachatbackend.services.CloudinaryService;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/files")
public class FileController {
    private final CloudinaryService cloudinaryService;

    public FileController(CloudinaryService cloudinaryService) {
        this.cloudinaryService = cloudinaryService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFile(@RequestParam("file") MultipartFile file,
                                        @RequestParam(value = "associatedIDMessageId", required = false) Long associatedIDMessageId) {
        try {
            File fileUpload = cloudinaryService.uploadFileAndSaveToDB_v3(file, associatedIDMessageId);
            return ResponseEntity.ok(fileUpload);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    @PostMapping("/upload_v2")
    public ResponseEntity<?> uploadFiles(@RequestParam("files") List<MultipartFile> files,
                                         @RequestParam(value = "associatedIDMessageId", required = false) Long associatedIDMessageId) {
        try {
            UploadFilesResponse response = cloudinaryService.uploadFileAndSaveToDB_v2(files, associatedIDMessageId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Upload failed: " + e.getMessage());
        }
    }

    //delete file and remove from database
    @PostMapping("/delete")
    public ResponseEntity<?> deleteFile(@RequestParam("publicId") String publicId) {
        try {
            cloudinaryService.deleteFile(publicId);
            return ResponseEntity.ok("File deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body("Delete failed: " + e.getMessage());
        }
    }

    @PostMapping("/download")
    public ResponseEntity<?> downloadFile(@RequestParam("publicId") String publicId,
                                          @RequestParam("savePath") String savePath) {
        try {
            Map<String, Object> response = cloudinaryService.downloadFile(publicId, savePath);
            return ResponseEntity.ok(response);
        } catch (NotFoundException e) {
            return ResponseEntity.status(404).body(Map.of(
                    "error", "Not Found",
                    "message", e.getMessage()
            ));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Download error",
                    "message", "Tải file thất bại: " + e.getMessage()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "Internal Server Error",
                    "message", "Lỗi khi xử lý yêu cầu tải file: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/upload/image")
    public ResponseEntity<String> uploadImage(
            @RequestParam("file") MultipartFile file)
            throws IOException {
        return ResponseEntity.ok(cloudinaryService.uploadImage(file));
    }

    @PostMapping("/upload/audio")
    public ResponseEntity<MessageResponse<FileResponse>> uploadAudio(
            @RequestParam("file") MultipartFile file) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    MessageResponse.<FileResponse>builder()
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message("Empty file provided")
                            .success(false)
                            .build()
            );
        }

        try {
            FileResponse fileResponse = cloudinaryService.uploadAudioFile(file);

            return ResponseEntity.ok(
                    MessageResponse.<FileResponse>builder()
                            .statusCode(HttpStatus.OK.value())
                            .message("Audio file uploaded successfully")
                            .success(true)
                            .data(fileResponse)
                            .build()
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(
                    MessageResponse.<FileResponse>builder()
                            .statusCode(HttpStatus.BAD_REQUEST.value())
                            .message(e.getMessage())
                            .success(false)
                            .build()
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    MessageResponse.<FileResponse>builder()
                            .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                            .message("Failed to upload audio: " + e.getMessage())
                            .success(false)
                            .build()
            );
        }
    }


}

