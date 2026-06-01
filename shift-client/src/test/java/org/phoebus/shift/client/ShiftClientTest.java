package org.phoebus.shift.client;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.phoebus.shift.client.model.Shift;
import org.phoebus.shift.client.model.ShiftType;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.*;

@WireMockTest
class ShiftClientTest {

    private ShiftClient client;

    @BeforeEach
    void setUp(WireMockRuntimeInfo wm) {
        client = ShiftClient.builder()
                .baseUrl(wm.getHttpBaseUrl())
                .build();
    }

    // -------------------------------------------------------------------------
    // getShift
    // -------------------------------------------------------------------------

    @Test
    void getShift_returnsActiveShift() throws ShiftClientException {
        stubFor(get("/shift/Operations")
                .willReturn(okJson(
                        "{\"id\":1,\"status\":\"Active\",\"owner\":\"operator\"," +
                        "\"type\":{\"id\":1,\"name\":\"Operations\"}," +
                        "\"startDate\":1716717600000}")));

        Shift shift = client.getShift("Operations");

        assertNotNull(shift);
        assertEquals(1, shift.getId());
        assertEquals("Active", shift.getStatus());
        assertEquals("operator", shift.getOwner());
        assertNotNull(shift.getType());
        assertEquals("Operations", shift.getType().getName());
        assertNotNull(shift.getStartDate());
    }

    @Test
    void getShift_returns404_givesNull() throws ShiftClientException {
        stubFor(get("/shift/Safety")
                .willReturn(aResponse().withStatus(404)));

        assertNull(client.getShift("Safety"));
    }

    @Test
    void getShift_serviceError_throwsException() {
        stubFor(get("/shift/Operations")
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        assertThrows(ShiftClientException.class, () -> client.getShift("Operations"));
    }

    @Test
    void getShift_ignoresUnknownFields() throws ShiftClientException {
        stubFor(get("/shift/Operations")
                .willReturn(okJson(
                        "{\"id\":7,\"status\":\"Active\",\"owner\":\"jdoe\"," +
                        "\"type\":{\"id\":1,\"name\":\"Operations\"}," +
                        "\"unknownFutureField\":\"some value\"}")));

        Shift shift = client.getShift("Operations");
        assertNotNull(shift);
        assertEquals(7, shift.getId());
    }

    // -------------------------------------------------------------------------
    // listShifts
    // -------------------------------------------------------------------------

    @Test
    void listShifts_returnsAllShifts() throws ShiftClientException {
        stubFor(get("/shifts")
                .willReturn(okJson(
                        "[{\"id\":1,\"status\":\"Active\",\"owner\":\"alice\"," +
                        "\"type\":{\"id\":1,\"name\":\"Operations\"}}," +
                        "{\"id\":2,\"status\":\"Closed\",\"owner\":\"bob\"," +
                        "\"type\":{\"id\":1,\"name\":\"Operations\"}}]")));

        List<Shift> shifts = client.listShifts();

        assertEquals(2, shifts.size());
        assertEquals("Active", shifts.get(0).getStatus());
        assertEquals("Closed", shifts.get(1).getStatus());
    }

    @Test
    void listShifts_emptyResponse() throws ShiftClientException {
        stubFor(get("/shifts").willReturn(okJson("[]")));

        assertTrue(client.listShifts().isEmpty());
    }

    @Test
    void listShifts_404_returnsEmptyList() throws ShiftClientException {
        stubFor(get("/shifts").willReturn(aResponse().withStatus(404)));

        assertTrue(client.listShifts().isEmpty());
    }

    // -------------------------------------------------------------------------
    // listTypes
    // -------------------------------------------------------------------------

    @Test
    void listTypes_returnsAllTypes() throws ShiftClientException {
        stubFor(get("/shiftTypes")
                .willReturn(okJson(
                        "[{\"id\":1,\"name\":\"Operations\"},{\"id\":2,\"name\":\"Safety\"}]")));

        List<ShiftType> types = client.listTypes();

        assertEquals(2, types.size());
        assertEquals("Operations", types.get(0).getName());
        assertEquals("Safety", types.get(1).getName());
    }

    // -------------------------------------------------------------------------
    // Auth
    // -------------------------------------------------------------------------

    @Test
    void basicAuth_sentOnRequests() throws ShiftClientException {
        ShiftClient authedClient = ShiftClient.builder()
                .baseUrl(client.baseUrl())
                .username("admin")
                .password("secret")
                .build();

        stubFor(get("/shift/Operations")
                .withHeader("Authorization", equalTo("Basic YWRtaW46c2VjcmV0"))
                .willReturn(okJson(
                        "{\"id\":1,\"status\":\"Active\",\"owner\":\"op\"," +
                        "\"type\":{\"id\":1,\"name\":\"Operations\"}}")));

        assertNotNull(authedClient.getShift("Operations"));
        verify(getRequestedFor(urlEqualTo("/shift/Operations"))
                .withHeader("Authorization", equalTo("Basic YWRtaW46c2VjcmV0")));
    }
}
