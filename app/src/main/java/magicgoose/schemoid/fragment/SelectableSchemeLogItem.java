package magicgoose.schemoid.fragment;

import magicgoose.schemoid.scheme.SchemeLogItem;
import magicgoose.schemoid.scheme.SchemeLogItemKind;

public class SelectableSchemeLogItem extends SchemeLogItem {

    public transient boolean isSelected;

    public SelectableSchemeLogItem(final SchemeLogItemKind kind, final String displayContent, final String formattedContent) {
        super(kind, displayContent, formattedContent);
    }
}
