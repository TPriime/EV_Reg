package com.prime.net.forms;

public class MultipartForm extends Form{
    private String boundary;
    private StringBuilder sb;

    public MultipartForm(String boundary){
        super("POST", String.format("multipart/form-data;boundary=%s",boundary));
        this.boundary = boundary;
        sb = new StringBuilder();
    }

    public void addInput(String name, String value){
        sb.append("--").append(boundary).append("\r\n");
        sb.append(String.format("Content-Disposition: form-data; name=\"%s\"\r\n\r\n", name));
        sb.append(value).append("\r\n");
    }

    public void addInputFile(String name, String fileName, byte[] bin){
        sb.append("--").append(boundary).append("\r\n");
        sb.append(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n\r\n", name, fileName));
        sb.append(new String(bin)).append("\r\n");
    }

    public String end(){
        sb.append("--").append(boundary).append("--");
        return sb.toString();
    }
}
