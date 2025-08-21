package org.vicky.vspe.nms.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Predicate;

public final class ReflectionUtil {
    private ReflectionUtil() {
    }

    public static Optional<Field> findFieldByType(Object instance, Class<?> type) {
        if (instance == null) return Optional.empty();
        Class<?> clazz = instance.getClass();
        while (clazz != null) {
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                try {
                    Object val = f.get(instance);
                    if (val != null && type.isInstance(val)) return Optional.of(f);
                } catch (IllegalAccessException ignored) {
                }
            }
            clazz = clazz.getSuperclass();
        }
        return Optional.empty();
    }

    public static Optional<Field> findFieldByPredicate(Object instance, Predicate<Field> pred) {
        if (instance == null) return Optional.empty();
        Class<?> clazz = instance.getClass();
        while (clazz != null) {
            for (Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                if (pred.test(f)) return Optional.of(f);
            }
            clazz = clazz.getSuperclass();
        }
        return Optional.empty();
    }

    public static Object getFieldValue(Field f, Object instance) {
        try {
            f.setAccessible(true);
            return f.get(instance);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setFieldValue(Field f, Object instance, Object value) {
        try {
            f.setAccessible(true);
            f.set(instance, value);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    public static Optional<Constructor<?>> findConstructorWithParamCount(Class<?> clazz, int paramCount) {
        return Arrays.stream(clazz.getDeclaredConstructors())
                .filter(c -> c.getParameterCount() == paramCount)
                .findFirst();
    }

    public static Optional<Method> findMethodByNameContains(Class<?> clazz, String namePart) {
        for (Method m : clazz.getMethods()) {
            if (m.getName().toLowerCase().contains(namePart.toLowerCase())) return Optional.of(m);
        }
        return Optional.empty();
    }
}