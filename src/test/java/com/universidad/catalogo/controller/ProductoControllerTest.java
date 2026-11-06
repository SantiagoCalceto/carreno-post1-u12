package com.universidad.catalogo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.universidad.catalogo.model.Producto;
import com.universidad.catalogo.service.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de la CAPA WEB: @WebMvcTest carga solo ProductoController (más
 * Jackson, validación y el manejo de errores de Spring MVC). ProductoService se
 * reemplaza con @MockBean y MockMvc simula las peticiones HTTP sin abrir un puerto.
 */
@WebMvcTest(ProductoController.class)
class ProductoControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean  ProductoService service;

    // ---------------------------------------------------------------- GET /{id}
    @Test
    void get_productoExiste_retorna200() throws Exception {
        Producto p = producto(1L, "Laptop", 1500.0, "ELECTRONICA");
        when(service.buscarPorId(1L)).thenReturn(p);

        mockMvc.perform(get("/api/productos/1"))
               .andExpect(status().isOk())
               .andExpect(content().contentType(MediaType.APPLICATION_JSON))
               .andExpect(jsonPath("$.nombre").value("Laptop"))
               .andExpect(jsonPath("$.precio").value(1500.0));
    }

    @Test
    void get_noExiste_retorna404() throws Exception {
        when(service.buscarPorId(99L)).thenThrow(new EntityNotFoundException("Producto no encontrado: 99"));

        mockMvc.perform(get("/api/productos/99"))
               .andExpect(status().isNotFound())
               .andExpect(content().string("Producto no encontrado: 99"));
    }

    @Test
    void get_idNoNumerico_retorna400() throws Exception {
        mockMvc.perform(get("/api/productos/abc"))
               .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    // ---------------------------------------------------------------- GET ?categoria=
    @Test
    void listar_porCategoria_retorna200ConLaLista() throws Exception {
        when(service.listarPorCategoria("ELECTRONICA")).thenReturn(List.of(
                producto(1L, "Laptop", 1500.0, "ELECTRONICA"),
                producto(2L, "Mouse", 50.0, "ELECTRONICA")));

        mockMvc.perform(get("/api/productos").param("categoria", "ELECTRONICA"))
               .andExpect(status().isOk())
               .andExpect(jsonPath("$", hasSize(2)))
               .andExpect(jsonPath("$[1].nombre").value("Mouse"));
    }

    @Test
    void listar_sinParametroCategoria_retorna400() throws Exception {
        mockMvc.perform(get("/api/productos"))
               .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    // ---------------------------------------------------------------- POST
    @Test
    void post_conDatosValidos_retorna201() throws Exception {
        Producto entrada = producto(null, "Mouse", 50.0, "ELECTRONICA");
        Producto guardado = producto(1L, "Mouse", 50.0, "ELECTRONICA");
        when(service.crear(any())).thenReturn(guardado);

        mockMvc.perform(post("/api/productos")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(entrada)))
               .andExpect(status().isCreated())
               .andExpect(jsonPath("$.id").value(1))
               .andExpect(jsonPath("$.nombre").value("Mouse"));
        verify(service).crear(any(Producto.class));
    }

    @Test
    void post_conPrecioInvalido_retorna400() throws Exception {
        Producto entrada = producto(null, "Auricular", -5.0, null);
        when(service.crear(any())).thenThrow(new IllegalArgumentException("El precio debe ser mayor a cero"));

        mockMvc.perform(post("/api/productos")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content(objectMapper.writeValueAsString(entrada)))
               .andExpect(status().isBadRequest())
               .andExpect(content().string("El precio debe ser mayor a cero"));
    }

    @Test
    void post_conJsonMalformado_retorna400() throws Exception {
        mockMvc.perform(post("/api/productos")
                       .contentType(MediaType.APPLICATION_JSON)
                       .content("{\"nombre\": \"Mouse\", \"precio\": "))
               .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }

    private static Producto producto(Long id, String nombre, double precio, String categoria) {
        Producto p = new Producto();
        p.setId(id);
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setCategoria(categoria);
        return p;
    }
}
