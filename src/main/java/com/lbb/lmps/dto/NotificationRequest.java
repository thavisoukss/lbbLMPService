package com.lbb.lmps.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationRequest {
    private String title;
    private String desc;
    private String phone;
}