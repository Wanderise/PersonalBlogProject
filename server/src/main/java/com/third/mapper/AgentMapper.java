package com.third.mapper;

import com.third.pojo.entity.Agent;
import com.third.pojo.vo.AgentVO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AgentMapper {
    @Insert("insert into blog.ai_agent(user_id, name, system_prompt, icon, gmt_create) " +
            "VALUES (#{userId}, #{name}, #{systemPrompt}, #{icon}, #{gmtCreate})")
    void addAgent(Agent agent);

    @Select("select id, name, system_prompt, icon, gmt_create from blog.ai_agent where user_id = #{userId}")
    List<AgentVO> getAgents(Integer userId);

    @Delete("delete from blog.ai_agent where id = #{id}")
    void deleteAgentById(Integer id);

    @Select("select count(*) from blog.ai_agent where id = #{id} and user_id = #{userId}")
    Long countOwnership(@Param("id") Integer id, @Param("userId") Integer userId);

}
