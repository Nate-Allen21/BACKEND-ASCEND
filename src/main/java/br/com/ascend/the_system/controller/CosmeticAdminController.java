package br.com.ascend.the_system.controller;
import br.com.ascend.the_system.service.*;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/cosmetics")
public class CosmeticAdminController {
    private final AuthService auth;
    private final CosmeticAdministration admin;
    public CosmeticAdminController(AuthService auth,CosmeticAdministration admin){this.auth=auth;this.admin=admin;}
    public record Settings(Integer price,Boolean active){}
    public record Reward(int amount){}
    public record Adjustment(long userId,int delta,String reason){}
    @GetMapping("/transactions")
    public List<Map<String,Object>> history(@RequestHeader(value="Authorization",required=false) String h,@RequestParam(defaultValue="0") int page){return admin.history(auth.resolveUser(h),page);}
    @GetMapping("/capabilities")
    public Map<String,Boolean> capabilities(@RequestHeader(value="Authorization",required=false) String h){return Map.of("admin",admin.allowed(auth.resolveUser(h)));}
    @GetMapping("/admin")
    public Map<String,Object> overview(@RequestHeader(value="Authorization",required=false) String h,@RequestParam(defaultValue="0") int page){return admin.overview(auth.resolveUser(h),page);}
    @PutMapping("/admin/items/{id}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void configure(@RequestHeader(value="Authorization",required=false) String h,@PathVariable String id,@RequestBody Settings s){admin.configure(auth.resolveUser(h),id,s.price(),s.active());}
    @PutMapping("/admin/reward")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void reward(@RequestHeader(value="Authorization",required=false) String h,@RequestBody Reward r){admin.reward(auth.resolveUser(h),r.amount());}
    @PostMapping("/admin/test-balance")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void adjust(@RequestHeader(value="Authorization",required=false) String h,@RequestBody Adjustment a){admin.testBalance(auth.resolveUser(h),a.userId(),a.delta(),a.reason());}
}
