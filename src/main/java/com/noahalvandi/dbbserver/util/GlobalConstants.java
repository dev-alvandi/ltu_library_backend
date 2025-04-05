package com.noahalvandi.dbbserver.util;

import org.springframework.stereotype.Component;

@Component
public class GlobalConstants {
    public static final String FRONTEND_BASE_URL = "http://localhost:5173";
    public static final int MINUTES_TO_EXPIRE_PASSWORD_TOKEN = 30;
}