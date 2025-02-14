/*
 * @(#)DescendantCombinator.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.css.ast;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.css.model.SelectorModel;
import org.jhotdraw8.css.parser.CssToken;
import org.jhotdraw8.css.parser.CssTokenType;

import java.util.function.Consumer;

/**
 * A "descendant combinator" matches an element if its first selector matches on
 * an ancestor of the element and if its second selector matches on the element
 * itself.
 *
 * @author Werner Randelshofer
 */
public class DescendantCombinator extends Combinator {

    public DescendantCombinator(SimpleSelector first, Selector second) {
        super(first, second);
    }

    @Override
    public @NonNull String toString() {
        return first + ".isAncestorOf(" + second + ")";
    }

    @Override
    public @Nullable <T> T match(@NonNull SelectorModel<T> model, T element) {
        T secondMatch = second.match(model, element);
        if (secondMatch != null) {
            for (T parentElement = model.getParent(element); parentElement != null;
                 parentElement = model.getParent(parentElement)) {
                T firstMatch = first.match(model, parentElement);
                if (firstMatch != null) {
                    return secondMatch;
                }
            }
        }
        return null;
    }

    @Override
    public int getSpecificity() {
        return first.getSpecificity() + second.getSpecificity();
    }


    @Override
    public void produceTokens(@NonNull Consumer<CssToken> consumer) {
        first.produceTokens(consumer);
        consumer.accept(new CssToken(CssTokenType.TT_S, " "));
        second.produceTokens(consumer);
    }

    /**
     * This selector matches only on a specific type, if its second
     * selector matches only on a specific type.
     *
     * @return {@code second.matchesOnlyOnASpecificType()}
     */
    @Override
    public @Nullable TypeSelector matchesOnlyOnASpecificType() {
        return second.matchesOnlyOnASpecificType();
    }
}
