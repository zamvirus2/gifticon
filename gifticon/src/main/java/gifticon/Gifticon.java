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
