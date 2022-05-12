package org.example;

public class MyException extends Exception{
    private String message;

    public String getMessage(){
        return  this.message;
    }
    public MyException(String message){
        this.message = message;
    }
}
