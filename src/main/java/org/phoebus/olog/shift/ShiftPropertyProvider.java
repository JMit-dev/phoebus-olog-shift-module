package org.phoebus.olog.shift;

import com.google.auto.service.AutoService;
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.preprocess.LogPropertyProvider;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link LogPropertyProvider} that automatically attaches the currently active shift
 * as a property on every new log entry. This is a phoebus-olog port of the cs-studio
 * shift logbook property plugin.
 *
 * <p>The "Shift" property added to each log entry contains:
 * <ul>
 *   <li>Id - the shift identifier</li>
 *   <li>Type - the shift type name</li>
 *   <li>URL - a link to the shift in the shift service</li>
 *   <li>Owner - the shift owner</li>
 * </ul>
 *
 * <p>Configuration is read from environment variables (preferred in Docker) or JVM system
 * properties (-D flags): SHIFT_URL / shift.url, SHIFT_TYPE / shift.type,
 * SHIFT_USERNAME / shift.username, SHIFT_PASSWORD / shift.password.
 *
 * <p>If the shift service is unavailable or no active shift exists, no property is added.
 *
 * <p>Loaded via Java SPI ({@link java.util.ServiceLoader}) by phoebus-olog's PreProcessorConfig.
 * Spring injection is not available in this loading path, so configuration is read directly
 * from the environment.
 */
@AutoService(LogPropertyProvider.class)
public class ShiftPropertyProvider implements LogPropertyProvider {

    public static final String PROPERTY_NAME = "Shift";
    public static final String ATTR_ID = "Id";
    public static final String ATTR_TYPE = "Type";
    public static final String ATTR_URL = "URL";
    public static final String ATTR_OWNER = "Owner";

    private static final Logger logger = Logger.getLogger(ShiftPropertyProvider.class.getName());

    private final String shiftUrl;
    private final String defaultType;
    private final RestTemplate restTemplate;

    // No-arg constructor used by ServiceLoader in production.
    public ShiftPropertyProvider() {
        this(
            resolve("SHIFT_URL", "shift.url", "http://localhost:8080/Shift/resources"),
            resolve("SHIFT_TYPE", "shift.type", "Operations"),
            buildRestTemplate(
                resolve("SHIFT_USERNAME", "shift.username", ""),
                resolve("SHIFT_PASSWORD", "shift.password", "")
            )
        );
    }

    // Package-private constructor for unit tests.
    ShiftPropertyProvider(String shiftUrl, String defaultType, RestTemplate restTemplate) {
        this.shiftUrl = shiftUrl;
        this.defaultType = defaultType;
        this.restTemplate = restTemplate;
    }

    private static String resolve(String envVar, String sysProp, String defaultValue) {
        String env = System.getenv(envVar);
        if (env != null && !env.isEmpty()) return env;
        return System.getProperty(sysProp, defaultValue);
    }

    private static RestTemplate buildRestTemplate(String username, String password) {
        RestTemplate rt = new RestTemplate();
        if (!username.isEmpty()) {
            rt.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));
        }
        return rt;
    }

    @Override
    public Property getProperty(Log log) {
        try {
            Shift shift = restTemplate.getForObject(shiftUrl + "/shift/" + defaultType, Shift.class);

            if (shift == null) {
                logger.log(Level.INFO, "No shift returned from service for type: " + defaultType);
                return null;
            }

            if (!"Active".equalsIgnoreCase(shift.getStatus())) {
                logger.log(Level.INFO, "No active shift for type: " + defaultType + ", status: " + shift.getStatus());
                return null;
            }

            String typeName = (shift.getType() != null) ? shift.getType().getName() : defaultType;
            String shiftId = (shift.getId() != null) ? shift.getId().toString() : "";

            Property property = new Property(PROPERTY_NAME);
            property.addAttributes(new Attribute(ATTR_ID, shiftId));
            property.addAttributes(new Attribute(ATTR_TYPE, typeName));
            property.addAttributes(new Attribute(ATTR_URL, shiftUrl + "/shift/" + typeName + "/" + shiftId));

            if (shift.getOwner() != null) {
                property.addAttributes(new Attribute(ATTR_OWNER, shift.getOwner()));
            }

            logger.log(Level.INFO, "Attaching shift property: id=" + shiftId + ", type=" + typeName);
            return property;

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error retrieving shift property", e);
            return null;
        }
    }
}
