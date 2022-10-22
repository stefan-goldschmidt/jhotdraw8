/*
 * @(#)XmlPoint3DConverter.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.xml.text;

import org.jhotdraw8.draw.css.text.Point3DConverter;

/**
 * Converts a {@code javafx.geometry.Point3D} into a {@code String} and vice
 * versa.
 *
 * @author Werner Randelshofer
 */
public class XmlPoint3DConverter extends Point3DConverter {

    public XmlPoint3DConverter() {
        this(false, true);
    }

    public XmlPoint3DConverter(boolean nullable) {
        this(nullable, true);
    }

    public XmlPoint3DConverter(boolean nullable, boolean withSpace) {
        super(nullable, withSpace);
    }
}
