# Gifticon (기프티콘 구매)


## 1. 서비스 시나리오


### 기능적 요구사항

```
1. 기프티콘 MD가 구매가능한 기프티콘정보, 구매가능 수량, 가격을 등록한다.
2. 고객이 기프티콘을 장바구니에 담는다.
3. 기프티콘이 장바구니에 담기면. 담은 수량만큼 기프티콘 구매가능 수량에서 차감된다. 
4. 장바구니에 담기면 결제정보를 승인한다.
5. 고객이 장바구니를 취소할 수 있다. 
6. 장바구니를 취소하면 결제가 취소된다.
7. 고객이 모든 진행내역을 볼 수 있어야 한다.
```

### 비기능적 요구사항
```
1. 트랜잭션
    1.1 구매 가능한 기프티콘 수량이 부족하면 장바구니 담기가 되지 않는다. --> Sync 호출
    1.2 장바구니가 취소되면 결제가 취소되고 구매가능한 수량이 증가한다. --> SAGA
2. 장애격리
    2.1 결제가 완료되지 않아도 장바구니 쇼핑은 항상 가능해야 한다. --> Async (event-driven), Eventual Consistency
    2.2 장바구니 쇼핑에 사용자가 몰려서 기프티콘 관리시스템의 부하가 과중하면, 장바구니 쇼핑을 잠시후에 할 수 있도록 유도한다. --> Circuit breaker, fallback
3. 성능
    3.1 고객이 상시 쇼핑, 결제 내역을 조회 할 수 있도록 성능을 고려하여 별도의 view(MyPage)로 구성한다. --> CQRS
```


## 2. 분석/설계

### Event Storming 결과
![image](https://user-images.githubusercontent.com/84003381/124440962-2802f180-ddb6-11eb-84ee-d3ab33009009.png)


### 헥사고날 아키텍처 다이어그램
![image](https://user-images.githubusercontent.com/84003381/124442984-3d791b00-ddb8-11eb-891a-7890e17004ab.png)


## 3. 구현

분석/설계 단계를 통해 각 마이크로서비스를 도출하였으며, 각 서비스를 로컬환경에서 아래와 같이 실행하였다.
로컬에서 각각의 포트번호는 8081~8084 이다.
```
  cd gifticon
  mvn spring-boot:run  
  
  cd cart
  mvn spring-boot:run  

  cd payment
  mvn spring-boot:run

  cd mypage
  mvn spring-boot:run  
```


### 3.1. DDD 적용

msaez.io를 통해 구현한 Aggregate 단위로 Entity를 구성하였다. (2. 분석/설계 참조)

Entity Pattern과 Repository Pattern을 사용하기 위해 Spring Data REST의 RestRepository를 적용하였다.

DDD 적용 후 REST API 테스트를 통해 정상 동작 확인하였다.

**Gifticon 서비스의 gifticon.java**

```java
package gifticon;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Gifticon_table")
public class Gifticon {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private Long gifticonId;
    private String name;
    private Integer availableQuantity;
    private Long price;

    @PostPersist
    public void onPostPersist(){
        GifticonRegistered gifticonRegistered = new GifticonRegistered();
        BeanUtils.copyProperties(this, gifticonRegistered);
        gifticonRegistered.publishAfterCommit();
    }

    @PostUpdate
    public void onPostUpdate(){
        QuantityModified quantityModified = new QuantityModified();
        BeanUtils.copyProperties(this, quantityModified);
        quantityModified.publishAfterCommit();
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Long getGifticonId() {
        return gifticonId;
    }
    public void setGifticonId(Long gifticonId) {
        this.gifticonId = gifticonId;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public Integer getAvailableQuantity() {
        return availableQuantity;
    }
    public void setAvailableQuantity(Integer availableQuantity) {
        this.availableQuantity = availableQuantity;
    }
    public Long getPrice() {
        return price;
    }
    public void setPrice(Long price) {
        this.price = price;
    }
}
```


**Payment 서비스의 PolicyHandler.java**

```java
package gifticon;

import gifticon.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired PaymentRepository paymentRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCartAdded_ApprovePayment(@Payload CartAdded cartAdded){

        if(!cartAdded.validate()) return;

        System.out.println("\n\n##### listener ApprovePayment : " + cartAdded.toJson() + "\n\n");

        Payment payment = new Payment();
        payment.setCartId(cartAdded.getId());
        payment.setStatus("PaymentApproved");
        paymentRepository.save(payment);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCartRemoved_CancelPayment(@Payload CartRemoved cartRemoved){

        if(!cartRemoved.validate()) return;

        System.out.println("\n\n##### listener CancelPayment : " + cartRemoved.toJson() + "\n\n");

        Payment payment = paymentRepository.findByCartId(cartRemoved.getId());
        payment.setStatus("PaymentCanceled");
        paymentRepository.save(payment);
    }

    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}
}
```


### 3.2. Saga, CQRS, Correlation, Req/Resp

기프티콘 구매 시스템의 각 마이크로 서비스별 역할은 다음과 같다.
마이크로 서비스간 통신은 기본적으로 Pub/Sub 을 통한 Event Driven 구조로 동작한다.

![image](https://user-images.githubusercontent.com/84003381/124505609-dafa3c00-de04-11eb-8da9-a2bd444bfd40.png)


|Name|Description|
|:----|:----|
|Saga|- 마이크로 서비스간 통신은 Kafka를 통해 Pub/Sub 통신하도록 구성함. 이를 통해 Event Driven 구조로 각 단계가 진행되도록 함<br>- 아래 테스트 시나리오의 전 구간 참조|
|CQRS|- mypage 서비스의 경우의 경우, 각 마이크로 서비스로부터 Pub/Sub 구조를 통해 받은 데이터를 이용하여 자체 DB로 View를 구성함.<br>- 이를 통해 여러 마이크로 서비스에 존재하는 DB간의 Join 등이 필요 없으며, 성능에 대한 이슈없이 빠른 조회가 가능함.<br>- 테스트 시나리오의 3.3 과 5.3 항목에 해당|
|Correlation|- 장바구니에 담게되면 cart > mypage(장바구니 담기내역), cart > payment > mypage(결제 및 결제취소 내역)로 진행되고, 장바구니 취소가 되면 각 status가 paymentCanceled로 Update 되는 것을 볼 수 있다.<br>- 또한 Correlation Key를 구현하기 위해 각 마이크로서비스에서 관리하는 데이터의 Id값을 전달받아서 서비스간의 연관 처리를 수행하였다.<br>- 이 결과로 서로 다른 마이크로 서비스 간에 트랜잭션이 묶여 있음을 알 수 있다.|
|Req/Resp|- gifticon 마이크로서비스의 구매가능 수량을 초과하여 장바구니에 담기를 시도할때는, cart 마이크로서비스에서 진행이 되지 않도록 처리함<br>- FeignClient 를 이용한 Req/Resp 연동<br>- 테스트 시나리오의 2.1, 2.2, 2.3 항목에 해당하며, 동기호출 결과는 3.1(담기 성공시)과 5.1(담기 취소시)에서 확인할 수 있다.|


**<구현기능 점검 테스트 시나리오>**

**1) MD가 기프티콘 정보 등록**

- http POST http://gifticon:8080/gifticons gifticonId="1" name="Americano" availableQuantity="100" price="5000"

![image](https://user-images.githubusercontent.com/84003381/124507593-0e3eca00-de09-11eb-9111-07b0197efb75.png)





**2) 사용자가 기프티콘 카트에 담기**

2-1) 정상처리 (예약번호 #1)

- http POST http://cart:8080/carts gifticonId="1" quantity="10"

![image](https://user-images.githubusercontent.com/84003381/124507693-4b0ac100-de09-11eb-866f-e7a1665d0298.png)


2-2) 정상처리 (예약번호 #2)

- http POST http://cart:8080/carts gifticonId="1" quantity="15"

![image](https://user-images.githubusercontent.com/84003381/124507797-860cf480-de09-11eb-812a-811722078585.png)


2-3) MD가 관리하는 기프티콘 정보의 잔여 수량을 초과하면 카트에 담기지 않도록 처리함

- FeignClient를 이용한 Req/Resp 연동
- http POST http://cart:8080/carts gifticonId="1" quantity="200"

![image](https://user-images.githubusercontent.com/84003381/124507897-bfddfb00-de09-11eb-9756-7cc7e8292638.png)




**3) 기프티콘 카트에 담기 후, 각 마이크로 서비스내 Pub/Sub을 통해 변경된 데이터 확인**

3-1) 기프티콘 정보 조회 (수량 차감여부 확인)  --> 수량이 75로 줄어듦
- http GET http://gifticon:8080/gifticons/1
![image](https://user-images.githubusercontent.com/84003381/124507999-f4ea4d80-de09-11eb-9b93-54ac162393f3.png)
   
3-2) 요금결제 내역 조회     --> 2 Row 생성 : Cart 생성 2건 후 > PaymentApproved 로 업데이트됨
- http GET http://payment:8080/payments
![image](https://user-images.githubusercontent.com/84003381/124508169-50b4d680-de0a-11eb-955e-3f32b4dcb2d1.png)

3-3) 마이페이지 조회        --> 2 Row 생성 : Cart 생성 2건 후 > PaymentApproved 로 업데이트됨
- http GET http://mypage:8080/mypages
![image](https://user-images.githubusercontent.com/84003381/124508419-d9337700-de0a-11eb-80aa-8a8731498c08.png)




**4) 사용자가 카트담기 취소**

4-1) 예약번호 #1을 취소함

- http DELETE http://cart:8080/carts/1

![image](https://user-images.githubusercontent.com/84003381/124508494-03853480-de0b-11eb-8e8d-b214b2fdda2c.png)


   
4-2) 취소내역 확인 (예약번호 #2만 남음)

- http GET http://cart:8080/carts

![image](https://user-images.githubusercontent.com/84003381/124508571-329ba600-de0b-11eb-9c1d-4c08ed46fb45.png)




**5) 카트담기 취소 후, 각 마이크로 서비스내 Pub/Sub을 통해 변경된 데이터 확인**

5-1) 기프티콘 정보 조회 (수량 증가여부 확인)  --> 수량이 85로 늘어남
- http GET http://gifticon:8080/gifticons/1
![image](https://user-images.githubusercontent.com/84003381/124508657-57901900-de0b-11eb-9463-98d7953b56ad.png)

5-2) 요금결제 내역 조회    --> 1번 카트에 대한 결제건이 paymentCancelled 로 업데이트됨
- http GET http://payment:8080/payments
![image](https://user-images.githubusercontent.com/84003381/124508808-8908e480-de0b-11eb-9a19-658e88ee51a7.png)

5-3) 마이페이지 조회       --> 1번 카트에 대한 결제건이 paymentCancelled 로 업데이트됨
- http GET http://mypage:8080/mypages
![image](https://user-images.githubusercontent.com/84003381/124508873-af2e8480-de0b-11eb-81b6-e7ac9052b6fd.png)


       

### 3.3. Polyglot Persistence 구조
cart, payment, mypage 서비스는 H2 DB를 사용하도록 구성하고, gifticon 서비스는 HSQLDB 를 사용하도록 구성하였다.
DB 부분을 Polyglot 구조로 동작하도록 처리하였다.


**1) gifticon 서비스 : pom.xml 내 DB설정 및 spring boot 기동로그**

![image](https://user-images.githubusercontent.com/84003381/124509581-36c8c300-de0d-11eb-8066-01e350fa6bb6.png)
![image](https://user-images.githubusercontent.com/84003381/124510050-046b9580-de0e-11eb-9b25-a88a65fb97e2.png)


**2) payment 서비스 : pom.xml 내 DB설정 및 spring boot 기동로그**

![image](https://user-images.githubusercontent.com/84003381/124510117-26651800-de0e-11eb-83de-ef2012059987.png)
![image](https://user-images.githubusercontent.com/84003381/124510366-9a9fbb80-de0e-11eb-837a-10f494a6d710.png)


### 3.4. Gateway 사용
gateway > application.yml 내 gateway 설정
![image](https://user-images.githubusercontent.com/84003381/124511000-033b6800-de10-11eb-856a-c3ac002be9c4.png)

gateway 테스트는 3.2 항목 > 구현기능 점검 테스트 시나리오에 캡쳐로 첨부한 이미지가 모두 gateway 접속에 해당됩니다.



## 4. 서비스 운영

### 4.1. Deploy



![image](https://user-images.githubusercontent.com/84003381/124511760-d7b97d00-de11-11eb-9f91-b7772966c9dd.png)







