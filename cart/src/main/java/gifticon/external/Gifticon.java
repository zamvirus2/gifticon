package gifticon.external;

public class Gifticon {

    private Long id;
    private Long gifticonId;
    private String name;
    private Integer availableQuantity;
    private Long price;

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
