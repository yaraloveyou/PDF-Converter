package org.converter.utils;

import java.util.Arrays;

public class PageExtract {
    public static boolean isExtract(String parts, int pageNumber) {
        int[] tempParts = Arrays.stream(parts.replaceAll("\\d-\\d,", "")
                .split(","))
                .mapToInt(Integer::parseInt)
                .toArray();

        boolean found = Arrays.stream(tempParts)
                .anyMatch(number -> pageNumber == number);

        if (found)
            return true;

        String[] pageParts = parts.split(",");
        for (int i = 0; i < pageParts.length; ++i) {
            int[] tempPage = Arrays.stream(pageParts[i].split("-"))
                    .mapToInt(Integer::parseInt)
                    .toArray();
            if (tempPage.length > 1 && tempPage[0] <= pageNumber && pageNumber <= tempPage[1])
                return true;
        }

        return false;
    }
}
