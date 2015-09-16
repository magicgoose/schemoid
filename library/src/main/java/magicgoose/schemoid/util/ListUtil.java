package magicgoose.schemoid.util;

import java.util.Iterator;
import java.util.ListIterator;

public class ListUtil {
    public static <T> Iterable<T> reverseIterator(ListIterator<T> iterator) {
        return () -> new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasPrevious();
            }

            @Override
            public T next() {
                return iterator.previous();
            }
        };
    }
}
