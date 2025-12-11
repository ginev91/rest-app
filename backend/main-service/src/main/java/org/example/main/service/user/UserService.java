package org.example.main.service.user;

import jakarta.servlet.http.HttpSession;
import org.example.main.dto.request.user.LoginRequestDto;
import org.example.main.dto.request.user.RegisterRequestDto;
import org.example.main.dto.response.user.AuthResponseDto;
import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.table.RestaurantTable;
import org.example.main.model.role.Role;
import org.example.main.model.user.User;
import org.example.main.repository.table.RestaurantTableRepository;
import org.example.main.repository.role.RoleRepository;
import org.example.main.repository.user.UserRepository;
import org.example.main.security.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.UUID;

@Service
@Transactional
public class UserService implements IUserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final RestaurantTableRepository restaurantTableRepository;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtils jwtUtils,
                       RestaurantTableRepository restaurantTableRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtils = jwtUtils;
        this.restaurantTableRepository = restaurantTableRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        if (user.isEmpty()) {
            throw new ResourceNotFoundException("User not found: " + username);
        }
        return user;
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
    }

    @Override
    public User create(User user, String rawPassword) {
        user.setPasswordHash(passwordEncoder.encode(rawPassword));

        // If no Role object set, ensure default role entity exists and assign it.
        if (user.getRole() == null) {
            Role defaultRole = roleRepository.findByName("ROLE_USER")
                    .orElseGet(() -> {
                        Role r = new Role();
                        r.setName("ROLE_USER");
                        return roleRepository.save(r);
                    });
            user.setRole(defaultRole);
        } else {
            // If client provided only role name in a DTO mapped earlier, ensure it's a managed Role entity.
            Role provided = user.getRole();
            if (provided.getName() != null) {
                Role roleEntity = roleRepository.findByName(provided.getName())
                        .orElseGet(() -> roleRepository.save(Role.builder().name(provided.getName()).build()));
                user.setRole(roleEntity);
            }
        }

        return userRepository.save(user);
    }

    @Override
    public User update(UUID id, User changes) {
        User existing = findById(id);
        if (changes.getFullName() != null) existing.setFullName(changes.getFullName());
        if (changes.getUsername() != null) existing.setUsername(changes.getUsername());
        // Allow role update via Role entity if provided
        if (changes.getRole() != null) {
            Role r = roleRepository.findByName(changes.getRole().getName())
                    .orElseGet(() -> roleRepository.save(Role.builder().name(changes.getRole().getName()).build()));
            existing.setRole(r);
        }
        return userRepository.save(existing);
    }

    @Override
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) throw new ResourceNotFoundException("User not found: " + id);
        userRepository.deleteById(id);
    }

    @Override
    public User assignRole(UUID userId, String roleName) {
        User user = findById(userId);
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> {
                    Role r = new Role();
                    r.setName(roleName);
                    return roleRepository.save(r);
                });
        user.setRole(role);
        return userRepository.save(user);
    }

    @Override
    public AuthResponseDto login(LoginRequestDto dto, HttpSession session) {
        if (dto == null || dto.getUsername() == null || dto.getPassword() == null) {
            throw new IllegalArgumentException("username and password are required");
        }
        UUID tableId = null;
        User user = userRepository.findByUsername(dto.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("Invalid credentials"));

        if (!passwordEncoder.matches(dto.getPassword(), user.getPasswordHash())) {
            throw new ResourceNotFoundException("Invalid credentials");
        }

        boolean isUserRole = Optional.ofNullable(user.getRole())
                .map(r -> r.getName().replace("ROLE_", "").equalsIgnoreCase("USER"))
                .orElse(false);

        Integer tableNumber = null;

        if (isUserRole) {
            tableNumber = dto.getTableNumber();
            String tablePin = dto.getTablePin();

            if (tableNumber == null) {
                throw new IllegalArgumentException("tableNumber required for USER role");
            }
            if (tablePin == null || tablePin.isBlank()) {
                throw new IllegalArgumentException("tablePin required for USER role");
            }

            String code = "T" + tableNumber;
            RestaurantTable table = restaurantTableRepository.findByCode(code)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid table number"));
            tableId = table.getId();

            if (!Objects.equals(table.getPinCode(), tablePin)) {
                throw new IllegalArgumentException("Invalid table pin");
            }

            user.setSessionTableNumber(tableNumber);
            userRepository.save(user);

            if (session != null) {
                session.setAttribute("tableNumber", tableNumber);
                session.setAttribute("tableId", table.getId());
                logger.debug("login: set session.tableNumber={} sessionId={}", tableNumber, session.getId());
            } else {
                logger.debug("login: no HttpSession available, persisted sessionTableNumber for user={}", user.getUsername());
            }
        } else {
            if (session != null) {
                session.removeAttribute("tableNumber");
                session.removeAttribute("tableId");
                logger.debug("login: removed session.tableNumber sessionId={}", session.getId());
            }
            if (user.getSessionTableNumber() != null) {
                user.setSessionTableNumber(null);
                userRepository.save(user);
            }
        }

        List<String> rolesList = List.of(Optional.ofNullable(user.getRole()).map(Role::getName).orElse("ROLE_USER"));

        String token = jwtUtils.generateToken(user.getUsername(), rolesList);

        return AuthResponseDto.builder()
                .token(token)
                .username(user.getUsername())
                .userId(user.getId())
                .role(user.getRole() != null ? user.getRole().getName() : null)
                .tableNumber(tableNumber)
                .tableId(tableId)
                .build();
    }

    @Override
    public AuthResponseDto register(RegisterRequestDto req) {
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new IllegalArgumentException("Username already taken");
        }

        User u = new User();
        u.setUsername(req.getUsername());
        u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        u.setFullName(req.getFullName());

        Role userRole = roleRepository.findByName("ROLE_USER").orElseGet(() -> {
            Role r = new Role();
            r.setName("ROLE_USER");
            return roleRepository.save(r);
        });
        u.setRole(userRole);

        User saved = userRepository.save(u);

        List<String> rolesList = List.of(saved.getRole().getName());
        String token = jwtUtils.generateToken(saved.getUsername(), rolesList);

        return AuthResponseDto.builder()
                .token(token)
                .username(saved.getUsername())
                .userId(saved.getId())
                .role(saved.getRole().getName())
                .build();
    }

    @Override
    public Map<String, Object> me(Authentication authentication, HttpSession session) {
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(String.valueOf(authentication.getPrincipal()))) {
            throw new IllegalArgumentException("Unauthenticated");
        }

        Object principal = authentication.getPrincipal();
        Map<String, Object> dto = new LinkedHashMap<>();
        String username = null;
        String fullName = null;

        if (principal instanceof UserDetails) {
            UserDetails ud = (UserDetails) principal;
            username = ud.getUsername();
            dto.put("username", username);
            dto.put("fullName", userRepository.findByUsername(username).get().getFullName());
            dto.put("authorities", ud.getAuthorities().stream()
                    .map(GrantedAuthority::getAuthority)
                    .collect(Collectors.toList()));
        } else if (principal instanceof String) {
            username = (String) principal;
            dto.put("username", username);
            dto.put("fullName", userRepository.findByUsername(username).get().getFullName());
        } else if (principal instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) principal;
            dto.putAll(map);
        } else {
            dto.put("principal", principal != null ? principal.toString() : null);
        }

        if (username != null) {
            Optional<User> userOpt = userRepository.findByUsername(username);
            userOpt.ifPresent(user -> dto.put("userId", user.getId()));
            userOpt.ifPresent(user -> dto.put("role", user.getRole() != null ? user.getRole().getName() : null));

            if (session != null) {
                logger.debug("/me sessionId={} attributes:", session.getId());
                Enumeration<String> names = session.getAttributeNames();
                while (names.hasMoreElements()) {
                    String n = names.nextElement();
                    logger.debug(" /me session[{}]={}", n, session.getAttribute(n));
                }
            } else {
                logger.debug("/me: session is null");
            }

            Integer tableNumber = null;

            if (session != null) {
                Object attr = session.getAttribute("tableNumber");
                if (attr instanceof Integer) {
                    tableNumber = (Integer) attr;
                } else if (attr instanceof Number) {
                    tableNumber = ((Number) attr).intValue();
                } else if (attr instanceof String) {
                    try {
                        tableNumber = Integer.valueOf((String) attr);
                    } catch (NumberFormatException ignored) {
                        logger.debug("/me: session.tableNumber is String but not numeric: {}", attr);
                    }
                } else if (attr != null) {
                    logger.debug("/me: session.tableNumber present but of type {} value={}", attr.getClass(), attr);
                }
            }

            if (tableNumber == null) {
                tableNumber = userOpt.map(User::getSessionTableNumber).orElse(null);
                logger.debug("/me: fallback to DB sessionTableNumber={}", tableNumber);
            } else {
                logger.debug("/me: using session tableNumber={}", tableNumber);
            }

            dto.put("tableNumber", tableNumber);

            UUID tableId = null;
            if (tableNumber != null) {
                String code = "T" + tableNumber;
                Optional<RestaurantTable> optTable = restaurantTableRepository.findByCode(code);
                if (optTable.isPresent()) {
                    tableId = optTable.get().getId();
                } else {
                    logger.debug("/me: no RestaurantTable found for code {}", code);
                }
            }
            dto.put("tableId", tableId);
        }

        return dto;
    }

    @Override
    public boolean verifyPassword(User user, String rawPassword) {
        if (user == null || rawPassword == null) return false;
        return passwordEncoder.matches(rawPassword, user.getPasswordHash());
    }

    @Override
    public void setPassword(UUID userId, String newRawPassword) {
        User u = findById(userId);
        u.setPasswordHash(passwordEncoder.encode(newRawPassword));
        userRepository.save(u);
    }
}