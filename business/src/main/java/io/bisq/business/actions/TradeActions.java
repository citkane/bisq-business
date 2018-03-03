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
import io.bisq.business.formatters.Message;
import io.bisq.business.formatters.TradeData;
import io.bisq.common.UserThread;
import io.bisq.core.btc.AddressEntry;
import io.bisq.core.trade.BuyerTrade;
import io.bisq.core.trade.SellerTrade;
import io.bisq.core.trade.Trade;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;

public class TradeActions extends Data {

    private static Message isTrade(String tradeId){
        Message message = new Message();
        if(!tradeManager.getTradeById(tradeId).isPresent()){
            message.success = false;
            message.message = "Could not find trade with id: "+tradeId;
            return message;
        }
        message.success = true;
        return message;
    }

    public static List<TradeData> listTrades(String tradeId){
        return tradeManager.getTradableList().stream().filter((trade)-> {
            return tradeId == null || trade.getId().matches(tradeId);
        }).map((trade)->{
            return TradeData.Map(trade);
        }).collect(toList());
    }

    public static Message getContract(String tradeId) throws ParseException {
        Message message = isTrade(tradeId);
        if(message.success.equals(false)) return message;

        message.message = "Found trade with id: "+tradeId;
        message.data = (JSONObject) new JSONParser().parse(tradeManager.getTradeById(tradeId).get().getContractAsJson());
        return message;
    }

    public static Message paymentStarted(String tradeId) throws ExecutionException, InterruptedException {
        Message message = isTrade(tradeId);
        if(message.success.equals(false)) return message;

        Trade trade = tradeManager.getTradeById(tradeId).get();

        if(trade instanceof BuyerTrade == false){
            message.success = false;
            message.message = "Not a buyer trade";
            return message;
        }
        if(!trade.getState().getPhase().toString().matches("DEPOSIT_CONFIRMED")){
            message.success = false;
            message.message = "Incorrect phase: "+trade.getState().getPhase();
            return message;
        }

        CompletableFuture<Message> promise = new CompletableFuture<>();
        UserThread.execute(()-> {
            ((BuyerTrade) trade).onFiatPaymentStarted(() -> {
                message.success = true;
                message.message = "Start of payment confirmed";
                promise.complete(message);
            }, (err) -> {
                message.success = false;
                message.message = err;
                promise.complete(message);
            });
        });

        return promise.get();
    }

    public static Message paymentReceived(String tradeId) throws ExecutionException, InterruptedException {
        Message message = isTrade(tradeId);
        if(message.success.equals(false)) return message;

        Trade trade = tradeManager.getTradeById(tradeId).get();

        if(trade instanceof SellerTrade == false){
            message.success = false;
            message.message = "Not a seller trade";
            return message;
        }
        if(!trade.getState().getPhase().toString().matches("FIAT_SENT")){
            message.success = false;
            message.message = "Incorrect phase: "+trade.getState().getPhase();
            return message;
        }

        CompletableFuture<Message> promise = new CompletableFuture<>();
        UserThread.execute(()-> {
            ((SellerTrade) trade).onFiatPaymentReceived(() -> {
                message.success = true;
                message.message = "Payment has been confirmed as received";
                promise.complete(message);
            }, (err) -> {
                message.success = false;
                message.message = err;
                promise.complete(message);
            });
        });

        return promise.get();
    }

    public static Message moveToBisqWallet(String tradeId) throws ExecutionException, InterruptedException {
        Message message = isTrade(tradeId);
        if(message.success.equals(false)) return message;

        Trade trade = tradeManager.getTradeById(tradeId).get();

        if(!trade.getState().getPhase().toString().matches("PAYOUT_PUBLISHED")){
            message.success = false;
            message.message = "Incorrect phase: "+trade.getState().getPhase();
            return message;
        }

        CompletableFuture<Message> promise = new CompletableFuture<>();
        UserThread.execute(()->{
            btcWalletService.swapTradeEntryToAvailableEntry(tradeId, AddressEntry.Context.TRADE_PAYOUT);
            tradeManager.addTradeToClosedTrades(trade);
            message.success = true;
            message.message = "Payout was moved to BISQ wallet";
            promise.complete(message);
        });

        return promise.get();
    }
}
