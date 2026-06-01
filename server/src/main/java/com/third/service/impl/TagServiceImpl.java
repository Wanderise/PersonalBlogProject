package com.third.service.impl;

import com.third.pojo.entity.Tag;
import com.third.mapper.TagMapper;
import com.third.service.TagService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 * 标签 服务实现类
 * </p>
 *
 * @author bsgm
 * @since 2026-04-15
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag> implements TagService {

}
