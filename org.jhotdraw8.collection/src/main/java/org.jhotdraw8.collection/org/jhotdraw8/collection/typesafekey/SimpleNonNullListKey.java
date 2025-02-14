/*
 * @(#)SimpleNonNullListKey.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.collection.typesafekey;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.collection.immutable.ImmutableArrayList;
import org.jhotdraw8.collection.immutable.ImmutableList;
import org.jhotdraw8.collection.reflect.TypeToken;

/**
 * An abstract {@link Key} that stores a list of values.
 *
 * @author Werner Randelshofer
 */
public class SimpleNonNullListKey<E> extends SimpleNonNullKey<ImmutableList<E>> {

    private static final long serialVersionUID = 1L;

    public SimpleNonNullListKey(@NonNull String key, @NonNull TypeToken<ImmutableList<E>> type) {
        super(key, type, ImmutableArrayList.of());
    }

    public SimpleNonNullListKey(@NonNull String key, @NonNull TypeToken<ImmutableList<E>> type, @NonNull ImmutableList<E> defaultValue) {
        super(key, type, defaultValue);
    }
}
