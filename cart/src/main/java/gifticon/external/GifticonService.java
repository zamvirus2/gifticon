
package gifticon.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Date;

@FeignClient(name="gifticon", url="http://localhost:8081")
public interface GifticonService {

    @RequestMapping(method= RequestMethod.GET, path="/checkAndModifyQuantity")
    public boolean modifyQuantity(@RequestParam("gifticonId") String gifticonId,
                                  @RequestParam("quantity") int quantityCount);
}