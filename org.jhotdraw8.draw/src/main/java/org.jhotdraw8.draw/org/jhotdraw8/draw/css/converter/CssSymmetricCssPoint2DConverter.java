/*
 * @(#)CssSymmetricCssPoint2DConverter.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw.css.converter;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.base.converter.IdResolver;
import org.jhotdraw8.base.converter.IdSupplier;
import org.jhotdraw8.css.converter.AbstractCssConverter;
import org.jhotdraw8.css.parser.CssToken;
import org.jhotdraw8.css.parser.CssTokenType;
import org.jhotdraw8.css.parser.CssTokenizer;
import org.jhotdraw8.css.value.CssSize;
import org.jhotdraw8.draw.css.CssPoint2D;

import java.io.IOException;
import java.text.ParseException;
import java.util.function.Consumer;


/**
 * Converts a {@code javafx.geometry.Point2D} into a {@code String} and vice
 * versa. If the X and the Y-value are identical, then only one value is output.
 *
 * @author Werner Randelshofer
 */
public class CssSymmetricCssPoint2DConverter extends AbstractCssConverter<CssPoint2D> {

    private final boolean withSpace;
    private final boolean withComma;

    public CssSymmetricCssPoint2DConverter() {
        this(false, true, false);
    }

    public CssSymmetricCssPoint2DConverter(boolean nullable) {
        this(nullable, true, false);
    }

    public CssSymmetricCssPoint2DConverter(boolean nullable, boolean withSpace, boolean withComma) {
        super(nullable);
        this.withSpace = withSpace;
        this.withComma = withComma || !withSpace;
    }

    @Override
    public @NonNull CssPoint2D parseNonNull(@NonNull CssTokenizer tt, @Nullable IdResolver idResolver) throws ParseException, IOException {
        final CssSize x, y;
        x = CssSizeConverter.parseSize(tt, "x");
        if (tt.next() == CssTokenType.TT_EOF) {
            y = x;
        } else {
            tt.pushBack();
            tt.skipIfPresent(CssTokenType.TT_COMMA);
            y = CssSizeConverter.parseSize(tt, "y");
        }

        return new CssPoint2D(x, y);
    }

    @Override
    protected <TT extends CssPoint2D> void produceTokensNonNull(@NonNull TT value, @Nullable IdSupplier idSupplier, @NonNull Consumer<CssToken> out) {
        CssSize x = value.getX();
        CssSize y = value.getY();
        out.accept(new CssToken(CssTokenType.TT_DIMENSION, x.getValue(), x.getUnits()));
        if (!x.equals(y)) {
            if (withComma) {
                out.accept(new CssToken(CssTokenType.TT_COMMA));
            }
            if (withSpace) {
                out.accept(new CssToken(CssTokenType.TT_S, " "));
            }

            out.accept(new CssToken(CssTokenType.TT_DIMENSION, y.getValue(), y.getUnits()));
        }
    }


    @Override
    public @NonNull CssPoint2D getDefaultValue() {
        return new CssPoint2D(0, 0);
    }

    @Override
    public @NonNull String getHelpText() {
        return "Format of ⟨SymmetricPoint2D⟩: ⟨xy⟩ ｜ ⟨x⟩ ⟨y⟩";
    }

}
