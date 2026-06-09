package com.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.third.pojo.entity.KnowledgeBase;
import com.third.pojo.vo.KnowledgeBaseVO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface KnowledgeBaseMapper extends BaseMapper<KnowledgeBase> {

}
