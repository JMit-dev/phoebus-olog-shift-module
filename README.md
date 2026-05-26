# Phoebus Olog Shift Module

This is an optional module for the Phoebus Olog electronic logbook service, see https://github.com/Olog/phoebus-olog. Its purpose is to automatically attach the currently active shift as a property on every new log entry by querying a running shift service REST API. This is a phoebus port of the cs-studio shift logbook property plugin.

### How it works

When a log entry is submitted, the module queries `GET {shift.url}/shift/{shift.type}` for each configured shift type in order. For the first type that returns an active shift, a `Shift` property is attached to the log entry with the following attributes:

- **Id** - the shift identifier
- **Type** - the shift type name
- **URL** - a direct link to the shift in the shift service
- **Owner** - the shift owner

If the shift service is unavailable or no active shift exists, no property is added and the log entry is saved normally.

### Running with Docker

The included `Dockerfile` and `docker-compose.yml` provide a complete stack:

```bash
docker compose up -d
```

This starts four services:

| Service | Description | Port |
|---|---|---|
| `olog` | phoebus-olog with shift module injected | 8080 (HTTP), 8181 (HTTPS) |
| `shift-mock` | WireMock stub serving a fake active shift | 8282 |
| `mongo` | MongoDB (log attachment storage) | 27017 |
| `elastic` | Elasticsearch (log index) | 9200 |

Once running, create a log entry:

```bash
curl --insecure --request PUT 'https://localhost:8181/Olog/logs' \
  --header 'Content-Type: application/json' \
  --header 'Authorization: Basic YWRtaW46YWRtaW5QYXNz' \
  --data '{
    "owner": "test",
    "source": "test entry",
    "title": "My Log",
    "level": "Info",
    "logbooks": [{"name": "operations", "owner": "olog-logs"}]
  }'
```

The response will include a `Shift` property automatically attached from the active shift.

### Configuration

The following environment variables (or Java system properties) configure the module at runtime:

| Variable | System property | Default | Description |
|---|---|---|---|
| `SHIFT_URL` | `shift.url` | `http://localhost:8080/Shift/resources` | Shift service base URL |
| `SHIFT_TYPE` | `shift.type` | `Operations` | Shift type(s) to query — comma-separated list, checked in order |
| `SHIFT_CACHE_TTL` | `shift.cache.ttl` | `30` | Seconds to cache shift responses (0 to disable) |
| `SHIFT_CONNECT_TIMEOUT` | `shift.connect.timeout` | `3000` | Connection timeout in milliseconds |
| `SHIFT_READ_TIMEOUT` | `shift.read.timeout` | `5000` | Read timeout in milliseconds |
| `SHIFT_USERNAME` | `shift.username` | *(none)* | HTTP Basic Auth username |
| `SHIFT_PASSWORD` | `shift.password` | *(none)* | HTTP Basic Auth password |
| `SHIFT_SSL_TRUST_ALL` | `shift.ssl.trust.all` | `false` | Accept any TLS certificate — dev/test only |
| `SHIFT_SSL_TRUSTSTORE` | `shift.ssl.truststore` | *(none)* | Path to a custom JKS trust store file |
| `SHIFT_SSL_TRUSTSTORE_PASSWORD` | `shift.ssl.truststore.password` | *(none)* | Password for the custom trust store |

Set these in `docker-compose.yml` under the `olog` service's `environment` block, or pass them as `-D` flags when running the JAR directly.

**Multiple shift types example** — check Operations first, fall back to Commissioning if no active Operations shift:

```yaml
environment:
  SHIFT_TYPE: Operations,Commissioning
```

### How injection works

The `Dockerfile` builds the module JAR, then extracts the phoebus-olog fat JAR into a plain directory, copies `shift-module.jar` into `BOOT-INF/lib/`, and appends it to `BOOT-INF/classpath.idx`. Spring Boot's `JarLauncher` is then invoked directly so all `BOOT-INF/lib/` entries are regular `file://` URLs — this is required for Java's `ServiceLoader` to discover the `LogPropertyProvider` SPI registered by the module.
