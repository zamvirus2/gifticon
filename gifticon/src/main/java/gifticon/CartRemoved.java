package gifticon;

public class CartRemoved extends AbstractEvent {

    private Long id;
    private String gifticonId;
    private Long quantity;
    private String status;

    public CartRemoved(){
        super();
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
