package org.example.main.service;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.Role;
import org.example.main.repository.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RoleServiceTest {

    @Mock
    RoleRepository roleRepository;

    @InjectMocks
    RoleService roleService;

    @Test
    void findAll_delegatesToRepository_andReturnsList() {
        Role r1 = new Role(); r1.setName("A");
        Role r2 = new Role(); r2.setName("B");
        when(roleRepository.findAll()).thenReturn(List.of(r1, r2));

        List<Role> res = roleService.findAll();

        assertThat(res).hasSize(2).containsExactly(r1, r2);
        verify(roleRepository).findAll();
        verifyNoMoreInteractions(roleRepository);
    }

    @Test
    void findByName_delegatesToRepository_andReturnsOptional() {
        Role r = new Role(); r.setName("ROLE_USER");
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(r));

        Optional<Role> out = roleService.findByName("ROLE_USER");

        assertThat(out).isPresent().contains(r);
        verify(roleRepository).findByName("ROLE_USER");
    }

    @Test
    void findById_returnsRole_whenPresent() {
        UUID id = UUID.randomUUID();
        Role r = new Role(); r.setId(id); r.setName("ROLE_TEST");
        when(roleRepository.findById(id)).thenReturn(Optional.of(r));

        Role out = roleService.findById(id);

        assertThat(out).isSameAs(r);
        verify(roleRepository).findById(id);
    }

    @Test
    void findById_throwsResourceNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(roleRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roleService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining(id.toString());

        verify(roleRepository).findById(id);
    }
}