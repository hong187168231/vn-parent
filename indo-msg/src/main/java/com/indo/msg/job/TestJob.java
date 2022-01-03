//package com.indo.msg.job;
//
//import com.alibaba.fastjson.JSONObject;
//import com.indo.common.enums.BrokerMessageStatus;
//import com.indo.common.rabbitmq.bo.Message;
//import com.indo.msg.brokery.RabbitBroker;
//import com.indo.msg.pojo.entity.BrokeryMessage;
//import com.indo.msg.service.IMessageStoreService;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//
//@Component
//@Slf4j
//public class TestJob {
//
//    private static final Integer max_retry_count = 3;
//
//    @Autowired
//    private IMessageStoreService iMessageStoreService;
//
//    @Autowired
//    private RabbitBroker rabbitBroker;
//
//    private void TestJob() {
//        List<BrokeryMessage> datList = iMessageStoreService.queryTimeoutMessage4Retry(BrokerMessageStatus.SENDING);
//        datList.forEach(brokeryMessage -> {
//            if (brokeryMessage.getTryCount() >= max_retry_count) {
//                this.iMessageStoreService.failure(brokeryMessage.getMessageId());
//                log.warn("消失设置为失败，消息id: {}  ====== ", brokeryMessage.getMessageId());
//            } else {
//                iMessageStoreService.updateTryCount(brokeryMessage.getMessageId());
//                Message message = JSONObject.parseObject(brokeryMessage.getMessage(), Message.class);
//                rabbitBroker.reliantSend(message);
//            }
//        });
//    }
//
//
//}
