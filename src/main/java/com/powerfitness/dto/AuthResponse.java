package com.powerfitness.dto;

import com.powerfitness.entity.Role;

public class AuthResponse {
    private String token;
    private Long userId;
    private String username;
    private String fullName;
    private Role role;
    private String memberCode;
    private String photoUrl;

    public AuthResponse() {}

    public AuthResponse(String token, Long userId, String username, String fullName, Role role, String memberCode, String photoUrl) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.fullName = fullName;
        this.role = role;
        this.memberCode = memberCode;
        this.photoUrl = photoUrl;
    }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
}
