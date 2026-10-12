package com.ankkun.ecommerce.module.address.dto;
import com.ankkun.ecommerce.module.address.entity.Address;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public record AddressResponse(String id, String userId, String recipientName, String phone,
        String province, String district, String ward, String street, boolean isDefault) {
    public static AddressResponse from(Address a) {
        return new AddressResponse(a.getId(), a.getUserId(), a.getRecipientName(), a.getPhone(),
                a.getProvince(), a.getDistrict(), a.getWard(), a.getStreet(), Boolean.TRUE.equals(a.getIsDefault()));
    }
}
