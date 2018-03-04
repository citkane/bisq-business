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
import io.bisq.business.formatters.BalanceData;
import io.bisq.business.formatters.Message;
import io.bisq.business.formatters.TransactionData;
import io.bisq.business.models.TransactionsListItem;
import io.bisq.common.UserThread;
import io.bisq.core.btc.AddressEntry;
import io.bisq.core.offer.OpenOffer;
import io.bisq.core.trade.Tradable;
import io.bisq.core.trade.Trade;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WalletActions extends Data {
    public static BalanceData getBalance() throws ExecutionException, InterruptedException {

        CompletableFuture<BalanceData> promise = new CompletableFuture<>();
        UserThread.execute(()->{
            BalanceData balance = new BalanceData();
            Coin aBalance = Coin.valueOf(tradeManager.getAddressEntriesForAvailableBalanceStream()
                    .mapToLong(addressEntry -> btcWalletService.getBalanceForAddress(addressEntry.getAddress()).getValue())
                    .sum());
            Coin rBalance = Coin.valueOf(openOfferManager.getObservableList().stream()
                    .map(openOffer -> {
                        final Optional<AddressEntry> addressEntryOptional = btcWalletService.getAddressEntry(openOffer.getId(), AddressEntry.Context.RESERVED_FOR_TRADE);
                        if (addressEntryOptional.isPresent()) {
                            Address address = addressEntryOptional.get().getAddress();
                            return btcWalletService.getBalanceForAddress(address);
                        } else {
                            return null;
                        }
                    })
                    .filter(e -> e != null)
                    .mapToLong(Coin::getValue)
                    .sum());
            Stream<Trade> lockedTrades = Stream.concat(closedTradableManager.getLockedTradesStream(), failedTradesManager.getLockedTradesStream());
            lockedTrades = Stream.concat(lockedTrades, tradeManager.getLockedTradesStream());
            Coin lBalance = Coin.valueOf(lockedTrades
                    .mapToLong(trade -> {
                        final Optional<AddressEntry> addressEntryOptional = btcWalletService.getAddressEntry(trade.getId(), AddressEntry.Context.MULTI_SIG);
                        if (addressEntryOptional.isPresent())
                            return addressEntryOptional.get().getCoinLockedInMultiSig().getValue();
                        else
                            return 0;
                    })
                    .sum());

            balance.available.longval = aBalance.toFriendlyString();
            balance.available.shortval = aBalance.toPlainString();
            balance.reservedForOffers.longval = rBalance.toFriendlyString();
            balance.reservedForOffers.shortval = rBalance.toPlainString();
            balance.lockedInTrades.longval = lBalance.toFriendlyString();
            balance.lockedInTrades.shortval = lBalance.toPlainString();

            promise.complete(balance);
        });

        return promise.get();
    }

    public static Message getFundingAddress() throws ExecutionException, InterruptedException {

        CompletableFuture<Message> promise = new CompletableFuture<>();
        UserThread.execute(()->{
            Message message = new Message();
            message.success = true;
            message.message = "Payment address to fund BISQ trading wallet";
            message.data = btcWalletService.getOrCreateAddressEntry(AddressEntry.Context.AVAILABLE).getAddressString();
            promise.complete(message);
        });

        return promise.get();
    }

    public static List<TransactionsListItem> getTransactionsRaw() {
        Stream<Tradable> concat1 = Stream.concat(openOfferManager.getObservableList().stream(), tradeManager.getTradableList().stream());
        Stream<Tradable> concat2 = Stream.concat(concat1, closedTradableManager.getClosedTradables().stream());
        Stream<Tradable> concat3 = Stream.concat(concat2, failedTradesManager.getFailedTrades().stream());
        Set<Tradable> all = concat3.collect(Collectors.toSet());

        return btcWalletService.getTransactions(false).stream().map(transaction->{
            Optional<Tradable> tradableOptional = all.stream().filter(tradable -> {
                String txId = transaction.getHashAsString();
                if (tradable instanceof OpenOffer)
                    return tradable.getOffer().getOfferFeePaymentTxId().equals(txId);
                else if (tradable instanceof Trade) {
                    Trade trade = (Trade) tradable;
                    boolean isTakeOfferFeeTx = txId.equals(trade.getTakerFeeTxId());
                    boolean isOfferFeeTx = trade.getOffer() != null &&
                            txId.equals(trade.getOffer().getOfferFeePaymentTxId());
                    boolean isDepositTx = trade.getDepositTx() != null &&
                            trade.getDepositTx().getHashAsString().equals(txId);
                    boolean isPayoutTx = trade.getPayoutTx() != null &&
                            trade.getPayoutTx().getHashAsString().equals(txId);

                    boolean isDisputedPayoutTx = disputeManager.getDisputesAsObservableList().stream().anyMatch(
                            dispute -> txId.equals(dispute.getDisputePayoutTxId()) && tradable.getId().equals(dispute.getTradeId())
                    );

                    return isTakeOfferFeeTx || isOfferFeeTx || isDepositTx || isPayoutTx || isDisputedPayoutTx;
                } else
                    return false;
            }).findAny();
            return new TransactionsListItem(transaction,btcWalletService,bsqWalletService,tradableOptional,bsFormatter);
        }).collect(Collectors.toList());
    }

    public static List<TransactionData> getTransactions() throws ExecutionException, InterruptedException {

        return getTransactionsRaw().stream().map((item) -> {
            TransactionData Item = new TransactionData();
            Item.date = item.getDate();
            Item.details = item.getDetails();
            Item.received = item.getReceived();
            Item.direction = item.getDirection();
            Item.address = item.getAddressString();
            Item.id = item.getTxId();
            Item.amount = item.getAmount();
            Item.confirmations = item.getNumConfirmations();
            return Item;
        }).collect(Collectors.toList());
    }
}
