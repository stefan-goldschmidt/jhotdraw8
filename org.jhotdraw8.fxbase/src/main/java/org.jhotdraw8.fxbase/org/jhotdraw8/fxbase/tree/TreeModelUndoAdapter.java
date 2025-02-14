/*
 * @(#)TreeModelUndoAdapter.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */

package org.jhotdraw8.fxbase.tree;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;
import org.jhotdraw8.base.event.Listener;
import org.jhotdraw8.fxbase.undo.FXUndoManager;

import javax.swing.event.UndoableEditEvent;
import javax.swing.event.UndoableEditListener;
import javax.swing.undo.AbstractUndoableEdit;
import javax.swing.undo.CannotRedoException;
import javax.swing.undo.CannotUndoException;
import javax.swing.undo.UndoableEdit;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * This adapter can be bound to a {@link javafx.scene.control.TextInputControl}
 * to support undo/redo with a {@link FXUndoManager}.
 * <p>
 * This text filter can be added to multiple tree models.
 * If you do this, make sure that you add the {@link FXUndoManager}
 * only once as a listener.
 *
 * @param <E> the element type of the tree
 */
public class TreeModelUndoAdapter<E> implements Listener<TreeModelEvent<E>> {
    private final @NonNull CopyOnWriteArrayList<UndoableEditListener> listeners = new CopyOnWriteArrayList<>();

    public TreeModelUndoAdapter() {
    }

    public TreeModelUndoAdapter(@NonNull TreeModel<E> model) {
        bind(model);
    }

    public void bind(@NonNull TreeModel<E> model) {
        unbind(model);
        model.addTreeModelListener(this);
    }

    public void unbind(@NonNull TreeModel<E> model) {
        model.removeTreeModelListener(this);
    }

    @Override
    public void handle(@NonNull TreeModelEvent<E> event) {
        UndoableEdit edit = switch (event.getEventType()) {
            case ROOT_CHANGED ->
                    new RootChangedEdit<>(event.getSource(), event.getOldRoot(), event.getNewRoot());
            case SUBTREE_NODES_CHANGED,
                    NODE_ADDED_TO_TREE,
                    NODE_REMOVED_FROM_TREE,
                    NODE_CHANGED -> null;
            case NODE_ADDED_TO_PARENT ->
                    new NodeAddedEdit<>(event.getSource(), event.getParent(), event.getChildIndex(), event.getChild());
            case NODE_REMOVED_FROM_PARENT ->
                    new NodeRemovedEdit<>(event.getSource(), event.getParent(), event.getChildIndex(), event.getChild());

        };
        if (edit != null) {
            final UndoableEditEvent editEvent = new UndoableEditEvent(event.getSource(), edit);
            listeners.forEach(e -> e.undoableEditHappened(editEvent));
        }
    }

    public void removeUndoEditListener(@NonNull UndoableEditListener listener) {
        listeners.remove(listener);
    }

    public void addUndoEditListener(@NonNull UndoableEditListener listener) {
        listeners.add(listener);
    }

    class RootChangedEdit<E> extends AbstractUndoableEdit {
        private final @NonNull TreeModel<E> model;
        private final @Nullable E oldRoot;
        private final @Nullable E newRoot;

        public RootChangedEdit(@NonNull TreeModel<E> model, @Nullable E oldRoot, @Nullable E newRoot) {
            this.model = model;
            this.oldRoot = oldRoot;
            this.newRoot = newRoot;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            model.setRoot(oldRoot);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            model.setRoot(newRoot);
        }

        @Override
        public String getPresentationName() {
            return "Set Root";
        }
    }

    class NodeAddedEdit<E> extends AbstractUndoableEdit {
        private final @NonNull TreeModel<E> model;
        private final @NonNull E parent;
        private final int childIndex;
        private final @NonNull E child;

        public NodeAddedEdit(@NonNull TreeModel<E> model, @NonNull E parent, int childIndex, @NonNull E child) {
            this.model = model;
            this.parent = parent;
            this.childIndex = childIndex;
            this.child = child;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            model.removeFromParent(child);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            model.insertChildAt(child, parent, childIndex);
        }

        @Override
        public String getPresentationName() {
            return "Add";
        }
    }

    class NodeRemovedEdit<E> extends AbstractUndoableEdit {
        private final @NonNull TreeModel<E> model;
        private final @NonNull E parent;
        private final int childIndex;
        private final @NonNull E child;

        public NodeRemovedEdit(@NonNull TreeModel<E> model, @NonNull E parent, int childIndex, @NonNull E child) {
            this.model = model;
            this.parent = parent;
            this.childIndex = childIndex;
            this.child = child;
        }

        @Override
        public void undo() throws CannotUndoException {
            super.undo();
            model.insertChildAt(child, parent, childIndex);
        }

        @Override
        public void redo() throws CannotRedoException {
            super.redo();
            model.removeFromParent(child);
        }

        @Override
        public String getPresentationName() {
            return "Remove";
        }
    }
}
