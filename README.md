# Modbus RTU와 Kafka 통합

이 애플리케이션은 Modbus RTU 장치에서 데이터를 읽고 Kafka로 전송합니다. KOS-500 프로토콜 참조 가이드에 명시된 KOS-500 프로토콜과 함께 작동하도록 설계되었습니다.

## 기능

- 시리얼 포트를 통해 Modbus RTU 장치에 연결
- 홀딩 레지스터, 입력 레지스터, 코일 및 이산 입력 읽기
- 데이터를 JSON 형식으로 Kafka 토픽에 전송
- 외부 설정 파일을 통한 쉬운 구성
- 다중 슬레이브 장치 지원 (최대 10개의 컴프레셔)
- 병렬 데이터 수집으로 성능 향상
- 연결 실패 시 자동 재시도 로직
- 모듈화된 코드 구조로 유지보수 용이
- 적절한 오류 처리 및 리소스 정리

## 요구 사항

- Java 8 이상
- Kafka 서버
- Modbus RTU 통신을 위한 시리얼 포트

## 의존성

- j2mod: Modbus RTU 라이브러리
- jssc: 시리얼 통신 라이브러리
- Kafka 클라이언트: Kafka로 데이터 전송
- Jackson: JSON 데이터 처리
- SLF4J: 로깅

## 프로젝트 구조

프로젝트는 다음과 같은 패키지 구조로 구성되어 있습니다:

```
com.one
├── Main.java (애플리케이션 진입점)
├── config
│   └── AppConfig.java (설정 관리)
├── model
│   ├── ModbusDataType.java (데이터 타입 열거형)
│   └── ModbusData.java (데이터 모델)
├── service
│   └── DataCollectionService.java (데이터 수집 서비스)
├── modbus
│   └── ModbusRtuClient.java (Modbus RTU 통신)
└── kafka
    ├── ModbusKafkaProducer.java (Kafka 메시지 생성)
    └── KafkaMessageFormatter.java (JSON 포맷팅)
```

## 구성

애플리케이션은 `src/main/resources/application.properties` 파일을 수정하여 구성할 수 있습니다:

### 설정 파일 구성

```properties
# Modbus RTU 설정
modbus.port=COM3                   # 시리얼 포트 이름
modbus.baudRate=9600               # 전송 속도
modbus.dataBits=8                  # 데이터 비트
modbus.stopBits=1                  # 정지 비트
modbus.parity=0                    # 패리티 (0=없음, 1=홀수, 2=짝수)
modbus.startAddress=0              # 레지스터 시작 주소
modbus.quantity=10                 # 읽을 레지스터 수량
modbus.slaveIds=1,2,3,4,5,6,7,8,9,10  # 슬레이브 ID 목록
modbus.retryAttempts=3             # 재시도 횟수
modbus.retryDelayMs=1000           # 재시도 간격(밀리초)

# Kafka 설정
kafka.bootstrapServers=192.168.219.51:9092  # Kafka 서버 주소
kafka.topic=modbus-data            # Kafka 토픽

# 애플리케이션 설정
app.pollingIntervalSeconds=10      # 폴링 간격(초)
app.threadPoolSize=5               # 병렬 처리 스레드 풀 크기
```

## 사용법

1. `src/main/resources/application.properties` 파일을 환경에 맞게 수정하여 애플리케이션 구성
2. Gradle을 사용하여 애플리케이션 빌드:
   ```
   ./gradlew build
   ```
3. 애플리케이션 실행:
   ```
   java -jar build/libs/serial-kafka2-1.0-SNAPSHOT.jar
   ```

## 데이터 형식

애플리케이션은 다음 데이터를 JSON 형식으로 Kafka에 전송합니다:

```json
{
  "slaveId": 1,
  "timestamp": 1634567890123,
  "dataType": "holding_registers",
  "values": [1, 2, 3, 4, 5, 6, 7, 8, 9, 10]
}
```

각 JSON 메시지에는 다음 필드가 포함됩니다:
- `slaveId`: 데이터를 제공한 슬레이브 장치의 ID
- `timestamp`: 데이터가 수집된 시간(밀리초 단위의 유닉스 타임스탬프)
- `dataType`: 데이터 유형 (holding_registers, input_registers, coils, discrete_inputs)
- `values`: 수집된 데이터 값의 배열

Kafka 메시지 키는 `slave_{slaveId}_{dataType}_{timestamp}` 형식으로 생성됩니다.

## 테스트 가이드

애플리케이션을 테스트하려면 Modbus RTU 시뮬레이터와 Kafka가 있는 테스트 환경을 설정해야 합니다.

### 테스트 환경 설정

#### 1. Modbus RTU 시뮬레이터

물리적 Modbus 장치 없이 테스트하려면 Modbus RTU 시뮬레이터를 사용할 수 있습니다:

- **Diagslave**: 무료 Modbus 슬레이브 시뮬레이터
    - 다운로드: https://www.modbusdriver.com/diagslave.html
    - RTU 모드로 실행: `diagslave -m rtu -a 1 -p /dev/ttyS1 -b 9600`

- **ModbusPal**: Java 기반 Modbus 시뮬레이터
    - 다운로드: https://sourceforge.net/projects/modbuspal/
    - 가상 시리얼 포트 사용하도록 구성

- **가상 시리얼 포트**: 테스트용 가상 시리얼 포트 쌍 생성
    - Linux: `socat`을 사용하여 가상 시리얼 포트 생성
      ```
      socat -d -d pty,raw,echo=0 pty,raw,echo=0
      ```
    - Windows: com0com 사용 (https://sourceforge.net/projects/com0com/)
    - macOS: socat 또는 Virtual Serial Port Driver와 같은 상용 솔루션 사용

#### 2. Kafka 설정

1. https://kafka.apache.org/downloads에서 Kafka 다운로드 및 압축 해제
2. ZooKeeper 시작:
   ```
   bin/zookeeper-server-start.sh config/zookeeper.properties
   ```
3. Kafka 서버 시작:
   ```
   bin/kafka-server-start.sh config/server.properties
   ```
4. 토픽 생성:
   ```
   bin/kafka-topics.sh --create --topic modbus-data --bootstrap-server 192.168.219.51:9092
   ```

### 애플리케이션 빌드

Gradle을 사용하여 애플리케이션 빌드:
```
./gradlew build
```

### 테스트 매개변수로 애플리케이션 실행

1. `application.properties` 파일을 테스트 환경에 맞게 수정:
    - `modbus.port`를 가상 시리얼 포트로 업데이트 (예: Linux에서 `/dev/pts/1`)
    - `modbus.slaveIds`를 테스트할 슬레이브 ID로 설정 (예: 테스트 시에는 `1`만 사용)
    - 다른 매개변수가 Modbus 시뮬레이터 구성과 일치하는지 확인

2. 애플리케이션 실행:
   ```
   java -jar build/libs/serial-kafka2-1.0-SNAPSHOT.jar
   ```

### 데이터 흐름 확인

1. Kafka 컨슈머를 시작하여 메시지 확인:
   ```
   bin/kafka-console-consumer.sh --topic modbus-data --from-beginning --bootstrap-server 192.168.219.51:9092
   ```

   다음과 같은 JSON 형식의 메시지가 표시되어야 합니다:
   ```json
   {"slaveId":1,"timestamp":1634567890123,"dataType":"holding_registers","values":[1,2,3,4,5,6,7,8,9,10]}
   ```

2. 애플리케이션 로그에서 성공적인 연결 및 데이터 읽기 확인:
    - "Modbus 슬레이브에 연결됨" 메시지 확인
    - "슬레이브 {ID}: 홀딩 레지스터 읽기 완료" 등의 메시지 확인
    - "슬레이브 {ID}: 홀딩 레지스터를 Kafka로 전송 완료" 등의 메시지 확인
    - "모든 슬레이브 데이터 수집 완료" 메시지 확인

3. Modbus 시뮬레이터를 사용하는 경우, 레지스터 값을 수정하고 Kafka 메시지에 반영된 변경 사항을 확인할 수 있습니다.

## 주요 개선 사항

이 프로젝트는 다음과 같은 주요 개선 사항을 포함하고 있습니다:

### 1. 설정 관리 개선
- 하드코딩된 상수 대신 외부 설정 파일(`application.properties`)을 사용하여 유연성 증가
- `AppConfig` 클래스를 통한 중앙 집중식 설정 관리

### 2. 다중 슬레이브 지원
- 최대 10개의 컴프레셔 기기(슬레이브)를 동시에 관리 가능
- 각 슬레이브의 데이터를 개별적으로 수집 및 전송

### 3. 성능 최적화
- 병렬 처리를 통한 데이터 수집 성능 향상
- 스레드 풀을 사용하여 여러 슬레이브의 데이터를 동시에 수집

### 4. 안정성 향상
- 연결 실패 시 자동 재시도 로직 구현
- 연결이 끊어진 경우 자동 재연결 시도
- 각 슬레이브별 오류 처리로 한 슬레이브의 오류가 다른 슬레이브에 영향을 미치지 않음

### 5. 데이터 형식 개선
- 데이터를 구조화된 JSON 형식으로 Kafka에 전송
- 각 메시지에 슬레이브 ID, 타임스탬프, 데이터 타입 등의 메타데이터 포함

### 6. 코드 품질 향상
- 모듈화된 코드 구조로 유지보수 용이
- 중복 코드 제거 및 제네릭 메서드 사용
- 명확한 책임 분리로 코드 가독성 향상

## 문제 해결

애플리케이션에 문제가 발생하면 로그에서 오류 메시지를 확인하세요. 애플리케이션은 SLF4J를 사용하여 로깅하며, 발생하는 모든 오류에 대한 자세한 정보를 제공합니다.

일반적인 문제:
- 시리얼 포트를 찾을 수 없음: 시리얼 포트가 올바르게 구성되어 있는지 확인
- Kafka 연결 문제: Kafka 서버가 실행 중이고 접근 가능한지 확인
- Modbus 통신 오류: 장치 연결 및 구성 확인
- 권한 문제: 시리얼 포트에 접근할 수 있는 권한이 있는지 확인 (Linux에서는 sudo가 필요할 수 있음)
- 설정 파일 문제: `application.properties` 파일이 올바른 경로에 있고 올바른 형식인지 확인

