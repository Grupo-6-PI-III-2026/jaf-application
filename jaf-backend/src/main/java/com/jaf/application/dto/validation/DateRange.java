package com.jaf.application.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = DateRangeValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DateRange {
    String message() default "A data de término deve ser posterior à data de início";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String startDateField();
    String endDateField();
}
