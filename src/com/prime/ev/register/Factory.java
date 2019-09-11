package com.prime.ev.register;

public class Factory{

    public interface EventListener{
        void onImageCaptured();
        void onUserRegistered();
        void onError();
    }
}
