/*
 * @(#)XmlStringConverter.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.xml.text;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.base.converter.Converter;
import org.jhotdraw8.base.converter.IdResolver;
import org.jhotdraw8.base.converter.IdSupplier;

import java.io.IOException;
import java.nio.CharBuffer;
import java.text.ParseException;

/**
 * Converts a {@code String} into the XML String representation.
 * <p>
 * Reference:
 * <a href="https://www.w3.org/TR/2004/REC-xmlschema-2-20041028/#string">W3C: XML
 * Schema Part 2: Datatypes Second Edition: 3.2.5 string</a>
 * </p>
 *
 * @author Werner Randelshofer
 */
public class XmlStringConverter implements Converter<String> {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance.
     */
    public XmlStringConverter() {
    }

    @Override
    public void toString(@NonNull Appendable buf, @Nullable IdSupplier idSupplier, String value) throws IOException {
        if (value != null) {
            buf.append(value);
        }
    }

    @Override
    public @Nullable String fromString(@NonNull CharBuffer in, @Nullable IdResolver idResolver) throws ParseException {
        if (in != null) {
            String converted = in.toString();
            in.position(in.position() + in.remaining());
            return converted;

        }
        return null;
    }

    @Override
    public @Nullable String getDefaultValue() {
        return null;
    }
}
