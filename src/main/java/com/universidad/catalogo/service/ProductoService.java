package com.universidad.catalogo.service;

import com.universidad.catalogo.model.Producto;
import com.universidad.catalogo.repository.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository repo;

    public ProductoService(ProductoRepository repo) {
        this.repo = repo;
    }

    public Producto crear(Producto producto) {
        if (producto.getNombre() == null || producto.getNombre().isBlank())
            throw new IllegalArgumentException("El nombre no puede estar vacío");
        if (producto.getPrecio() <= 0)
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        return repo.save(producto);
    }

    // Lanza EntityNotFoundException en vez de retornar null: contrato explícito y verificable con assertThrows
    public Producto buscarPorId(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Producto no encontrado: " + id));
    }

    public List<Producto> listarPorCategoria(String categoria) {
        return repo.findByCategoria(categoria);
    }

    public Producto actualizarPrecio(Long id, double nuevoPrecio) {
        if (nuevoPrecio <= 0)
            throw new IllegalArgumentException("El precio debe ser mayor a cero");
        Producto p = buscarPorId(id);
        p.setPrecio(nuevoPrecio);
        return repo.save(p);
    }
}
