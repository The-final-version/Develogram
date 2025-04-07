package com.goorm.clonestagram.batch;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import com.goorm.clonestagram.post.EntityType;
import com.goorm.clonestagram.post.domain.SoftDelete;
import com.goorm.clonestagram.post.repository.PostsRepository;
import com.goorm.clonestagram.post.repository.SoftDeleteRepository;
import com.goorm.clonestagram.user.domain.service.UserInternalQueryService;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableBatchProcessing
@RequiredArgsConstructor
public class SoftDeleteBatchConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final SoftDeleteRepository softDeleteRepository;
    private final PostsRepository postsRepository;
    private final UserInternalQueryService userQueryService;

    @Bean
    public Job cleanUpJob() {
        return new JobBuilder("cleanUpJob", jobRepository)
            .start(cleanUpStep())
            .build();
    }

    @Bean
    public Step cleanUpStep() {
        return new StepBuilder("cleanUpStep", jobRepository)
            .tasklet((contribution, chunkContext) -> {
                LocalDateTime now = LocalDateTime.now();
                LocalDateTime threshold = now.minusMonths(1);

                // 한 달 지난 소프트 삭제 엔티티 조회
                List<SoftDelete> softDeletes = softDeleteRepository.findByDeletedAtBefore(threshold);

                for (SoftDelete softDelete : softDeletes) {
                    if (softDelete.getEntityType() == EntityType.POST) {
                        postsRepository.deleteById(softDelete.getEntityId());
                    } else if (softDelete.getEntityType() == EntityType.USER) {
                        userQueryService.deleteUserId(softDelete.getEntityId());
                    }
                    softDeleteRepository.delete(softDelete);
                }

                return RepeatStatus.FINISHED;
            }, transactionManager)
            .build();
    }
}
