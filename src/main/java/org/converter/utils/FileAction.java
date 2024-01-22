package org.converter.utils;

public enum FileAction {
    CONVERT_PDF_TO_TEXT("CONVERT PDF TO TEXT"),
    MERGE_PDF("MERGE PDF FILES"),
    SEPARATE_PDF("SPLIT PDF FILE");

    private final String actionDescription;

    FileAction(String actionDescription) {
        this.actionDescription = actionDescription;
    }

    public String getActionDescription() {
        return actionDescription;
    }
}
