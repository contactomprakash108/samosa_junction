package com.samosajunction.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldErrorDetail> fieldErrors
) {
    public static ApiError of(int status, String code, String message, String path) {
        return new ApiError(Instant.now(), status, code, message, path, List.of());
    }

    public static ApiError of(
            int status,
            String code,
            String message,
            String path,
            List<FieldErrorDetail> fieldErrors
    ) {
        return new ApiError(Instant.now(), status, code, message, path, fieldErrors);
    }
}
