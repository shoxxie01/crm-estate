package pl.delta.crm.client;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.delta.crm.client.dictionary.ClientStatus;
import pl.delta.crm.client.dictionary.LeadSource;
import pl.delta.crm.property.dto.DictionaryEntry;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Słowniki formularza klienta w jednym miejscu — ten sam wzorzec co przy
 * ofertach, żeby front nie powielał enumów w TypeScripcie.
 */
@RestController
@RequestMapping("/api/clients/dictionaries")
public class ClientDictionaryController {

    @GetMapping
    public Map<String, Object> dictionaries() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", DictionaryEntry.of(LeadSource.class));
        result.put("status", DictionaryEntry.of(ClientStatus.class));
        return result;
    }
}
