package org.example.main.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.main.dto.response.RoleResponseDto;
import org.example.main.mapper.RoleMapper;
import org.example.main.model.Role;
import org.example.main.service.IRoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    @Mock
    IRoleService roleService;

    @Mock
    RoleMapper mapper;

    private MockMvc mvc;
    private RoleController controller;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        controller = new RoleController(roleService, mapper);

        
        mvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void list_returnsDtoList() throws Exception {
        Role r = new Role();
        r.setId(UUID.randomUUID());
        r.setName("ADMIN");

        RoleResponseDto dto = RoleResponseDto.builder().id(r.getId()).name(r.getName()).build();

        when(roleService.findAll()).thenReturn(List.of(r));
        when(mapper.toDtoList(any())).thenReturn(List.of(dto));

        mvc.perform(get("/api/roles").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$[0].id").value(dto.getId().toString()))
                .andExpect(jsonPath("$[0].name").value("ADMIN"));
    }

    @Test
    void get_returnsDto() throws Exception {
        UUID id = UUID.randomUUID();
        Role r = new Role();
        r.setId(id);
        r.setName("USER");

        RoleResponseDto dto = RoleResponseDto.builder().id(id).name("USER").build();

        when(roleService.findById(id)).thenReturn(r);
        when(mapper.toDto(r)).thenReturn(dto);

        mvc.perform(get("/api/roles/{id}", id.toString()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.name").value("USER"));
    }
}