package org.example.main.service.user;

import jakarta.servlet.http.HttpSession;
import org.example.main.dto.request.user.LoginRequestDto;
import org.example.main.dto.request.user.RegisterRequestDto;
import org.example.main.dto.response.user.AuthResponseDto;
import org.example.main.model.user.User;
import org.springframework.security.core.Authentication;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface IUserService {
    List<User> findAll();
    Optional<User> findByUsername(String username);
    User findById(UUID id);
    User create(User user, String rawPassword);
    User update(UUID id, User changes);
    void delete(UUID id);
    User assignRole(UUID userId, String roleName);
    AuthResponseDto register(RegisterRequestDto req);
    AuthResponseDto login(LoginRequestDto dto, HttpSession session);
    Map<String, Object> me(Authentication authentication, HttpSession session);
    boolean verifyPassword(User user, String rawPassword);
    void setPassword(UUID userId, String newRawPassword);
    User block(UUID userId, boolean blocked);

}