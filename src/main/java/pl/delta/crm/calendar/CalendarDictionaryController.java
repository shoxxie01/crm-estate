package pl.delta.crm.calendar;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.delta.crm.calendar.dictionary.EventOutcome;
import pl.delta.crm.calendar.dictionary.EventStatus;
import pl.delta.crm.calendar.dictionary.EventType;
import pl.delta.crm.property.dto.DictionaryEntry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Słowniki formularza terminu — ten sam wzorzec co przy ofertach i klientach,
 * żeby front nie powielał enumów w TypeScripcie.
 */
@RestController
@RequestMapping("/api/calendar/dictionaries")
public class CalendarDictionaryController {

    @GetMapping
    public Map<String, Object> dictionaries() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("type", DictionaryEntry.of(EventType.class));
        result.put("status", DictionaryEntry.of(EventStatus.class));
        result.put("outcome", DictionaryEntry.of(EventOutcome.class));
        return result;
    }
}
