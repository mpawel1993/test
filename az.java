import c.Flip;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;

public class KdbMapper {

    // Formatka, żeby zapobiec notacji naukowej przy konwersji Double -> String
    private static final DecimalFormat df = new DecimalFormat("0.##########");

    public static List<Map<String, String>> mapFlipToListOfMaps(Flip flip) {
        List<Map<String, String>> resultList = new ArrayList<>();

        String[] columnNames = flip.x; // Nazwy kolumn
        Object[] columnData = flip.y;  // Dane kolumn (tablice)

        if (columnNames == null || columnNames.length == 0) {
            return resultList;
        }

        // Ustalamy liczbę wierszy na podstawie długości pierwszej kolumny
        int rowCount = Array.getLength(columnData[0]);

        for (int i = 0; i < rowCount; i++) {
            Map<String, String> rowMap = new LinkedHashMap<>(); // LinkedHashMap zachowa kolejność kolumn

            for (int col = 0; col < columnNames.length; col++) {
                String colName = columnNames[col];
                Object colArray = columnData[col];

                // Pobieramy wartość dla konkretnego wiersza i kolumny
                Object rawValue = Array.get(colArray, i);

                // Mapujemy i zamieniamy na String
                rowMap.put(colName, convertValueToString(rawValue));
            }

            resultList.add(rowMap);
        }

        return resultList;
    }

    private static String convertValueToString(Object value) {
        if (value == null) {
            return ""; // Lub null, zależnie od Twoich wymagań biznesowych
        }

        // 1. Obsługa tablicy znaków (char[]) -> String
        if (value instanceof char[]) {
            return new String((char[]) value);
        }

        // 2. Obsługa Double / Float -> BigDecimal -> String (czyste formatowanie)
        if (value instanceof Double || value instanceof Float) {
            double dValue = ((Number) value).doubleValue();
            if (Double.isNaN(dValue) || Double.isInfinite(dValue)) {
                return "0"; // kdb ma swoje specyficzne nulle (np. 0N), sterownik mapuje je na NaN
            }
            // Konwersja na BigDecimal rozwiązuje problemy z precyzją zmiennoprzecinkową
            return new BigDecimal(df.format(dValue)).toPlainString();
        }

        // 3. Zabezpieczenie dla kdb-owych typów czasowych/datowych lub innych obiektów
        // c.java mapuje np. c.Timespan, c.Date, które mają czytelne metody toString()
        return value.toString().trim();
    }
}