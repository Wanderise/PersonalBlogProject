package com.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.third.pojo.entity.Agent;
import com.third.pojo.entity.Conversations;
import com.third.pojo.vo.AgentVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversations> {


    @Insert("insert into blog.ai_conversation(user_id, title, agent_id, gmt_create, gmt_modified) " +
            "VALUES(#{userId}, #{title}, #{agentId}, #{gmtCreate}, #{gmtModified}) ")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void addConversation(Conversations conversations);

}
