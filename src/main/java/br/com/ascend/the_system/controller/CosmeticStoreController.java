package br.com.ascend.the_system.controller;
import br.com.ascend.the_system.dto.*;
import br.com.ascend.the_system.service.*;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/cosmetics")
public class CosmeticStoreController {
 private final CosmeticStoreService store; private final AuthService auth;
 public CosmeticStoreController(CosmeticStoreService store,AuthService auth){this.store=store;this.auth=auth;}
 @GetMapping public CosmeticStateResponse state(@RequestHeader(value="Authorization",required=false) String header){return store.state(auth.resolveUser(header));}
 @PostMapping("/purchase") public CosmeticStateResponse purchase(@RequestHeader(value="Authorization",required=false) String header,@RequestBody(required=false) ShopPurchaseRequest request){return store.purchase(auth.resolveUser(header),request);}
 @PutMapping("/equipped") public CosmeticStateResponse equip(@RequestHeader(value="Authorization",required=false) String header,@RequestBody(required=false) EquipCosmeticRequest request){return store.equip(auth.resolveUser(header),request==null?null:request.itemId());}
 @DeleteMapping("/equipped/{slot}") public CosmeticStateResponse unequip(@RequestHeader(value="Authorization",required=false) String header,@PathVariable String slot){return store.unequip(auth.resolveUser(header),slot.toUpperCase(java.util.Locale.ROOT));}
}
