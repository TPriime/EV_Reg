package com.prime.ev.register

class Factory{
    interface EventListener{
        fun onImageCaptured()
        fun onUserRegistered()
        fun onError()
    }
}
