/*
 * Copyright (c) 2018. Michael Jonker (http://openpoint.ie)
 *
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package io.bisq.business.actions;

import io.bisq.business.Data;
import io.bisq.business.formatters.AccountData;
import io.bisq.business.formatters.Message;
import io.bisq.common.UserThread;
import io.bisq.core.payment.PaymentAccount;
import org.bitcoinj.core.Coin;
import org.json.simple.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class AccountActions extends Data{

    public static List<AccountData> getPaymentAccounts(){

        if (user.getPaymentAccounts().isEmpty()) return new ArrayList<AccountData>();
        return user.getPaymentAccounts().stream().map((PaymentAccount acc) -> {
            AccountData account = new AccountData();
            account.id = acc.getId();
            account.name = acc.getAccountName();
            account.paymentmethod = acc.getPaymentMethod().getId();
            account.currencies = acc.getTradeCurrencies().stream().map((a)->{
                return a.getCode();
            }).collect(Collectors.toList());
            HashMap<String,AccountData.Limit> limit = new HashMap<>();
            acc.getTradeCurrencies().stream().forEach((curr)->{
                Coin Max = Coin.valueOf(accountAgeWitnessService.getMyTradeLimit(acc,curr.getCode()));
                AccountData.Limit l = new AccountData.Limit();
                l.max= Double.parseDouble(Max.toPlainString());
                l.min = 0.001; // TODO how to get min trade amount from account?
                limit.put(curr.getCode(),l);
            });
            account.limits = new JSONObject(limit);
            return account;
        }).collect(Collectors.toList());

    }

    public static Message accountDetail(String id){
        Message message = new Message();

        PaymentAccount account = user.getPaymentAccount(id);
        if(account != null){
            message.success = true;
            message.message = account.getAccountName();
            message.data = account.getPaymentAccountPayload();
        }else{
            message.success = false;
            message.message = "Account \""+id+"\" was not found.";
        }

        return message;
    }

    public static Message deleteAccount(String id) throws ExecutionException, InterruptedException {
        Message message = new Message();

        CompletableFuture<Message> promise = new CompletableFuture<>();
        UserThread.execute(()-> {
            PaymentAccount account = user.getPaymentAccount(id);
            if (account != null) {
                user.removePaymentAccount(account);
                message.success = true;
                message.message = "Account \"" + id + "\" was removed.";
            } else {
                message.success = false;
                message.message = "Account \"" + id + "\" was not found.";
            }
            promise.complete(message);
        });

        return promise.get();
    }
}
