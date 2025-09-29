package Wonbin.FinalProject.auth.service;

import Wonbin.FinalProject.auth.common.AuthProvider;
import Wonbin.FinalProject.auth.dto.OAuthAttributes;
import Wonbin.FinalProject.auth.domain.User;
import Wonbin.FinalProject.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        log.info("OAuth2 사용자 정보 로드 시작");
        OAuth2User oAuth2User = super.loadUser(request);

        String registrationId = request.getClientRegistration().getRegistrationId();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        log.info("Provider: {}", registrationId);
        log.info("사용자 정보: {}", oAuth2User.getAttributes());

        OAuthAttributes attr = OAuthAttributes.of(registrationId, attributes);
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        // DB에서 사용자 조회 or 저장
        User user = userRepository.findByProviderAndProviderId(provider.toString(), attr.getProviderId())
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .provider(provider.toString())
                                .providerId(attr.getProviderId())
                                .email(attr.getEmail())
                                .name(attr.getName())
                                .build()
                ));

        log.info("사용자 저장/조회 완료: {}", user.getEmail());

        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "email"
        );
    }
}