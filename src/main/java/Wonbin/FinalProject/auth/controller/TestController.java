package Wonbin.FinalProject.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal OAuth2User principal) {
        if (principal == null) return "비로그인 상태입니다.";

        String email = principal.getAttribute("email");
        String name = principal.getAttribute("name");

        return "로그인 성공! " + name + " (" + email + ")";
    }

    @GetMapping("/secure")
    public ResponseEntity<String> secureEndpoint(Authentication authentication) {
        return ResponseEntity.ok("인증된 사용자: " + authentication.getName());
    }
}