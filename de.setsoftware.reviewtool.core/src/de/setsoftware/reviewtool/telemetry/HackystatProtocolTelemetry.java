package de.setsoftware.reviewtool.telemetry;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.datatype.XMLGregorianCalendar;

import org.hackystat.sensorbase.client.SensorBaseClient;
import org.hackystat.sensorbase.client.SensorBaseClientException;
import org.hackystat.sensorbase.resource.sensordata.jaxb.Properties;
import org.hackystat.sensorbase.resource.sensordata.jaxb.Property;
import org.hackystat.sensorbase.resource.sensordata.jaxb.SensorData;
import org.hackystat.sensorbase.resource.users.jaxb.UserRef;
import org.hackystat.utilities.tstamp.Tstamp;

import de.setsoftware.reviewtool.base.ReviewtoolException;

/**
 * Send telemetry data using the Hackystat protocol.
 *
 * <p>The Hackystat project is more or less dead, but we need to use some protocol anyway, so why not use the one from
 * Hackystat, so that some existing tools can be reused.
 */
public class HackystatProtocolTelemetry extends AbstractTelemetry {

    private final String host;
    private final String hackystatUser;
    private final String password;

    private final Set<String> knownUsers = new HashSet<>();

    public HackystatProtocolTelemetry(String host, String hackystatUser, String password) {
        this.host = host;
        this.hackystatUser = hackystatUser;
        this.password = password;
    }

    @Override
    protected void putData(
            String eventType,
            String ticketKey,
            String user,
            Map<String, String> furtherProperties) {

        final String userMail = obfuscate(user) + "@reviewtool.fake";

        try {
            this.ensureUserExists(userMail);

            final XMLGregorianCalendar tstamp = Tstamp.makeTimestamp();
            final SensorData data = new SensorData();
            //TODO use a more specific name
            data.setTool("Code Review Tool");
            data.setOwner(userMail);
            data.setSensorDataType(eventType);
            data.setTimestamp(tstamp);
            data.setResource(ticketKey);
            data.setRuntime(tstamp);

            final Properties properties = new Properties();
            for (final Entry<String, String> e : furtherProperties.entrySet()) {
                final Property property = new Property();
                property.setKey(e.getKey());
                property.setValue(e.getValue());
                properties.getProperty().add(property);
            }
            data.setProperties(properties);

            final SensorBaseClient client = this.createClient();
            client.putSensorData(data);
        } catch (final SensorBaseClientException e1) {
            throw new ReviewtoolException(e1);
        }
    }

    private void ensureUserExists(String user) throws SensorBaseClientException {
        if (this.knownUsers.isEmpty()) {
            for (final UserRef u : this.createClient().getUserIndex().getUserRef()) {
                this.knownUsers.add(u.getEmail());
            }
        }

        if (!this.knownUsers.contains(user)) {
            SensorBaseClient.registerUser(this.host, user);
            this.knownUsers.add(user);
        }
    }

    private SensorBaseClient createClient() {
        return new SensorBaseClient(this.host, this.hackystatUser, this.password);
    }
}
