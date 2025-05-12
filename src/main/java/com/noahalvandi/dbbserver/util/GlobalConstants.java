package com.noahalvandi.dbbserver.util;

import org.springframework.stereotype.Component;

@Component
public class GlobalConstants {
    public static final String FRONTEND_BASE_URL = "http://localhost:5173";

    public static final int JWT_EXPIRATION_DAY_TO_MILLISECONDS = 24 * 60 * 60 * 1000;

    public static final int MINUTES_TO_EXPIRE_RESET_PASSWORD_TOKEN = 30;

    public static final int LOAN_DAYS_FOR_COURSE_LITERATURE_BOOKS = 14;
    public static final int LOAN_DAYS_FOR_NON_COURSE_LITERATURE_BOOKS = 30;
    public static final int LOAN_DAYS_FOR_FILMS = 7;

    public static final int MAXIMUM_ACTIVE_LOANS_PER_ADMIN_AND_LIBRARIAN = 10;
    public static final int MAXIMUM_ACTIVE_LOANS_PER_UNIVERSITY_STAFF = 10;
    public static final int MAXIMUM_ACTIVE_LOANS_PER_RESEARCHER = 7;
    public static final int MAXIMUM_ACTIVE_LOANS_PER_STUDENT = 5;
    public static final int MAXIMUM_ACTIVE_LOANS_PER_PUBLIC = 3;

    public static final int DAILY_OVERDUE_FEE = 10;

    public static final int MAXIMUM_LOAN_EXTENSION_COUNT = 3;

    public static final int CLOUD_URL_EXPIRATION_TIME_IN_MINUTES = 5;

    public static final long HOURS_UNTIL_RESERVATION_EXPIRES = 48;
}