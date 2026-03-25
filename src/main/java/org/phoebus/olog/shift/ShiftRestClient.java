package org.phoebus.olog.shift;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * REST client for communicating with the shift service.
 */
@Component
public class ShiftRestClient {

    private static final Logger logger = Logger.getLogger(ShiftRestClient.class.getName());

    @Autowired
    private ShiftPreferences preferences;

    private RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        restTemplate = new RestTemplate();
        if (preferences.getUsername() != null && !preferences.getUsername().isEmpty()) {
            restTemplate.getInterceptors().add(
                    new BasicAuthenticationInterceptor(preferences.getUsername(), preferences.getPassword()));
        }
    }

    /**
     * Retrieves the last open shift for the given type from the shift service.
     *
     * @param type the shift type name
     * @return the last open Shift, or null if none found or service unavailable
     */
    public Shift getLastOpenShift(String type) {
        String url = preferences.getShiftUrl() + "/shift/" + type;
        try {
            return restTemplate.getForObject(url, Shift.class);
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to retrieve shift from " + url, e);
            return null;
        }
    }
}
