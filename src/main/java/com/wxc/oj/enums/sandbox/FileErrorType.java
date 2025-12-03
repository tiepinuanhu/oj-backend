package com.wxc.oj.enums.sandbox;

/**
 * enum FileErrorType {
 *     CopyInOpenFile = 'CopyInOpenFile',
 *     CopyInCreateFile = 'CopyInCreateFile',
 *     CopyInCopyContent = 'CopyInCopyContent',
 *     CopyOutOpen = 'CopyOutOpen',
 *     CopyOutNotRegularFile = 'CopyOutNotRegularFile',
 *     CopyOutSizeExceeded = 'CopyOutSizeExceeded',
 *     CopyOutCreateFile = 'CopyOutCreateFile',
 *     CopyOutCopyContent = 'CopyOutCopyContent',
 *     CollectSizeExceeded = 'CollectSizeExceeded',
 * }
 */
public enum FileErrorType {
    CopyInOpenFile("CopyInOpenFile"),
    CopyInCreateFile("CopyInCreateFile"),
    CopyInCopyContent("CopyInCopyContent"),
    CopyOutOpen("CopyOutOpen"),
    CopyOutNotRegularFile("CopyOutNotRegularFile"),
    CopyOutSizeExceeded("CopyOutSizeExceeded"),
    CopyOutCreateFile("CopyOutCreateFile"),
    CopyOutCopyContent("CopyOutCopyContent"),
    CollectSizeExceeded("CollectSizeExceeded");

    private final String value;

    FileErrorType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
