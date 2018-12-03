package com.ge.predix.audit.examples.controller;

import com.ge.predix.audit.sdk.exception.AuditException;
import com.ge.predix.audit.examples.service.PublisherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by Martin Saad on 2/9/2017.
 */
@RestController
public class PublisherController {

    @Autowired
    private PublisherService publisherService;

    @ResponseBody
    @RequestMapping(value = "/publishAsync", method = RequestMethod.GET)
    public ResponseEntity publishAsync() throws AuditException {
        return ResponseEntity.status(HttpStatus.OK).body(publisherService.publishAsync());
    }

    @ResponseBody
    @RequestMapping(value = "/getLastResponse", method = RequestMethod.GET)
    public ResponseEntity getLastResponse() {
        return ResponseEntity.status(HttpStatus.OK).body(publisherService.getLastResponse());
    }
}
