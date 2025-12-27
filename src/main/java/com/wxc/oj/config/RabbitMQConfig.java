package com.wxc.oj.config;

import com.wxc.oj.constant.RabbitConstant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Slf4j(topic = "RabbitMQConfig")
public class RabbitMQConfig {



    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        

        // 开启 mandatory，配合 ReturnCallback
        rabbitTemplate.setMandatory(true);

        // Confirm 回调（生产者确认）
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                // 1. 记录日志
                log.error("消息发送失败, correlationData={}, cause={}",
                        correlationData, cause);

                // 2. 可选：重试 / 落库 / 告警
            }
        });

        // Return 回调（路由失败）
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("消息路由失败: exchange={}, routingKey={}, msg={}",
                    returned.getExchange(),
                    returned.getRoutingKey(),
                    returned.getMessage());
        });

        return rabbitTemplate;
    }

// =================================================统计提交信息：1个交换机，2个队列，使用同一个rk=================================================================
    /**
     * topic交换机，用于转发submission信息。
     * 消费者：
     * 1. 修改题目信息的消费者
     * 2. 修改用户AC数的消费者
     * auto-delete: true表示当没有队列与之绑定时，自动删除该交换机
     * @return
     */
    @Bean
    public TopicExchange submissionStatusExchange() {
//        return ExchangeBuilder
//                .topicExchange(RabbitConstant.SUBMISSION_STATUS_EXCHANGE)
//                .durable(true)
//                .build();
        return new TopicExchange(RabbitConstant.SUBMISSION_STATUS_EXCHANGE, true, false);
    }

    @Bean
    public Queue acceptedRankQueue() {
        return QueueBuilder
            .durable(RabbitConstant.SUBMISSION_STATUS_AC_QUEUE).build();
    }

    @Bean
    public Queue submissionProblemQueue() {
        return QueueBuilder.durable(RabbitConstant.SUBMISSION_STATUS_PROBLEM_QUEUE).build();
    }


    @Bean
    public Binding rankBinding() {
        return BindingBuilder
                .bind(acceptedRankQueue())
                .to(submissionStatusExchange())
                .with(RabbitConstant.SUBMISSION_STATUS_TOPIC);
    }

    @Bean
    public Binding problemBinding() {
        return BindingBuilder
                .bind(submissionProblemQueue())
                .to(submissionStatusExchange())
                .with(RabbitConstant.SUBMISSION_STATUS_TOPIC);
    }
    // =================================================submission=================================================================
    /**
     * 创建direct交换机，用于转发submissionID。
     */
    @Bean
    public Exchange submissionExchange(){
        return ExchangeBuilder
                .directExchange(RabbitConstant.SUBMISSION_EXCHANGE)
                .build();
    }

    /**
     * 创建消息队列，用于存储submissionID。
     * @return
     */
    @Bean     //定义消息队列
    public Queue submissionQueue(){
        return QueueBuilder
                .durable(RabbitConstant.SUBMISSION_QUEUE)   //非持久化类型
                .build();
    }

    /**
     * 绑定directExchange和submission队列，并指定routingKey为submission
     * @param exchange
     * @param queue
     * @return
     */
    @Bean("binding")
    public Binding binding(@Qualifier("submissionExchange") Exchange exchange,
                           @Qualifier("submissionQueue") Queue queue) {
      	//将我们刚刚定义的交换机和队列进行绑定
        return BindingBuilder
                .bind(queue)   //绑定队列
                .to(exchange)  //到交换机
                .with(RabbitConstant.SUBMISSION_ROUTING_KEY)   //使用自定义的routingKey
                .noargs();
    }
// ===============================================比赛期间的提交===================================================================


    /**
     * 创建direct交换机，用于转发submissionID。
     */
    @Bean
    public Exchange contestSubmissionExchange(){
        return ExchangeBuilder.directExchange("contest_exchange").build();
    }

    /**
     * 创建消息队列，用于存储submissionID。
     * @return
     */
    @Bean    //定义消息队列
    public Queue contestSubmissionQueue(){
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
    public Binding binding_contest(@Qualifier("contestSubmissionExchange") Exchange exchange,
                           @Qualifier("contestSubmissionQueue") Queue queue) {
        //将我们刚刚定义的交换机和队列进行绑定
        return BindingBuilder
                .bind(queue)   //绑定队列
                .to(exchange)  //到交换机
                .with("submission_routing_key")   //使用自定义的routingKey
                .noargs();
    }

// =============================================1个延迟交换机 2个延迟队列==================================================


    /**
     * 创建延迟交换机
     */
    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-delayed-type", "direct");
        return new CustomExchange(RabbitConstant.CONTEST_TIME_EXCHANGE,
                "x-delayed-message", true, false, args);
    }

    /**
     * 发生比赛开始消息
     * 这里的消息会在比赛开始的时间被消费者消费
     */
    @Bean
    public Queue delayQueue1() {
        return QueueBuilder.durable(RabbitConstant.CONTEST_PUBLISH_QUEUE).build();
    }

    /**
     * 发生比赛结束消息
     * 这里的消息会在比赛结束时被消费者消费
     * @return
     */
    @Bean
    public Queue delayQueue2() {
        return QueueBuilder.durable(RabbitConstant.CONTEST_FINISH_QUEUE).build();
    }


    /**
     * 绑定交换机和队列
     */
    @Bean
    public Binding delayBinding1() {
        return BindingBuilder
                .bind(delayQueue1())
                .to(delayExchange())
                .with(RabbitConstant.CONTEST_PUBLISH_KEY).noargs();
    }
    @Bean
    public Binding delayBinding2() {
        return BindingBuilder
                .bind(delayQueue2())
                .to(delayExchange())
                .with(RabbitConstant.CONTEST_FINISH_KEY).noargs();
    }
// ====================================================================================================================
    /**
     * 创建一个用于JSON转换的Bean
     * 用于将对象转换为JSON格式的消息
     * 和将JSON格式的消息转换为对象
     * @return
     */
    @Bean("jacksonConverter")
    public Jackson2JsonMessageConverter converter(){
        return new Jackson2JsonMessageConverter();
    }
}