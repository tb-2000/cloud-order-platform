package com.cloudorder.service.service;

import com.cloudorder.service.dto.ProductResponse;
import com.cloudorder.service.entity.Product;
import com.cloudorder.service.exception.ProductNotFoundException;
import com.cloudorder.service.repository.ProductRepository;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import org.springframework.test.util.ReflectionTestUtils;

import com.cloudorder.service.dto.ProductRequest;
import com.cloudorder.service.dto.StockUpdateRequest;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {
    
    @Mock 
    private ProductRepository productRepository;

    @Mock 
    private EntityManager entityManager;

    @InjectMocks 
    private ProductService productService;
    private Product testProduct;

    @BeforeEach
    void setUp() {
        testProduct = new Product(
                "Gaming Laptop",
                "32 GB RAM",
                new BigDecimal("1499.99"),
                10
        );

        ReflectionTestUtils.setField(testProduct, "id", 1L);
    }

    @Test 
    void getProductById_shouldReturnProduct_whenProductExists() {
        // Arrange
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response = productService.getProductById(productId);

        // Assert
        assertThat(response.name()).isEqualTo("Gaming Laptop");
        assertThat(response.description()).isEqualTo("32 GB RAM");
        assertThat(response.price()).isEqualTo(new BigDecimal("1499.99"));
        assertThat(response.stock()).isEqualTo(10);

        verify(productRepository).findById(productId);
    }

    @Test 
    void getProductById_shouldThrowException_whenProductDoesNotExist() {
        // Arrange
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.getProductById(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found with ID: " + productId);

        verify(productRepository).findById(productId);
    }

    @Test 
    void getAllProducts_shouldReturnAllProducts() {

        // Arrange
        when(productRepository.findAll()).thenReturn(List.of(testProduct));

        // Act
        List<ProductResponse> responses = productService.getAllProducts();

        // Assert
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).id()).isEqualTo(1L);
        assertThat(responses.get(0).name()).isEqualTo("Gaming Laptop");
        assertThat(responses.get(0).description()).isEqualTo("32 GB RAM");
        assertThat(responses.get(0).price()).isEqualTo(new BigDecimal("1499.99"));
        assertThat(responses.get(0).stock()).isEqualTo(10);

        verify(productRepository).findAll();
    }

    @Test
    void getAllProducts_shouldReturnEmptyList() {
        // Arrange
        when(productRepository.findAll()).thenReturn(List.of());

        // Act
        List<ProductResponse> responses = productService.getAllProducts();

        // Assert
        assertThat(responses).isEmpty();

        verify(productRepository).findAll();
    }

    @Test
    void createProduct_shouldSaveAndReturnProduct() {
        // Arrange
        ProductRequest request = new ProductRequest(
                "Smartphone",
                "128 GB Storage",
                new BigDecimal("699.99"),
                20
        );

        when(productRepository.saveAndFlush(any(Product.class))).thenAnswer(invocation -> {
            Product product = invocation.getArgument(0);
            ReflectionTestUtils.setField(product, "id", 2L);
            return product;
        });

        // Act
        ProductResponse response = productService.createProduct(request);

        // Assert
        assertThat(response.id()).isEqualTo(2L);
        assertThat(response.name()).isEqualTo("Smartphone");
        assertThat(response.description()).isEqualTo("128 GB Storage");
        assertThat(response.price()).isEqualTo(new BigDecimal("699.99"));
        assertThat(response.stock()).isEqualTo(20);

        verify(productRepository).saveAndFlush(any(Product.class));
        verify(entityManager).refresh(any(Product.class));
    }

    @Test
    void updateProduct_shouldUpdateExistingProduct() {
        // Arrange
        Long productId = 1L;
        ProductRequest request = new ProductRequest(
                "Gaming Laptop Pro",
                "64 GB RAM",
                new BigDecimal("1999.99"),
                5
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response = productService.updateProduct(productId, request);

        // Assert
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.name()).isEqualTo("Gaming Laptop Pro");
        assertThat(response.description()).isEqualTo("64 GB RAM");
        assertThat(response.price()).isEqualTo(new BigDecimal("1999.99"));
        assertThat(response.stock()).isEqualTo(5);

        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class)); // Ensure save is not called on mock object productRepository
    }

    @Test
    void updateProduct_shouldThrowException_whenProductDoesNotExist() {
        // Arrange
        Long productId = 999L;
        ProductRequest request = new ProductRequest(
                "Non-existent Product",
                "No Description",
                new BigDecimal("0.00"),
                0
        );

        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.updateProduct(productId, request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found with ID: " + productId);

        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class)); // Ensure save is not called on mock object productRepository
    }

    @Test
    void deleteProduct_shouldDeleteExistingProduct() {
        // Arrange
        Long productId = 1L;
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // Act
        productService.deleteProduct(productId);

        // Assert
        verify(productRepository).findById(productId);
        verify(productRepository).delete(testProduct);
    }

    @Test
    void deleteProduct_shouldThrowException_whenProductDoesNotExist() {
        // Arrange
        Long productId = 999L;
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.deleteProduct(productId))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found with ID: " + productId);

        verify(productRepository).findById(productId);
        verify(productRepository, never()).delete(any(Product.class)); // Ensure delete is not called on mock object productRepository
    }

    @Test 
    void updateStock_shouldThrowException_whenProductDoesNotExist() {
        // Arrange
        Long productId = 999L;
        StockUpdateRequest request = new StockUpdateRequest(5);
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> productService.updateStock(productId, request))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("Product not found with ID: " + productId);

        verify(productRepository).findById(productId);
        verify(productRepository, never()).save(any(Product.class)); // Ensure save is not called on mock object productRepository
    }

    @Test 
    void updateStock_shouldUpdateStockOnly() {
        // Arrange
        Long productId = 1L;
        StockUpdateRequest request = new StockUpdateRequest(15);
        when(productRepository.findById(productId)).thenReturn(Optional.of(testProduct));

        // Act
        ProductResponse response = productService.updateStock(productId, request);

        // Assert
        assertThat(response.id()).isEqualTo(productId);
        assertThat(response.stock()).isEqualTo(15);
        assertThat(response.name()).isEqualTo("Gaming Laptop"); // Ensure other fields remain unchanged
        assertThat(response.description()).isEqualTo("32 GB RAM");
        assertThat(response.price()).isEqualTo(new BigDecimal("1499.99"));

        verify(productRepository).findById(productId); // Ensure findById is called on mock object productRepository
        verify(productRepository, never()).save(any(Product.class)); // Ensure save is not called on mock object productRepository
    }
}
