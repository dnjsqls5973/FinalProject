package Wonbin.FinalProject.auth.controller;

import Wonbin.FinalProject.auth.jwt.JwtProvider;
import Wonbin.FinalProject.auth.service.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/token")
public class TokenController {

    private final JwtProvider jwtProvider;
    private final RefreshTokenService refreshTokenService;

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestBody Map<String, String> body) {
        try {
            String refreshToken = body.get("refreshToken");
            if (!jwtProvider.validate(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid refresh token");
            }

            String email = jwtProvider.getEmail(refreshToken);
            if (email == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Email extraction failed");
            }

            Optional<String> storedToken = refreshTokenService.findByEmail(email);
            if (storedToken.isEmpty() || !storedToken.get().equals(refreshToken)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh token mismatch");
            }

            String newAccessToken = jwtProvider.createAccessToken(email);
            return ResponseEntity.ok(Map.of("accessToken", newAccessToken));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Exception: " + e.getMessage()));
        }
    }
}