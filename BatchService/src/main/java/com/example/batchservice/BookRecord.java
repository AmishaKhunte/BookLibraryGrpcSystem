package com.example.batchservice;

import com.example.batchservice.entity.BookEntity;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.batch.item.jdbc.JdbcBatchItemWriter;
import org.springframework.batch.item.jdbc.builder.JdbcBatchItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableBatchProcessing
public class BatchJobConfig {

    @Bean
    public ItemReader<BookRecord> bookReader() {
        FlatFileItemReader<BookRecord> reader = new FlatFileItemReader<>();
        reader.setResource(new ClassPathResource("books.csv"));
        reader.setName("bookReader");
        reader.setLinesToSkip(1);

        DelimitedLineTokenizer tokenizer = new DelimitedLineTokenizer();
        tokenizer.setNames("id", "title", "author", "isbn", "available");

        BeanWrapperFieldSetMapper<BookRecord> mapper = new BeanWrapperFieldSetMapper<>();
        mapper.setTargetType(BookRecord.class);

        DefaultLineMapper<BookRecord> lineMapper = new DefaultLineMapper<>();
        lineMapper.setLineTokenizer(tokenizer);
        lineMapper.setFieldSetMapper(mapper);

        reader.setLineMapper(lineMapper);
        return reader;
    }

    @Bean
    public ItemProcessor<BookRecord, BookEntity> bookProcessor() {
        return item -> {
            BookEntity entity = new BookEntity();
            entity.setId(item.id());
            entity.setTitle(item.title());
            entity.setAuthor(item.author());
            entity.setIsbn(item.isbn());
            entity.setAvailable(Boolean.parseBoolean(item.available()));
            return entity;
        };
    }

    @Bean
    public JdbcBatchItemWriter<BookEntity> bookWriter(DataSource dataSource) {
        return new JdbcBatchItemWriterBuilder<BookEntity>()
                .dataSource(dataSource)
                .sql("INSERT INTO book_record (id, title, author, isbn, available) VALUES (:id, :title, :author, :isbn, :available)")
                .beanMapped()
                .build();
    }

    @Bean
    public Step bookImportStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<BookRecord> reader,
            ItemProcessor<BookRecord, BookEntity> processor,
            ItemWriter<BookEntity> writer) {
        return new StepBuilder("bookImportStep", jobRepository)
                .<BookRecord, BookEntity>chunk(10, transactionManager)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job bookImportJob(JobRepository jobRepository, Step bookImportStep) {
        return new JobBuilder("bookImportJob", jobRepository)
                .start(bookImportStep)
                .build();
    }
}
