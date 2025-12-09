package org.example.main.service;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.MenuItem;
import org.example.main.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MenuItemService covering:
 * - findAll, findByName, findById (found + not found)
 * - create
 * - update (happy path + not found + field-preservation behavior)
 * - delete (exists + not exists)
 */
@ExtendWith(MockitoExtension.class)
class MenuItemServiceTest {

    @Mock
    MenuItemRepository menuItemRepository;

    @InjectMocks
    MenuItemService menuItemService;

    @Test
    void findAll_returnsRepositoryList() {
        MenuItem a = new MenuItem(); a.setName("A");
        MenuItem b = new MenuItem(); b.setName("B");
        when(menuItemRepository.findAll()).thenReturn(List.of(a, b));

        List<MenuItem> out = menuItemService.findAll();

        assertThat(out).hasSize(2).extracting(MenuItem::getName).containsExactly("A", "B");
        verify(menuItemRepository).findAll();
    }

    @Test
    void findByName_delegatesToRepository_andReturnsOptional() {
        MenuItem m = new MenuItem();
        m.setName("X");
        when(menuItemRepository.findByName("X")).thenReturn(Optional.of(m));

        Optional<MenuItem> o = menuItemService.findByName("X");

        assertThat(o).isPresent().contains(m);
        verify(menuItemRepository).findByName("X");
    }

    @Test
    void findById_returnsEntity_whenFound() {
        UUID id = UUID.randomUUID();
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setName("Found");
        when(menuItemRepository.findById(id)).thenReturn(Optional.of(m));

        MenuItem out = menuItemService.findById(id);

        assertThat(out).isSameAs(m);
        verify(menuItemRepository).findById(id);
    }

    @Test
    void findById_throwsResourceNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(menuItemRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> menuItemService.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MenuItem not found")
                .hasMessageContaining(id.toString());

        verify(menuItemRepository).findById(id);
    }

    @Test
    void create_savesViaRepository_andReturnsSavedEntity() {
        MenuItem in = new MenuItem();
        in.setName("New");
        MenuItem saved = new MenuItem();
        saved.setId(UUID.randomUUID());
        saved.setName("New");

        when(menuItemRepository.save(in)).thenReturn(saved);

        MenuItem out = menuItemService.create(in);

        assertThat(out).isSameAs(saved);
        verify(menuItemRepository).save(in);
    }

    @Test
    void update_updatesOnlyProvidedFields_andSaves() {
        UUID id = UUID.randomUUID();
        MenuItem existing = new MenuItem();
        existing.setId(id);
        existing.setName("OldName");
        existing.setDescription("OldDesc");
        existing.setPrice(BigDecimal.valueOf(1.23));

        when(menuItemRepository.findById(id)).thenReturn(Optional.of(existing));
        when(menuItemRepository.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuItem changes = new MenuItem();
        // name provided -> should be updated
        changes.setName("NewName");
        // description explicitly set to null in changes -> service sets description unconditionally
        changes.setDescription(null);
        // price null -> should be preserved on existing
        changes.setPrice(null);

        MenuItem out = menuItemService.update(id, changes);

        assertThat(out.getId()).isEqualTo(id);
        assertThat(out.getName()).isEqualTo("NewName");
        // description is overwritten to null by service implementation
        assertThat(out.getDescription()).isNull();
        // price preserved
        assertThat(out.getPrice()).isEqualByComparingTo(BigDecimal.valueOf(1.23));

        verify(menuItemRepository).findById(id);
        verify(menuItemRepository).save(existing);
    }

    @Test
    void update_throws_whenEntityMissing() {
        UUID id = UUID.randomUUID();
        when(menuItemRepository.findById(id)).thenReturn(Optional.empty());

        MenuItem changes = new MenuItem();
        changes.setName("X");

        assertThatThrownBy(() -> menuItemService.update(id, changes))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MenuItem not found");

        verify(menuItemRepository).findById(id);
        verify(menuItemRepository, never()).save(any());
    }

    @Test
    void delete_deletesWhenExists() {
        UUID id = UUID.randomUUID();
        when(menuItemRepository.existsById(id)).thenReturn(true);

        menuItemService.delete(id);

        verify(menuItemRepository).existsById(id);
        verify(menuItemRepository).deleteById(id);
    }

    @Test
    void delete_throws_whenNotExists() {
        UUID id = UUID.randomUUID();
        when(menuItemRepository.existsById(id)).thenReturn(false);

        assertThatThrownBy(() -> menuItemService.delete(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("MenuItem not found")
                .hasMessageContaining(id.toString());

        verify(menuItemRepository).existsById(id);
        verify(menuItemRepository, never()).deleteById(any());
    }
}