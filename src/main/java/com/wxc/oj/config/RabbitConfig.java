package com.wxc.oj.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j(topic = "RabbitConfig💕💕💕💕")
public class RabbitConfig {



    /**
     * 创建direct交换机，用于转发submissionID。
     */
    @Bean("directExchange")
    public Exchange exchange(){
        return ExchangeBuilder.directExchange("amq.direct").build();
    }

    /**
     * 创建消息队列，用于存储submissionID。
     * @return
     */
    @Bean("submission")     //定义消息队列
    public Queue queue(){
        return QueueBuilder
                    .durable("submission")   //非持久化类型
                    .build();
    }

    /**
     * 绑定directExchange和submission队列，并指定routingKey为submission
     * @param exchange
     * @param queue
     * @return
     */
    @Bean("binding")
    public Binding binding(@Qualifier("directExchange") Exchange exchange,
                           @Qualifier("submission") Queue queue) {
      	//将我们刚刚定义的交换机和队列进行绑定
        return BindingBuilder
                .bind(queue)   //绑定队列
                .to(exchange)  //到交换机
                .with("submission")   //使用自定义的routingKey
                .noargs();
    }


    /**
     * 创建direct交换机，用于转发submissionID。
     */
    @Bean
    public Exchange contest_exchange(){
        return ExchangeBuilder.directExchange("contest_exchange").build();
    }

    /**
     * 创建消息队列，用于存储submissionID。
     * @return
     */
    @Bean("contest_submission_queue")     //定义消息队列
    public Queue contest_submission_queue(){
        return QueueBuilder
                .durable("contest_submission_queue")   //非持久化类型
                .build();
    }

    /**
     * 绑定directExchange和submission队列，并指定routingKey为submission
     * @param exchange
     * @param queue
     * @return
     */
    @Bean
    public Binding binding_contest(@Qualifier("contest_exchange") Exchange exchange,
                           @Qualifier("contest_submission_queue") Queue queue) {
        //将我们刚刚定义的交换机和队列进行绑定
        return BindingBuilder
                .bind(queue)   //绑定队列
                .to(exchange)  //到交换机
                .with("submission_routing_key")   //使用自定义的routingKey
                .noargs();
    }

    @Bean
    public Exchange submission_status_exchange(){
        return ExchangeBuilder.directExchange("submission_status_exchange").build();
    }

    /**
     * 创建消息队列，用于存储submissionID。
     * @return
     */
    @Bean("submission_status_queue")     //定义消息队列
    public Queue problem_queue(){
        return QueueBuilder
                .durable("submission_status_queue")   //非持久化类型
                .build();
    }

    /**
     * 绑定directExchange和submission队列，并指定routingKey为submission
     * @param exchange
     * @param queue
     * @return
     */
    @Bean
    public Binding binding_problem(@Qualifier("submission_status_exchange") Exchange exchange,
                                   @Qualifier("submission_status_queue") Queue queue) {
        //将我们刚刚定义的交换机和队列进行绑定
        return BindingBuilder
                .bind(queue)   //绑定队列
                .to(exchange)  //到交换机
                .with("submission_status_key")   //使用自定义的routingKey
                .noargs();
    }

    /**
     * 创建延迟交换机
     */
    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange("delayExchange", "x-delayed-message", true, false, args);
    }

    /**
     * 创建2个延迟队列
     */
    @Bean("timePublish")
    public Queue delayQueue1() {
        return QueueBuilder.durable("timePublish").build();
    }

    @Bean("timeFinish")
    public Queue delayQueue2() {
        return QueueBuilder.durable("timeFinish").build();
    }


    /**
     * 绑定交换机和队列
     */
    @Bean
    public Binding delayBinding1() {
        return BindingBuilder
                .bind(delayQueue1()).to(delayExchange()).with("timePublish").noargs();
    }
    @Bean
    public Binding delayBinding2() {
        return BindingBuilder
                .bind(delayQueue2()).to(delayExchange()).with("timeFinish").noargs();
    }
    /**
     * 创建一个用于JSON转换的Bean
     * @return
     */
    @Bean("jacksonConverter")
    public Jackson2JsonMessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }
}