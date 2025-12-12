package org.example.main.controller.category;

import org.example.main.dto.request.category.CategoryRequestDto;
import org.example.main.dto.response.category.CategoryResponseDto;
import org.example.main.mapper.category.CategoryMapper;
import org.example.main.model.category.CategoryEntity;
import org.example.main.model.enums.ItemType;
import org.example.main.service.category.ICategoryService;
import org.example.main.exception.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CategoryController.
 * These tests call controller methods directly (no Spring context) to exercise mapping and interaction
 * with the service.
 */
@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    ICategoryService service;

    @InjectMocks
    CategoryController controller;

    @Test
    void list_returnsMappedResponseList() {
        CategoryEntity e1 = new CategoryEntity();
        e1.setId(UUID.randomUUID());
        e1.setItemType(ItemType.KITCHEN);

        CategoryEntity e2 = new CategoryEntity();
        e2.setId(UUID.randomUUID());
        e2.setItemType(ItemType.BAR);

        when(service.findAll()).thenReturn(List.of(e1, e2));

        ResponseEntity<List<CategoryResponseDto>> resp = controller.list();

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        List<CategoryResponseDto> body = resp.getBody();
        assertThat(body).hasSize(2);
        assertThat(body.get(0).getId()).isEqualTo(e1.getId());
        assertThat(body.get(1).getItemType()).isEqualTo(e2.getItemType());
        verify(service).findAll();
    }

    @Test
    void get_returnsMappedEntity() {
        UUID id = UUID.randomUUID();
        CategoryEntity e = new CategoryEntity();
        e.setId(id);
        e.setItemType(ItemType.BAR);

        when(service.findById(id)).thenReturn(e);

        ResponseEntity<CategoryResponseDto> resp = controller.get(id);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        CategoryResponseDto body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(id);
        assertThat(body.getItemType()).isEqualTo(ItemType.BAR);
        verify(service).findById(id);
    }

    @Test
    void get_propagatesNotFound() {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenThrow(new ResourceNotFoundException("Category not found: " + id));

        assertThatThrownBy(() -> controller.get(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
        verify(service).findById(id);
    }

    @Test
    void create_returnsCreatedResponseAndLocationHeader() {
        CategoryRequestDto req = new CategoryRequestDto();
        req.setItemType(ItemType.KITCHEN);

        CategoryEntity toCreate = CategoryMapper.toEntity(req);
        CategoryEntity created = new CategoryEntity();
        UUID id = UUID.randomUUID();
        created.setId(id);
        created.setItemType(ItemType.KITCHEN);

        when(service.create(any(CategoryEntity.class))).thenReturn(created);

        ResponseEntity<CategoryResponseDto> resp = controller.create(req);

        assertThat(resp.getStatusCodeValue()).isEqualTo(201);
        assertThat(resp.getHeaders().getLocation()).isEqualTo(URI.create("/api/categories/" + id));
        CategoryResponseDto body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(id);
        assertThat(body.getItemType()).isEqualTo(ItemType.KITCHEN);

        ArgumentCaptor<CategoryEntity> captor = ArgumentCaptor.forClass(CategoryEntity.class);
        verify(service).create(captor.capture());
        // ensure mapper produced entity with correct itemType
        assertThat(captor.getValue().getItemType()).isEqualTo(ItemType.KITCHEN);
    }

    @Test
    void create_propagatesServiceError() {
        CategoryRequestDto req = new CategoryRequestDto();
        req.setItemType(ItemType.BAR);

        when(service.create(any())).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.CONFLICT, "dup"));

        assertThatThrownBy(() -> controller.create(req))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("dup");
        verify(service).create(any());
    }

    @Test
    void update_returnsOkWithUpdatedBody() {
        UUID id = UUID.randomUUID();
        CategoryRequestDto req = new CategoryRequestDto();
        req.setItemType(ItemType.BAR);

        CategoryEntity changes = CategoryMapper.toEntity(req);
        CategoryEntity updated = new CategoryEntity();
        updated.setId(id);
        updated.setItemType(ItemType.BAR);

        when(service.update(eq(id), any(CategoryEntity.class))).thenReturn(updated);

        ResponseEntity<CategoryResponseDto> resp = controller.update(id, req);

        assertThat(resp.getStatusCodeValue()).isEqualTo(200);
        CategoryResponseDto body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(id);
        assertThat(body.getItemType()).isEqualTo(ItemType.BAR);

        ArgumentCaptor<CategoryEntity> captor = ArgumentCaptor.forClass(CategoryEntity.class);
        verify(service).update(eq(id), captor.capture());
        assertThat(captor.getValue().getItemType()).isEqualTo(ItemType.BAR);
    }

    @Test
    void delete_returnsNoContentAndInvokesService() {
        UUID id = UUID.randomUUID();
        doNothing().when(service).delete(id);

        ResponseEntity<Void> resp = controller.delete(id);

        assertThat(resp.getStatusCodeValue()).isEqualTo(204);
        verify(service).delete(id);
    }

    @Test
    void delete_propagatesNotFound() {
        UUID id = UUID.randomUUID();
        doThrow(new ResourceNotFoundException("Category not found: " + id)).when(service).delete(id);

        assertThatThrownBy(() -> controller.delete(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Category not found");
        verify(service).delete(id);
    }
}