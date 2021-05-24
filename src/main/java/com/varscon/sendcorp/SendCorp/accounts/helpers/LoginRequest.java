package com.varscon.sendcorp.SendCorp.accounts.helpers;

import com.mongodb.lang.NonNull;
import lombok.*;

@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class LoginRequest {
    @NonNull
    private String email;
    @NonNull
    private String password;
}
