package org.example.main.service.category;

import org.example.main.exception.ResourceNotFoundException;
import org.example.main.model.category.CategoryEntity;
import org.example.main.model.enums.ItemType;
import org.example.main.repository.category.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategoryService to achieve full branch coverage.
 */
@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    CategoryRepository repository;

    @InjectMocks
    CategoryService service;

    @Test
    void findAll_delegatesToRepository() {
        CategoryEntity e = new CategoryEntity();
        when(repository.findAll()).thenReturn(List.of(e));

        List<CategoryEntity> all = service.findAll();
        assertThat(all).containsExactly(e);
        verify(repository).findAll();
    }

    @Test
    void findById_returnsEntity_whenPresent() {
        UUID id = UUID.randomUUID();
        CategoryEntity e = new CategoryEntity();
        e.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(e));

        CategoryEntity out = service.findById(id);
        assertThat(out).isSameAs(e);
    }

    @Test
    void findById_throwsResourceNotFound_whenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void create_throwsOnNullPayload() {
        assertThatThrownBy(() -> service.create(null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Category payload required");
    }

    @Test
    void create_throwsWhenItemTypeMissing() {
        CategoryEntity e = new CategoryEntity();
        e.setItemType(null);

        assertThatThrownBy(() -> service.create(e))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("itemType is required");
    }

    @Test
    void create_throwsWhenCategoryForTypeExists() {
        CategoryEntity e = new CategoryEntity();
        e.setItemType(ItemType.KITCHEN);

        when(repository.findByItemType(ItemType.KITCHEN)).thenReturn(Optional.of(new CategoryEntity()));

        assertThatThrownBy(() -> service.create(e))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Category for itemType already exists");
    }

    @Test
    void create_savesAndReturnsEntity_whenValid() {
        CategoryEntity e = new CategoryEntity();
        e.setItemType(ItemType.BAR);

        when(repository.findByItemType(ItemType.BAR)).thenReturn(Optional.empty());
        when(repository.save(e)).thenAnswer(inv -> {
            CategoryEntity saved = inv.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        CategoryEntity out = service.create(e);
        assertThat(out.getId()).isNotNull();
        assertThat(out.getItemType()).isEqualTo(ItemType.BAR);
        verify(repository).save(e);
    }

    @Test
    void update_changesItemType_whenNotConflicting() {
        UUID id = UUID.randomUUID();
        CategoryEntity existing = new CategoryEntity();
        existing.setId(id);
        existing.setItemType(ItemType.KITCHEN);

        CategoryEntity changes = new CategoryEntity();
        changes.setItemType(ItemType.BAR);

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.findByItemType(ItemType.BAR)).thenReturn(Optional.empty());
        when(repository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        CategoryEntity out = service.update(id, changes);
        assertThat(out.getItemType()).isEqualTo(ItemType.BAR);
        verify(repository).save(existing);
    }

    @Test
    void update_throwsWhenNewTypeTakenByOther() {
        UUID id = UUID.randomUUID();
        CategoryEntity existing = new CategoryEntity();
        existing.setId(id);
        existing.setItemType(ItemType.KITCHEN);

        CategoryEntity other = new CategoryEntity();
        other.setId(UUID.randomUUID());
        other.setItemType(ItemType.BAR);

        CategoryEntity changes = new CategoryEntity();
        changes.setItemType(ItemType.BAR);

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.findByItemType(ItemType.BAR)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(id, changes))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Category for itemType already exists");
        // ensure no save called
        verify(repository, never()).save(any());
    }

    @Test
    void update_keepsExisting_whenChangesHasNullItemType() {
        UUID id = UUID.randomUUID();
        CategoryEntity existing = new CategoryEntity();
        existing.setId(id);
        existing.setItemType(ItemType.KITCHEN);

        CategoryEntity changes = new CategoryEntity();
        changes.setItemType(null);

        when(repository.findById(id)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenAnswer(inv -> inv.getArgument(0));

        CategoryEntity out = service.update(id, changes);
        assertThat(out.getItemType()).isEqualTo(ItemType.KITCHEN);
        verify(repository).save(existing);
    }

    @Test
    void delete_deletes_whenPresent() {
        UUID id = UUID.randomUUID();
        CategoryEntity e = new CategoryEntity();
        e.setId(id);
        when(repository.findById(id)).thenReturn(Optional.of(e));
        doNothing().when(repository).deleteById(id);

        service.delete(id);

        verify(repository).deleteById(id);
    }

    @Test
    void delete_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
    }
}