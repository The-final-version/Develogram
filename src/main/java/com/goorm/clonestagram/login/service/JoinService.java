package com.goorm.clonestagram.login.service;

import com.goorm.clonestagram.login.dto.JoinDto;
import com.goorm.clonestagram.login.dto.JoinDto;
import com.goorm.clonestagram.user.domain.Users;
import com.goorm.clonestagram.user.dto.UserProfileDto;
import com.goorm.clonestagram.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class JoinService {

    private final UserRepository userRepository;

    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    // 이메일 형식 체크를 위한 정규 표현식
    private static final String EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@(.+)$";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    public void joinProcess(JoinDto joinDto) {

        // 이메일 형식 체크
        if (!isValidEmail(joinDto.getEmail())) {
            throw new IllegalStateException("유효한 이메일 주소를 입력해주세요.");
        }

        // 이메일 중복 체크
        if (userRepository.existsByEmail(joinDto.getEmail())) {
            throw new IllegalStateException("이미 존재하는 이메일입니다.");
        }

        // 비밀번호 유효성 체크 (비어 있으면 예외 발생)
        if (joinDto.getPassword() == null || joinDto.getPassword().isEmpty()) {
            throw new IllegalStateException("비밀번호를 입력해주세요.");
        }

        // 비밀번호 확인
        if (!joinDto.getPassword().equals(joinDto.getConfirmPassword())) {
            throw new IllegalStateException("비밀번호가 일치하지 않습니다.");
        }

        // 이름과 닉네임 유효성 체크 (비어 있으면 예외 발생)
        if (joinDto.getUsername() == null || joinDto.getUsername().isEmpty()) {
            throw new IllegalStateException("이름을 입력해주세요.");
        }

        // 새로운 사용자 정보 저장
        Users user = new Users();
        user.setUsername(joinDto.getUsername());// 이름 추가
        user.setPassword(bCryptPasswordEncoder.encode(joinDto.getPassword()));
        user.setEmail(joinDto.getEmail());

        // 데이터베이스에 저장
        userRepository.save(user);
    }

    // 이메일 형식 검증
    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        Matcher matcher = EMAIL_PATTERN.matcher(email);
        return matcher.matches();
    }
}
