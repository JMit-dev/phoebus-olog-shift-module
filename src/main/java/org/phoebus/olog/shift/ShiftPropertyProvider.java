package org.phoebus.olog.shift;

import com.google.auto.service.AutoService;
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.preprocess.LogPropertyProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

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
 * <p>If the shift service is unavailable or no active shift exists, no property is added.
 */
@AutoService(LogPropertyProvider.class)
@Component
public class ShiftPropertyProvider implements LogPropertyProvider {

    public static final String PROPERTY_NAME = "Shift";
    public static final String ATTR_ID = "Id";
    public static final String ATTR_TYPE = "Type";
    public static final String ATTR_URL = "URL";
    public static final String ATTR_OWNER = "Owner";

    private static final Logger logger = Logger.getLogger(ShiftPropertyProvider.class.getName());

    @Autowired
    private ShiftRestClient shiftRestClient;

    @Autowired
    private ShiftPreferences preferences;

    @Override
    public Property getProperty(Log log) {
        try {
            String type = preferences.getDefaultType();
            Shift shift = shiftRestClient.getLastOpenShift(type);

            if (shift == null) {
                logger.log(Level.INFO, "No shift returned from service for type: " + type);
                return null;
            }

            if (!"Active".equalsIgnoreCase(shift.getStatus())) {
                logger.log(Level.INFO, "No active shift found for type: " + type + ", status: " + shift.getStatus());
                return null;
            }

            Property property = new Property(PROPERTY_NAME);

            String typeName = (shift.getType() != null) ? shift.getType().getName() : type;
            String shiftId = (shift.getId() != null) ? shift.getId().toString() : "";
            String shiftUrl = preferences.getShiftUrl() + "/shift/" + typeName + "/" + shiftId;

            property.addAttributes(new Attribute(ATTR_ID, shiftId));
            property.addAttributes(new Attribute(ATTR_TYPE, typeName));
            property.addAttributes(new Attribute(ATTR_URL, shiftUrl));

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
