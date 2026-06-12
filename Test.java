@Service
@RequiredArgsConstructor
public class KdbService {

    private final GenericObjectPool<c> pool;

    public Object query(String qQuery) {
        c conn = null;
        try {
            conn = pool.borrowObject();
            return conn.k(qQuery);          // synchroniczne zapytanie q
        } catch (KException | IOException e) {
            throw new RuntimeException("Błąd zapytania KDB+: " + e.getMessage(), e);
        } catch (Exception e) {
            throw new RuntimeException("Błąd puli połączeń", e);
        } finally {
            if (conn != null) pool.returnObject(conn);
        }
    }

    // Zapytanie z parametrami (funkcja + argumenty)
    public Object queryWithParams(String function, Object... args) {
        c conn = null;
        try {
            conn = pool.borrowObject();
            return conn.k(function, args);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            if (conn != null) pool.returnObject(conn);
        }
    }

    // Przykład: pobierz tabelę jako flip (c.Flip)
    public c.Flip getTable(String tableName) {
        return (c.Flip) query("select from " + tableName);
    }
}



@RestController
@RequiredArgsConstructor
@RequestMapping("/api/kdb")
public class KdbController {

    private final KdbService kdbService;

    @GetMapping("/query")
    public ResponseEntity<Object> query(@RequestParam String q) {
        Object result = kdbService.query(q);
        return ResponseEntity.ok(result);
    }
}