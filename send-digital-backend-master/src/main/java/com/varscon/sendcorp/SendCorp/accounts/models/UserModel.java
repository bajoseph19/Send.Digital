package com.varscon.sendcorp.SendCorp.accounts.models;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;

@EqualsAndHashCode()
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "user")
public class UserModel implements UserDetails{

    @Id
    private String id;
    private String firstName;
    private String lastName;
    private String email;
    private String password;
    private String transaction_pin;
    private Boolean hasKyc;
    private Boolean hasPin;
    private LocalDate dob;
    private String phone;
    private GenderModel genderModel;
    private UserAddressModel address;
    private UserRoleModel appUserRole;
    private UserKYCModel userKYCModel;
    private Status status;
    private Boolean locked = false;
    private Boolean enabled = false;

    public UserModel(String firstName,
                   String lastName,
                   String email,
                   String password,
                   String transaction_pin,
                   Boolean hasKyc,
                   Boolean hasPin,
                   LocalDate dob,
                   String phone,
                   GenderModel genderModel,
                   UserAddressModel address,
                   Status status,
                   UserRoleModel appUserRole) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.password = password;
        this.transaction_pin = transaction_pin;
        this.hasKyc = hasKyc;
        this.hasPin = hasPin;
        this.dob = dob;
        this.phone = phone;
        this.genderModel = genderModel;
        this.address = address;
        this.status = status;
        this.appUserRole = appUserRole;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        SimpleGrantedAuthority authority =
                new SimpleGrantedAuthority(appUserRole.name());
        return Collections.singletonList(authority);
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }


}
