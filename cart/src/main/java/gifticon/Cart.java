package gifticon;

import javax.persistence.*;
import org.springframework.beans.BeanUtils;
import java.util.List;
import java.util.Date;

@Entity
@Table(name="Cart_table")
public class Cart {

    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String gifticonId;
    private Long quantity;
    private String status;


    // *************************************************************************
    // FeignClient로 동기호출 구현
    // *************************************************************************
    // Cart에 주문 추가할때 Gifticon availableQuantity를 초과할 경우, Cart에 persist(POST)가 되지 않도록 처리한다.
    //
    // Gifticon MicroService에서 수량 체크 (http://localhost:8081/gifticons의 /checkAndModifyQuantity) 결과가
    // false 로 나올 경우 Exception을 발생시킨다.
    // *************************************************************************
    @PostPersist
    public void onPostPersist() throws Exception {

        boolean rslt = CartApplication.applicationContext.getBean(gifticon.external.GifticonService.class)
            .modifyQuantity(this.getGifticonId(), this.getQuantity().intValue());

            if(rslt) {
                System.out.println("##### Cart - Result : true #####");
                this.setStatus("CartAdded");
                CartAdded cartAdded = new CartAdded();
                BeanUtils.copyProperties(this, cartAdded);
                cartAdded.publishAfterCommit();
                
            }else{
                System.out.println("##### Cart - Result : false - too big quantity count #####");
                throw new Exception("Too big quantity Count");
            }

    }


    @PreRemove
    public void onPreRemove(){
        CartRemoved cartRemoved = new CartRemoved();
        BeanUtils.copyProperties(this, cartRemoved);
        cartRemoved.publishAfterCommit();


    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }
    public String getGifticonId() {
        return gifticonId;
    }

    public void setGifticonId(String gifticonId) {
        this.gifticonId = gifticonId;
    }
    public Long getQuantity() {
        return quantity;
    }

    public void setQuantity(Long quantity) {
        this.quantity = quantity;
    }
    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }




}
