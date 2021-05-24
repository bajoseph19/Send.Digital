package com.varscon.sendcorp.SendCorp.accounts.helpers;

import com.varscon.sendcorp.SendCorp.accounts.models.GenderModel;
import com.varscon.sendcorp.SendCorp.accounts.models.UserAddressModel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@EqualsAndHashCode
@ToString
public class RegistrationRequest {
    private final String firstName;
    private final String lastName;
    private final String email;
    private final String phone;
    private final String password;
    private final LocalDate dob;
    private final GenderModel gender;
    private final UserAddressModel address;
}
