/*
 * @(#)AbstractTreeModel.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.fxbase.tree;

import javafx.beans.InvalidationListener;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.base.event.Listener;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * AbstractTreeModel.
 *
 * @author Werner Randelshofer
 */
public abstract class AbstractTreeModel<E> implements TreeModel<E> {

    private final CopyOnWriteArrayList<Listener<TreeModelEvent<E>>> treeModelListeners = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<InvalidationListener> invalidationListeners = new CopyOnWriteArrayList<>();

    public AbstractTreeModel() {
    }

    @Override
    public final @NonNull CopyOnWriteArrayList<Listener<TreeModelEvent<E>>> getTreeModelListeners() {
        return treeModelListeners;
    }

    @Override
    public final @NonNull CopyOnWriteArrayList<InvalidationListener> getInvalidationListeners() {
        return invalidationListeners;
    }
}
