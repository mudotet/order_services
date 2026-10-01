package com.example.order_services.repository;

import com.example.order_services.entity.*;
import com.example.order_services.controller.InventoryController;
import com.example.order_services.exception.GlobalExceptionHandler;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.example.order_services.service.impl.InventoryServiceImpl;
import com.example.order_services.exception.ApplicationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.*;

@DataJpaTest(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class InventorySearchTest {
    @Autowired jakarta.persistence.EntityManager entityManager;
    @Autowired InventoryRepository inventories;
    @Autowired ProductRepository products;
    @Autowired ProductVariantRepository variants;

    @Test
    void searchesNamesAndPagesVariantsWithStockStates() {
        seed("Milk Tea", -1, false, false, false);
        seed("Milk Tea", 0, false, false, false);
        seed("Milk Tea", 1, false, false, false);
        seed("Milk Tea", 19, false, false, false);
        seed("Milk Tea", 20, false, false, false);
        seed("Milk Tea", 21, false, false, false);
        seed("Coffee", 5, false, false, false);
        seed("Milk Tea", 5, true, false, false);
        seed("Milk Tea", 5, false, true, false);
        seed("Milk Tea", 5, false, false, true);
        var service = new InventoryServiceImpl(inventories);
        var first = service.getProductsInventoryInStockWithQuery(" tEA ", 0);
        var second = service.getProductsInventoryInStockWithQuery("tea", 1);
        assertThat(first.getTotalElements()).isEqualTo(6);
        assertThat(first.getTotalPages()).isEqualTo(2);
        assertThat(first.getContent()).hasSize(4);
        assertThat(second.getContent()).hasSize(2);
        var all = java.util.stream.Stream.concat(first.stream(), second.stream()).toList();
        assertThat(all).extracting(r -> r.getProductVariantId()).doesNotHaveDuplicates();
        assertThat(all).allSatisfy(r -> {
            assertThat(r.getProductName()).isEqualTo("Milk Tea");
            assertThat(r.getProductPrice()).isEqualByComparingTo("12.50");
            int stock = r.getProductStockQuantity().intValue();
            String expected = switch (stock) {
                case -1, 0 -> "OUT_OF_STOCK";
                case 1, 19 -> "LIMITED_STOCK";
                case 20, 21 -> "IN_STOCK";
                default -> throw new AssertionError("Unexpected stock");
            };
            assertThat(r.getProductStockState()).isEqualTo(expected);
        });
        assertThat(service.getProductsInventoryInStockWithQuery("", 0).getTotalElements()).isEqualTo(7);
        assertThat(service.getProductsInventoryInStockWithQuery("absent", 0)).isEmpty();
        assertThat(service.getProductsInventoryInStockWithQuery("tea", 2)).isEmpty();
        assertThatThrownBy(() -> service.getProductsInventoryInStockWithQuery("", -1))
                .isInstanceOf(ApplicationException.class);
    }

    @Test
    void endpointReturnsFourItemsAndPageMetadataAndRejectsNegativePage() throws Exception {
        for (int i = 0; i < 5; i++) {
            seed("Tea", 20, false, false, false);
        }
        var controller = new InventoryController(
                new InventoryServiceImpl(inventories));
        var mvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
        mvc.perform(get("/api/inventories/productsInStock")
                        .param("query", "tea"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(4))
                .andExpect(jsonPath("$.data.totalElements").value(5))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.content[0].productStockState").value("IN_STOCK"));
        mvc.perform(get("/api/inventories/productsInStock")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void persistsUpdatesAcrossAllThreeTables() {
        seed("Old", 10, false, false, false);
        var inventory = inventories.findAll().getFirst();
        String inventoryId = inventory.getId();
        String variantId = inventory.getProductVariant().getId();
        String productId = inventory.getProductVariant().getProduct().getId();
        var service = new InventoryServiceImpl(inventories);
        var request = com.example.order_services.dto.request.UpdateProductInStockRequest.builder()
                .productName("New").productType("Food").productDescription("Large updated")
                .productPrice(new BigDecimal("25.50")).quantityInStock(0).build();
        assertThat(service.updateProductInStock(productId, variantId, request).getProductStockState())
                .isEqualTo("OUT_OF_STOCK");
        entityManager.flush();
        entityManager.clear();
        assertThat(products.findById(productId).orElseThrow().getProductName()).isEqualTo("New");
        assertThat(products.findById(productId).orElseThrow().getProductType()).isEqualTo("Food");
        assertThat(variants.findById(variantId).orElseThrow().getPrice()).isEqualByComparingTo("25.50");
        assertThat(variants.findById(variantId).orElseThrow().getProductVariant()).isEqualTo("Large updated");
        assertThat(inventories.findById(inventoryId).orElseThrow().getQuantityInStock()).isZero();
    }

    private void seed(String name, int quantity, boolean deletedProduct, boolean deletedVariant, boolean deletedInventory) {
        Product product = Product.builder().productName(name).productType("Drink").build();
        product.setDeleted(deletedProduct);
        products.save(product);
        ProductVariant variant = ProductVariant.builder().product(product).productVariant("Large")
                .price(new BigDecimal("12.50")).build();
        variant.setDeleted(deletedVariant);
        variants.save(variant);
        Inventory inventory = Inventory.builder().productVariant(variant).quantityInStock(quantity).build();
        inventory.setDeleted(deletedInventory);
        inventories.saveAndFlush(inventory);
    }
}
