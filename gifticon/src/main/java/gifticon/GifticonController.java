package gifticon;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.List;

@RestController
public class GifticonController {
        @Autowired
        GifticonRepository gifticonRepository;


        @RequestMapping(value = "/checkAndModifyQuantity",
                method = RequestMethod.GET,
                produces = "application/json;charset=UTF-8")

        public boolean modifyQuantity(HttpServletRequest request, HttpServletResponse response) throws Exception {
                System.out.println("##### /gifticon/modifyQuantity  called #####");
                
                boolean status = false;
                Long gifticonId = Long.valueOf(request.getParameter("gifticonId"));
                int quantity = Integer.parseInt(request.getParameter("quantity"));

                Gifticon gifticon = gifticonRepository.findByGifticonId(gifticonId);

                if(gifticon.getAvailableQuantity() >= quantity) {
                        status = true;
                        gifticon.setAvailableQuantity(gifticon.getAvailableQuantity() - quantity);
                        gifticonRepository.save(gifticon);

                }
                return status;
        }

 }
