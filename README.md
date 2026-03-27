# Phoebus Olog Shift Module

This is an optional module for the Phoebus Olog electronic logbook service, see https://github.com/Olog/phoebus-olog. Its purpose is to automatically attach the currently active shift as a property on every new log entry by querying a running shift service REST API. This is a phoebus port of the cs-studio shift logbook property plugin.

### How it works

When a log entry is submitted, the module queries `GET {shift.url}/shift/{shift.type}`. If the returned shift has `status=Active`, a `Shift` property is attached to the log entry with the following attributes:

- **Id** - the shift identifier
- **Type** - the shift type name
- **URL** - a direct link to the shift in the shift service
- **Owner** - the shift owner

If the shift service is unavailable or no active shift exists, no property is added and the log entry is saved normally.

### Configuration

#### Add to service-olog pom.xml:

```xml
<dependency>
    <groupId>org.phoebus</groupId>
    <artifactId>olog-shift-module</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

#### Add to olog application.properties:

```properties
# URL of the shift service REST API (required)
shift.url=http://localhost:8080/Shift/resources

# Shift type to look up when attaching to log entries
shift.type=Operations

# Optional HTTP Basic Auth credentials for the shift service
# shift.username=
# shift.password=
```
