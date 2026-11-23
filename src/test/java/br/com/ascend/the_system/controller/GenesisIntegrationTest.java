package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.service.*;
import br.com.ascend.the_system.dto.*;
import br.com.ascend.the_system.exception.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class GenesisIntegrationTest {
    @Autowired AuthService auth;
    @Autowired CosmeticStoreController store;
    @Autowired CosmeticAdminController admin;
    @Autowired JdbcTemplate db;
    @Test void completeWalletAndInventoryFlow() throws Exception {
        String email=UUID.randomUUID()+"@example.com", password="Password123!";
        var account=auth.register(new RegisterRequest("Genesis Test",email,password));
        String header="Bearer "+account.token();
        var frames=store.state(header).items().stream().filter(i->"GENESIS".equals(i.collection())).toList();
        assertEquals(List.of(500,1000,1800),frames.stream().map(CosmeticItemResponse::cost).toList());
        var core=new ShopPurchaseRequest("genesis-frame-01");
        assertThrows(UnauthorizedException.class,()->store.state(null));
        assertThrows(ForbiddenException.class,()->admin.overview(header,0));
        assertThrows(ForbiddenException.class,()->admin.adjust(header,new CosmeticAdminController.Adjustment(account.user().id(),100,"teste")));
        assertThrows(BadRequestException.class,()->store.purchase(header,core));
        assertThrows(BadRequestException.class,()->store.equip(header,new EquipCosmeticRequest(core.itemId())));
        db.update("update users set coins=1500 where id=?",account.user().id());
        try(var pool=Executors.newFixedThreadPool(2)){
            var start=new CountDownLatch(1);
            Callable<Boolean> buy=()->{start.await();try{store.purchase(header,core);return true;}catch(ConflictException ex){return false;}};
            var first=pool.submit(buy);var second=pool.submit(buy);start.countDown();
            assertNotEquals(first.get(15,TimeUnit.SECONDS),second.get(15,TimeUnit.SECONDS));
        }
        assertEquals(1000,store.balance(header).get("coins"));
        assertEquals(1,admin.history(header,0).size());
        store.equip(header,new EquipCosmeticRequest(core.itemId()));
        store.purchase(header,new ShopPurchaseRequest("genesis-frame-02"));
        store.equip(header,new EquipCosmeticRequest("genesis-frame-02"));
        auth.logout(header);
        assertThrows(UnauthorizedException.class,()->store.state(header));
        String next="Bearer "+auth.login(new AuthRequest(email,password)).token();
        assertEquals(List.of("genesis-frame-02"),store.equipped(next).stream().map(CosmeticItemResponse::id).toList());
        assertEquals(2,store.inventory(next).size());
        store.unequip(next,"AVATAR_FRAME");
        assertTrue(store.equipped(next).isEmpty());
    }
}
