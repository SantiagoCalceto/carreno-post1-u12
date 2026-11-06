package com.universidad.catalogo.service;

import com.universidad.catalogo.model.Producto;
import com.universidad.catalogo.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas UNITARIAS de ProductoService: el repositorio es un mock de Mockito,
 * así que no hay base de datos ni contexto de Spring. Se prueba solo la lógica
 * de negocio (validaciones y manejo del Optional).
 */
@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock        ProductoRepository repo;
    @InjectMocks ProductoService service;

    // ---------------------------------------------------------------- crear
    @Test
    void crear_conDatosValidos_guardaYRetorna() {
        Producto p = producto("Teclado mecánico", 120.0, "ELECTRONICA");
        when(repo.save(any())).thenReturn(p);

        assertThat(service.crear(p).getNombre()).isEqualTo("Teclado mecánico");
        verify(repo).save(p);
    }

    @Test
    void crear_conPrecioNegativo_lanzaIllegalArgumentException() {
        Producto p = producto("Mouse", -10.0, null);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.crear(p));
        assertThat(ex).hasMessage("El precio debe ser mayor a cero");
        verify(repo, never()).save(any()); // la validación rechaza ANTES de tocar el repositorio
    }

    @Test
    void crear_conPrecioCero_lanzaIllegalArgumentException() {
        Producto p = producto("Cable USB", 0.0, "ELECTRONICA");

        assertThrows(IllegalArgumentException.class, () -> service.crear(p));
        verify(repo, never()).save(any());
    }

    @Test
    void crear_conNombreVacio_lanzaIllegalArgumentException() {
        Producto p = producto("   ", 50.0, "PAPELERIA");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> service.crear(p));
        assertThat(ex).hasMessage("El nombre no puede estar vacío");
        verify(repo, never()).save(any());
    }

    @Test
    void crear_conNombreNulo_lanzaIllegalArgumentException() {
        Producto p = producto(null, 50.0, "PAPELERIA");

        assertThrows(IllegalArgumentException.class, () -> service.crear(p));
        verify(repo, never()).save(any());
    }

    // ---------------------------------------------------------------- buscarPorId
    @Test
    void buscarPorId_existe_retornaProducto() {
        Producto p = producto("Laptop", 1500.0, "ELECTRONICA");
        p.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(p));

        assertThat(service.buscarPorId(1L)).isSameAs(p);
    }

    @Test
    void buscarPorId_noExiste_lanzaEntityNotFoundException() {
        when(repo.findById(99L)).thenReturn(Optional.empty());

        EntityNotFoundException ex = assertThrows(EntityNotFoundException.class, () -> service.buscarPorId(99L));
        assertThat(ex).hasMessage("Producto no encontrado: 99");
    }

    // ---------------------------------------------------------------- listarPorCategoria
    @Test
    void listarPorCategoria_delegaEnElRepositorio_retornaSuResultado() {
        List<Producto> electronica = List.of(producto("Laptop", 1500.0, "ELECTRONICA"));
        when(repo.findByCategoria("ELECTRONICA")).thenReturn(electronica);

        assertThat(service.listarPorCategoria("ELECTRONICA")).isEqualTo(electronica);
        verify(repo).findByCategoria("ELECTRONICA");
    }

    // ---------------------------------------------------------------- actualizarPrecio
    @Test
    void actualizarPrecio_conValorValido_actualizaYGuarda() {
        Producto p = new Producto(); p.setId(1L); p.setPrecio(100.0);
        when(repo.findById(1L)).thenReturn(Optional.of(p));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.actualizarPrecio(1L, 150.0).getPrecio()).isEqualTo(150.0);
        verify(repo).save(p);
    }

    @Test
    void actualizarPrecio_conPrecioInvalido_lanzaIllegalArgumentExceptionSinConsultar() {
        assertThrows(IllegalArgumentException.class, () -> service.actualizarPrecio(1L, -1.0));
        verifyNoInteractions(repo); // ni siquiera busca el producto
    }

    @Test
    void actualizarPrecio_productoNoExiste_lanzaEntityNotFoundException() {
        when(repo.findById(7L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.actualizarPrecio(7L, 80.0));
        verify(repo, never()).save(any());
    }

    private static Producto producto(String nombre, double precio, String categoria) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setCategoria(categoria);
        return p;
    }
}
