package br.com.ascend.the_system.controller;
import br.com.ascend.the_system.dto.*;
import br.com.ascend.the_system.service.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/cosmetics")
public class CosmeticStoreController {
 private final CosmeticStoreService store; private final AuthService auth;
 public CosmeticStoreController(CosmeticStoreService store,AuthService auth){this.store=store;this.auth=auth;}
 @GetMapping public CosmeticStateResponse state(@RequestHeader(value="Authorization",required=false) String header){return store.state(auth.resolveUser(header));}
 @GetMapping("/items/{id}") public CosmeticItemResponse detail(@RequestHeader(value="Authorization",required=false) String header,@PathVariable String id){
  return state(header).items().stream().filter(i->i.id().equals(id)).findFirst().orElseThrow(()->new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND));
 }
 @GetMapping("/inventory") public java.util.List<CosmeticItemResponse> inventory(@RequestHeader(value="Authorization",required=false) String header){return state(header).items().stream().filter(CosmeticItemResponse::owned).toList();}
 @GetMapping("/equipped") public java.util.List<CosmeticItemResponse> equipped(@RequestHeader(value="Authorization",required=false) String header){return state(header).items().stream().filter(CosmeticItemResponse::equipped).toList();}
 @GetMapping("/balance") public java.util.Map<String,Integer> balance(@RequestHeader(value="Authorization",required=false) String header){return java.util.Map.of("coins",auth.resolveUser(header).getCoins());}
 @PostMapping("/purchase") public CosmeticStateResponse purchase(@RequestHeader(value="Authorization",required=false) String header,@RequestBody(required=false) ShopPurchaseRequest request){return store.purchase(auth.resolveUser(header),request);}
 @PutMapping("/equipped") public CosmeticStateResponse equip(@RequestHeader(value="Authorization",required=false) String header,@RequestBody(required=false) EquipCosmeticRequest request){return store.equip(auth.resolveUser(header),request==null?null:request.itemId());}
 @DeleteMapping("/equipped/{slot}") public CosmeticStateResponse unequip(@RequestHeader(value="Authorization",required=false) String header,@PathVariable String slot){return store.unequip(auth.resolveUser(header),slot.toUpperCase(java.util.Locale.ROOT));}
}
