package br.com.ascend.the_system.service;

import br.com.ascend.the_system.domain.*;
import br.com.ascend.the_system.dto.*;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.exception.ConflictException;
import br.com.ascend.the_system.repository.*;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service
public class CosmeticStoreService implements ApplicationRunner {
 private final StoreItemRepository items; private final CosmeticOwnershipRepository ownership;
 private final EquippedCosmeticRepository equipped; private final CosmeticPurchaseRepository purchases;
 private final UserRepository users; private final VaultTransactionRepository transactions;
 public CosmeticStoreService(StoreItemRepository items,CosmeticOwnershipRepository ownership,EquippedCosmeticRepository equipped,
   CosmeticPurchaseRepository purchases,UserRepository users,VaultTransactionRepository transactions){
  this.items=items;this.ownership=ownership;this.equipped=equipped;this.purchases=purchases;this.users=users;this.transactions=transactions;
 }

 @Override @Transactional public void run(ApplicationArguments args){
  seed("frame-nebula","AVATAR_FRAME","EPIC","Nebula Frame","Uma moldura cósmica para destacar seu avatar.",420,true);
  seed("frame-circuit","AVATAR_FRAME","RARE","Circuit Frame","Linhas de circuito em azul para seu avatar.",260,false);
  seed("banner-aurora","PROFILE_BANNER","LEGENDARY","Aurora Banner","Uma aurora luminosa para o cartão do seu perfil.",680,true);
  seed("title-architect","TITLE","EPIC","The Architect","Título cosmético para quem constrói seu próprio caminho.",500,false);
  seed("badge-ascendant","BADGE","LEGENDARY","Ascendant Emblem","Emblema cosmético de raridade lendária.",750,true);
  seed("companion-orbit","COMPANION","MYTHIC","Orbit Companion","Um pequeno assistente holográfico para acompanhar seu perfil.",900,true);
  seed("theme-aurora","THEME","RARE","Aurora Theme","Paleta fria inspirada nas luzes do norte.",360,false);
  items.findByActiveTrueOrderByFeaturedDescIdAsc();
 }
 private void seed(String id,String category,String rarity,String name,String description,int cost,boolean featured){
  if(!items.existsById(id)) items.save(new StoreItem(id,category,rarity,name,description,cost,null,featured));
 }

 @Transactional(readOnly=true) public CosmeticStateResponse state(User account){
  User user=users.findById(account.getId()).orElseThrow();
  Map<String,EquippedCosmetic> slots=new HashMap<>();
  equipped.findByUser_Id(user.getId()).forEach(e->slots.put(e.getSlot(),e));
  Set<String> owned=new HashSet<>();
  ownership.findByUser_IdOrderByAcquiredAtDesc(user.getId()).forEach(o->owned.add(o.getItem().getId()));
  List<CosmeticItemResponse> result=items.findByActiveTrueOrderByFeaturedDescIdAsc().stream().map(item->{
   String slot=slotForCategory(item.getCategory()); EquippedCosmetic active=slots.get(slot);
   return new CosmeticItemResponse(item.getId(),item.getCategory(),item.getRarity(),item.getName(),item.getDescription(),item.getCost(),item.getPreviewUrl(),item.isFeatured(),owned.contains(item.getId()),active!=null&&active.getItem().getId().equals(item.getId()),slot);
  }).toList();
  return new CosmeticStateResponse(user.getCoins(),result);
 }

 @Transactional public CosmeticStateResponse purchase(User account,ShopPurchaseRequest request){
  if(request==null||request.itemId()==null||request.itemId().isBlank()) throw new BadRequestException("Selecione um cosmético.");
  User user=users.findByIdForUpdate(account.getId()).orElseThrow();
  StoreItem item=items.findById(request.itemId()).filter(StoreItem::isActive).orElseThrow(()->new BadRequestException("Cosmético indisponível."));
  if(ownership.existsByUser_IdAndItem_Id(user.getId(),item.getId())) throw new ConflictException("Você já possui este cosmético.");
  if(user.getCoins()<item.getCost()) throw new BadRequestException("Moedas insuficientes.");
  user.spendCoins(item.getCost());
  ownership.saveAndFlush(new CosmeticOwnership(user,item));
  purchases.save(new CosmeticPurchase(user,item,item.getCost()));
  transactions.save(new VaultTransaction(user,VaultTransaction.VaultTransactionType.EXPENSE,"Cosmético: "+item.getName(),item.getCost()));
  return state(user);
 }

 @Transactional public CosmeticStateResponse equip(User account,String itemId){
  User user=users.findByIdForUpdate(account.getId()).orElseThrow();
  if(itemId==null||itemId.isBlank()) throw new BadRequestException("Informe um item para equipar.");
  StoreItem item=items.findById(itemId).filter(StoreItem::isActive).orElseThrow(()->new BadRequestException("Cosmético indisponível."));
  if(!ownership.existsByUser_IdAndItem_Id(user.getId(),item.getId())) throw new BadRequestException("Você precisa possuir o cosmético para equipá-lo.");
  String slot=slotForCategory(item.getCategory());
  equipped.deleteByUser_IdAndSlot(user.getId(),slot);
  equipped.saveAndFlush(new EquippedCosmetic(user,item,slot));
  return state(user);
 }
 @Transactional public CosmeticStateResponse unequip(User account,String slot){
  User user=users.findByIdForUpdate(account.getId()).orElseThrow();
  if(!Set.of("AVATAR_FRAME","PROFILE_BANNER","TITLE","BADGE","COMPANION","THEME").contains(slot)) throw new BadRequestException("Slot de cosmético inválido.");
  equipped.deleteByUser_IdAndSlot(user.getId(),slot);
  return state(user);
 }
 private String slotForCategory(String category){
  if(!Set.of("AVATAR_FRAME","PROFILE_BANNER","PROFILE_EFFECT","NAMEPLATE","TITLE","BADGE","COMPANION","THEME").contains(category)) throw new BadRequestException("Categoria de cosmético inválida.");
  return category;
 }
}
