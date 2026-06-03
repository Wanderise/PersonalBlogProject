package com.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.third.pojo.dto.AIMessage;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MessageMapper extends BaseMapper<AIMessage> {

}
