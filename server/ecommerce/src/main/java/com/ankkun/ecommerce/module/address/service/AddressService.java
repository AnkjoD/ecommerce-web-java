package com.ankkun.ecommerce.module.address.service;

import com.ankkun.ecommerce.common.exception.NotFoundException;
import com.ankkun.ecommerce.module.address.dto.CreateAddressDto;
import com.ankkun.ecommerce.module.address.entity.Address;
import com.ankkun.ecommerce.module.address.repository.AddressRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final com.ankkun.ecommerce.module.order.repository.OrderRepository orders;
    private final com.ankkun.ecommerce.module.user.repository.UserRepository users;

    public List<Address> findAll(String userId) {
        return addressRepository.findByUserIdOrderByIsDefaultDesc(userId);
    }

    public Address findOne(String id, String userId) {
        return addressRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new NotFoundException("Địa chỉ không tồn tại"));
    }

    @Transactional
    public Address create(String userId, CreateAddressDto dto) {
        users.findLocked(userId).orElseThrow();
        long count = addressRepository.countByUserId(userId);
        boolean isDefault = count == 0 || Boolean.TRUE.equals(dto.getIs_default());

        if (isDefault) {
            addressRepository.clearDefault(userId);
        }

        Address address = Address.builder()
                .userId(userId)
                .recipientName(dto.getRecipient_name())
                .phone(dto.getPhone())
                .province(dto.getProvince())
                .district(dto.getDistrict())
                .ward(dto.getWard())
                .street(dto.getStreet())
                .isDefault(isDefault)
                .build();
        return addressRepository.save(address);
    }

    @Transactional
    public Address update(String id, String userId, CreateAddressDto dto) {
        users.findLocked(userId).orElseThrow();
        Address address = findOne(id, userId);

        if (Boolean.TRUE.equals(dto.getIs_default())) {
            addressRepository.clearDefault(userId);
            address.setIsDefault(true);
        }
        if (dto.getRecipient_name() != null) address.setRecipientName(dto.getRecipient_name());
        if (dto.getPhone() != null) address.setPhone(dto.getPhone());
        if (dto.getProvince() != null) address.setProvince(dto.getProvince());
        if (dto.getDistrict() != null) address.setDistrict(dto.getDistrict());
        if (dto.getWard() != null) address.setWard(dto.getWard());
        if (dto.getStreet() != null) address.setStreet(dto.getStreet());
        return addressRepository.save(address);
    }

    @Transactional
    public void remove(String id, String userId) {
        users.findLocked(userId).orElseThrow();
        Address address = findOne(id, userId);
        if(orders.existsByAddressId(id))throw new com.ankkun.ecommerce.common.exception.BadRequestException("Địa chỉ đã được dùng cho đơn hàng; bạn có thể thêm địa chỉ mới");
        addressRepository.delete(address);

        if (Boolean.TRUE.equals(address.getIsDefault())) {
            addressRepository.findByUserIdOrderByIsDefaultDesc(userId).stream()
                    .findFirst()
                    .ifPresent(a -> {
                        a.setIsDefault(true);
                        addressRepository.save(a);
                    });
        }
    }

    @Transactional
    public Address setDefault(String id, String userId) {
        users.findLocked(userId).orElseThrow();
        Address address = findOne(id, userId);
        addressRepository.clearDefault(userId);
        address.setIsDefault(true);
        return addressRepository.save(address);
    }
}
