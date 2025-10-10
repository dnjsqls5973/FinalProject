package Wonbin.FinalProject.auth.service;

import Wonbin.FinalProject.auth.domain.GoogleUserInfo;
import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.auth.domain.UserRole;
import Wonbin.FinalProject.auth.exception.OAuth2AuthenticationException;
import Wonbin.FinalProject.auth.exception.UserNotFoundException;
import Wonbin.FinalProject.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RestTemplate restTemplate;  // ✅ 생성자 주입으로 변경
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret}")
    private String googleClientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri}")
    private String redirectUri;

    /**
     * Google OAuth 로그인 처리
     * Authorization Code를 받아서 사용자 정보를 가져오고 회원가입/로그인 처리
     */
    public User processGoogleLogin(String authorizationCode) {
        try {
            // 1. Authorization Code로 Access Token 요청
            String accessToken = getGoogleAccessToken(authorizationCode);

            // 2. Access Token으로 사용자 정보 요청
            GoogleUserInfo userInfo = getGoogleUserInfo(accessToken);

            // 3. 사용자 정보로 회원가입/로그인 처리
            return processGoogleUser(userInfo);

        } catch (Exception e) {
            log.error("Google 로그인 처리 중 오류 발생", e);
            throw new OAuth2AuthenticationException("Google 로그인 처리에 실패했습니다: " + e.getMessage(), e);
        }
    }

    /**
     * Authorization Code를 Access Token으로 교환
     */
    private String getGoogleAccessToken(String authorizationCode) {
        String tokenUrl = "https://oauth2.googleapis.com/token";

        // 요청 파라미터 설정
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", googleClientId);
        params.add("client_secret", googleClientSecret);
        params.add("code", authorizationCode);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri);

        // HTTP 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        try {
            // Google Token API 호출
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, request, String.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Google Token API 호출 실패: " + response.getStatusCode());
            }

            // JSON 응답에서 access_token 추출
            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            String accessToken = jsonNode.get("access_token").asText();

            log.info("Google Access Token 획득 성공");
            return accessToken;

        } catch (Exception e) {
            log.error("Google Access Token 획득 실패", e);
            throw new RuntimeException("Google Access Token 획득에 실패했습니다", e);
        }
    }

    /**
     * Access Token으로 Google 사용자 정보 조회
     */
    private GoogleUserInfo getGoogleUserInfo(String accessToken) {
        String userInfoUrl = "https://www.googleapis.com/oauth2/v2/userinfo";

        // HTTP 헤더에 Access Token 추가
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        try {
            // Google UserInfo API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                    userInfoUrl,
                    HttpMethod.GET,
                    request,
                    String.class
            );

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new RuntimeException("Google UserInfo API 호출 실패: " + response.getStatusCode());
            }

            // JSON 응답을 GoogleUserInfo 객체로 변환
            JsonNode jsonNode = objectMapper.readTree(response.getBody());

            GoogleUserInfo userInfo = GoogleUserInfo.builder()
                    .email(jsonNode.get("email").asText())
                    .name(jsonNode.get("name").asText())
                    .picture(jsonNode.has("picture") ? jsonNode.get("picture").asText() : null)
                    .googleId(jsonNode.get("id").asText())
                    .build();

            log.info("Google 사용자 정보 조회 성공 - 이메일: {}", userInfo.getEmail());
            return userInfo;

        } catch (Exception e) {
            log.error("Google 사용자 정보 조회 실패", e);
            throw new RuntimeException("Google 사용자 정보 조회에 실패했습니다", e);
        }
    }

    /**
     * Google 사용자 정보로 회원가입/로그인 처리
     */
    private User processGoogleUser(GoogleUserInfo userInfo) {
        // 이메일로 기존 사용자 확인
        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());

        if (existingUser.isPresent()) {
            // 기존 사용자 정보 업데이트
            User user = existingUser.get();
            user.updateGoogleInfo(userInfo.getName(), userInfo.getPicture());
            User savedUser = userRepository.save(user);

            log.info("기존 사용자 로그인 - 이메일: {}", userInfo.getEmail());
            return savedUser;

        } else {
            // 새 사용자 생성
            User newUser = User.builder()
                    .email(userInfo.getEmail())
                    .name(userInfo.getName())
                    .picture(userInfo.getPicture())
                    .provider("GOOGLE")
                    .providerId(userInfo.getGoogleId())
                    .role(UserRole.USER)
                    .build();

            User savedUser = userRepository.save(newUser);
            log.info("새 사용자 회원가입 - 이메일: {}", userInfo.getEmail());

            return savedUser;
        }
    }

    /**
     * 이메일로 사용자 조회
     */
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> UserNotFoundException.byEmail(email));
    }

    /**
     * 사용자 존재 여부 확인
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }
}