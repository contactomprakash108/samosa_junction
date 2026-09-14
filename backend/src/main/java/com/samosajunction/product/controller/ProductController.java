package com.samosajunction.product.controller;

import com.samosajunction.auth.security.UserPrincipal;
import com.samosajunction.common.response.PageResponse;
import com.samosajunction.product.dto.CategoryResponse;
import com.samosajunction.product.dto.ProductRequest;
import com.samosajunction.product.dto.ProductResponse;
import com.samosajunction.product.dto.ProductSearchCriteria;
import com.samosajunction.product.entity.SpiceLevel;
import com.samosajunction.product.service.ProductImageService;
import com.samosajunction.product.service.ProductService;
import com.samosajunction.staff.service.StaffAuditService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class ProductController {

    private final ProductService productService;
    private final ProductImageService productImageService;
    private final StaffAuditService staffAuditService;

    public ProductController(
            ProductService productService,
            ProductImageService productImageService,
            StaffAuditService staffAuditService
    ) {
        this.productService = productService;
        this.productImageService = productImageService;
        this.staffAuditService = staffAuditService;
    }

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return productService.listCategories();
    }

    @GetMapping("/products")
    public PageResponse<ProductResponse> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) SpiceLevel spiceLevel,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String dietaryTag,
            @PageableDefault(size = 20, sort = "name") Pageable pageable
    ) {
        var criteria = new ProductSearchCriteria(search, category, spiceLevel, available, dietaryTag);
        return PageResponse.from(productService.search(criteria, pageable));
    }

    @GetMapping("/products/{id}")
    public ProductResponse get(@PathVariable UUID id) {
        return productService.getById(id);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse created = productService.create(request);
        staffAuditService.record(principal.getId(), "PRODUCT_CREATE", "PRODUCT", created.id(), created.name());
        return created;
    }

    @PutMapping("/products/{id}")
    public ProductResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request
    ) {
        ProductResponse updated = productService.update(id, request);
        staffAuditService.record(principal.getId(), "PRODUCT_UPDATE", "PRODUCT", id, updated.name());
        return updated;
    }

    @DeleteMapping("/products/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable UUID id) {
        productService.delete(id);
        staffAuditService.record(principal.getId(), "PRODUCT_DELETE", "PRODUCT", id, null);
    }

    @PostMapping("/products/{id}/image")
    public ProductResponse uploadImage(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        productImageService.upload(id, file);
        return productService.getById(id);
    }

    @DeleteMapping("/products/{id}/image")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteImage(@PathVariable UUID id) {
        productImageService.delete(id);
    }
}
