package magicgoose.schemoid.fragment;

import magicgoose.schemoid.scheme.SchemeLogItem;
import magicgoose.schemoid.scheme.SchemeLogItemKind;

// This is a hacky solution, need to get rid of it eventually
public class SelectableSchemeLogItem extends SchemeLogItem {

    public transient boolean isSelected;

    public SelectableSchemeLogItem(final SchemeLogItemKind kind, final String content) {
        super(kind, content);
    }

    public SelectableSchemeLogItem(final SchemeLogItem x) {
        this(x.kind, x.content);
    }
}
