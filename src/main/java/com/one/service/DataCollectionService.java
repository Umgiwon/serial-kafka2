package com.one.service;

import com.one.kafka.ModbusKafkaProducer;
import com.one.modbus.ModbusRtuClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

/**
 * Modbus 데이터 수집 및 Kafka 전송을 처리하는 서비스
 */
public class DataCollectionService {
    private static final Logger logger = LoggerFactory.getLogger(DataCollectionService.class);
    
    private final ModbusRtuClient modbusClient;
    private final ModbusKafkaProducer kafkaProducer;
    private final int[] slaveIds;
    private final int pollingInterval;
    private final int threadPoolSize;
    
    private ExecutorService dataCollectionExecutor;
    private ScheduledExecutorService scheduler;
    
    /**
     * DataCollectionService 생성자
     * 
     * @param modbusClient Modbus RTU 클라이언트
     * @param kafkaProducer Kafka 프로듀서
     * @param slaveIds 슬레이브 ID 배열
     * @param pollingInterval 폴링 간격(초)
     * @param threadPoolSize 스레드 풀 크기
     */
    public DataCollectionService(ModbusRtuClient modbusClient, ModbusKafkaProducer kafkaProducer,
                                int[] slaveIds, int pollingInterval, int threadPoolSize) {
        this.modbusClient = modbusClient;
        this.kafkaProducer = kafkaProducer;
        this.slaveIds = slaveIds;
        this.pollingInterval = pollingInterval;
        this.threadPoolSize = threadPoolSize;
    }
    
    /**
     * 데이터 수집 서비스 시작
     * 
     * @throws Exception 서비스 시작 중 오류 발생 시
     */
    public void start() throws Exception {
        // Modbus 연결
        modbusClient.connect();
        
        // 병렬 처리를 위한 스레드 풀 생성
        dataCollectionExecutor = Executors.newFixedThreadPool(threadPoolSize);
        
        // 주기적으로 데이터를 폴링하기 위한 스케줄러 생성
        scheduler = Executors.newSingleThreadScheduledExecutor();
        
        // 폴링 작업 스케줄링
        scheduler.scheduleAtFixedRate(this::pollData, 0, pollingInterval, TimeUnit.SECONDS);
        
        // 종료 훅 등록
        registerShutdownHook();
        
        logger.info("데이터 수집 서비스가 시작되었습니다.");
    }
    
    /**
     * Modbus 데이터 폴링 및 Kafka 전송
     */
    private void pollData() {
        try {
            long timestamp = System.currentTimeMillis();
            logger.info("Modbus 데이터 폴링 시작 (타임스탬프: {})", timestamp);
            
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            
            for (int slaveId : slaveIds) {
                CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                    collectAndSendData(slaveId, timestamp);
                }, dataCollectionExecutor);
                
                futures.add(future);
            }
            
            // 모든 작업이 완료될 때까지 대기
            try {
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(pollingInterval - 1, TimeUnit.SECONDS);
                logger.info("모든 슬레이브 데이터 수집 완료");
            } catch (TimeoutException e) {
                logger.warn("일부 슬레이브 데이터 수집이 제한 시간 내에 완료되지 않았습니다");
            } catch (Exception e) {
                logger.error("슬레이브 데이터 수집 중 오류 발생", e);
            }
        } catch (Exception e) {
            logger.error("Modbus 데이터 폴링 중 오류 발생", e);
        }
    }
    
    /**
     * 특정 슬레이브의 데이터 수집 및 전송
     * 
     * @param slaveId 슬레이브 ID
     * @param timestamp 타임스탬프
     */
    private void collectAndSendData(int slaveId, long timestamp) {
        try {
            logger.info("슬레이브 {}: Modbus 데이터 폴링 중...", slaveId);
            
            // 데이터 수집
            int[] holdingRegisters = modbusClient.readHoldingRegisters(slaveId);
            int[] inputRegisters = modbusClient.readInputRegisters(slaveId);
            boolean[] coils = modbusClient.readCoils(slaveId);
            boolean[] discreteInputs = modbusClient.readDiscreteInputs(slaveId);
            
            // 데이터 전송
            kafkaProducer.sendHoldingRegisters(slaveId, holdingRegisters, timestamp);
            kafkaProducer.sendInputRegisters(slaveId, inputRegisters, timestamp);
            kafkaProducer.sendCoils(slaveId, coils, timestamp);
            kafkaProducer.sendDiscreteInputs(slaveId, discreteInputs, timestamp);
            
            logger.info("슬레이브 {}: 데이터 수집 및 전송 완료", slaveId);
        } catch (Exception e) {
            logger.error("슬레이브 {}: Modbus 데이터 폴링 중 오류 발생", slaveId, e);
        }
    }
    
    /**
     * 종료 훅 등록
     */
    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                logger.info("데이터 수집 서비스 종료 중...");
                shutdown();
                logger.info("데이터 수집 서비스가 정상적으로 종료되었습니다");
            } catch (Exception e) {
                logger.error("서비스 종료 중 오류 발생", e);
            }
        }));
    }
    
    /**
     * 데이터 수집 서비스 종료
     * 
     * @throws Exception 서비스 종료 중 오류 발생 시
     */
    public void shutdown() throws Exception {
        logger.info("리소스 정리 중...");
        
        if (scheduler != null) {
            scheduler.shutdown();
        }
        
        if (dataCollectionExecutor != null) {
            dataCollectionExecutor.shutdown();
            if (!dataCollectionExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                dataCollectionExecutor.shutdownNow();
            }
        }
        
        modbusClient.disconnect();
        kafkaProducer.close();
    }
}