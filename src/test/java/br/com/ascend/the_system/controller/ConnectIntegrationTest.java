package br.com.ascend.the_system.controller;
import br.com.ascend.the_system.service.AuthService;
import br.com.ascend.the_system.dto.RegisterRequest;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.repository.UserRepository;
import br.com.ascend.the_system.repository.SocialConnectionRepository;
import br.com.ascend.the_system.domain.SocialConnection;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest
class ConnectIntegrationTest {
 @Autowired AuthService auth; @Autowired ConnectController connect;
 @Autowired UserRepository users; @Autowired SocialConnectionRepository social;
 @Test void privateMessagesPersistAndEnforceMembershipAndReadReceipts(){
  String n=UUID.randomUUID().toString();
  var a=auth.register(new RegisterRequest("Alice Test","a"+n+"@example.com","Password123!"));
  var b=auth.register(new RegisterRequest("Bruno Test","b"+n+"@example.com","Password123!"));
  var c=auth.register(new RegisterRequest("Carlos Test","c"+n+"@example.com","Password123!"));
  String ah="Bearer "+a.token(),bh="Bearer "+b.token(),ch="Bearer "+c.token();
  long id=((Number)connect.create(ah,b.user().id()).get("id")).longValue();
  assertThat(connect.create(bh,a.user().id()).get("id")).isEqualTo(id);
  String uuid=UUID.randomUUID().toString();var body=Map.of("content","Olá!","clientId",uuid);
  var sent=connect.send(ah,id,body);assertThat(connect.send(ah,id,body).get("id")).isEqualTo(sent.get("id"));
  assertThat(connect.list(bh).get(0).get("unread")).isEqualTo(1L);
  assertThat(connect.history(bh,id,Long.MAX_VALUE)).hasSize(1);
  assertThatThrownBy(()->connect.history(ch,id,Long.MAX_VALUE)).isInstanceOf(BadRequestException.class);
  assertThatThrownBy(()->connect.send(ah,id,Map.of("content"," ","clientId",UUID.randomUUID().toString()))).isInstanceOf(BadRequestException.class);
  connect.read(bh,id,Map.of("messageId",((Number)sent.get("id")).longValue()));
  assertThat(connect.list(bh).get(0).get("unread")).isEqualTo(0L);
  assertThat(connect.history(ah,id,Long.MAX_VALUE).get(0).get("read")).isEqualTo(true);
 }
 @Test void searchPaginationAndBlocks(){
  String n=UUID.randomUUID().toString();
  var a=auth.register(new RegisterRequest("Diana Example","d"+n+"@example.com","Password123!"));
  var b=auth.register(new RegisterRequest("Edgar Example","e"+n+"@example.com","Password123!"));
  String ah="Bearer "+a.token(),bh="Bearer "+b.token();
  String username=users.findById(b.user().id()).orElseThrow().getUsername();
  assertThat(connect.search(ah,"@"+username)).extracting(row->row.get("id")).contains(b.user().id());
  long id=((Number)connect.create(ah,b.user().id()).get("id")).longValue();
  for(int i=0;i<51;i++)connect.send(i%2==0?ah:bh,id,Map.of("content","Mensagem "+i,"clientId",UUID.randomUUID().toString()));
  var latest=connect.history(ah,id,Long.MAX_VALUE);assertThat(latest).hasSize(50);
  long firstId=((Number)latest.get(0).get("id")).longValue();
  assertThat(connect.history(ah,id,firstId)).hasSize(1);
  social.saveAndFlush(new SocialConnection(users.findById(b.user().id()).orElseThrow(),users.findById(a.user().id()).orElseThrow(),"BLOCK","ACCEPTED"));
  assertThat(connect.search(ah,"@"+username)).isEmpty();
  assertThatThrownBy(()->connect.history(ah,id,Long.MAX_VALUE)).isInstanceOf(BadRequestException.class);
  assertThatThrownBy(()->connect.send(bh,id,Map.of("content","blocked","clientId",UUID.randomUUID().toString()))).isInstanceOf(BadRequestException.class);
 }
 @Test void concurrentCreationReturnsSameConversation() throws Exception {
  String n=UUID.randomUUID().toString();
  var a=auth.register(new RegisterRequest("Felipe Test","f"+n+"@example.com","Password123!"));
  var b=auth.register(new RegisterRequest("Gabriela Test","g"+n+"@example.com","Password123!"));
  var executor=Executors.newFixedThreadPool(4);
  try{var start=new CountDownLatch(1);List<Future<Object>> tasks=new ArrayList<>();
   for(int i=0;i<4;i++){final boolean first=i%2==0;tasks.add(executor.submit(()->{start.await();return connect.create("Bearer "+(first?a.token():b.token()),first?b.user().id():a.user().id()).get("id");}));}
   start.countDown();Set<Object> ids=new HashSet<>();for(var task:tasks)ids.add(task.get(20,TimeUnit.SECONDS));assertThat(ids).hasSize(1);
  }finally{executor.shutdownNow();}
 }
}
