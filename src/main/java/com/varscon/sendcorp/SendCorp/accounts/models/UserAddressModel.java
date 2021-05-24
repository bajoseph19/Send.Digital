package com.varscon.sendcorp.SendCorp.accounts.models;

import lombok.*;

@EqualsAndHashCode()
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class UserAddressModel {
    private String streetAddress;
    private String state;
    private String province;
    private String zipCode;
    private String country;
}
