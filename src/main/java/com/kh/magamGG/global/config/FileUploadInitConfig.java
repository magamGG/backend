package com.kh.magamGG.global.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 업로드 디렉터리 초기화 설정.
 * - 서버 기동 시 uploads 및 하위 attendance 디렉터리를 자동 생성한다.
 * - 이미 존재하면 아무 동작도 하지 않는다.
 */
@Configuration
@Slf4j
public class FileUploadInitConfig {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @PostConstruct
    public void initUploadDirs() {
        try {
            Path baseDir = Paths.get(uploadDir).toAbsolutePath();
            // 기본 uploads 디렉터리
            Files.createDirectories(baseDir);
            // 근태 첨부파일 디렉터리 (uploads/attendance)
            Path attendanceDir = baseDir.resolve("attendance");
            Files.createDirectories(attendanceDir);
            log.info("파일 업로드 디렉터리 초기화 완료: {}, {}", baseDir, attendanceDir);
        } catch (Exception e) {
            log.warn("파일 업로드 디렉터리 생성 실패(무시): {}", e.getMessage());
        }
    }
}

