package com.noahalvandi.dbbserver.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ResourceException extends ResponseStatusException {


    public ResourceException(HttpStatus status, String reason) {
        super(status, reason);
    }

    public static ResourceException notFound(String resourceName) {
        return new ResourceException(HttpStatus.NOT_FOUND, resourceName + " not found.");
    }

    public static ResourceException conflict(String message) {
        return new ResourceException(HttpStatus.CONFLICT, message);
    }

    public static ResourceException badRequest(String message) {
        return new ResourceException(HttpStatus.BAD_REQUEST, message);
    }

    public static ResourceException forbidden(String message) {
        return new ResourceException(HttpStatus.FORBIDDEN, message);
    }
}