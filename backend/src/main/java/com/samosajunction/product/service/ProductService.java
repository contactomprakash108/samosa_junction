package com.samosajunction.product.service;

import com.samosajunction.common.exception.InvalidRequestException;
import com.samosajunction.common.exception.ResourceConflictException;
import com.samosajunction.common.exception.ResourceNotFoundException;
import com.samosajunction.inventory.repository.InventoryRepository;
import com.samosajunction.inventory.service.InventoryService;
import com.samosajunction.product.dto.CategoryResponse;
import com.samosajunction.product.dto.ProductRequest;
import com.samosajunction.product.dto.ProductResponse;
import com.samosajunction.product.dto.ProductSearchCriteria;
import com.samosajunction.product.entity.Category;
import com.samosajunction.product.entity.Product;
import com.samosajunction.product.repository.CategoryRepository;
import com.samosajunction.product.repository.ProductRepository;
import com.samosajunction.product.repository.ProductSpecifications;
import com.samosajunction.product.support.InrMoney;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "name", "name",
            "price", "pricePaise",
            "calories", "calories",
            "protein", "proteinGrams",
            "createdAt", "createdAt",
            "spiceLevel", "spiceLevel"
    );

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryService inventoryService;
    private final InventoryRepository inventoryRepository;
    private final ProductImageService productImageService;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            InventoryService inventoryService,
            InventoryRepository inventoryRepository,
            ProductImageService productImageService
    ) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryService = inventoryService;
        this.inventoryRepository = inventoryRepository;
        this.productImageService = productImageService;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> new CategoryResponse(category.getCode(), category.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(ProductSearchCriteria criteria, Pageable pageable) {
        var page = productRepository.findAll(ProductSpecifications.from(criteria), sanitize(pageable));
        var urls = productImageService.presignedUrls(page.getContent().stream().map(Product::getId).toList());
        return page.map(product -> ProductResponse.from(product, urls.get(product.getId()), stockOf(product.getId())));
    }

    @Transactional(readOnly = true)
    public ProductResponse getById(UUID id) {
        return ProductResponse.from(getDetailed(id), productImageService.presignedUrl(id).orElse(null), stockOf(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request) {
        String name = request.name().trim();
        if (productRepository.existsByNameIgnoreCase(name)) {
            throw new ResourceConflictException("A product with this name already exists");
        }
        var product = new Product(
                name,
                request.description().trim(),
                requireCategory(request.category()),
                InrMoney.toPaise(request.price()),
                request.calories(),
                request.protein(),
                request.spiceLevel(),
                request.available(),
                normalizeIngredients(request.ingredients()),
                normalizeTags(request.dietaryTags())
        );
        Product saved = productRepository.save(product);
        inventoryService.createForNewProduct(saved.getId());
        return ProductResponse.from(saved, null, 0);
    }

    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        Product product = getDetailed(id);
        String name = request.name().trim();
        if (productRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ResourceConflictException("A product with this name already exists");
        }
        product.update(
                name,
                request.description().trim(),
                requireCategory(request.category()),
                InrMoney.toPaise(request.price()),
                request.calories(),
                request.protein(),
                request.spiceLevel(),
                request.available(),
                normalizeIngredients(request.ingredients()),
                normalizeTags(request.dietaryTags())
        );
        return ProductResponse.from(product, productImageService.presignedUrl(id).orElse(null), stockOf(id));
    }

    @Transactional
    public void delete(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        productImageService.deleteIfPresent(id);
        productRepository.delete(product);
    }

    private int stockOf(UUID productId) {
        return inventoryRepository.findById(productId).map(com.samosajunction.inventory.entity.Inventory::getQuantity).orElse(0);
    }

    private Product getDetailed(UUID id) {
        return productRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
    }

    private Category requireCategory(String code) {
        return categoryRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new InvalidRequestException("Unknown category: " + code));
    }

    private static Set<String> normalizeIngredients(Set<String> values) {
        return values.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private static Set<String> normalizeTags(Set<String> values) {
        return values.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private Pageable sanitize(Pageable pageable) {
        Sort mapped = Sort.unsorted();
        for (Sort.Order order : pageable.getSort()) {
            String entityField = SORTABLE_FIELDS.get(order.getProperty());
            if (entityField == null) {
                throw new InvalidRequestException(
                        "Unsupported sort field '%s'. Allowed: %s".formatted(
                                order.getProperty(),
                                SORTABLE_FIELDS.keySet()
                        )
                );
            }
            mapped = mapped.and(Sort.by(new Sort.Order(order.getDirection(), entityField)));
        }
        if (mapped.isUnsorted()) {
            mapped = Sort.by("name").ascending();
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), mapped);
    }
}
