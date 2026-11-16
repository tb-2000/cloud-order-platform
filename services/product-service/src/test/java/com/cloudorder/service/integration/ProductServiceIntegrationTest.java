package com.cloudorder.service.integration;

import com.cloudorder.service.dto.ProductRequest;
import com.cloudorder.service.dto.ProductResponse;
import com.cloudorder.service.dto.StockUpdateRequest;
import com.cloudorder.service.entity.Product;
import com.cloudorder.service.repository.ProductRepository;
import com.cloudorder.service.service.ProductService;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;


@SpringBootTest
@Testcontainers
class ProductServiceIntegrationTest {
    
    @Container 
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17")
        .withDatabaseName("productdb_test")
        .withUsername("testuser")
        .withPassword("testpasswort");

    @Autowired 
    private ProductService productService;

    @Autowired 
    private ProductRepository productRepository;

    @Autowired 
    private EntityManager entityManager;

    @Autowired 
    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void cleanDatabase() {
        productRepository.deleteAll();
    }

    @Test 
    void createProduct_shouldPersistProductInDatabase() {
        ProductRequest request = new ProductRequest(
            "Gaming Laptop",
            "32 Gb RAM, 1 TB SSD, NVIDIA RTX 3080",
            new BigDecimal("1499.99"),
            10
        );

        ProductResponse response = productService.createProduct(request);

        Product savedProduct = productRepository.findById(response.id()).orElseThrow();

        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo(request.name());
        assertThat(savedProduct.getDescription()).isEqualTo(request.description());
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(request.price());
        assertThat(savedProduct.getStock()).isEqualTo(request.stock());
        assertThat(savedProduct.getCreatedAt()).isNotNull();
    }

    @Test 
    void updateProduct_shouldPersistChangesUsingDirtyChecking() {
        Product product = new Product(
            "Gaming Laptop",
            "16 Gb RAM, 1 TB SSD, NVIDIA RTX 3080",
            new BigDecimal("1199.99"),
            10
        );

        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        ProductRequest request = new ProductRequest(
            "Gaming Laptop",
            "32 Gb RAM, 1 TB SSD, NVIDIA RTX 3080",
            new BigDecimal("1499.99"),
            10
        );

        productService.updateProduct(productId, request);

        // Start a new transaction and clear its persistence context
        // before reading the updated entity.
        Product updatedProduct = transactionTemplate.execute(status -> {
            entityManager.clear();
            return productRepository.findById(productId).orElseThrow();
        });

        assertThat(updatedProduct).isNotNull();
        assertThat(updatedProduct.getName()).isEqualTo(request.name());
        assertThat(updatedProduct.getDescription()).isEqualTo(request.description());
        assertThat(updatedProduct.getPrice()).isEqualByComparingTo(request.price());
        assertThat(updatedProduct.getStock()).isEqualTo(request.stock());
    }

    @Test 
    void updateStock_shouldPersistStockChange() {
        Product product = new Product(
            "Gaming Laptop",
            "16 Gb RAM, 1 TB SSD, NVIDIA RTX 3080",
            new BigDecimal("1199.99"),
            10
        );

        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        StockUpdateRequest request = new StockUpdateRequest(5);

        productService.updateStock(productId, request);

        // Start a new transaction and clear its persistence context
        // before reading the updated entity.
        Product updatedProduct = transactionTemplate.execute(status -> {
            entityManager.clear();
            return productRepository.findById(productId).orElseThrow();
        });

        assertThat(updatedProduct).isNotNull();
        assertThat(updatedProduct.getStock()).isEqualTo(request.stock());
    }

    @Test 
    void deleteProduct_shouldRemoveProductFromDatabase() {
        Product product = new Product(
            "Gaming Laptop",
            "16 Gb RAM, 1 TB SSD, NVIDIA RTX 3080",
            new BigDecimal("1199.99"),
            10
        );

        Product savedProduct = productRepository.save(product);
        Long productId = savedProduct.getId();

        productService.deleteProduct(productId);

        boolean exists = productRepository.existsById(productId);
        assertThat(exists).isFalse();
    }
}
