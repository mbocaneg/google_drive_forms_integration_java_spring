package com.example.googleapitest.googleutils;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Permission;
import com.google.api.services.forms.v1.Forms;
import com.google.api.services.forms.v1.model.*;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class GoogleService {

    private  Drive driveService;
    private  Forms formsService;

    private static final String PERMISSION_ROLE_OWNER = "owner";
    private static final String PERMISSION_ROLE_READER = "reader";
    private static final String PERMISSION_ROLE_WRITER = "writer";
    private static final String PERMISSION_TYPE_ANYONE = "anyone";
    private static final String PERMISSION_TYPE_DOMAIN = "domain";
    private static final String PERMISSION_TYPE_GROUP = "group";
    private static final String PERMISSION_TYPE_USER = "user";

    private GoogleCredentials credentials;

    static class FormResponseRecord {
        public String respondentId;
        public String questionText;
        public String answerText;
        FormResponseRecord(String respondentId, String questionText, String answerText) {
            this.respondentId = respondentId;
            this.questionText = questionText;
            this.answerText = answerText;
        }
    }
    class FormsHelper {

        private List<Request> requests;
        private int currentIndex;

        private Form form;
        private String formId;
        FormsHelper(){
            requests = new ArrayList<>();
            currentIndex = 0;
        }

        public FormsHelper createForm(
                String title,
                String description,
                Forms formsService,
                String accessToken) throws IOException{
            Form form = new Form();
            form.setInfo(new Info().setTitle(title));
            form = formsService.forms().create(form)
                    .setAccessToken(accessToken)
                    .execute();
            this.formId = form.getFormId();
            System.out.println("[Form Wrapper] created Form: " + this.formId);
            return this;
        }

        public FormsHelper addScaleQuestion(
                String questionText,
                int low,
                int high,
                String lowLabel,
                String highLabel) {
            CreateItemRequest createItemRequest = new CreateItemRequest();
            Item item = new Item().setTitle(questionText);

            Question question = new Question();
            ScaleQuestion scaleQuestion = new ScaleQuestion();
            scaleQuestion
                    .setLow(low)
                    .setLowLabel(lowLabel)
                    .setHigh(high)
                    .setHighLabel(highLabel);
            if (currentIndex == 0) {
                question.setScaleQuestion(scaleQuestion).setQuestionId("123abcd");
            } else {
                question.setScaleQuestion(scaleQuestion);
            }
            item.setQuestionItem(new QuestionItem().setQuestion(question));
            this.requests.add(new Request()
                    .setCreateItem(
                            new CreateItemRequest()
                                    .setLocation(new Location().setIndex(this.currentIndex))
                                    .setItem(item)));
            this.currentIndex += 1;
            return this;
        }

        public FormsHelper buildForm(Forms formsService, String accessToken) throws IOException{
            BatchUpdateFormRequest batchUpdateFormRequest = new BatchUpdateFormRequest();

            batchUpdateFormRequest.setRequests(this.requests);

            formsService.forms()
                    .batchUpdate(this.formId, batchUpdateFormRequest)
                    .setAccessToken(accessToken)
                    .execute();
            return this;
        }

        public static List<FormResponseRecord> getFormResponses(String formId, Forms formsService, String accessToken)
                throws IOException{
            List<FormResponseRecord> formQuestionAnswers = new ArrayList<>();
            ListFormResponsesResponse response = formsService
                    .forms()
                    .responses()
                    .list(formId)
                    .setAccessToken(accessToken)
                    .execute();

            List<FormResponse> formResponses = response.getResponses();
            HashMap<String, String> questionIdMap = new HashMap<>();
            Form form = formsService
                    .forms().get(formId).setAccessToken(accessToken).execute();
            List<Item> formItems = form.getItems();
            for (Item item: formItems) {
                String questionTitle = item.getTitle();
                String questionId = item.getQuestionItem().getQuestion().getQuestionId();
                questionIdMap.put(questionId, questionTitle);
            }

            int respondentId = 0;
            for (FormResponse formResponse: formResponses) {
                Map<String, Answer> answerMap = formResponse.getAnswers();
                System.out.println("Response Id: " + respondentId);

                for (Answer answer: answerMap.values()) {
                    String questionText = questionIdMap.get(answer.getQuestionId());
                    String answerText = answer.getTextAnswers().getAnswers().get(0).getValue();
                    System.out.println("Question: " + questionText);
                    System.out.println( "Answer: " + answerText);
                    FormResponseRecord temp = new FormResponseRecord(String.valueOf(respondentId), questionText, answerText);
                    formQuestionAnswers.add(temp);
                }
                System.out.println();
                respondentId += 1;
            }
            return formQuestionAnswers;
        }

    }

    @Autowired
    public GoogleService(Drive driveService,
                         Forms formsService,
                         GoogleCredentials credentials) {
        this.driveService = driveService;
        this.formsService = formsService;
        this.credentials = credentials;
    }

    public List<FormResponseRecord> listFormResponses(String formId) throws IOException {
        return FormsHelper.getFormResponses(formId, formsService, provideAccessToken());
    }

    public String createNewForm(String title, String description) throws IOException {
        FormsHelper helper = new FormsHelper()
                .createForm(
                    title,
                    description,
                    formsService,
                    provideAccessToken())
                .addScaleQuestion(
                        "Overall, how satisfied were you with the Event?",
                        1,
                        5,
                        "Extremely disatisfied",
                        "Extremely satisfied")
                .addScaleQuestion(
                        "How likely will you conduct business with us in the future?",
                        1,
                        5,
                        "Extremely unlikely",
                        "Extremely likely")
                .buildForm(formsService, provideAccessToken());
        String formId = helper.formId;

        System.out.println("Form Created: " + formId);
        return formId;
    }

    public String shareForm(String formId, String email) throws IOException {
        shareFile(
                formId,
                email,
                PERMISSION_TYPE_USER,
                PERMISSION_ROLE_WRITER,
                provideAccessToken());
        return formId;
    }

    // driveId MUST be a shared drive. Will not work for personal drives!
    public File moveToDrive(String fileId, String driveId, String token) {
        try {
            return driveService.files()
                    .update(fileId, null)
                    .setAddParents(driveId)
                    .setSupportsAllDrives(true)
                    .setOauthToken(token)
                    .execute();
        } catch (IOException e) {
            System.out.println("Cannot move file: " + fileId + " to drive: " + driveId);
            throw new RuntimeException(e);
        }
    }

    public Permission shareFile(String fileId, String email, String permissionType, String permissionRole, String token) {
        try {
            if (permissionRole.equals(PERMISSION_ROLE_OWNER)) {
                Permission permission = new Permission()
                        .setRole(PERMISSION_ROLE_WRITER)
                        .setType(permissionType)
                        .setEmailAddress(email)
                        .setPendingOwner(true);
                Permission permission1 = driveService
                        .permissions()
                        .create(fileId, permission)
                        .setSendNotificationEmail(true)
                        .setSupportsAllDrives(true)
                        .setOauthToken(token)
                        .execute();

                return driveService
                        .permissions()
                        .update(fileId, permission1.getId(), new Permission()
                                .setRole(PERMISSION_ROLE_OWNER)
                        )
                        .setTransferOwnership(true)
                        .setOauthToken(provideAccessToken())
                        .execute();
            }
            return driveService
                    .permissions()
                    .create(fileId,
                            new Permission().setRole(permissionRole).setType(permissionType).setEmailAddress(email))
                    .setSendNotificationEmail(true)
                    .setSupportsAllDrives(true)
                    .setOauthToken(token)
                    .execute();
        } catch (IOException e) {
            System.out.println("Cannot create permissions for user: " + email + " for filedId: " + fileId);
            throw new RuntimeException(e);
        }
    }

    public String provideAccessToken() throws IOException {
        String token = credentials.getAccessToken() != null ?
                credentials.getAccessToken().getTokenValue()
                :
                credentials.refreshAccessToken().getTokenValue();
        System.out.println("Access Token: " + token);
        return token;
    }

}
