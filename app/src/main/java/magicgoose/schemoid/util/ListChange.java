package magicgoose.schemoid.util;

public class ListChange {
    public final ListChangeKind listChangeKind;
    public final int rangeStart;
    public final int count;

    public ListChange(final ListChangeKind listChangeKind, final int rangeStart, final int count) {
        this.listChangeKind = listChangeKind;
        this.rangeStart = rangeStart;
        this.count = count;
    }

    public static ListChange insert(int start, int size) {
        return new ListChange(ListChangeKind.Insert, start, size);
    }

    public static ListChange delete(int start, int size) {
        return new ListChange(ListChangeKind.Delete, start, size);
    }

    public static ListChange update(int start, int size) {
        return new ListChange(ListChangeKind.Update, start, size);
    }
}
