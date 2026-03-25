package org.phoebus.olog.shift;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Property;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShiftPropertyProviderTest {

    private ShiftRestClient mockShiftRestClient;
    private ShiftPreferences mockPreferences;
    private ShiftPropertyProvider provider;

    @BeforeEach
    void setUp() {
        mockShiftRestClient = mock(ShiftRestClient.class);
        mockPreferences = mock(ShiftPreferences.class);

        provider = new ShiftPropertyProvider();
        // inject mocks via reflection since Spring isn't running
        setField(provider, "shiftRestClient", mockShiftRestClient);
        setField(provider, "preferences", mockPreferences);

        when(mockPreferences.getDefaultType()).thenReturn("Operations");
        when(mockPreferences.getShiftUrl()).thenReturn("http://localhost:8080/Shift/resources");
    }

    @Test
    void testGetPropertyWithActiveShift() {
        Shift shift = new Shift();
        shift.setId(42);
        shift.setStatus("Active");
        shift.setOwner("jdoe");
        ShiftType type = new ShiftType();
        type.setId(1);
        type.setName("Operations");
        shift.setType(type);

        when(mockShiftRestClient.getLastOpenShift("Operations")).thenReturn(shift);

        Log mockLog = mock(Log.class);
        Property property = provider.getProperty(mockLog);

        assertNotNull(property);
        assertEquals("Shift", property.getName());

        String id = getAttributeValue(property, "Id");
        String typeName = getAttributeValue(property, "Type");
        String url = getAttributeValue(property, "URL");
        String owner = getAttributeValue(property, "Owner");

        assertEquals("42", id);
        assertEquals("Operations", typeName);
        assertEquals("http://localhost:8080/Shift/resources/shift/Operations/42", url);
        assertEquals("jdoe", owner);
    }

    @Test
    void testGetPropertyWithInactiveShift() {
        Shift shift = new Shift();
        shift.setId(10);
        shift.setStatus("Closed");

        when(mockShiftRestClient.getLastOpenShift("Operations")).thenReturn(shift);

        Log mockLog = mock(Log.class);
        Property property = provider.getProperty(mockLog);

        assertNull(property);
    }

    @Test
    void testGetPropertyWhenServiceReturnsNull() {
        when(mockShiftRestClient.getLastOpenShift("Operations")).thenReturn(null);

        Log mockLog = mock(Log.class);
        Property property = provider.getProperty(mockLog);

        assertNull(property);
    }

    @Test
    void testGetPropertyWhenServiceThrowsException() {
        when(mockShiftRestClient.getLastOpenShift("Operations"))
                .thenThrow(new RuntimeException("Connection refused"));

        Log mockLog = mock(Log.class);
        Property property = provider.getProperty(mockLog);

        assertNull(property);
    }

    @Test
    void testGetPropertyWithNullShiftType() {
        Shift shift = new Shift();
        shift.setId(7);
        shift.setStatus("Active");
        shift.setType(null);

        when(mockShiftRestClient.getLastOpenShift("Operations")).thenReturn(shift);

        Log mockLog = mock(Log.class);
        Property property = provider.getProperty(mockLog);

        assertNotNull(property);
        assertEquals("Operations", getAttributeValue(property, "Type"));
    }

    private String getAttributeValue(Property property, String attributeName) {
        return property.getAttributes().stream()
                .filter(a -> attributeName.equals(a.getName()))
                .map(Attribute::getValue)
                .findFirst()
                .orElse(null);
    }

    private void setField(Object target, String fieldName, Object value) {
        try {
            var field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field " + fieldName, e);
        }
    }
}
