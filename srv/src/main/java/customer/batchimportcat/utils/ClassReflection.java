package customer.batchimportcat.utils;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.google.common.reflect.ClassPath;

import java.util.Map;
import java.util.stream.Collectors;

public class ClassReflection {
    public static List<Map<String, Object>> getClassbyInterface(Class interfaceClass)
            throws IOException {
        // ClassPath.from(ClassLoader.getSystemClassLoader()).getAllClasses()
        return ClassPath.from(ClassLoader.getSystemClassLoader()).getTopLevelClassesRecursive("customer").stream()
                .filter(clazz -> {
                    // interfaceClass.
                    Class claz1;
                    Class[] interfaces;

                    try {
                        claz1 = clazz.load();
                        interfaces = claz1.getInterfaces();
                    } catch (Exception e) {
                        // TODO: handle exception
                        interfaces = null;
                    }

                    return Arrays.asList(interfaces).contains(interfaceClass);
                }).map(clazz -> {
                    Map<String, ? super Object> data = new HashMap<>();
                    data.put("Name", clazz.getName());
                    data.put("Description", clazz.getSimpleName());
                    return data;
                    // return new HashMap<>()
                }).collect(Collectors.toList());
    }
}
