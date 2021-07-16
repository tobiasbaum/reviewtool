package de.setsoftware.reviewtool.ticketconnectors.jira;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;

/**
 * Utility class that provides/creates a bearer token when using OAuth 2.0.
 */
final class BearerTokenProvider {

    private BearerTokenProvider() {
    }

    /**
     * Creates a bearer token.
     */
    static final String createBearerToken(
            final String oauthIssuer,
            final String oauthAudience,
            final String oauthClientID,
            final String oauthClientSecret) throws IOException {

        final String issuerUri = oauthIssuer + "oauth/token";
        final HttpURLConnection c = (HttpURLConnection) new URL(issuerUri).openConnection();

        c.setRequestMethod("POST");
        c.addRequestProperty("Content-Type", "application/json");
        c.setDoOutput(true);
        c.connect();

        final JsonObject data = new JsonObject();
        data.add("client_id", oauthClientID);
        data.add("client_secret", oauthClientSecret);
        data.add("audience", oauthAudience);
        data.add("grant_type", "client_credentials");

        try (OutputStream out = c.getOutputStream()) {
            out.write(data.toString().getBytes(StandardCharsets.UTF_8));
        }

        try (
                final InputStream input = c.getInputStream();
                final InputStreamReader reader = new InputStreamReader(input, StandardCharsets.UTF_8);) {

            final JsonObject json = Json.parse(reader).asObject();
            final String bearerToken = json.getString("access_token", null); //$NON-NLS-1$

            final String[] tokenParts = bearerToken.split("\\."); //$NON-NLS-1$
            if (tokenParts.length != 3) {
                throw new IOException("Received invalid bearer token"); //$NON-NLS-1$
            }

            final JsonObject jsonHeader = Json.parse(decodeBase64EncodedString(tokenParts[0])).asObject();
            final String type = jsonHeader.get("typ").asString(); //$NON-NLS-1$
            if (!type.equals("JWT")) { //$NON-NLS-1$
                throw new IOException("JSON web token expected: " + type); //$NON-NLS-1$
            }

            final JsonObject jsonData = Json.parse(decodeBase64EncodedString(tokenParts[1])).asObject();
            final String jwtAudience = jsonData.get("aud").asString(); //$NON-NLS-1$
            if (!jwtAudience.equals(oauthAudience)) {
                throw new IOException("Invalid audience in token: " + jwtAudience); //$NON-NLS-1$
            }

            final String jwtIssuer = jsonData.get("iss").asString(); //$NON-NLS-1$
            if (!jwtIssuer.equals(oauthIssuer)) {
                throw new IOException("Invalid issuer in token: " + jwtIssuer); //$NON-NLS-1$
            }

            return bearerToken;
        } finally {
            c.disconnect();
        }
    }

    /**
     * Decodes a Base64 encoded String.
     */
    private static String decodeBase64EncodedString(final String input) {
        return new String(Base64.getUrlDecoder().decode(input), StandardCharsets.ISO_8859_1);
    }
}
