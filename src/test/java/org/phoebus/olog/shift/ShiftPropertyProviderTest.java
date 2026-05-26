package org.phoebus.olog.shift;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Property;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShiftPropertyProviderTest {

    private static final String SHIFT_URL = "http://localhost:8080/Shift/resources";
    private static final String SHIFT_TYPE = "Operations";

    private RestTemplate mockRestTemplate;
    private ShiftPropertyProvider provider;

    @BeforeEach
    void setUp() {
        mockRestTemplate = mock(RestTemplate.class);
        provider = new ShiftPropertyProvider(SHIFT_URL, SHIFT_TYPE, mockRestTemplate);
    }

    @Test
    void testGetPropertyWithActiveShift() {
        ShiftType type = new ShiftType();
        type.setId(1);
        type.setName("Operations");

        Shift shift = new Shift();
        shift.setId(42);
        shift.setStatus("Active");
        shift.setOwner("jdoe");
        shift.setType(type);

        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenReturn(shift);

        Property property = provider.getProperty(mock(Log.class));

        assertNotNull(property);
        assertEquals("Shift", property.getName());
        assertEquals("42", attr(property, "Id"));
        assertEquals("Operations", attr(property, "Type"));
        assertEquals(SHIFT_URL + "/shift/Operations/42", attr(property, "URL"));
        assertEquals("jdoe", attr(property, "Owner"));
    }

    @Test
    void testGetPropertyWithInactiveShift() {
        Shift shift = new Shift();
        shift.setId(10);
        shift.setStatus("Closed");

        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenReturn(shift);

        assertNull(provider.getProperty(mock(Log.class)));
    }

    @Test
    void testGetPropertyWhenServiceReturnsNull() {
        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenReturn(null);

        assertNull(provider.getProperty(mock(Log.class)));
    }

    @Test
    void testGetPropertyWhenServiceThrowsException() {
        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenThrow(new RuntimeException("Connection refused"));

        assertNull(provider.getProperty(mock(Log.class)));
    }

    @Test
    void testGetPropertyWithNullShiftType() {
        Shift shift = new Shift();
        shift.setId(7);
        shift.setStatus("Active");
        shift.setType(null);

        when(mockRestTemplate.getForObject(SHIFT_URL + "/shift/" + SHIFT_TYPE, Shift.class))
                .thenReturn(shift);

        Property property = provider.getProperty(mock(Log.class));

        assertNotNull(property);
        assertEquals("Operations", attr(property, "Type"));
    }

    private String attr(Property property, String name) {
        return property.getAttributes().stream()
                .filter(a -> name.equals(a.getName()))
                .map(Attribute::getValue)
                .findFirst()
                .orElse(null);
    }
}
