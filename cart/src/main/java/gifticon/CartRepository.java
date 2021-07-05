package gifticon;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="carts", path="carts")
public interface CartRepository extends PagingAndSortingRepository<Cart, Long>{


}
