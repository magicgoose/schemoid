package magicgoose.schemoid.util;

import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import rx.Observable;
import rx.subjects.PublishSubject;

public class ReactiveList<T> implements List<T> {

    private final ArrayList<T> items = new ArrayList<>();

    private final PublishSubject<ListChange> changes = PublishSubject.create();

    public Observable<ListChange> getChanges() {
        return changes;
    }

    private void notifyDelete(int start, int size) {
        changes.onNext(ListChange.delete(start, size));
    }

    private void notifyInsert(int start, int size) {
        changes.onNext(ListChange.insert(start, size));
    }

    @Override
    public boolean add(final T object) {
        items.add(object);
        notifyInsert(items.size() - 1, 1);
        return true;
    }

    @Override
    public void add(final int index, final T object) {
        items.add(index, object);
        notifyInsert(index, 1);
    }

    @Override
    public boolean addAll(@NonNull final Collection<? extends T> collection) {
        final int insertCount = collection.size();
        final int start = items.size();
        final boolean modified = items.addAll(collection);
        if (insertCount > 0) {
            notifyInsert(start, insertCount);
        }
        return modified;
    }

    @Override
    public boolean addAll(final int index, @NonNull final Collection<? extends T> collection) {
        final int insertCount = collection.size();
        final boolean modified = items.addAll(index, collection);
        if (modified) {
            notifyInsert(index, insertCount);
        }
        return modified;
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
//        items.clear();
    }

    @Override
    public T get(final int index) {return items.get(index);}

    @Override
    public int size() {return items.size();}

    @Override
    public boolean isEmpty() {return items.isEmpty();}

    @Override
    public boolean contains(final Object object) {return items.contains(object);}

    @Override
    public int indexOf(final Object object) {return items.indexOf(object);}

    @Override
    public int lastIndexOf(final Object object) {return items.lastIndexOf(object);}

    @Override
    public T remove(final int index) {
        final T result = items.remove(index);
        notifyDelete(index, 1);
        return result;
    }

    @Override
    public boolean remove(final Object object) {
        throw new UnsupportedOperationException();
//        return items.remove(object);
    }

    @Override
    public T set(final int index, final T object) {
        throw new UnsupportedOperationException();
//        return items.set(index, object);
    }

    @NonNull
    @Override
    public Object[] toArray() {return items.toArray();}

    @NonNull
    @Override
    public <T1> T1[] toArray(@NonNull final T1[] contents) {
        //noinspection SuspiciousToArrayCall
        return items.toArray(contents);
    }

    @NonNull
    @Override
    public Iterator<T> iterator() {return items.iterator();}

    @Override
    public ListIterator<T> listIterator() {
        throw new UnsupportedOperationException();
//        return items.listIterator();
    }

    @NonNull
    @Override
    public ListIterator<T> listIterator(final int location) {
        throw new UnsupportedOperationException();
//        return items.listIterator(location);
    }

    @NonNull
    @Override
    public List<T> subList(final int start, final int end) {return items.subList(start, end);}

    @Override
    public boolean containsAll(@NonNull final Collection<?> collection) {return items.containsAll(collection);}

    @Override
    public boolean removeAll(@NonNull final Collection<?> collection) {
        throw new UnsupportedOperationException();
//        return items.removeAll(collection);
    }

    @Override
    public boolean retainAll(@NonNull final Collection<?> collection) {
        throw new UnsupportedOperationException();
//        return items.retainAll(collection);
    }

}
