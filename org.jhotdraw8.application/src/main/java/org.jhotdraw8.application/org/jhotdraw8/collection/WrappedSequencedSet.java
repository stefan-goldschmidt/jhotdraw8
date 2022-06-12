/*
 * @(#)WrappedSequencedSet.java
 * Copyright © 2022 The authors and contributors of JHotDraw. MIT License.
 */
package org.jhotdraw8.collection;

import org.jhotdraw8.annotation.NonNull;
import org.jhotdraw8.annotation.Nullable;

import java.util.Iterator;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Wraps {@code Set} functions into the {@link Set} interface.
 *
 * @author Werner Randelshofer
 */
public class WrappedSequencedSet<E> extends WrappedSet<E> implements SequencedSet<E> {

    private final @NonNull Supplier<E> getFirstFunction;
    private final @NonNull Supplier<E> getLastFunction;
    private final @NonNull Consumer<E> addFirstFunction;
    private final @NonNull Consumer<E> addLastFunction;
    private final @NonNull Supplier<Iterator<E>> reversedIteratorFunction;

    public WrappedSequencedSet(@NonNull ReadOnlyCollection<E> backingSet,
                               @NonNull Supplier<Iterator<E>> reversedIteratorFunction) {
        this(backingSet::iterator, reversedIteratorFunction, backingSet::size,
                backingSet::contains, null, null, null, null, null, null);
    }

    public WrappedSequencedSet(@NonNull ReadOnlySequencedCollection<E> backingSet) {
        this(backingSet::iterator, () -> backingSet.readOnlyReversed().iterator(),
                backingSet::size,
                backingSet::contains, null, null, null, null, null, null);
    }

    public WrappedSequencedSet(@NonNull Set<E> backingSet,
                               @NonNull Supplier<Iterator<E>> reversedIteratorFunction) {
        this(backingSet::iterator, reversedIteratorFunction, backingSet::size,
                backingSet::contains, backingSet::clear, backingSet::remove, null, null, null, null);
    }

    public WrappedSequencedSet(@NonNull Supplier<Iterator<E>> iteratorFunction,
                               @NonNull Supplier<Iterator<E>> reversedIteratorFunction,
                               @NonNull IntSupplier sizeFunction,
                               @NonNull Predicate<Object> containsFunction) {
        this(iteratorFunction, reversedIteratorFunction,
                sizeFunction, containsFunction, null, null, null, null, null, null);
    }

    public WrappedSequencedSet(@NonNull Supplier<Iterator<E>> iteratorFunction,
                               @NonNull Supplier<Iterator<E>> reversedIteratorFunction,
                               @NonNull IntSupplier sizeFunction,
                               @NonNull Predicate<Object> containsFunction,
                               @Nullable Runnable clearFunction,
                               @Nullable Predicate<Object> removeFunction,
                               @Nullable Supplier<E> getFirstFunction,
                               @Nullable Supplier<E> getLastFunction,
                               @Nullable Consumer<E> addFirstFunction,
                               @Nullable Consumer<E> addLastFunction) {
        super(iteratorFunction, sizeFunction, containsFunction, clearFunction, removeFunction);
        this.getFirstFunction = getFirstFunction == null ? () -> {
            throw new UnsupportedOperationException();
        } : getFirstFunction;
        this.getLastFunction = getLastFunction == null ? () -> {
            throw new UnsupportedOperationException();
        } : getLastFunction;
        this.addFirstFunction = addFirstFunction == null ? e -> {
            throw new UnsupportedOperationException();
        } : addFirstFunction;
        this.addLastFunction = addLastFunction == null ? e -> {
            throw new UnsupportedOperationException();
        } : addLastFunction;
        this.reversedIteratorFunction = reversedIteratorFunction;
    }

    @Override
    public void addFirst(E e) {
        addFirstFunction.accept(e);
    }

    @Override
    public void addLast(E e) {
        addLastFunction.accept(e);
    }

    @Override
    public E removeLast() {
        E e = getLastFunction.get();
        removeFunction.test(e);
        return e;
    }

    @Override
    public E getFirst() {
        return getFirstFunction.get();
    }

    @Override
    public E getLast() {
        return getLastFunction.get();
    }

    @Override
    public SequencedSet<E> reversed() {
        return new WrappedSequencedSet<E>(
                reversedIteratorFunction,
                iteratorFunction,
                sizeFunction,
                containsFunction,
                clearFunction,
                removeFunction,
                getLastFunction,
                getFirstFunction,
                addLastFunction,
                addFirstFunction);
    }
}
