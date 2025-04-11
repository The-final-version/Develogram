package com.goorm.clonestagram.util;

import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin")
public class UserExportController {

    private final UserRepository userRepository;

    @GetMapping("/export-users")
    public ResponseEntity<byte[]> exportUsersAsCSV() {
        List<Users> users = userRepository.findAll();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(outputStream, true, StandardCharsets.UTF_8);

        // CSV 헤더
        writer.println("userId,username,password");

        // CSV 내용
        for (Users user : users) {
            writer.printf("%d,%s,%s%n",
                    user.getId(),
                    user.getUsername(),
                    "password123"  // JMeter용으로 일괄 입력
            );
        }

        writer.flush();

        byte[] csvBytes = outputStream.toByteArray();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=users.csv")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csvBytes);
    }
}

