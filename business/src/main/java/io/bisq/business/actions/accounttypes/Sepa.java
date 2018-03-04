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

package io.bisq.business.actions.accounttypes;

import io.bisq.business.Data;
import io.bisq.business.formatters.Message;
import io.bisq.common.locale.*;
import io.bisq.core.payment.SepaAccount;

import java.util.List;

import static java.util.stream.Collectors.toList;

public class Sepa extends Data {
    public static Message newSepa(String holderName, String iban, String bic, String countryCode, String currencyCode){
        Message message = new Message();

        SepaAccount account = new SepaAccount();
        account.init();

        FiatCurrency currency;
        if(CurrencyUtil.isFiatCurrency(currencyCode)){
            currency = new FiatCurrency(currencyCode);
        }else{
            message.success = false;
            message.message = "\""+currencyCode+"\" is not a valid currency code";
            return message;
        }

        Country country;
        List<Country> clist = CountryUtil.getAllSepaCountries().stream().filter((c) -> c.code.equals(countryCode)).collect(toList());
        if (!clist.isEmpty()) {
            country = clist.get(0);
        } else {
            message.success = false;
            message.message = "\""+countryCode+"\" is not a valid SEPA country code";
            return message;
        }
        if(!valid.ibanValidator.validate(iban).isValid){
            message.success = false;
            message.message = "IBAN: "+valid.ibanValidator.validate(iban).errorMessage;
            return message;
        };
        if(!valid.bicValidator.validate(bic).isValid){
            message.success = false;
            message.message = "BIC: "+valid.bicValidator.validate(bic).errorMessage;
            return message;
        };
        if(!valid.inputValidator.validate(holderName).isValid){
            message.success = false;
            message.message = "HOLDERNAME: "+valid.inputValidator.validate(holderName).errorMessage;
            return message;
        }

        account.setHolderName(holderName);
        account.setBic(bic);
        account.setIban(iban);
        account.setCountry(country);
        account.addCurrency(currency);
        account.setSelectedTradeCurrency(currency);
        String method = Res.get(account.getPaymentMethod().getId());
        account.setAccountName(method.concat(" (").concat(currency.getCode()).concat("/").concat(country.code).concat("): ").concat(iban));

        message.data = account.paymentAccountPayload;
        if(
                user.getPaymentAccounts().isEmpty() ||
                        user.getPaymentAccounts().stream().filter((a)->a.getAccountName().equals(account.getAccountName())).collect(toList()).isEmpty()
                ){
            user.addPaymentAccount(account);
            preferences.addFiatCurrency(currency);

            message.success = true;
            message.message = "New SEPA account \""+account.getAccountName()+"\" was created";
        }else{
            message.success = false;
            message.message = "Account with name \""+account.getAccountName()+"\" already exists";
        }

        return message;
    }
}
