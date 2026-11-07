package com.cloudorder.service.service;

import com.cloudorder.service.dto.ProductRequest;
import com.cloudorder.service.dto.ProductResponse;
import com.cloudorder.service.entity.Product;
import com.cloudorder.service.repository.ProductRepository;
import java.util.List;
import jakarta.persistence.EntityManager;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.cloudorder.service.dto.StockUpdateRequest;
import com.cloudorder.service.exception.ProductNotFoundException;

@Service
public class ProductService {

    final private ProductRepository productRepository;
    final private EntityManager entityManager;

    public ProductService(ProductRepository productRepository, EntityManager entityManager) {
        this.productRepository = productRepository;
        this.entityManager = entityManager;
    }

    @Transactional 
    public ProductResponse createProduct(ProductRequest request) {

        Product product = new Product(
            request.name(),
            request.description(),
            request.price(),
            request.stock()
        );

        Product savedProduct = productRepository.saveAndFlush(product);

        // Reload database-generated fields, e.g. created_at
        entityManager.refresh(savedProduct);

        return new ProductResponse(
            savedProduct.getId(),
            savedProduct.getName(),
            savedProduct.getDescription(),
            savedProduct.getPrice(),
            savedProduct.getStock(),
            savedProduct.getCreatedAt()
        );
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        List<Product> products = productRepository.findAll();

        return products.stream()
            .map(product -> new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCreatedAt()
            ))
            .toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductById(Long id) {

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));
        
        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock(),
            product.getCreatedAt()
        );
    }

    @Transactional 
    public ProductResponse updateProduct(Long id, ProductRequest request) {

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        product.setName(request.name());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStock(request.stock());

        Product updatedProduct = productRepository.save(product);

        return new ProductResponse(
            updatedProduct.getId(),
            updatedProduct.getName(),
            updatedProduct.getDescription(),
            updatedProduct.getPrice(),
            updatedProduct.getStock(),
            updatedProduct.getCreatedAt()
        );
    }

    @Transactional 
    public void deleteProduct(Long id) {

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        productRepository.delete(product);
    }

    @Transactional 
    public ProductResponse updateStock(Long id, StockUpdateRequest request) {

        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ProductNotFoundException(id));

        product.setStock(request.stock());

        productRepository.save(product);

        return new ProductResponse(
            product.getId(),
            product.getName(),
            product.getDescription(),
            product.getPrice(),
            product.getStock(),
            product.getCreatedAt()
        );
    }
}
