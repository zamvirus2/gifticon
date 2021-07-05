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
    @Autowired GifticonRepository gifticonRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void onStringEventListener(@Payload String eventString){}

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverCartRemoved_IncreaseQuantity(@Payload CartRemoved cartRemoved){

        if(!cartRemoved.validate()) return;

        // System.out.println("\n\n##### listener IncreaseQuantity : " + cartRemoved.toJson() + "\n\n");

        Gifticon gifticon = gifticonRepository.findByGifticonId(Long.valueOf(cartRemoved.getGifticonId()));
        gifticon.setAvailableQuantity(gifticon.getAvailableQuantity() + cartRemoved.getQuantity().intValue());
        gifticonRepository.save(gifticon);


    }

}
