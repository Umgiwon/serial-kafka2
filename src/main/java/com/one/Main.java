package com.one;

import com.one.config.AppConfig;
import com.one.modbus.ModbusRtuClient;
import com.one.kafka.ModbusKafkaProducer;
import com.one.service.DataCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Modbus RTU 장치에서 데이터를 읽고 Kafka로 전송한다.
 */
public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        logger.info("Modbus RTU에서 Kafka로 애플리케이션 시작 중");

        try {
            // 설정 로드
            AppConfig config = AppConfig.getInstance();
            
            // Modbus RTU 클라이언트 초기화
            ModbusRtuClient modbusClient = new ModbusRtuClient(
                config.getProperty("modbus.port"),
                config.getIntProperty("modbus.baudRate"),
                config.getIntProperty("modbus.dataBits"),
                config.getIntProperty("modbus.stopBits"),
                config.getIntProperty("modbus.parity"),
                config.getIntProperty("modbus.startAddress"),
                config.getIntProperty("modbus.quantity")
            );

            // Kafka 프로듀서 초기화
            ModbusKafkaProducer kafkaProducer = new ModbusKafkaProducer(
                config.getProperty("kafka.bootstrapServers"),
                config.getProperty("kafka.topic")
            );

            // 데이터 수집 서비스 초기화 및 시작
            DataCollectionService dataCollectionService = new DataCollectionService(
                modbusClient,
                kafkaProducer,
                config.getIntArrayProperty("modbus.slaveIds"),
                config.getIntProperty("app.pollingIntervalSeconds"),
                config.getIntProperty("app.threadPoolSize")
            );
            
            dataCollectionService.start();
            
            logger.info("애플리케이션이 시작되었습니다. 종료하려면 Ctrl+C를 누르세요.");
            
        } catch (Exception e) {
            logger.error("애플리케이션 초기화 중 오류 발생", e);
        }
    }
}