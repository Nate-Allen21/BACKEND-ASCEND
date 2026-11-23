package br.com.ascend.the_system.controller;
import br.com.ascend.the_system.domain.*;
import br.com.ascend.the_system.repository.*;
import br.com.ascend.the_system.service.AuthService;
import br.com.ascend.the_system.exception.BadRequestException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.time.OffsetDateTime;
import java.util.*;
@RestController @RequestMapping("/api/connect")
public class ConnectController {
 private final AuthService auth; private final UserRepository users; private final SocialConnectionRepository social; private final ConnectConversationRepository conversations; private final ConnectMessageRepository messages;
 private final EquippedCosmeticRepository frames;
 public ConnectController(EquippedCosmeticRepository frames, AuthService a,UserRepository u,SocialConnectionRepository s,ConnectConversationRepository c,ConnectMessageRepository m){this.frames=frames;auth=a;users=u;social=s;conversations=c;messages=m;}
 private void available(User a,User b){
  if(social.existsBySourceAndTargetAndKind(a,b,"BLOCK")||social.existsBySourceAndTargetAndKind(b,a,"BLOCK")) throw new BadRequestException("Conversa indisponível.");
  boolean friends=social.existsBySourceAndTargetAndKindAndStatus(a,b,"FRIEND","ACCEPTED")||social.existsBySourceAndTargetAndKindAndStatus(b,a,"FRIEND","ACCEPTED");
  if("PRIVATE".equals(b.getProfileVisibility()) || ("FRIENDS".equals(b.getProfileVisibility())&&!friends)) throw new BadRequestException("Este perfil não está dispon?vel.");
 }
 private ConnectConversation member(Long id,User me,boolean lock){ConnectConversation c=(lock?conversations.lockById(id):conversations.findById(id)).orElseThrow(()->new BadRequestException("Conversa indisponível.")); if(!c.first.getId().equals(me.getId())&&!c.second.getId().equals(me.getId()))throw new BadRequestException("Conversa indisponível."); available(me,c.other(me));return c;}
 private Map<String,Object> message(ConnectMessage m,ConnectConversation c,User me){Map<String,Object> out=new LinkedHashMap<>();out.put("id",m.id);out.put("senderId",m.sender.getId());out.put("content",m.content);out.put("createdAt",m.createdAt);out.put("read",m.sender.getId().equals(me.getId()) && m.id <= (c.first.getId().equals(me.getId())?c.secondRead:c.firstRead));return out;}
 private Map<String,Object> summary(ConnectConversation c,User me){User other=c.other(me);Map<String,Object> out=new LinkedHashMap<>();out.put("id",c.id);out.put("userId",other.getId());out.put("isAscendFounder",Set.of(2L,8L).contains(other.getId()));out.put("name",other.getName());out.put("username",other.getUsername());out.put("avatar",other.getAvatar());out.put("profileImage",other.getProfileImage());frames.findByUser_IdAndSlot(other.getId(),"AVATAR_FRAME").map(EquippedCosmetic::getItem).filter(i->i.getStaticAsset()!=null).ifPresent(i->out.put("frame",Map.of("staticAsset",i.getStaticAsset(),"animatedAsset",i.getAnimatedAsset())));out.put("updatedAt",c.updatedAt);out.put("unread",messages.countByConversationAndSenderNotAndIdGreaterThan(c,me,c.first.getId().equals(me.getId())?c.firstRead:c.secondRead));messages.findFirstByConversationOrderByIdDesc(c).ifPresent(m->out.put("lastMessage",message(m,c,me)));return out;}
 @GetMapping("/users") @Transactional(readOnly=true)
 public List<Map<String,Object>> search(@RequestHeader(value="Authorization",required=false) String h,@RequestParam(defaultValue="") String q){
  User me=auth.resolveUser(h); String query=q.trim(); if(query.length()<2 || query.length()>120)return List.of();
  List<User> candidates=new ArrayList<>(users.findTop20ByNameContainingIgnoreCaseOrderByNameAsc(query));
  String handle=query.startsWith("@")?query.substring(1):query;
  String number=handle.replaceFirst("^.*-", "");
  if(number.matches("[0-9]{1,18}"))try{users.findById(Long.parseLong(number)).filter(u->handle.equalsIgnoreCase(u.getUsername()) || handle.equals(number)).ifPresent(candidates::add);}catch(NumberFormatException ignored){}
  Map<Long,Map<String,Object>> result=new LinkedHashMap<>();
  for(User u:candidates){if(u.getId().equals(me.getId()))continue;try{available(me,u);}catch(BadRequestException ignored){continue;}Map<String,Object> row=new LinkedHashMap<>();row.put("id",u.getId());row.put("name",u.getName());row.put("username",u.getUsername());row.put("avatar",u.getAvatar());result.put(u.getId(),row);if(result.size()==20)break;}
  return new ArrayList<>(result.values());
 }
 @GetMapping("/conversations") @Transactional(readOnly=true) public List<Map<String,Object>> list(@RequestHeader(value="Authorization",required=false) String h){User me=auth.resolveUser(h);List<Map<String,Object>> out=new ArrayList<>();for(var c:conversations.forUser(me)){try{available(me,c.other(me));out.add(summary(c,me));}catch(BadRequestException ignored){}}return out;}
 @PostMapping("/conversations/{userId}") @Transactional public Map<String,Object> create(@RequestHeader(value="Authorization",required=false) String h,@PathVariable Long userId){User me=auth.resolveUser(h);if(me.getId().equals(userId))throw new BadRequestException("Não é possível conversar consigo mesmo.");User other=users.findById(userId).orElseThrow(()->new BadRequestException("Usuário indisponível."));available(me,other);User first=me.getId()<other.getId()?me:other,second=first==me?other:me;users.findByIdForUpdate(first.getId()).orElseThrow();ConnectConversation c=conversations.findByFirstAndSecond(first,second).orElseGet(()->conversations.save(new ConnectConversation(first,second)));return summary(c,me);}
 @GetMapping("/conversations/{id}/messages") @Transactional(readOnly=true) public List<Map<String,Object>> history(@RequestHeader(value="Authorization",required=false) String h,@PathVariable Long id,@RequestParam(defaultValue="9223372036854775807") Long before){User me=auth.resolveUser(h);ConnectConversation c=member(id,me,false);List<ConnectMessage> rows=messages.findByConversationAndIdLessThanOrderByIdDesc(c,before,PageRequest.of(0,50));Collections.reverse(rows);return rows.stream().map(m->message(m,c,me)).toList();}
 @PostMapping("/conversations/{id}/messages") @Transactional public Map<String,Object> send(@RequestHeader(value="Authorization",required=false) String h,@PathVariable Long id,@RequestBody Map<String,String> body){User me=auth.resolveUser(h);ConnectConversation c=member(id,me,true);String content=Objects.toString(body.get("content"),"").trim(),clientId=Objects.toString(body.get("clientId"),"");try{UUID.fromString(clientId);}catch(IllegalArgumentException e){throw new BadRequestException("Identificador inválido.");}if(content.isBlank()||content.length()>2000)throw new BadRequestException("A mensagem deve ter entre 1 e 2000 caracteres.");var existing=messages.findBySenderAndClientId(me,clientId);if(existing.isPresent()){if(!existing.get().conversation.id.equals(id))throw new BadRequestException("Identificador já utilizado.");return message(existing.get(),c,me);}users.findByIdForUpdate(me.getId()).orElseThrow();if(messages.countBySenderAndCreatedAtAfter(me,OffsetDateTime.now().minusMinutes(1))>=30)throw new BadRequestException("Limite de mensagens atingido. Aguarde um momento.");ConnectMessage m=messages.saveAndFlush(new ConnectMessage(c,me,clientId,content));c.updatedAt=m.createdAt;return message(m,c,me);}
 @PostMapping("/conversations/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT) @Transactional public void read(@RequestHeader(value="Authorization",required=false) String h,@PathVariable Long id,@RequestBody Map<String,Long> body){User me=auth.resolveUser(h);ConnectConversation c=member(id,me,true);Long upTo=body.get("messageId");if(upTo==null)throw new BadRequestException("Mensagem inválida.");ConnectMessage m=messages.findById(upTo).orElseThrow(()->new BadRequestException("Mensagem inválida."));if(!m.conversation.id.equals(id))throw new BadRequestException("Mensagem inválida.");if(c.first.getId().equals(me.getId()))c.firstRead=Math.max(c.firstRead,upTo);else c.secondRead=Math.max(c.secondRead,upTo);}
}
