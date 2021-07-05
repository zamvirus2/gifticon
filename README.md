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



