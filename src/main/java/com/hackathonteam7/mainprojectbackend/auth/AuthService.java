package com.hackathonteam7.mainprojectbackend.auth;

import com.hackathonteam7.mainprojectbackend.auth.dto.LoginRequest;
import com.hackathonteam7.mainprojectbackend.auth.dto.LoginResponse;
import com.hackathonteam7.mainprojectbackend.auth.dto.RegisterRequest;
import com.hackathonteam7.mainprojectbackend.auth.dto.RegisterResponse;
import com.hackathonteam7.mainprojectbackend.common.error.ApiException;
import com.hackathonteam7.mainprojectbackend.common.error.ErrorCode;
import com.hackathonteam7.mainprojectbackend.organization.Organization;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationMember;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationMemberRepository;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationMemberRole;
import com.hackathonteam7.mainprojectbackend.organization.OrganizationRepository;
import com.hackathonteam7.mainprojectbackend.security.JwtTokenProvider;
import com.hackathonteam7.mainprojectbackend.user.Role;
import com.hackathonteam7.mainprojectbackend.user.User;
import com.hackathonteam7.mainprojectbackend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.adminEmail())) {
            throw new ApiException(ErrorCode.EMAIL_DUPLICATED);
        }

        Organization organization = organizationRepository.save(
                Organization.builder()
                        .name(request.organizationName())
                        .type(request.organizationType())
                        .build()
        );

        User admin = userRepository.save(
                User.builder()
                        .name(request.adminName())
                        .email(request.adminEmail())
                        .password(passwordEncoder.encode(request.adminPassword()))
                        .role(Role.ORGANIZATION)
                        .build()
        );

        organizationMemberRepository.save(
                OrganizationMember.builder()
                        .organization(organization)
                        .user(admin)
                        .role(OrganizationMemberRole.OWNER)
                        .build()
        );

        String accessToken = jwtTokenProvider.generateAccessToken(
                admin.getId(), admin.getEmail(), admin.getRole(), organization.getId());

        return new RegisterResponse(organization.getId(), admin.getId(), accessToken);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED);
        }

        Long orgId = resolveOrganizationId(user);
        String accessToken = jwtTokenProvider.generateAccessToken(user.getId(), user.getEmail(), user.getRole(), orgId);
        return LoginResponse.of(accessToken, user);
    }

    private Long resolveOrganizationId(User user) {
        if (user.getRole() != Role.ORGANIZATION) {
            return null;
        }
        return organizationMemberRepository.findFirstByUserIdOrderById(user.getId())
                .map(member -> member.getOrganization().getId())
                .orElse(null);
    }
}
