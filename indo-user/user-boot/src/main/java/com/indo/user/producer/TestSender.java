//package com.indo.user.producer;
//
//import org.springframework.amqp.core.AmqpTemplate;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.util.Date;
//
//@Component
//public class TestSender {
//
//    @Autowired
//    private AmqpTemplate rabbitTemplate;
//
//    public void send(){
//        String context = "hello " + new Date();
//        System.out.println("Sender : "+ context);
//        this.rabbitTemplate.convertAndSend("hello", context);
//    }
//
//}
