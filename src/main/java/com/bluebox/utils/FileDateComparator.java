package com.bluebox.utils;

import java.io.File;
import java.util.Comparator;

/*
 * Sort ascending order based on File lastModified value.
 */
public class FileDateComparator implements Comparator<File> {
    @Override
    public int compare(File x, File y) {
        if (x.lastModified() < y.lastModified()) {
            return -1;
        }
        if (x.length() > y.length()) {
            return 1;
        }
        return 0;
    }
}
