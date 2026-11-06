package com.universidad.catalogo.repository;

import com.universidad.catalogo.model.Producto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pruebas de INTEGRACIÓN del repositorio: @DataJpaTest carga solo la capa de
 * persistencia (entidades, repositorios y H2 en memoria). Cada prueba corre en
 * una transacción que se revierte al terminar, por eso setUp() puede insertar
 * los mismos dos productos antes de cada prueba sin que se acumulen.
 */
@DataJpaTest
class ProductoRepositoryTest {

    @Autowired ProductoRepository repo;
    @Autowired TestEntityManager em;

    @BeforeEach
    void setUp() {
        em.persistAndFlush(crear("Laptop", 1500.0, "ELECTRONICA"));
        em.persistAndFlush(crear("Cuaderno", 5.0, "PAPELERIA"));
    }

    @Test
    void findByCategoria_electronica_retornaUnProducto() {
        assertThat(repo.findByCategoria("ELECTRONICA")).hasSize(1)
                .extracting("nombre").containsExactly("Laptop");
    }

    @Test
    void findByCategoria_sinCoincidencias_retornaListaVacia() {
        assertThat(repo.findByCategoria("JUGUETERIA")).isEmpty();
    }

    @Test
    void findByCategoria_variosProductos_retornaTodosLosDeEsaCategoria() {
        em.persistAndFlush(crear("Lápiz", 1.5, "PAPELERIA"));

        assertThat(repo.findByCategoria("PAPELERIA"))
                .extracting("nombre").containsExactlyInAnyOrder("Cuaderno", "Lápiz");
    }

    @Test
    void findByCategoria_distingueMayusculas_retornaListaVacia() {
        // La consulta derivada compara por igualdad exacta
        assertThat(repo.findByCategoria("electronica")).isEmpty();
    }

    @Test
    void save_productoNuevo_asignaIdYSePuedeRecuperar() {
        Producto guardado = repo.save(crear("Monitor", 800.0, "ELECTRONICA"));
        em.flush();
        em.clear(); // fuerza a leer desde H2 y no desde el contexto de persistencia

        assertThat(guardado.getId()).isNotNull();
        assertThat(repo.findById(guardado.getId())).hasValueSatisfying(p -> {
            assertThat(p.getNombre()).isEqualTo("Monitor");
            assertThat(p.getPrecio()).isEqualTo(800.0);
        });
    }

    @Test
    void count_trasSetUp_retornaSoloLosDosProductosDeLaPrueba() {
        assertThat(repo.count()).isEqualTo(2);
    }

    private Producto crear(String nombre, double precio, String categoria) {
        Producto p = new Producto();
        p.setNombre(nombre);
        p.setPrecio(precio);
        p.setCategoria(categoria);
        return p;
    }
}
