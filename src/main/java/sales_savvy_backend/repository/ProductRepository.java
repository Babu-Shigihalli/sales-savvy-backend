package sales_savvy_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import sales_savvy_backend.entity.Product;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Modifying
    @Query("update Product p set p.stockQuantity = p.stockQuantity - :qty where p.id = :id and p.stockQuantity >= :qty")
    int decreaseStock(@Param("id") Integer id, @Param("qty") Integer qty);

    @Modifying
    @Query("update Product p set p.stockQuantity = p.stockQuantity + :qty where p.id = :id")
    int increaseStock(@Param("id") Integer id, @Param("qty") Integer qty);
}
