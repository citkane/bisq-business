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
import io.bisq.business.formatters.OfferData;
import io.bisq.common.UserThread;
import io.bisq.core.offer.Offer;
import io.bisq.core.offer.OfferPayload;
import io.bisq.core.offer.OpenOffer;
import io.bisq.core.payment.PaymentAccount;
import org.bitcoinj.core.Coin;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static io.bisq.business.formatters.OfferData.*;
import static java.util.stream.Collectors.toList;

public class OfferActions extends Data {

    public static FeesData getFees(String offerId) {


        List<FeesData> list = offerBookService.getOffers().stream().filter(
                offer -> offer.getId().equals(offerId)
        ).map((offer) -> {
            try {
                CompletableFuture<Coin> promise = new CompletableFuture<>();
                UserThread.execute(()-> {
                    models.takeOffer.getTxFee(offer,promise);
                });
                Coin txFee = promise.get();
                return MapFees(offer,txFee);
            } catch (ExecutionException | InterruptedException e) {
                return new FeesData();
            }
        }).collect(toList());
        if(list.isEmpty()) return new FeesData();
        return list.get(0);
    }

    public static Message takeOffer(String offerId, String accountId, BigDecimal Amount) throws ExecutionException, InterruptedException {
        Message message = new Message();
        Offer offer;
        PaymentAccount account;

        List<Offer> oList = offerBookService.getOffers().stream().filter(
                o->o.getId().equals(offerId)
        ).collect(toList());

        if(oList.isEmpty()){
            message.success = false;
            message.message = "Offer with id "+offerId+" was not found";
            return message;
        }else{
            offer = oList.get(0);
        }
        if(offer.isMyOffer(keyRing)){
            message.success = false;
            message.message = "Offer is your own";
            return message;
        }
        if(user.getPaymentAccounts().isEmpty()){
            message.success = false;
            message.message = "No available payment accounts were found";
            return message;
        }
        List<PaymentAccount> aList = user.getPaymentAccounts().stream().filter(
                a->a.getId().equals(accountId)
        ).collect(toList());

        if(aList.isEmpty()){
            message.success = false;
            message.message = "Account with id "+accountId+" was not found";
            return message;
        }else{
            account = aList.get(0);
        }
        if (Amount == null) Amount = new BigDecimal(offer.getAmount().longValue());

        Long am = Amount.multiply(new BigDecimal(100000000)).longValue();
        Coin amount = Coin.valueOf(am);

        CompletableFuture<Message> promise = new CompletableFuture<>();
        UserThread.execute(()-> {
            models.takeOffer.preflight(offer,account,promise);
        });
        message = promise.get();
        if(!message.success) return message;
        CompletableFuture<Message> promise2 = new CompletableFuture<>();
        UserThread.execute(()-> {
            models.takeOffer.commit(offer,account,amount,promise2);
        });

        return promise2.get();
    }

    public static Message createOffer(
            String paymentAccountId,
            String direction,
            BigDecimal Amount,
            BigDecimal MinAmount,
            String priceModel,
            BigDecimal tPrice,
            boolean commit
    ) throws ExecutionException, InterruptedException {
        Message message = new Message();
        PaymentAccount paymentAccount = user.getPaymentAccount(paymentAccountId);
        if(paymentAccount == null){
            message.success = false;
            message.message = "Payment account was not found";
            return message;
        }

        if(MinAmount == null) MinAmount = BigDecimal.ZERO;
        final long amount = Amount.multiply(new BigDecimal(100000000)).longValue();
        long minAmountTemp = MinAmount.multiply(new BigDecimal(100000000)).longValue();
        if(minAmountTemp  == 0) minAmountTemp  = amount;
        final long minAmount = minAmountTemp;

        OfferPayload.Direction dir = direction.equals("BUY")?OfferPayload.Direction.BUY:OfferPayload.Direction.SELL;

        if(paymentAccount.getTradeCurrencies().isEmpty()){
            message.success = false;
            message.message = "Could not find a currency for the account";
            return message;
        };

        boolean fiat = !paymentAccount.getPaymentAccountPayload().getPaymentMethodId().matches("BLOCK_CHAINS");
        if(!fiat && !priceModel.equals("PERCENTAGE")){
            String foo = bsFormatter.reciprocal(String.valueOf(tPrice));
            tPrice = new BigDecimal(foo);
        }

        BigDecimal price = tPrice;
        double margin = price.divide(new BigDecimal(100),8, RoundingMode.CEILING).doubleValue();

        CompletableFuture<Message> promise = new CompletableFuture<>();
        UserThread.execute(()->{
            models.createOffer.preflight(priceModel,Coin.valueOf(amount),Coin.valueOf(minAmount),margin,paymentAccount,dir,commit,promise);
        });

        message = promise.get();
        if(!commit || !message.success) return message;

        CompletableFuture<Message> promise2 = new CompletableFuture<>();
        Offer offer = (Offer) message.data;
        UserThread.execute(()->{
            models.createOffer.commit(offer,promise2);
        });
        return promise2.get();
    }
    public static List<OfferData> listOpenOffers(String currency){
        return offerBookService.getOffers().stream().filter(
                offer->currency == null || offer.getCurrencyCode().equals(currency)
        ).map((offer)-> OfferData.Map(offer)).collect(toList());
    }

    public static Message offerById(String offerId){
        Message message = new Message();
        List<OfferData> list = offerBookService.getOffers().stream().filter(
                offer->offer.getId().equals(offerId)
        ).map((offer)->{
            return OfferData.Map(offer);
        }).collect(toList());

        if(list.isEmpty()){
            message.success=false;
            message.message="Offer "+offerId+" was not found";
            return message;
        }
        message.success = true;
        message.message = offerId;
        message.data = list.get(0);
        return message;
    }

    public static Message removeOffer(String offerId){
        Message message = new Message();
        Optional<OpenOffer> toDelete = openOfferManager.getOpenOfferById(offerId);
        if(!toDelete.isPresent()){
            message.message = "Offer "+offerId+" is not available for deletion.";
            message.success = false;
            return message;
        }
        OpenOffer Delete = toDelete.get();
        openOfferManager.removeOpenOffer(Delete,()->{
            message.message = "Offer " + offerId + " was removed.";
        },(err)->{
            message.message = "Error: "+err;
            message.success = false;
        });
        return message;
    }
}
