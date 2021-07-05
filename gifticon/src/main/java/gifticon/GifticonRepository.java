package gifticon;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource(collectionResourceRel="gifticons", path="gifticons")
public interface GifticonRepository extends PagingAndSortingRepository<Gifticon, Long>{

    Gifticon findByGifticonId(Long GifticonId);

}
