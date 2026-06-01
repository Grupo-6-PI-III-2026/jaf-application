package com.jaf.application.dto.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.time.LocalDate;

public class DateRangeValidator implements ConstraintValidator<DateRange, Object> {

    private String startDateField;
    private String endDateField;

    @Override
    public void initialize(DateRange constraintAnnotation) {
        this.startDateField = constraintAnnotation.startDateField();
        this.endDateField = constraintAnnotation.endDateField();
    }

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        if (object == null) {
            return true;
        }

        BeanWrapper beanWrapper = new BeanWrapperImpl(object);
        Object startDateValue = beanWrapper.getPropertyValue(startDateField);
        Object endDateValue = beanWrapper.getPropertyValue(endDateField);

        if (startDateValue == null || endDateValue == null) {
            return true; // Let @NotNull handle null checks
        }

        if (startDateValue instanceof LocalDate && endDateValue instanceof LocalDate) {
            LocalDate startDate = (LocalDate) startDateValue;
            LocalDate endDate = (LocalDate) endDateValue;
            return !endDate.isBefore(startDate);
        }

        return false;
    }
}
