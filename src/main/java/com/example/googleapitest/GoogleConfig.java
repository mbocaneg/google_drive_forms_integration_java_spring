package com.example.googleapitest;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.forms.v1.Forms;
import com.google.api.services.forms.v1.FormsScopes;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.Scanner;

@Configuration
public class GoogleConfig {

    public static final String PROJECT_NAME = "My First Project";

    @Bean
    @Qualifier("form_scopes")
    public Collection<String> provideFormScopes() {
        return FormsScopes.all();
    }

    @Bean
    public GoogleCredentials provideGoogleCredentials(@Qualifier("form_scopes") Collection<String> scopes) throws IOException {
        Resource resource = new ClassPathResource("creds.json");
        File file = resource.getFile();
        Scanner input = new Scanner(file);
        GoogleCredentials credential = GoogleCredentials.fromStream(resource.getInputStream())
                .createScoped(scopes);
        return credential;
    }

    @Bean
    public JsonFactory provideJsonFactory() {
        return GsonFactory.getDefaultInstance();
    }

    @Bean
    public Drive provideDriveService(JsonFactory jsonFactory) throws GeneralSecurityException, IOException {
       return new Drive.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                jsonFactory, null)
                .setApplicationName(PROJECT_NAME).build();
    }

    @Bean
    public Forms provideFormsService(JsonFactory jsonFactory) throws  GeneralSecurityException, IOException{
        return new Forms.Builder(GoogleNetHttpTransport.newTrustedTransport(),
                jsonFactory, null)
                .setApplicationName(PROJECT_NAME).build();
    }
}
