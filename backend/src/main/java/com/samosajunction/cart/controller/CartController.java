package com.samosajunction.cart.controller;

import com.samosajunction.auth.security.UserPrincipal;
import com.samosajunction.cart.dto.AddCartItemRequest;
import com.samosajunction.cart.dto.CartResponse;
import com.samosajunction.cart.dto.UpdateCartItemRequest;
import com.samosajunction.cart.service.CartService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse get(@AuthenticationPrincipal UserPrincipal principal) {
        return cartService.getCart(principal.getId());
    }

    @PostMapping("/items")
    public CartResponse add(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody AddCartItemRequest request
    ) {
        return cartService.addItem(principal.getId(), request.productId(), request.quantity());
    }

    @PutMapping("/items/{productId}")
    public CartResponse update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemRequest request
    ) {
        return cartService.updateItem(principal.getId(), productId, request.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse remove(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable UUID productId
    ) {
        return cartService.removeItem(principal.getId(), productId);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void clear(@AuthenticationPrincipal UserPrincipal principal) {
        cartService.clear(principal.getId());
    }
}
