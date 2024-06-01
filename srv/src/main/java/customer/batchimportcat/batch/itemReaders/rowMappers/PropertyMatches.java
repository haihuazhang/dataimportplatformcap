package customer.batchimportcat.batch.itemReaders.rowMappers;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

public final class PropertyMatches {
    // Source code is decompiled from a .class file using FernFlower decompiler.
    // package org.springframework.batch.extensions.excel.mapping;

    // final class PropertyMatches {
    private final String propertyName;
    private final String[] possibleMatches;

    static PropertyMatches forProperty(String propertyName, Class<?> beanClass, int maxDistance) {
        return new PropertyMatches(propertyName, beanClass, maxDistance);
    }

    private PropertyMatches(String propertyName, Class<?> beanClass, int maxDistance) {
        this.propertyName = propertyName;
        this.possibleMatches = this.calculateMatches(BeanUtils.getPropertyDescriptors(beanClass), maxDistance);
    }

    String[] getPossibleMatches() {
        return this.possibleMatches;
    }

    private String[] calculateMatches(PropertyDescriptor[] propertyDescriptors, int maxDistance) {
        List<String> candidates = new ArrayList();
        PropertyDescriptor[] var4 = propertyDescriptors;
        int var5 = propertyDescriptors.length;

        for (int var6 = 0; var6 < var5; ++var6) {
            PropertyDescriptor propertyDescriptor = var4[var6];
            if (propertyDescriptor.getWriteMethod() != null) {
                String possibleAlternative = propertyDescriptor.getName();
                int distance = this.calculateStringDistance(this.propertyName, possibleAlternative);
                if (distance <= maxDistance) {
                    candidates.add(possibleAlternative);
                }
            }
        }

        Collections.sort(candidates);
        return StringUtils.toStringArray(candidates);
    }

    private int calculateStringDistance(String s1, String s2) {
        if (s1.length() == 0) {
            return s2.length();
        } else if (s2.length() == 0) {
            return s1.length();
        } else {
            int[][] d = new int[s1.length() + 1][s2.length() + 1];

            int i;
            for (i = 0; i <= s1.length(); d[i][0] = i++) {
            }

            for (i = 0; i <= s2.length(); d[0][i] = i++) {
            }

            for (i = 1; i <= s1.length(); ++i) {
                char s_i = s1.charAt(i - 1);

                for (int j = 1; j <= s2.length(); ++j) {
                    char t_j = s2.charAt(j - 1);
                    byte cost;
                    if (Character.toLowerCase(s_i) == Character.toLowerCase(t_j)) {
                        cost = 0;
                    } else {
                        cost = 1;
                    }

                    d[i][j] = Math.min(Math.min(d[i - 1][j] + 1, d[i][j - 1] + 1), d[i - 1][j - 1] + cost);
                }
            }

            return d[s1.length()][s2.length()];
        }
    }
}

// }
