package com.third.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.third.mapper.RagFileMapper;
import com.third.pojo.entity.RagFile;
import com.third.service.RagJobDispatcher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Slf4j
public class RagRecoveryRunner implements ApplicationRunner {

    private final RagFileMapper ragFileMapper;
    private final RagJobDispatcher ragJobDispatcher;

    public RagRecoveryRunner(RagFileMapper ragFileMapper, RagJobDispatcher ragJobDispatcher) {
        this.ragFileMapper = ragFileMapper;
        this.ragJobDispatcher = ragJobDispatcher;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<RagFile> pending = ragFileMapper.selectList(new LambdaQueryWrapper<RagFile>()
                .eq(RagFile::getStatus, "PROCESSING"));
        if (!pending.isEmpty()) {
            log.info("Recovering {} unfinished RAG ingestion jobs", pending.size());
            pending.forEach(file -> ragJobDispatcher.dispatch(file.getId()));
        }
    }
}
