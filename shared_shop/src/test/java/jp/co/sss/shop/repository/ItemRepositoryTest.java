package jp.co.sss.shop.repository;

import static org.assertj.core.api.Assertions.assertThat;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import jp.co.sss.shop.entity.Item;
import jp.co.sss.shop.entity.OrderItem;
import jakarta.persistence.EntityManager;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
public class ItemRepositoryTest {
    @Autowired
    private EntityManager entityManager;

    @Autowired
    private ItemRepository itemRepository;

    @Test
    public void testFindAllByOrderByOrderItemCountDesc() {
        Item item1 = new Item();
        item1.setName("Item 1");
        entityManager.persist(item1);

        Item item2 = new Item();
        item2.setName("Item 2");
        entityManager.persist(item2);

        Item item3 = new Item();
        item3.setName("Item 3");
        entityManager.persist(item3);

        entityManager.flush();

        // Explicitly set delete_flag to 0 because @Column(insertable = false) prevents it from being set via persist()
        entityManager.createNativeQuery("UPDATE items SET delete_flag = 0").executeUpdate();

        // Item 2 has 3 orders
        for (int i = 0; i < 3; i++) {
            OrderItem oi = new OrderItem();
            oi.setItem(item2);
            oi.setPrice(100);
            oi.setQuantity(1);
            entityManager.persist(oi);
        }

        // Item 1 has 1 order
        OrderItem oi1 = new OrderItem();
        oi1.setItem(item1);
        oi1.setPrice(100);
        oi1.setQuantity(1);
        entityManager.persist(oi1);

        // Item 3 has 0 orders

        entityManager.flush();
        entityManager.clear();

        List<Item> results = itemRepository.findAllByOrderByOrderItemCountDesc();

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getName()).isEqualTo("Item 2");
        assertThat(results.get(1).getName()).isEqualTo("Item 1");
        assertThat(results.get(2).getName()).isEqualTo("Item 3");
    }
}
