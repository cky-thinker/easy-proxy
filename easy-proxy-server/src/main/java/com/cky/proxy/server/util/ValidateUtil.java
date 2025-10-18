package com.cky.proxy.server.util;

import java.util.Set;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

public class ValidateUtil {
    private static final Validator VALIDATOR = Validation.buildDefaultValidatorFactory().getValidator();

    public static <T> void validate(T obj) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(obj);
        if (!violations.isEmpty()) {
            ConstraintViolationException ex = new ConstraintViolationException(violations);
            throw ex;
        }
    }
    
    public static <T> void validate(T obj, Class<?>... groups) {
        Set<ConstraintViolation<T>> violations = VALIDATOR.validate(obj, groups);
        if (!violations.isEmpty()) {
            ConstraintViolationException ex = new ConstraintViolationException(violations);
            throw ex;
        }
    }
}
