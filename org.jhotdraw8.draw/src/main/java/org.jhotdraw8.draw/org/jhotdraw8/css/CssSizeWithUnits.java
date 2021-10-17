/*
 * @(#)CssSize.java
 * Copyright © 2021 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.css;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;

/**
 * Represents a size specified in a particular unit.
 * <p>
 * A CssSize can be used to hold the value of a CSS {@code number-token},
 * {@code percentage-token} or {@code dimension-token}.
 * <p>
 * Unlike {@link javafx.css.Size} this class supports an open ended
 * set of units.
 * <p>
 * References:
 * <dl>
 * <dt>CSS Syntax Module Level 3, Chapter 4. Tokenization</dt>
 * <dd><a href="https://www.w3.org/TR/2019/CR-css-syntax-3-20190716/#tokenization">w3.org</a></dd>
 * </dl>
 *
 * @author Werner Randelshofer
 */
public class CssSizeWithUnits extends CssSize {
    private final @NonNull String units;

    public CssSizeWithUnits(double value, @Nullable String units) {
        super(value);
        this.units = units;
    }

    public @NonNull String getUnits() {
        return units;
    }

}
