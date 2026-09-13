package pl.delta.crm.inquiry;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Limit zgłoszeń z jednego adresu IP — druga, obok pola-pułapki, zapora przed
 * zasypaniem skrzynki przez skrypt.
 *
 * <p>W pamięci procesu, bez zewnętrznego magazynu: przy jednej instancji
 * aplikacji to wystarcza, a restart co najwyżej zeruje liczniki. Przy kilku
 * instancjach za load balancerem limit trzeba przenieść do współdzielonego
 * magazynu (patrz „Przed produkcją" w README).
 */
@Component
public class IntakeRateLimiter {

    static final int MAX_SUBMISSIONS = 5;
    static final Duration WINDOW = Duration.ofMinutes(10);

    private final Map<String, Deque<Instant>> submissions = new ConcurrentHashMap<>();
    private final Clock clock;

    public IntakeRateLimiter() {
        this(Clock.systemUTC());
    }

    IntakeRateLimiter(Clock clock) {
        this.clock = clock;
    }

    /** Rejestruje próbę i zwraca {@code false}, gdy limit dla adresu jest już wyczerpany. */
    public boolean tryAcquire(String address) {
        Instant now = clock.instant();
        Instant windowStart = now.minus(WINDOW);

        Deque<Instant> recent = submissions.computeIfAbsent(address, key -> new ArrayDeque<>());
        synchronized (recent) {
            while (!recent.isEmpty() && recent.peekFirst().isBefore(windowStart)) {
                recent.pollFirst();
            }
            if (recent.size() >= MAX_SUBMISSIONS) {
                return false;
            }
            recent.addLast(now);
        }

        // Sprzątanie adresów, które dawno nic nie wysłały — inaczej mapa rośnie bez końca.
        if (submissions.size() > 10_000) {
            submissions.entrySet().removeIf(entry -> {
                synchronized (entry.getValue()) {
                    Instant last = entry.getValue().peekLast();
                    return last == null || last.isBefore(windowStart);
                }
            });
        }
        return true;
    }
}
