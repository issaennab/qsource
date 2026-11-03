package com.qsource.api.service;

import com.qsource.api.domain.Organization;
import com.qsource.api.domain.OrganizationMember;
import com.qsource.api.domain.OrgRole;
import com.qsource.api.domain.RefreshToken;
import com.qsource.api.domain.UserAccount;
import com.qsource.api.dto.Dtos.*;
import com.qsource.api.repo.OrganizationMemberRepository;
import com.qsource.api.repo.OrganizationRepository;
import com.qsource.api.repo.RefreshTokenRepository;
import com.qsource.api.repo.UserAccountRepository;
import com.qsource.api.security.JwtService;
import com.qsource.api.security.SecurityUtils;
import com.qsource.api.security.TokenHasher;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTtlDays;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${qsource.jwt.refresh-ttl-days:30}") long refreshTtlDays) {
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTtlDays = refreshTtlDays;
    }

    @Transactional
    public AuthTokens signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered");
        }
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setDisplayName(request.displayName().trim());
        user = userAccountRepository.save(user);

        Organization org = createOrganization(request.organizationName().trim());
        OrganizationMember member = new OrganizationMember();
        member.setOrganizationId(org.getId());
        member.setUserId(user.getId());
        member.setRole(OrgRole.OWNER);
        organizationMemberRepository.save(member);

        return issueTokens(user);
    }

    @Transactional
    public AuthTokens login(LoginRequest request) {
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        UserAccount user = userAccountRepository
                .findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return issueTokens(user);
    }

    @Transactional
    public AuthTokens refresh(RefreshRequest request) {
        String hash = TokenHasher.sha256(request.refreshToken());
        RefreshToken stored = refreshTokenRepository
                .findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));
        if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }
        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);
        UserAccount user = userAccountRepository
                .findById(stored.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        return issueTokens(user);
    }

    @Transactional
    public void logout(RefreshRequest request) {
        refreshTokenRepository.findByTokenHash(TokenHasher.sha256(request.refreshToken())).ifPresent(token -> {
            token.setRevokedAt(Instant.now());
            refreshTokenRepository.save(token);
        });
    }

    @Transactional(readOnly = true)
    public MeResponse me() {
        var principal = SecurityUtils.requirePrincipal();
        if (principal.apiKey()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "API keys cannot call /me");
        }
        UserAccount user = userAccountRepository
                .findById(principal.userId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        var orgs = organizationMemberRepository.findByUserIdOrderByCreatedAtAsc(user.getId()).stream()
                .map(m -> {
                    Organization org = organizationRepository.findById(m.getOrganizationId()).orElse(null);
                    if (org == null) {
                        return null;
                    }
                    return new OrgMembershipResponse(org.getId(), org.getName(), org.getSlug(), m.getRole());
                })
                .filter(o -> o != null)
                .toList();
        return new MeResponse(toUser(user), orgs);
    }

    public Organization createOrganization(String name) {
        Organization org = new Organization();
        org.setName(name);
        org.setSlug(uniqueSlug(name));
        return organizationRepository.save(org);
    }

    private AuthTokens issueTokens(UserAccount user) {
        String access = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getDisplayName());
        String refreshRaw = randomToken();
        RefreshToken refresh = new RefreshToken();
        refresh.setUserId(user.getId());
        refresh.setTokenHash(TokenHasher.sha256(refreshRaw));
        refresh.setExpiresAt(Instant.now().plus(refreshTtlDays, ChronoUnit.DAYS));
        refreshTokenRepository.save(refresh);
        return new AuthTokens(access, refreshRaw, "Bearer", toUser(user));
    }

    private String uniqueSlug(String name) {
        String base = name.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+|-+$", "");
        if (base.isBlank()) {
            base = "org";
        }
        String slug = base;
        int i = 0;
        while (organizationRepository.existsBySlug(slug)) {
            i++;
            slug = base + "-" + i;
        }
        return slug;
    }

    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private UserResponse toUser(UserAccount user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getCreatedAt());
    }
}
