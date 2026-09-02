package com.ankkun.ecommerce.module.address.controller;

import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.address.dto.CreateAddressDto;
import com.ankkun.ecommerce.module.address.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<?>> getAll(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.findAll(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<?>> create(@AuthenticationPrincipal String userId,
                                                  @Valid @RequestBody CreateAddressDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(addressService.create(userId, dto)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> update(@PathVariable String id,
                                                  @AuthenticationPrincipal String userId,
                                                  @RequestBody CreateAddressDto dto) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.update(id, userId, dto)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> remove(@PathVariable String id,
                                                     @AuthenticationPrincipal String userId) {
        addressService.remove(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Deleted", null));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<?>> setDefault(@PathVariable String id,
                                                      @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(addressService.setDefault(id, userId)));
    }
}
