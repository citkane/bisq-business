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

package io.bisq.business;

import com.google.inject.Injector;
import io.bisq.business.models.CreateOfferDataModel;
import io.bisq.business.models.TakeOfferDataModel;
import io.bisq.business.util.BSFormatter;
import io.bisq.business.util.validation.BICValidator;
import io.bisq.business.util.validation.IBANValidator;
import io.bisq.business.util.validation.InputValidator;
import io.bisq.common.Clock;
import io.bisq.common.crypto.KeyRing;
import io.bisq.core.alert.AlertManager;
import io.bisq.core.alert.PrivateNotificationManager;
import io.bisq.core.app.BisqEnvironment;
import io.bisq.core.arbitration.ArbitratorManager;
import io.bisq.core.arbitration.DisputeManager;
import io.bisq.core.btc.wallet.BsqWalletService;
import io.bisq.core.btc.wallet.BtcWalletService;
import io.bisq.core.btc.wallet.WalletsManager;
import io.bisq.core.btc.wallet.WalletsSetup;
import io.bisq.core.dao.DaoManager;
import io.bisq.core.filter.FilterManager;
import io.bisq.core.offer.OfferBookService;
import io.bisq.core.offer.OpenOfferManager;
import io.bisq.core.payment.AccountAgeWitnessService;
import io.bisq.core.provider.fee.FeeService;
import io.bisq.core.provider.price.PriceFeedService;
import io.bisq.core.trade.TradeManager;
import io.bisq.core.trade.closed.ClosedTradableManager;
import io.bisq.core.trade.failed.FailedTradesManager;
import io.bisq.core.trade.statistics.TradeStatisticsManager;
import io.bisq.core.user.Preferences;
import io.bisq.core.user.User;
import io.bisq.network.crypto.EncryptionService;
import io.bisq.network.p2p.P2PService;

public class Data {

    public static WalletsManager walletsManager;
    public static WalletsSetup walletsSetup;
    public static BtcWalletService btcWalletService;
    public static BsqWalletService bsqWalletService;
    public static PriceFeedService priceFeedService;
    public static ArbitratorManager arbitratorManager;
    public static P2PService p2PService;
    public static TradeManager tradeManager;
    public static OpenOfferManager openOfferManager;
    public static DisputeManager disputeManager;
    public static Preferences preferences;
    public static User user;
    public static AlertManager alertManager;
    public static PrivateNotificationManager privateNotificationManager;
    public static FilterManager filterManager;
    public static TradeStatisticsManager tradeStatisticsManager;
    public static Clock clock;
    public static FeeService feeService;
    public static DaoManager daoManager;
    public static EncryptionService encryptionService;
    public static KeyRing keyRing;
    public static BisqEnvironment bisqEnvironment;
    public static FailedTradesManager failedTradesManager;
    public static ClosedTradableManager closedTradableManager;
    public static AccountAgeWitnessService accountAgeWitnessService;
    public static BSFormatter bsFormatter;
    public static OfferBookService offerBookService;
    public static Runnable checkIfLocalHostNodeIsRunning;

    public final static Valid valid = new Valid();

    public static Injector injector;

    public static void inject(Injector _injector, Runnable _checkIfLocalHostNodeIsRunning){
        injector = _injector;
        walletsManager = injector.getInstance(WalletsManager.class);
        walletsSetup = injector.getInstance(WalletsSetup.class);
        btcWalletService = injector.getInstance(BtcWalletService.class);
        bsqWalletService = injector.getInstance(BsqWalletService.class);
        priceFeedService = injector.getInstance(PriceFeedService.class);
        arbitratorManager = injector.getInstance(ArbitratorManager.class);
        p2PService = injector.getInstance(P2PService.class);
        tradeManager = injector.getInstance(TradeManager.class);
        openOfferManager = injector.getInstance(OpenOfferManager.class);
        disputeManager = injector.getInstance(DisputeManager.class);
        preferences = injector.getInstance(Preferences.class);
        user = injector.getInstance(User.class);
        alertManager = injector.getInstance(AlertManager.class);
        privateNotificationManager = injector.getInstance(PrivateNotificationManager.class);
        filterManager = injector.getInstance(FilterManager.class);
        tradeStatisticsManager = injector.getInstance(TradeStatisticsManager.class);
        clock = injector.getInstance(Clock.class);
        feeService = injector.getInstance(FeeService.class);
        daoManager = injector.getInstance(DaoManager.class);
        encryptionService = injector.getInstance(EncryptionService.class);
        keyRing = injector.getInstance(KeyRing.class);
        bisqEnvironment = injector.getInstance(BisqEnvironment.class);
        failedTradesManager = injector.getInstance(FailedTradesManager.class);
        closedTradableManager = injector.getInstance(ClosedTradableManager.class);
        accountAgeWitnessService = injector.getInstance(AccountAgeWitnessService.class);
        bsFormatter = injector.getInstance(BSFormatter.class);
        offerBookService = injector.getInstance(OfferBookService.class);
        checkIfLocalHostNodeIsRunning = _checkIfLocalHostNodeIsRunning;

        /* Convenience classes */

        valid.ibanValidator = new IBANValidator();
        valid.bicValidator = new BICValidator();
        valid.inputValidator = new InputValidator();

    }

    public static class Valid{
        public IBANValidator ibanValidator;
        public BICValidator bicValidator;
        public InputValidator inputValidator;

        /*TODO add all validators in this convenience class */
    }
}
