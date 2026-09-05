package com.powerfitness.service;

import com.powerfitness.dto.AuthRequest;
import com.powerfitness.dto.AuthResponse;
import com.powerfitness.entity.Member;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.User;
import com.powerfitness.repository.MemberRepository;
import com.powerfitness.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    // In-memory token store for lightweight secure session management
    private final Map<String, Long> tokenToUserId = new ConcurrentHashMap<>();

    public String hashPassword(String raw) {
        return passwordEncoder.encode(raw);
    }

    public boolean verifyPassword(String raw, String hash) {
        return passwordEncoder.matches(raw, hash);
    }

    public AuthResponse login(AuthRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new IllegalArgumentException("Username and password are required");
        }

        String username = request.getUsername().trim();
        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isEmpty() || !verifyPassword(request.getPassword(), userOpt.get().getPassword())) {
            throw new IllegalArgumentException("Invalid credentials. Please check your phone/username and password.");
        }

        User user = userOpt.get();
        String token = "PFK-" + UUID.randomUUID().toString();
        tokenToUserId.put(token, user.getId());

        String memberCode = null;
        String photoUrl = null;
        if (user.getRole() == Role.USER) {
            Optional<Member> memberOpt = memberRepository.findByUserId(user.getId());
            if (memberOpt.isPresent()) {
                memberCode = memberOpt.get().getMemberCode();
                photoUrl = memberOpt.get().getPhotoUrl();
            }
        }

        return new AuthResponse(token, user.getId(), user.getUsername(), user.getFullName(), user.getRole(), memberCode, photoUrl);
    }

    public void logout(String token) {
        if (token != null) {
            tokenToUserId.remove(token.replace("Bearer ", "").trim());
        }
    }

    public User getAuthenticatedUser(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7).trim();
        Long userId = tokenToUserId.get(token);
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    public boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }
}
