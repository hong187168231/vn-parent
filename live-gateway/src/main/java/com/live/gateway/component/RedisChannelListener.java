package com.live.gateway.component;

import com.live.common.constant.GlobalConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;

import java.nio.charset.StandardCharsets;

public class RedisChannelListener implements MessageListener {

    @Autowired
    private UrlPermRolesLocalCache urlPermRolesLocalCache;

    @Override
    public void onMessage(Message message, byte[] bytes) {
        String msg = new String(message.getBody(), StandardCharsets.UTF_8);
        String channel = new String(message.getChannel(), StandardCharsets.UTF_8);
        urlPermRolesLocalCache.remove(GlobalConstants.URL_PERM_ROLES_KEY);

    }
}
