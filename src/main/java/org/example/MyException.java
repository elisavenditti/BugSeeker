package org.example;

public class MyException extends Exception{
    private final String message;
    @Override
    public String getMessage(){
        return  this.message;
    }
    public MyException(String message){
        this.message = message;
    }
}
