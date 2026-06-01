package com.third.service.impl;

import com.third.pojo.entity.ArticleTag;
import com.third.mapper.ArticleTagMapper;
import com.third.service.ArticleTagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 文章标签关系表 服务实现类
 * </p>
 *
 * @author bsgm
 * @since 2026-04-15
 */
@Service
public class ArticleTagServiceImpl extends ServiceImpl<ArticleTagMapper, ArticleTag> implements ArticleTagService {

}
