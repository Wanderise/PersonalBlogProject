package com.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.third.pojo.entity.Tag;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 标签 Mapper 接口
 * </p>
 *
 * @author bsgm
 * @since 2026-04-15
 */
@Mapper
public interface TagMapper extends BaseMapper<Tag> {

}
