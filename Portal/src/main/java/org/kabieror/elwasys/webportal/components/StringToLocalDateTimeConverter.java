package org.kabieror.elwasys.webportal.components;

import com.vaadin.data.util.converter.Converter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Locale;

public class StringToLocalDateTimeConverter implements Converter<String, LocalDateTime> {

    /**
     * 
     */
    private static final long serialVersionUID = 3799737164880980572L;

    @Override
    public LocalDateTime convertToModel(String value, Class<? extends LocalDateTime> targetType,
            Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
        return LocalDateTime.parse(value);
    }

    @Override
    public String convertToPresentation(LocalDateTime value, Class<? extends String> targetType,
            Locale locale) throws com.vaadin.data.util.converter.Converter.ConversionException {
        if (value == null)
            return "";
        return value.format(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT));
    }

    @Override
    public Class<LocalDateTime> getModelType() {
        // TODO Auto-generated method stub
        return LocalDateTime.class;
    }

    @Override
    public Class<String> getPresentationType() {
        // TODO Auto-generated method stub
        return String.class;
    }

}
