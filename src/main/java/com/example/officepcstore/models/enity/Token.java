package com.example.officepcstore.models.enity;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
public class Token {
    private String verificationCode;
    private LocalDateTime expireTime;
}
