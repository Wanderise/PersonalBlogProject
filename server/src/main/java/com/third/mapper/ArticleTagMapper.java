package com.third.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.third.pojo.entity.ArticleTag;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 文章标签关系表 Mapper 接口
 * </p>
 *
 * @author bsgm
 * @since 2026-04-15
 */
@Mapper
public interface ArticleTagMapper extends BaseMapper<ArticleTag> {

}
