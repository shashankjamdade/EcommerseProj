package org.example.authservice.config;

import org.example.authservice.model.AuthSession;
import org.example.authservice.model.UserAccount;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.mapping.event.AfterConvertCallback;
import org.springframework.data.r2dbc.mapping.event.AfterSaveCallback;
import reactor.core.publisher.Mono;

@Configuration
public class R2dbcEntityCallbacksConfig {

    @Bean
    public AfterConvertCallback<UserAccount> userAccountAfterConvertCallback() {
        return (userAccount, table) -> Mono.just(userAccount.markPersisted());
    }

    @Bean
    public AfterSaveCallback<UserAccount> userAccountAfterSaveCallback() {
        return (userAccount, outboundRow, table) -> Mono.just(userAccount.markPersisted());
    }

    @Bean
    public AfterConvertCallback<AuthSession> authSessionAfterConvertCallback() {
        return (authSession, table) -> Mono.just(authSession.markPersisted());
    }

    @Bean
    public AfterSaveCallback<AuthSession> authSessionAfterSaveCallback() {
        return (authSession, outboundRow, table) -> Mono.just(authSession.markPersisted());
    }
}

