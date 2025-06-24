package Wonbin.FinalProject.auth.service;

import Wonbin.FinalProject.auth.common.AuthProvider;
import Wonbin.FinalProject.auth.dto.OAuthAttributes;
import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.auth.jwt.JwtProvider;
import Wonbin.FinalProject.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);

        String registrationId = request.getClientRegistration().getRegistrationId(); // google, kakao, naver
        Map<String, Object> attributes = oAuth2User.getAttributes();

        OAuthAttributes attr = OAuthAttributes.of(registrationId, attributes);
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // DB에서 사용자 조회 or 저장
        User user = userRepository.findByProviderAndProviderId(provider, attr.getProviderId())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .provider(provider)
                                .providerId(attr.getProviderId())
                                .email(attr.getEmail())
                                .name(attr.getName())
                                .build()
                ));
        // JWT 발급
        String token = jwtProvider.createToken(attr.getEmail());
        System.out.println("JWT Token 발급: " + token);

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "email"
        );
    }
}
