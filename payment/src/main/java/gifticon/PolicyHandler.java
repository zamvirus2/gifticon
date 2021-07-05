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
