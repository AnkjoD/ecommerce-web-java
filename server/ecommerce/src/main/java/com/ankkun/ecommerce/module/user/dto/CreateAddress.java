package com.ankkun.ecommerce.module.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CreateAddress {
    String recipientName;
    String phone;
    String province;
    String district;
    String ward;
    String street;
    boolean defaultAddress;
}
