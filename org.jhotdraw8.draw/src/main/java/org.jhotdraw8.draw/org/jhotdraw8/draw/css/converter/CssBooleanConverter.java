/*
 * @(#)CssBooleanConverter.java
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

import java.io.IOException;
import java.text.ParseException;
import java.util.function.Consumer;

/**
 * Converts a {@code Boolean} into the CSS String representation.
 *
 * @author Werner Randelshofer
 */
public class CssBooleanConverter extends AbstractCssConverter<Boolean> {

    private static final long serialVersionUID = 1L;

    private final String trueString = "true";
    private final String falseString = "false";

    public CssBooleanConverter(boolean nullable) {
        super(nullable);
    }

    @Override
    public @NonNull Boolean parseNonNull(@NonNull CssTokenizer tt, @Nullable IdResolver idResolver) throws ParseException, IOException {
        tt.requireNextToken(CssTokenType.TT_IDENT, "⟨Boolean⟩ identifier expected.");
        String s = tt.currentStringNonNull();
        switch (s) {
        case trueString:
            return Boolean.TRUE;
        case falseString:
            return Boolean.FALSE;
        default:
            throw new ParseException("⟨Boolean⟩ " + trueString + " or " + falseString + " expected.", tt.getStartPosition());
        }
    }

    @Override
    public <TT extends Boolean> void produceTokensNonNull(@NonNull TT value, @Nullable IdSupplier idSupplier, @NonNull Consumer<CssToken> out) {
        out.accept(new CssToken(CssTokenType.TT_IDENT, value.booleanValue() ? trueString : falseString));
    }

    @Override
    public @NonNull String getHelpText() {
        if (isNullable()) {
            return "Format of ⟨NullableBoolean⟩: none｜true｜false";
        } else {
            return "Format of ⟨Boolean⟩: true｜false";
        }
    }

}
