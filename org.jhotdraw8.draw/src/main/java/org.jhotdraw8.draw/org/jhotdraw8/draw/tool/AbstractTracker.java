/*
 * @(#)AbstractTracker.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.draw.tool;

import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.application.AbstractDisableable;

/**
 * AbstractAction.
 *
 * @author Werner Randelshofer
 */
public abstract class AbstractTracker extends AbstractDisableable implements Tracker {

    // ---
    // Fields
    // ---
    protected final BorderPane node = new BorderPane();

    // ---
    // Constructors
    // ---

    /**
     * Creates a new instance.
     */
    public AbstractTracker() {

    }

    // ---
    // Properties
    // ---
    // ---
    // Behaviors
    // ---
    @Override
    public @NonNull Node getNode() {
        return node;
    }
}
