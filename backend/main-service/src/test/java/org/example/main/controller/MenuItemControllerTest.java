package org.example.main.controller;

import org.example.main.dto.request.MenuItemRequestDto;
import org.example.main.dto.response.MenuItemResponseDto;
import org.example.main.model.CategoryEntity;
import org.example.main.model.MenuItem;
import org.example.main.model.Macros;
import org.example.main.model.enums.ItemType;
import org.example.main.service.IMenuItemService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MenuItemControllerTest {

    @Mock
    IMenuItemService menuItemService;

    @Test
    public void list_returnsMappedResponses() {
        MenuItem m1 = new MenuItem();
        m1.setId(UUID.randomUUID());
        m1.setName("Item1");
        m1.setPrice(BigDecimal.valueOf(1.23));
        m1.setCategory(new CategoryEntity());
        m1.setCalories(100);
        m1.setMacros(new Macros(1,2,3));
        m1.setAvailable(true);
        m1.setItemType(ItemType.BAR);

        MenuItem m2 = new MenuItem();
        m2.setId(UUID.randomUUID());
        m2.setName("Item2");
        m2.setPrice(BigDecimal.valueOf(4.56));
        m2.setCategory(new CategoryEntity());
        m2.setCalories(200);
        m2.setMacros(new Macros(4,5,6));
        m2.setAvailable(false);
        m2.setItemType(ItemType.KITCHEN);

        when(menuItemService.findAll()).thenReturn(List.of(m1, m2));

        MenuItemController ctrl = new MenuItemController(menuItemService);
        ResponseEntity<List<MenuItemResponseDto>> resp = ctrl.list();

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        List<MenuItemResponseDto> body = resp.getBody();
        assertThat(body).hasSize(2);
        assertThat(body).extracting(MenuItemResponseDto::getId).containsExactly(m1.getId(), m2.getId());
    }

    @Test
    public void get_returnsMappedItem() {
        UUID id = UUID.randomUUID();
        MenuItem m = new MenuItem();
        m.setId(id);
        m.setName("Thing");
        m.setPrice(BigDecimal.valueOf(9.99));
        m.setCategory(new CategoryEntity());
        m.setCalories(123);
        m.setMacros(new Macros(1,1,1));
        m.setAvailable(true);
        m.setItemType(ItemType.BAR);

        when(menuItemService.findById(id)).thenReturn(m);

        MenuItemController ctrl = new MenuItemController(menuItemService);
        ResponseEntity<MenuItemResponseDto> resp = ctrl.get(id);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        MenuItemResponseDto dto = resp.getBody();
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getName()).isEqualTo("Thing");
    }

    @Test
    public void create_callsService_and_returnsCreatedWithLocation() {
        MenuItemRequestDto req = new MenuItemRequestDto();
        req.setName("New");
        req.setDescription("Desc");
        req.setPrice(BigDecimal.valueOf(2.50));
        req.setCalories(50);
        req.setAvailable(true);
        req.setItemType(ItemType.BAR);

        MenuItem created = new MenuItem();
        UUID id = UUID.randomUUID();
        created.setId(id);
        created.setName(req.getName());
        created.setPrice(req.getPrice());
        created.setCategory(new CategoryEntity());
        created.setCalories(req.getCalories());
        created.setMacros(new Macros(0,0,0));
        created.setAvailable(req.isAvailable());
        created.setItemType(req.getItemType());

        when(menuItemService.create(any(MenuItem.class))).thenReturn(created);

        MenuItemController ctrl = new MenuItemController(menuItemService);
        ResponseEntity<MenuItemResponseDto> resp = ctrl.create(req);

        assertThat(resp.getStatusCodeValue()).isEqualTo(201);
        assertThat(resp.getHeaders().getLocation()).isEqualTo(URI.create("/api/menu/" + id));
        MenuItemResponseDto body = resp.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getId()).isEqualTo(id);

        ArgumentCaptor<MenuItem> captor = ArgumentCaptor.forClass(MenuItem.class);
        verify(menuItemService).create(captor.capture());
        MenuItem passed = captor.getValue();
        assertThat(passed.getName()).isEqualTo("New");
    }

    @Test
    public void update_findsCopiesAndUpdates_thenReturnsUpdated() {
        UUID id = UUID.randomUUID();
        MenuItemRequestDto changes = new MenuItemRequestDto();
        changes.setName("Changed");
        changes.setPrice(BigDecimal.valueOf(7.77));
        changes.setCalories(77);
        changes.setAvailable(false);
        changes.setItemType(ItemType.KITCHEN);

        MenuItem existing = new MenuItem();
        existing.setId(id);
        existing.setName("Old");
        existing.setPrice(BigDecimal.valueOf(1.11));
        existing.setCategory(new CategoryEntity());
        existing.setCalories(10);
        existing.setMacros(new Macros(1,2,3));
        existing.setAvailable(true);
        existing.setItemType(ItemType.BAR);

        MenuItem updated = new MenuItem();
        updated.setId(id);
        updated.setName(changes.getName());
        updated.setPrice(changes.getPrice());
        updated.setCategory(existing.getCategory());
        updated.setCalories(changes.getCalories());
        updated.setMacros(existing.getMacros());
        updated.setAvailable(changes.isAvailable());
        updated.setItemType(changes.getItemType());

        when(menuItemService.findById(id)).thenReturn(existing);
        when(menuItemService.update(eq(id), any(MenuItem.class))).thenReturn(updated);

        MenuItemController ctrl = new MenuItemController(menuItemService);
        ResponseEntity<MenuItemResponseDto> resp = ctrl.update(id, changes);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        MenuItemResponseDto dto = resp.getBody();
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(id);
        assertThat(dto.getName()).isEqualTo("Changed");
        verify(menuItemService).findById(id);
        verify(menuItemService).update(eq(id), any(MenuItem.class));
    }

    @Test
    public void delete_callsService_and_returnsNoContent() {
        UUID id = UUID.randomUUID();
        MenuItemController ctrl = new MenuItemController(menuItemService);
        ResponseEntity<Void> resp = ctrl.delete(id);

        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();
        verify(menuItemService).delete(id);
    }
}