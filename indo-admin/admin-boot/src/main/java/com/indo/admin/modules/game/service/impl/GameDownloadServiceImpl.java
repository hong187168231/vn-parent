package com.indo.admin.modules.game.service.impl;

import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.indo.admin.common.util.AdminBusinessRedisUtils;
import com.indo.admin.modules.game.mapper.*;
import com.indo.admin.modules.game.service.IGameDownloadService;
import com.indo.admin.pojo.criteria.GameDownloadQueryCriteria;
import com.indo.common.constant.RedisConstants;
import com.indo.common.redis.utils.RedisUtils;
import com.indo.common.utils.QueryHelpPlus;
import com.indo.core.pojo.entity.game.GameDownload;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GameDownloadServiceImpl extends ServiceImpl<GameDownloadMapper, GameDownload> implements IGameDownloadService {


    public List<GameDownload> queryAllGameDownload() {
        Map<Object, Object> map = RedisUtils.hmget(RedisConstants.GAME_DOWNLOAD_KEY);
        List<GameDownload> gameDownloads;
        if(ObjectUtil.isEmpty(map)){
            GameDownloadQueryCriteria criteria = new GameDownloadQueryCriteria();
            gameDownloads = baseMapper.selectList(QueryHelpPlus.getPredicate(GameDownload.class, criteria));
        }else {
            gameDownloads = new ArrayList(map.values());
        }
        gameDownloads.sort(Comparator.comparing(GameDownload::getCreateTime));
        return gameDownloads;
    }

    public boolean addGameDownload(GameDownload gameDownload) {
        if (this.baseMapper.insert(gameDownload) > 0) {
            AdminBusinessRedisUtils.hset(RedisConstants.GAME_DOWNLOAD_KEY, gameDownload.getId() + "", gameDownload);
            return true;
        }
        return false;
    }

    public boolean deleteBatchGameDownload(List<String> list) {
        if (this.baseMapper.deleteBatchIds(list) > 0) {
            list.forEach(id -> {
                AdminBusinessRedisUtils.hdel(RedisConstants.GAME_DOWNLOAD_KEY, id + "");
            });
            return true;
        }
        return false;
    }

    public boolean modifyGameDownload(GameDownload gameDownload) {
        if (this.baseMapper.updateById(gameDownload) > 0) {
            AdminBusinessRedisUtils.hset(RedisConstants.GAME_DOWNLOAD_KEY, gameDownload.getId() + "", gameDownload);
            return true;
        }
        return false;
    }


}
