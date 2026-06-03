package com.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.third.pojo.entity.Agent;
import com.third.pojo.entity.Conversations;
import com.third.pojo.vo.AgentVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ConversationMapper extends BaseMapper<Conversations> {


    @Insert("insert into blog.ai_conversation(id, user_id, title, agent_id, gmt_create, gmt_modified) " +
            "VALUES(#{id}, #{userId}, #{title}, #{agentId}, #{gmtCreate}, #{gmtModified}) ")
    void addConversation(Conversations conversations);

}
