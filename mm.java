package com.example.kdb;

import com.kx.c;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generyczny mapper kdb+ Flip (tabeli) -> liste obiektow Java (POJO).
 *
 * Domyslnie:
 *  - kolumna kdb 'f'/'j' (double/long) -> pole typu BigDecimal jesli pole jest BigDecimal,
 *    w przeciwnym razie zostaje odpowiedni typ prosty (double/long/...).
 *  - kolumna stringowa (lista znakow per wiersz, czyli typowy String w q) -> String,
 *    a nie char[].
 *  - kolumna symbol -> String (bez zmian, bo c.java juz zwraca String[]).
 *
 * Mapowanie pole<->kolumna: domyslnie po nazwie pola (dokladnie, a jak nie znajdzie -
 * to case-insensitive). Mozna nadpisac przez @KdbColumn(name = "...").
 */
public final class KdbMapper {

    private KdbMapper() {
    }

    /** Adnotacja pozwalajaca nadpisac nazwe kolumny kdb dla danego pola oraz wlaczyc trim() Stringow. */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface KdbColumn {
        /** Nazwa kolumny w kdb+, jesli inna niz nazwa pola. */
        String name() default "";

        /** Czy przyciac biale znaki ze stringow (kdb czesto je dopelnia spacjami w fixed-width listach). */
        boolean trim() default false;
    }

    /**
     * Mapuje cala tabele (Flip) na liste obiektow typu T.
     *
     * @param flip        tabela zwrocona przez kdb+ (np. wynik c.k(...))
     * @param targetClass klasa docelowa, musi miec konstruktor bezargumentowy
     */
    public static <T> List<T> map(c.Flip flip, Class<T> targetClass) {
        List<T> result = new ArrayList<>();
        if (flip == null || flip.x == null || flip.x.length == 0) {
            return result;
        }

        Map<String, Object> columnsByName = new HashMap<>();
        for (int i = 0; i < flip.x.length; i++) {
            columnsByName.put(flip.x[i], flip.y[i]);
        }

        int rowCount = columnLength(flip.y[0]);

        List<Field> fields = new ArrayList<>();
        for (Field f : targetClass.getDeclaredFields()) {
            f.setAccessible(true);
            fields.add(f);
        }

        try {
            for (int row = 0; row < rowCount; row++) {
                T instance = targetClass.getDeclaredConstructor().newInstance();
                for (Field field : fields) {
                    KdbColumn ann = field.getAnnotation(KdbColumn.class);
                    String colName = (ann != null && !ann.name().isEmpty()) ? ann.name() : field.getName();

                    Object colData = columnsByName.get(colName);
                    if (colData == null) {
                        colData = findCaseInsensitive(columnsByName, colName);
                    }
                    if (colData == null) {
                        continue; // brak takiej kolumny w wyniku - pole zostaje null/domyslne
                    }

                    boolean trim = ann != null && ann.trim();
                    Object raw = readRaw(colData, row);
                    Object value = convert(raw, field.getType(), trim);
                    if (value != null) {
                        field.set(instance, value);
                    }
                }
                result.add(instance);
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Nie udalo sie zmapowac " + targetClass.getName(), e);
        }
        return result;
    }

    /** Mapuje pojedynczy wiersz - przydatne np. dla wyniku zapytania zwracajacego jeden rekord. */
    public static <T> T mapSingleRow(c.Flip flip, int row, Class<T> targetClass) {
        List<T> all = map(flip, targetClass);
        return (row < 0 || row >= all.size()) ? null : all.get(row);
    }

    // ---------------------------------------------------------------------

    private static Object findCaseInsensitive(Map<String, Object> columns, String name) {
        for (Map.Entry<String, Object> e : columns.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    private static int columnLength(Object column) {
        if (column instanceof Object[]) return ((Object[]) column).length;
        if (column instanceof double[]) return ((double[]) column).length;
        if (column instanceof long[]) return ((long[]) column).length;
        if (column instanceof int[]) return ((int[]) column).length;
        if (column instanceof short[]) return ((short[]) column).length;
        if (column instanceof float[]) return ((float[]) column).length;
        if (column instanceof boolean[]) return ((boolean[]) column).length;
        if (column instanceof byte[]) return ((byte[]) column).length;
        if (column instanceof char[]) return ((char[]) column).length;
        if (column instanceof char[][]) return ((char[][]) column).length;
        throw new IllegalArgumentException("Nieznany typ kolumny kdb: " + column.getClass());
    }

    /** Wyciaga "surowa" wartosc dla danego wiersza z tablicy kolumnowej kdb. */
    private static Object readRaw(Object colData, int row) {
        if (colData instanceof double[]) return ((double[]) colData)[row];
        if (colData instanceof long[]) return ((long[]) colData)[row];
        if (colData instanceof int[]) return ((int[]) colData)[row];
        if (colData instanceof short[]) return ((short[]) colData)[row];
        if (colData instanceof float[]) return ((float[]) colData)[row];
        if (colData instanceof boolean[]) return ((boolean[]) colData)[row];
        if (colData instanceof byte[]) return ((byte[]) colData)[row];
        if (colData instanceof char[]) return ((char[]) colData)[row];     // kolumna pojedynczych znakow
        if (colData instanceof char[][]) return ((char[][]) colData)[row]; // rzadziej spotykane, ale obsluzone
        if (colData instanceof Object[]) return ((Object[]) colData)[row]; // m.in. listy stringow, symbole, daty...
        throw new IllegalArgumentException("Nieobslugiwany typ kolumny kdb: " + colData.getClass());
    }

    /**
     * Konwertuje "surowa" wartosc kdb (po wyciagnieciu z tablicy kolumnowej) na typ docelowy pola Java.
     * Tutaj dzieje sie cala "magia": double -> BigDecimal, char[] -> String, itd.
     */
    private static Object convert(Object raw, Class<?> targetType, boolean trim) {
        if (raw == null) {
            return null;
        }

        // q string (lista znakow reprezentujaca jedna wartosc tekstowa w wierszu) -> String
        if (raw instanceof char[]) {
            String s = new String((char[]) raw);
            if (trim) {
                s = s.trim();
            }
            return s;
        }

        // kolumna pojedynczych znakow (q char atom per wiersz)
        if (raw instanceof Character) {
            char ch = (Character) raw;
            if (targetType == String.class) {
                return String.valueOf(ch);
            }
            return raw;
        }

        if (raw instanceof Double) {
            double d = (Double) raw;
            if (targetType == BigDecimal.class) {
                return BigDecimal.valueOf(d);
            }
            if (targetType == Float.class || targetType == float.class) {
                return (float) d;
            }
            return d;
        }

        if (raw instanceof Long) {
            long l = (Long) raw;
            if (targetType == BigDecimal.class) {
                return BigDecimal.valueOf(l);
            }
            if (targetType == Integer.class || targetType == int.class) {
                return (int) l;
            }
            return l;
        }

        if (raw instanceof Integer) {
            int i = (Integer) raw;
            if (targetType == BigDecimal.class) {
                return BigDecimal.valueOf(i);
            }
            if (targetType == Long.class || targetType == long.class) {
                return (long) i;
            }
            return i;
        }

        if (raw instanceof Float) {
            float f = (Float) raw;
            if (targetType == BigDecimal.class) {
                return BigDecimal.valueOf(f);
            }
            if (targetType == Double.class || targetType == double.class) {
                return (double) f;
            }
            return f;
        }

        // q timestamp (starszy klient c.java) - jesli pole chce LocalDateTime/LocalDate, a kdb dalo java.sql.Timestamp/java.util.Date
        if (raw instanceof java.sql.Timestamp) {
            if (targetType == LocalDateTime.class) {
                return ((java.sql.Timestamp) raw).toLocalDateTime();
            }
            return raw;
        }
        if (raw instanceof java.util.Date) {
            if (targetType == LocalDate.class) {
                return ((java.util.Date) raw).toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDate();
            }
            if (targetType == LocalDateTime.class) {
                return ((java.util.Date) raw).toInstant().atZone(java.time.ZoneOffset.UTC).toLocalDateTime();
            }
            return raw;
        }

        // boolean, short, byte, UUID, symbole (String), LocalDate/LocalDateTime/LocalTime (jesli klient juz je zwraca) - bez zmian
        return raw;
    }
}
