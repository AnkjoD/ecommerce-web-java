package com.ankkun.ecommerce.module.user.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class UpdateProfileDto {
    @JsonProperty("full_name")
    @jakarta.validation.constraints.Size(min=1,max=255) @com.fasterxml.jackson.annotation.JsonAlias("name") private String fullName;
    private String phone;
    @jakarta.validation.constraints.Size(max=200) private String avatar;
    @JsonProperty("date_of_birth") @jakarta.validation.constraints.PastOrPresent private java.time.LocalDate dateOfBirth;
    @jakarta.validation.constraints.Pattern(regexp="male|female|other") private String gender;
}
