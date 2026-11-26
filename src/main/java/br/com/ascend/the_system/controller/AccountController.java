package br.com.ascend.the_system.controller;

import br.com.ascend.the_system.dto.AccountDeletionRequest;
import br.com.ascend.the_system.service.AccountDeletionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class AccountController {
    private final AccountDeletionService accountDeletionService;

    public AccountController(AccountDeletionService accountDeletionService) {
        this.accountDeletionService = accountDeletionService;
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteOwnAccount(
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestBody AccountDeletionRequest request
    ) {
        accountDeletionService.delete(authorization, request);
    }
}
