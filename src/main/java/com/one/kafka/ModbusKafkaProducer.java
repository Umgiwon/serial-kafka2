package com.one.kafka;

import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.Arrays;

/**
 * Modbus RTU 데이터를 Kafka 토픽으로 전송하기 위한 Kafka 프로듀서
 */
public class ModbusKafkaProducer {
    private static final Logger logger = LoggerFactory.getLogger(ModbusKafkaProducer.class);
    
    private final Producer<String, String> producer;
    private final String topic;
    
    /**
     * ModbusKafkaProducer 생성자
     * 
     * @param bootstrapServers Kafka 부트스트랩 서버 (예: localhost:9092)
     * @param topic 데이터를 전송할 Kafka 토픽
     */
    public ModbusKafkaProducer(String bootstrapServers, String topic) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        
        this.producer = new KafkaProducer<>(props);
        this.topic = topic;
        
        logger.info("ModbusKafkaProducer 초기화 완료 - 부트스트랩 서버: {}, 토픽: {}", 
                   bootstrapServers, topic);
    }
    
    /**
     * 홀딩 레지스터 데이터를 Kafka로 전송
     * 
     * @param registers 레지스터 값 배열
     * @param timestamp 데이터 타임스탬프
     * @throws ExecutionException 데이터 전송 중 오류 발생 시
     * @throws InterruptedException 스레드가 중단된 경우
     */
    public void sendHoldingRegisters(int[] registers, long timestamp) 
            throws ExecutionException, InterruptedException {
        String key = "holding_registers_" + timestamp;
        String value = Arrays.toString(registers);
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record).get();
        
        logger.info("홀딩 레지스터를 Kafka로 전송 완료: {}", value);
    }
    
    /**
     * 입력 레지스터 데이터를 Kafka로 전송
     * 
     * @param registers 레지스터 값 배열
     * @param timestamp 데이터 타임스탬프
     * @throws ExecutionException 데이터 전송 중 오류 발생 시
     * @throws InterruptedException 스레드가 중단된 경우
     */
    public void sendInputRegisters(int[] registers, long timestamp) 
            throws ExecutionException, InterruptedException {
        String key = "input_registers_" + timestamp;
        String value = Arrays.toString(registers);
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record).get();
        
        logger.info("입력 레지스터를 Kafka로 전송 완료: {}", value);
    }
    
    /**
     * 코일 데이터를 Kafka로 전송
     * 
     * @param coils 코일 값 배열
     * @param timestamp 데이터 타임스탬프
     * @throws ExecutionException 데이터 전송 중 오류 발생 시
     * @throws InterruptedException 스레드가 중단된 경우
     */
    public void sendCoils(boolean[] coils, long timestamp) 
            throws ExecutionException, InterruptedException {
        String key = "coils_" + timestamp;
        String value = Arrays.toString(coils);
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record).get();
        
        logger.info("코일을 Kafka로 전송 완료: {}", value);
    }
    
    /**
     * 이산 입력 데이터를 Kafka로 전송
     * 
     * @param discreteInputs 이산 입력 값 배열
     * @param timestamp 데이터 타임스탬프
     * @throws ExecutionException 데이터 전송 중 오류 발생 시
     * @throws InterruptedException 스레드가 중단된 경우
     */
    public void sendDiscreteInputs(boolean[] discreteInputs, long timestamp) 
            throws ExecutionException, InterruptedException {
        String key = "discrete_inputs_" + timestamp;
        String value = Arrays.toString(discreteInputs);
        
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, value);
        producer.send(record).get();
        
        logger.info("이산 입력을 Kafka로 전송 완료: {}", value);
    }
    
    /**
     * Kafka 프로듀서 종료
     */
    public void close() {
        producer.close();
        logger.info("ModbusKafkaProducer 종료됨");
    }
}