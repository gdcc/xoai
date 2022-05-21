package io.gdcc.xoai.dataprovider.request;

import io.gdcc.xoai.model.oaipmh.Request;

public class OAIRequest extends Request {
    public OAIRequest(String baseUrl) {
        super(baseUrl);
    }
    
    // TODO: add resumption token handling here
    
}
