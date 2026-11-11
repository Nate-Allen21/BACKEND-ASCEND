package br.com.ascend.the_system.service;

import br.com.ascend.the_system.domain.*;
import br.com.ascend.the_system.dto.ShopPurchaseRequest;
import br.com.ascend.the_system.exception.BadRequestException;
import br.com.ascend.the_system.exception.ConflictException;
import br.com.ascend.the_system.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CosmeticStoreServiceTest {
 private StoreItemRepository items; private CosmeticOwnershipRepository ownership; private EquippedCosmeticRepository equipped;
 private CosmeticPurchaseRepository purchases; private UserRepository users; private VaultTransactionRepository transactions;
 private CosmeticStoreService service; private User user; private StoreItem item;
 @BeforeEach void setUp(){
  items=mock(StoreItemRepository.class); ownership=mock(CosmeticOwnershipRepository.class); equipped=mock(EquippedCosmeticRepository.class);
  purchases=mock(CosmeticPurchaseRepository.class); users=mock(UserRepository.class); transactions=mock(VaultTransactionRepository.class);
  service=new CosmeticStoreService(items,ownership,equipped,purchases,users,transactions);
  user=mock(User.class); when(user.getId()).thenReturn(42L); when(user.getCoins()).thenReturn(580);
  item=new StoreItem("frame-nebula","AVATAR_FRAME","EPIC","Nebula Frame","Frame",420,null,true);
  when(users.findByIdForUpdate(42L)).thenReturn(Optional.of(user)); when(users.findById(42L)).thenReturn(Optional.of(user));
  when(items.findById("frame-nebula")).thenReturn(Optional.of(item)); when(items.findByActiveTrueOrderByFeaturedDescIdAsc()).thenReturn(List.of(item));
  when(equipped.findByUser_Id(42L)).thenReturn(List.of()); when(ownership.findByUser_IdOrderByAcquiredAtDesc(42L)).thenReturn(List.of());
 }
 @Test void purchaseUsesServerPriceAndCreatesOwnershipAndLedgerEntry(){
  when(ownership.existsByUser_IdAndItem_Id(42L,"frame-nebula")).thenReturn(false);
  service.purchase(user,new ShopPurchaseRequest("frame-nebula"));
  verify(users).findByIdForUpdate(42L); verify(user).spendCoins(420);
  verify(ownership).saveAndFlush(any(CosmeticOwnership.class)); verify(purchases).save(any(CosmeticPurchase.class));
  verify(transactions).save(argThat(t->t.getType()==VaultTransaction.VaultTransactionType.EXPENSE&&t.getAmount()==420));
 }
 @Test void duplicatePurchaseIsRejected(){
  when(ownership.existsByUser_IdAndItem_Id(42L,"frame-nebula")).thenReturn(true);
  assertThrows(ConflictException.class,()->service.purchase(user,new ShopPurchaseRequest("frame-nebula")));
  verify(user,never()).spendCoins(anyInt()); verify(ownership,never()).saveAndFlush(any());
 }
 @Test void itemNotOwnedCannotBeEquipped(){
  when(ownership.existsByUser_IdAndItem_Id(42L,"frame-nebula")).thenReturn(false);
  assertThrows(BadRequestException.class,()->service.equip(user,"frame-nebula"));
  verify(equipped,never()).saveAndFlush(any());
 }
}
