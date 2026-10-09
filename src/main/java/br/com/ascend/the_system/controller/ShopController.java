package br.com.ascend.the_system.controller;
import br.com.ascend.the_system.dto.*;
import br.com.ascend.the_system.service.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/shop")
public class ShopController {
    private final ShopService shop;
    private final AuthService auth;
    public ShopController(ShopService shop, AuthService auth) {this.shop=shop;this.auth=auth;}

    @GetMapping
    public ShopStateResponse get(@RequestHeader(value="Authorization", required=false) String authorization) {
        return shop.getState(auth.resolveUser(authorization));
    }

    @PostMapping("/purchase")
    public ShopStateResponse buy(@RequestHeader(value="Authorization", required=false) String authorization,
            @RequestBody(required=false) ShopPurchaseRequest request) {
        return shop.purchase(auth.resolveUser(authorization),request);
    }
}
