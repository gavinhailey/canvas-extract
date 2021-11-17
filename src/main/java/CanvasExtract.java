import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.Events;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.List;

public class CanvasExtract {
    private static final String APPLICATION_NAME = "Canvas Extract";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Collections.singletonList(CalendarScopes.CALENDAR);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object
     * @param HTTP_TRANSPORT The network HTTP Transport
     * @return An authorized Credential object
     * @throws IOException If the credentials.json file cannot be found
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = CanvasExtract.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Credential credential = getCredentials(HTTP_TRANSPORT);
        Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();

        com.google.api.services.calendar.model.Calendar canvas =
                service.calendars().get(System.getenv("CANVAS_CALENDAR")).execute();
        System.out.println(canvas.getDescription());

        com.google.api.services.calendar.model.Calendar assignments =
                service.calendars().get(System.getenv("ASSIGNMENTS_CALENDAR")).execute();
        System.out.println(assignments.getDescription());

        String pageToken = null;
        do {
            Events events = service.events().list(canvas.getId()).setPageToken(pageToken).execute();
            List<Event> items = events.getItems();
            for (Event event : items) {
                if ((event.getDescription() == null ||
                        !event.getDescription().contains("Zoom")) &&
                        !event.getSummary().contains("Ritchie School Information and Resources")) {
                    event.setOrganizer(new Event.Organizer());
                    System.out.println(event.getReminders());
                    try {
                        Event importedEvent = service.events().calendarImport(assignments.getId(), event).execute();
                        System.out.println("[" + java.time.LocalDateTime.now() + "] " + importedEvent.getSummary());
                    } catch (GoogleJsonResponseException e) {
                        System.err.println("[" + java.time.LocalDateTime.now() + "] " + e.getMessage());
                    }
                }
            }
            pageToken = events.getNextPageToken();
        } while (pageToken != null);
    }
}
