package org.phoebus.olog.shift;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration preferences for the shift service connection.
 */
@Component("shiftPreferences")
public class ShiftPreferences {

    @Value("${shift.url:http://localhost:8080/Shift/resources}")
    private String shiftUrl;

    @Value("${shift.type:Operations}")
    private String defaultType;

    @Value("${shift.username:}")
    private String username;

    @Value("${shift.password:}")
    private String password;

    public String getShiftUrl() {
        return shiftUrl;
    }

    public String getDefaultType() {
        return defaultType;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
