package org.example.main.controller.user;

import org.example.main.model.user.User;
import org.example.main.service.user.IUserService;
import org.example.main.dto.request.user.UpdateProfileRequestDto;
import org.example.main.dto.request.user.ChangePasswordRequestDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileControllerTest {

    @Mock
    IUserService userService;

    @InjectMocks
    UserProfileController controller;

    @Test
    void me_returnsUserInfo_mapFromService() {
        Authentication auth = mock(Authentication.class);
        Map<String, Object> returned = Map.of("username", "bob", "userId", UUID.randomUUID());
        when(userService.me(eq(auth), isNull())).thenReturn(returned);

        ResponseEntity<?> resp = controller.me(auth);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(resp.getBody()).isEqualTo(returned);
        verify(userService).me(eq(auth), isNull());
    }

    @Test
    void updateMe_updatesProfile_and_returnsUpdatedUser() {
        Authentication auth = mock(Authentication.class);
        UUID uid = UUID.randomUUID();
        Map<String, Object> meMap = Map.of("username", "bob", "userId", uid);
        when(userService.me(eq(auth), isNull())).thenReturn(meMap);

        User updated = new User();
        updated.setId(uid);
        updated.setUsername("newname");
        updated.setFullName("New Name");

        when(userService.update(eq(uid), any(User.class))).thenReturn(updated);

        UpdateProfileRequestDto req = new UpdateProfileRequestDto();
        req.setUsername("newname");
        req.setFullName("New Name");

        ResponseEntity<?> resp = controller.updateMe(auth, req);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        Object body = resp.getBody();
        assertThat(body).isInstanceOf(User.class);
        User u = (User) body;
        assertThat(u.getUsername()).isEqualTo("newname");
        assertThat(u.getFullName()).isEqualTo("New Name");

        verify(userService).update(eq(uid), any(User.class));
    }

    @Test
    void changePassword_success_callsSetPassword_and_returnsOk() {
        Authentication auth = mock(Authentication.class);
        UUID uid = UUID.randomUUID();
        Map<String, Object> meMap = Map.of("username", "bob", "userId", uid);
        when(userService.me(eq(auth), isNull())).thenReturn(meMap);

        User u = new User();
        u.setId(uid);
        u.setPasswordHash("encoded");
        when(userService.findById(uid)).thenReturn(u);
        when(userService.verifyPassword(eq(u), eq("old"))).thenReturn(true);
        doNothing().when(userService).setPassword(eq(uid), eq("new"));

        ChangePasswordRequestDto req = new ChangePasswordRequestDto();
        req.setOldPassword("old");
        req.setNewPassword("new");

        ResponseEntity<?> resp = controller.changePassword(auth, req);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(userService).setPassword(uid, "new");
    }

    @Test
    void changePassword_wrongOld_throwsForbidden_and_doesNotChange() {
        Authentication auth = mock(Authentication.class);
        UUID uid = UUID.randomUUID();
        Map<String, Object> meMap = Map.of("username", "bob", "userId", uid);
        when(userService.me(eq(auth), isNull())).thenReturn(meMap);

        User u = new User();
        u.setId(uid);
        u.setPasswordHash("encoded");
        when(userService.findById(uid)).thenReturn(u);
        when(userService.verifyPassword(eq(u), eq("badold"))).thenReturn(false);

        ChangePasswordRequestDto req = new ChangePasswordRequestDto();
        req.setOldPassword("badold");
        req.setNewPassword("new");

        ResponseStatusException ex = catchThrowableOfType(() -> controller.changePassword(auth, req), ResponseStatusException.class);
        assertThat(ex).isNotNull();
        assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.FORBIDDEN.value());

        verify(userService, never()).setPassword(any(), anyString());
    }
}