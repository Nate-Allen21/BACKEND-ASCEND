package br.com.ascend.the_system.service;
import br.com.ascend.the_system.domain.*;
import br.com.ascend.the_system.repository.*;
import br.com.ascend.the_system.exception.*;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import java.util.*;

@Service
public class CosmeticAdministration {
    private final JdbcTemplate db;
    private final EntityManager em;
    private final UserRepository users;
    private final Environment environment;
    @Value("${ASCEND_COSMETIC_ADMIN_IDS:}") private List<Long> admins;
    @Value("${ASCEND_COSMETIC_TEST_USER_IDS:}") private List<Long> testAccounts;
    public CosmeticAdministration(JdbcTemplate db,EntityManager em,UserRepository users,Environment environment){
        this.db=db; this.em=em; this.users=users; this.environment=environment;
    }
    public boolean allowed(User actor){return admins.contains(actor.getId());}
    private void guard(User actor){if(!allowed(actor)) throw new ForbiddenException();}
    private int offset(int page){
        if(page<0 || page>1000000) throw new BadRequestException("Página inválida.");
        return page*50;
    }
    private List<Map<String,Object>> rows(String sql,Object... params){
        return db.query(sql,(rs,index)->{
            Map<String,Object> row=new LinkedHashMap<>();
            var columns=rs.getMetaData();
            for(int i=1;i<=columns.getColumnCount();i++) row.put(columns.getColumnLabel(i).toLowerCase(Locale.ROOT),rs.getObject(i));
            return row;
        },params);
    }
    public List<Map<String,Object>> history(User user,int page){
        return rows("select type, amount, description, created_at from vault_transactions where user_id=? order by id desc limit 50 offset ?",user.getId(),offset(page));
    }
    @Transactional(readOnly=true)
    public Map<String,Object> overview(User actor,int page){
        guard(actor);
        int start=offset(page);
        return Map.of("items",rows("select id,name,cost,active from store_items where collection_name is not null order by id"),
            "purchases", rows("select user_id,item_id,cost,purchased_at from cosmetic_purchase_history order by id desc limit 50 offset ?",start),
            "transactions",rows("select user_id,type,amount,description,created_at from vault_transactions order by id desc limit 50 offset ?",start),
            "reward",db.queryForList("select amount from cosmetic_audit where action='REWARD' order by id desc limit 1",Integer.class).stream().findFirst().orElse(0),
            "testBalances",environment.acceptsProfiles(Profiles.of("local","test")) && !testAccounts.isEmpty());
    }
    @Transactional
    public void configure(User actor,String id,Integer price,Boolean active){
        guard(actor);
        if(price==null || price<1 || active==null) throw new BadRequestException("Informe preço positivo e estado.");
        StoreItem item=em.find(StoreItem.class,id,LockModeType.PESSIMISTIC_WRITE);
        if(item==null || item.getCollectionName()==null) throw new BadRequestException("Item inválido.");
        em.persist(new CosmeticAudit(actor,"ITEM",id+" "+item.getCost()+" / "+item.isActive()+" -> "+price+" / "+active,price));
        item.configure(price,active);
    }
    @Transactional
    public void reward(User actor,int amount){
        guard(actor);
        if(amount<0) throw new BadRequestException("Recompensa inválida.");
        em.persist(new CosmeticAudit(actor,"REWARD","Recompensas futuras",amount));
    }
    @Transactional
    public void testBalance(User actor,long target,int delta,String reason){
        guard(actor);
        if(!environment.acceptsProfiles(Profiles.of("local","test")) || !testAccounts.contains(target)) throw new ForbiddenException();
        if(delta==0 || Math.abs((long)delta)>10000 || reason==null || reason.isBlank() || reason.length()>80) throw new BadRequestException("Informe motivo e ajuste de até 10.000 AC.");
        User user=users.findByIdForUpdate(target).orElseThrow(()->new BadRequestException("Conta desconhecida."));
        user.adjustTestBalance(delta);
        em.persist(new VaultTransaction(user,delta>0?VaultTransaction.VaultTransactionType.INCOME:VaultTransaction.VaultTransactionType.EXPENSE,"Teste por #"+actor.getId()+": "+reason,Math.abs(delta)));
        em.persist(new CosmeticAudit(actor,"TEST_BALANCE","#"+target+" "+reason,delta));
    }
}
