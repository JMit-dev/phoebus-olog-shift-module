package org.phoebus.olog.shift;

import com.google.auto.service.AutoService;
import org.phoebus.olog.entity.Attribute;
import org.phoebus.olog.entity.Log;
import org.phoebus.olog.entity.Property;
import org.phoebus.olog.entity.preprocess.LogPropertyProvider;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * A {@link LogPropertyProvider} that automatically attaches the currently active shift
 * as a property on every new log entry. This is a phoebus-olog port of the cs-studio
 * shift logbook property plugin.
 *
 * <p>The "Shift" property added to each log entry contains:
 * <ul>
 *   <li>Id    – the shift identifier</li>
 *   <li>Type  – the shift type name</li>
 *   <li>URL   – a link to the shift in the shift service</li>
 *   <li>Owner – the shift owner</li>
 * </ul>
 *
 * <p>When multiple shift types are configured, the provider checks them in order and
 * attaches the property for the first active shift found.
 *
 * <p>Responses are cached for {@code SHIFT_CACHE_TTL} seconds to avoid querying the
 * shift service on every log entry. Exceptions are never cached.
 *
 * <p>Configuration is read from environment variables (preferred in Docker) or JVM
 * system properties (-D flags). See the README for the full list of options.
 *
 * <p>Loaded via Java SPI ({@link java.util.ServiceLoader}) by phoebus-olog's
 * PreProcessorConfig. Spring injection is not available in this path, so all
 * configuration is read directly from the environment.
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
    private final List<String> shiftTypes;
    private final RestTemplate restTemplate;
    private final ShiftCache cache;

    /** No-arg constructor used by ServiceLoader in production. */
    public ShiftPropertyProvider() {
        this(
            resolve("SHIFT_URL", "shift.url", "http://localhost:8080/Shift/resources"),
            parseTypes(resolve("SHIFT_TYPE", "shift.type", "Operations")),
            buildRestTemplate(
                resolve("SHIFT_USERNAME", "shift.username", ""),
                resolve("SHIFT_PASSWORD", "shift.password", ""),
                parseInt("SHIFT_CONNECT_TIMEOUT", "shift.connect.timeout", 3000),
                parseInt("SHIFT_READ_TIMEOUT", "shift.read.timeout", 5000),
                parseBoolean("SHIFT_SSL_TRUST_ALL", "shift.ssl.trust.all", false),
                resolve("SHIFT_SSL_TRUSTSTORE", "shift.ssl.truststore", ""),
                resolve("SHIFT_SSL_TRUSTSTORE_PASSWORD", "shift.ssl.truststore.password", "")
            ),
            parseLong("SHIFT_CACHE_TTL", "shift.cache.ttl", 30) * 1000L,
            Clock.systemUTC()
        );
    }

    /** Package-private constructor for unit tests. */
    ShiftPropertyProvider(String shiftUrl, List<String> shiftTypes, RestTemplate restTemplate,
                          long cacheTtlMs, Clock clock) {
        this.shiftUrl = shiftUrl;
        this.shiftTypes = shiftTypes;
        this.restTemplate = restTemplate;
        this.cache = new ShiftCache(cacheTtlMs, clock);
        logger.log(Level.INFO, "ShiftPropertyProvider initialized: url=" + shiftUrl
                + ", types=" + shiftTypes + ", cacheTtlMs=" + cacheTtlMs);
    }

    @Override
    public Property getProperty(Log log) {
        for (String type : shiftTypes) {
            Property p = fetchProperty(type);
            if (p != null) return p;
        }
        return null;
    }

    private Property fetchProperty(String type) {
        Shift shift;
        ShiftCache.CacheEntry cached = cache.lookup(type);
        if (cached != null) {
            shift = cached.shift;
        } else {
            try {
                shift = restTemplate.getForObject(shiftUrl + "/shift/" + type, Shift.class);
                cache.store(type, shift);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error retrieving shift for type: " + type, e);
                return null;
            }
        }

        if (shift == null) {
            return null;
        }

        if (!"Active".equalsIgnoreCase(shift.getStatus())) {
            return null;
        }

        String typeName = (shift.getType() != null) ? shift.getType().getName() : type;
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
    }

    // -------------------------------------------------------------------------
    // Configuration helpers
    // -------------------------------------------------------------------------

    private static String resolve(String envVar, String sysProp, String defaultValue) {
        String env = System.getenv(envVar);
        if (env != null && !env.isEmpty()) return env;
        return System.getProperty(sysProp, defaultValue);
    }

    private static List<String> parseTypes(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static int parseInt(String envVar, String sysProp, int defaultValue) {
        try {
            return Integer.parseInt(resolve(envVar, sysProp, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static long parseLong(String envVar, String sysProp, long defaultValue) {
        try {
            return Long.parseLong(resolve(envVar, sysProp, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    private static boolean parseBoolean(String envVar, String sysProp, boolean defaultValue) {
        String value = resolve(envVar, sysProp, String.valueOf(defaultValue));
        return Boolean.parseBoolean(value);
    }

    // -------------------------------------------------------------------------
    // RestTemplate construction
    // -------------------------------------------------------------------------

    private static RestTemplate buildRestTemplate(String username, String password,
                                                   int connectTimeout, int readTimeout,
                                                   boolean trustAll, String trustStorePath,
                                                   String trustStorePassword) {
        SimpleClientHttpRequestFactory factory;

        if (trustAll || !trustStorePath.isEmpty()) {
            factory = buildSslFactory(trustAll, trustStorePath, trustStorePassword,
                    connectTimeout, readTimeout);
        } else {
            factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(connectTimeout);
            factory.setReadTimeout(readTimeout);
        }

        RestTemplate rt = new RestTemplate(factory);
        if (!username.isEmpty()) {
            rt.getInterceptors().add(new BasicAuthenticationInterceptor(username, password));
        }
        return rt;
    }

    /**
     * Builds a {@link SimpleClientHttpRequestFactory} with a custom SSL context.
     * When {@code trustAll} is true every certificate is accepted (for dev/test only).
     * Otherwise the given trust store file is loaded and used.
     */
    private static SimpleClientHttpRequestFactory buildSslFactory(boolean trustAll,
                                                                   String trustStorePath,
                                                                   String trustStorePassword,
                                                                   int connectTimeout,
                                                                   int readTimeout) {
        try {
            SSLContext sslContext;
            if (trustAll) {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    public void checkClientTrusted(X509Certificate[] c, String a) {}
                    public void checkServerTrusted(X509Certificate[] c, String a) {}
                }}, new SecureRandom());
            } else {
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                try (FileInputStream fis = new FileInputStream(trustStorePath)) {
                    ks.load(fis, trustStorePassword.toCharArray());
                }
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                        TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, tmf.getTrustManagers(), null);
            }

            final SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            final boolean skipHostnameVerification = trustAll;

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(HttpURLConnection connection, String httpMethod)
                        throws IOException {
                    if (connection instanceof HttpsURLConnection) {
                        HttpsURLConnection https = (HttpsURLConnection) connection;
                        https.setSSLSocketFactory(sslSocketFactory);
                        if (skipHostnameVerification) {
                            https.setHostnameVerifier((hostname, session) -> true);
                        }
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            };
            factory.setConnectTimeout(connectTimeout);
            factory.setReadTimeout(readTimeout);
            return factory;

        } catch (Exception e) {
            throw new RuntimeException("Failed to configure SSL for shift service client", e);
        }
    }
}
