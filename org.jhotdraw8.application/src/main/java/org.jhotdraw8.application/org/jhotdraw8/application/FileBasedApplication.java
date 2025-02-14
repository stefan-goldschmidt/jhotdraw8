/*
 * @(#)FileBasedApplication.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.application;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.collection.typesafekey.NonNullKey;
import org.jhotdraw8.collection.typesafekey.SimpleNonNullKey;

public interface FileBasedApplication extends Application {
    @NonNull
    NonNullKey<Boolean> ALLOW_MULTIPLE_ACTIVITIES_WITH_SAME_URI = new SimpleNonNullKey<Boolean>("allowMultipleActivitiesWithSameURI", Boolean.class,
            Boolean.FALSE);
}
