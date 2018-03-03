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
import io.bisq.core.payment.payload.PaymentAccountPayload;
import io.bisq.core.trade.BuyerTrade;
import io.bisq.core.trade.Contract;
import io.bisq.core.trade.Trade;
import io.bisq.network.p2p.NodeAddress;
import org.bitcoinj.core.Coin;

import java.math.BigDecimal;
import java.util.Date;

public class TradeData extends Data{
    public String id;
    public Date date;
    public Boolean isMyOffer;
    public Boolean fiat;
    public String type;
    public String state;
    public String phase;
    public NodeAddress mediator;
    public NodeAddress arbitrator;
    public Peer peer = new Peer();
    public double deposit;
    public String paymentMethod;
    public Money money = new Money();

    public class Peer{
        public NodeAddress nodeAddress;
        public PaymentAccountPayload account;
    }
    public class Money{
        public String code;
        public BigDecimal price;
        public double amount;
        public double volume;
    }
    public static TradeData Map(Trade trade) {
        TradeData tr = new TradeData();
        Contract co = trade.getContract();

        tr.id = trade.getId();
        tr.date = trade.getTakeOfferDate();
        tr.isMyOffer = trade.getOffer().isMyOffer(keyRing);
        tr.fiat = !co.getOfferPayload().getPaymentMethodId().matches("BLOCK_CHAINS");
        tr.type = trade instanceof BuyerTrade ?"BuyerTrade":"SellerTrade";
        tr.state = trade.getState().toString();
        tr.phase = trade.getState().getPhase().toString();
        tr.mediator = co.getMediatorNodeAddress();
        tr.arbitrator = co.getArbitratorNodeAddress();
        tr.peer.nodeAddress = tr.type.matches("BuyerTrade")?co.getSellerNodeAddress():co.getBuyerNodeAddress();
        tr.peer.account = tr.isMyOffer?co.getTakerPaymentAccountPayload():co.getMakerPaymentAccountPayload();
        tr.paymentMethod = tr.fiat?co.getOfferPayload().getPaymentMethodId():co.getOfferPayload().getBaseCurrencyCode();
        tr.money.code = tr.fiat?co.getOfferPayload().getCounterCurrencyCode():co.getOfferPayload().getBaseCurrencyCode();
        if(tr.fiat){
            tr.money.price = BigDecimal.valueOf(Double.parseDouble(co.getTradePrice().toPlainString()));
        }else{
            tr.money.price = BigDecimal.valueOf(Double.parseDouble(bsFormatter.reciprocal(co.getTradePrice().toPlainString())));
        }
        tr.money.amount = Double.parseDouble(co.getTradeAmount().toPlainString());
        tr.money.volume = tr.money.price.doubleValue()*tr.money.amount;
        Coin deposit = tr.type.matches("BuyerTrade")?trade.getOffer().getBuyerSecurityDeposit():trade.getOffer().getSellerSecurityDeposit();
        tr.deposit = Double.parseDouble(deposit.toPlainString());
        return tr;
    }
}
