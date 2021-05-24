package com.varscon.sendcorp.SendCorp.accounts.helpers;

import com.varscon.sendcorp.SendCorp.accounts.models.UserModel;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Document(collection = "tokens")
public class ConfirmationToken {

    @Id
    private String id;

    @NonNull
    private String token;

    @NonNull
    private LocalDateTime createdAt;

    @NonNull
    private LocalDateTime expiresAt;

    private LocalDateTime confirmedAt;

    private UserModel appUser;

    public ConfirmationToken(String token,
                             LocalDateTime createdAt,
                             LocalDateTime expiresAt,
                             UserModel appUser) {
        this.token = token;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.appUser = appUser;
    }
}