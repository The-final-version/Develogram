package com.goorm.clonestagram.batch;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class BatchScheduler {
	private final JobLauncher jobLauncher;
	private final Job cleanUpJob;

	@Scheduled(cron = "0 0 0 * * *")  // 매일 자정
	public void runCleanupJob() throws Exception {
		jobLauncher.run(cleanUpJob, new JobParametersBuilder()
			.addLong("time", System.currentTimeMillis())
			.toJobParameters());
	}
}

