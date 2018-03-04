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

package io.bisq.business.formatters;

import io.bisq.business.Data;
import io.bisq.core.offer.Offer;
import io.bisq.core.offer.OfferPayload;
import org.bitcoinj.core.Coin;
import org.bitcoinj.utils.Fiat;

public class OfferData extends Data {
    public String id;
    public long date;
    public String paymentMethod;
    public String direction;
    public Currencies currencies = new Currencies();
    public Money money = new Money();
    public Boolean isMyOffer;
    public String buyerSecurityDeposit;
    public String makerFee;
    public String makerNodeAddress;
    public String error;

    public class Currencies{
        public String currency;
        public String counterCurrency;
        public String baseCurrency;
    };
    public class Money{
        public double amount;
        public double minAmount;
        public Boolean useMarketPrice;
        public String marketPriceMargin;
        public double price;
        public double volume;
    };

    public static class FeesData{
        public double buyerSecurityDeposit;
        public String buyerPercent;
        public double sellerSecurityDeposit;
        public String sellerPercent;
        public double minerFee;
        public String minerPercent;
        public double transactionFee;
        public String transactionPercent;
    }

    public static OfferData Map(Offer offer){
        OfferPayload op = offer.getOfferPayload();
        OfferData offr = new OfferData();
        boolean fiat = offer.getPrice().getMonetary() instanceof Fiat;

        offr.id = op.getId();
        offr.date = op.getDate();
        offr.currencies.currency = op.getCurrencyCode();
        offr.currencies.counterCurrency = op.getCounterCurrencyCode();
        offr.currencies.baseCurrency = op.getBaseCurrencyCode();
        offr.paymentMethod = op.getPaymentMethodId();
        offr.direction = offer.getDirection().toString();
        offr.money.amount = Double.parseDouble(offer.getAmount().toPlainString());
        offr.money.minAmount = Double.parseDouble(offer.getMinAmount().toPlainString());
        offr.money.useMarketPrice = op.isUseMarketBasedPrice();
        offr.money.marketPriceMargin = bsFormatter.formatToPercent(offer.getMarketPriceMargin());
        offr.money.price = Double.parseDouble(bsFormatter.formatPrice(offer.getPrice()));
        if(!fiat) offr.money.price = Double.parseDouble(bsFormatter.reciprocal(String.valueOf(offr.money.price)));
        offr.money.volume = Double.parseDouble(bsFormatter.formatVolume(offer.getVolume()));
        offr.isMyOffer = offer.isMyOffer(keyRing);
        offr.buyerSecurityDeposit = offer.getBuyerSecurityDeposit().toPlainString();
        offr.makerFee = offer.getMakerFee().toPlainString();
        offr.makerNodeAddress = offer.getMakerNodeAddress().getFullAddress();
        offr.error = offer.getErrorMessage();

        return offr;
    }

    public static FeesData MapFees(Offer offer, Coin txFee){
        FeesData fees = new FeesData();
        OfferData offr = Map(offer);

        fees.buyerSecurityDeposit = Double.parseDouble(offer.getBuyerSecurityDeposit().toPlainString());
        fees.buyerPercent = bsFormatter.formatToPercentWithSymbol(fees.buyerSecurityDeposit/offr.money.amount);
        fees.sellerSecurityDeposit = Double.parseDouble(offer.getSellerSecurityDeposit().toPlainString());
        fees.sellerPercent = bsFormatter.formatToPercentWithSymbol(fees.sellerSecurityDeposit/offr.money.amount);
        fees.minerFee = Double.parseDouble(txFee.toPlainString());
        fees.minerPercent = bsFormatter.formatToPercentWithSymbol(fees.minerFee/offr.money.amount);
        fees.transactionFee = Double.parseDouble(offer.getMakerFee().toPlainString());
        fees.transactionPercent = bsFormatter.formatToPercentWithSymbol(fees.transactionFee/offr.money.amount);

        return fees;
    }
}
