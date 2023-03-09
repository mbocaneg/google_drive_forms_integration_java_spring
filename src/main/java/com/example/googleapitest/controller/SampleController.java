package com.example.googleapitest.controller;

import com.example.googleapitest.googleutils.GoogleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/")
public class SampleController {

    private GoogleService googleService;

    @Autowired
    public SampleController(GoogleService googleService) {
        this.googleService = googleService;
    }
    @GetMapping(value = "create_form/{title}/{description}")
    public Object createForm(@PathVariable String title, @PathVariable String description) throws IOException {
        String formId = googleService
                .createNewForm(
                        title.equals("")?"My First Form":title,
                        description.equals("")?"My First Form Description":description);
        String formUrl = "https://docs.google.com/forms/d/" + formId;
        return "Form Created: " + formUrl;
    }

    @GetMapping(value = "share_form/{formId}/{email}")
    public Object shareForm(@PathVariable String formId, @PathVariable String email) throws IOException {
        return googleService.shareForm(formId, email);
    }

    // NOTE: This method will return null IF a form has not yet received any responses
    // ENSURE that a form has responses prior to calling this method
    @GetMapping(value = "form_responses/{formId}")
    public Object formResponses(@PathVariable String formId) throws IOException {
        return googleService.listFormResponses(formId);
    }
}
