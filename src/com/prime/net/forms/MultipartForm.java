package com.prime.net.forms;

import java.io.IOException;
import java.io.OutputStream;

public class MultipartForm extends Form{
    private String boundary;
    private StringBuilder sb;
    private OutputStream out;

    public MultipartForm(String boundary, OutputStream requestOutputStream){
        super("POST", String.format("multipart/form-data;boundary=%s",boundary));
        this.boundary = boundary;
        out = requestOutputStream;
        sb = new StringBuilder();
    }

    public void addInput(String name, String value) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append(String.format("Content-Disposition: form-data; name=\"%s\"\r\n\r\n", name));
        sb.append(value).append("\r\n");

        out.write(sb.toString().getBytes());
        this.sb.append(sb.toString());
    }

    public void addInputFile(String name, String fileName, byte[] bin) throws IOException{
        StringBuilder sb = new StringBuilder();
        sb.append("--").append(boundary).append("\r\n");
        sb.append(String.format("Content-Disposition: form-data; name=\"%s\"; filename=\"%s\"\r\n\r\n", name, fileName));

        out.write(sb.toString().getBytes());
        out.write(bin);

        sb.append(bin).append("\r\n");
        out.write("\r\n".getBytes());
        this.sb.append(sb.toString());
    }

    public String end() throws IOException{
        StringBuilder sb =new StringBuilder();
        sb.append("--").append(boundary).append("--");

        out.write(sb.toString().getBytes());
        out.close();

        this.sb.append(sb);
        return this.sb.toString();
    }
}
