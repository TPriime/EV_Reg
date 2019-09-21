package com.prime.net.forms;

public abstract class Form {
    String method;
    String contentType;

    public Form(String _method, String _contentType){
        method = _method;
        contentType = _contentType;
    }

    public String getMethod(){return method;}
    public String getContentType(){return contentType;}
}
