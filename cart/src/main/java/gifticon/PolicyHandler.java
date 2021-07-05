package gifticon;

import java.util.Optional;

import gifticon.config.kafka.KafkaProcessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

@Service
public class PolicyHandler{
    @Autowired CartRepository cartRepository;

    @StreamListener(KafkaProcessor.INPUT)
    public void wheneverPaymentApproved_UpdateStatus(@Payload PaymentApproved paymentApproved){

        if(!paymentApproved.validate()) return;

        // System.out.println("\n\n##### listener UpdateStatus : " + paymentApproved.toJson() + "\n\n");

        Optional<Cart> optionalCart = cartRepository.findById(paymentApproved.getCartId());
        Cart cart = optionalCart.get();
        cart.setStatus(paymentApproved.getStatus());
        cartRepository.save(cart);
            
    }


    @StreamListener(KafkaProcessor.INPUT)
    public void whatever(@Payload String eventString){}


}
