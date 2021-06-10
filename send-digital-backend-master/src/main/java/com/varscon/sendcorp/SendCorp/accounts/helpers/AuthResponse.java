package com.varscon.sendcorp.SendCorp.accounts.helpers;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.SneakyThrows;

import java.util.HashMap;

@Getter
@Setter
@RequiredArgsConstructor
public class AuthResponse<T> extends GenericResponse<T>{
    final String token;

    public AuthResponse(T data, String token) {
        super(true, "success", data);
        this.token = token;
    }

    @SneakyThrows
    @Override
    public String toString() {
        var payload = new HashMap<>();
        payload.put("success", this.isSuccess());
        payload.put("message", this.getMessage());
        payload.put("token", this.getToken());
        payload.put("data", this.getData());

        return new ObjectMapper().writeValueAsString(payload);
    }
}
