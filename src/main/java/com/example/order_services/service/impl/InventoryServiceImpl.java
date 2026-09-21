package com.example.order_services.service.impl;

import com.example.order_services.common.EnumCode;
import com.example.order_services.common.StockStatus;
import com.example.order_services.dto.request.UpdateInventoryQuantityRequest;
import com.example.order_services.dto.request.UpdateProductInStockRequest;
import com.example.order_services.dto.response.GeneralnventoryResponse;
import com.example.order_services.dto.response.InventoryResponse;
import com.example.order_services.dto.response.ProductInventoryResponse;
import com.example.order_services.entity.Inventory;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.InventoryRepository;
import com.example.order_services.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class InventoryServiceImpl implements InventoryService {
    private final InventoryRepository inventoryRepository;

    // Update the quantity of a product variant in the inventory
    @Override
    @Transactional
    public InventoryResponse updateQuantity(String productVariantId, UpdateInventoryQuantityRequest request) {
        if (request.getQuantity() == null || request.getQuantity() < 0) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Inventory quantity must be nonnegative");
        }
        Inventory inventory = inventoryRepository.findByProductVariantIdsForUpdate(List.of(productVariantId)).stream()
                .findFirst().orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Inventory not found"));
        inventory.setQuantityInStock(request.getQuantity());
        inventoryRepository.save(inventory);
        return new InventoryResponse(inventory.getId(), productVariantId, inventory.getQuantityInStock());
    }


    // Get general infomation
    @Override
    public GeneralnventoryResponse getGeneralInventoryInfo() {
        // Implementation for getting general inventory information
        if (inventoryRepository.findAll().isEmpty()){
            throw new ApplicationException(EnumCode.NOT_FOUND, "Inventory has nothing");
        }

        return GeneralnventoryResponse.builder()
                .totalInventoryValue(inventoryRepository.countTotalInventoryValue())
                .totalProductsInStock(inventoryRepository.countTotalProductInInventory())
                .totalProductsPrepareToOutOfStock(inventoryRepository.countTotalProductAboutToOutOfStock())
                .build();
    }

    // findProduct in stock with filter
    @Override
    @Transactional(readOnly = true)
    public Page<ProductInventoryResponse> getProductsInventoryInStockWithQuery(String query, int page) {
        if (page < 0) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Page must be nonnegative");
        }
        String productName = query == null ? "" : query.trim();
        return inventoryRepository.findProductsInventoryByName(productName, PageRequest.of(page, 4))
                .map(inventory -> {
                    var variant = inventory.getProductVariant();
                    int quantity = inventory.getQuantityInStock();
                    StockStatus status = quantity <= 0 ? StockStatus.OUT_OF_STOCK
                            : quantity < 20 ? StockStatus.LIMITED_STOCK : StockStatus.IN_STOCK;
                    return new ProductInventoryResponse(
                            variant.getProduct().getProductName(), variant.getId(), variant.getPrice(),
                            quantity, status.name());
                });
     }

     // update product in stock

    @Override
    @Transactional
    public ProductInventoryResponse updateProductInStock(String productId, String variantsId,
                                                         UpdateProductInStockRequest request) {
        if (request == null
                || (request.getQuantityInStock() != null && request.getQuantityInStock() < 0)
                || (request.getProductPrice() != null && request.getProductPrice().signum() < 0)
                || (request.getProductName() != null && request.getProductName().isBlank())
                || (request.getProductType() != null && request.getProductType().isBlank())
                || (request.getProductDescription() != null && request.getProductDescription().isBlank())) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Invalid product information");
        }
        if (request.getProductVariantId() != null && !variantsId.equals(request.getProductVariantId())) {
            throw new ApplicationException(EnumCode.BAD_REQUEST, "Product variant ID cannot be changed");
        }
        Inventory inventory = inventoryRepository.findByProductVariantIdsForUpdate(List.of(variantsId))
                .stream().findFirst()
                .orElseThrow(() -> new ApplicationException(EnumCode.NOT_FOUND, "Inventory not found"));
        var variant = inventory.getProductVariant();
        var product = variant.getProduct();
        if (!product.getId().equals(productId)) {
            throw new ApplicationException(EnumCode.NOT_FOUND, "Variant does not belong to product");
        }

        // PATCH: omitted fields retain their current values.
        product.setProductName(request.getProductName());
        product.setProductType(request.getProductType());
        variant.setPrice(request.getProductPrice());
        variant.setProductVariant(request.getProductDescription());
        inventory.setQuantityInStock(request.getQuantityInStock());

        // All three entities are managed; JPA flushes their changes in this transaction.
        int quantity = inventory.getQuantityInStock();
        StockStatus status = quantity <= 0 ? StockStatus.OUT_OF_STOCK
                : quantity < 20 ? StockStatus.LIMITED_STOCK : StockStatus.IN_STOCK;
        return new ProductInventoryResponse(product.getProductName(), variant.getId(), variant.getPrice(),
                quantity, status.name());
    }
}
