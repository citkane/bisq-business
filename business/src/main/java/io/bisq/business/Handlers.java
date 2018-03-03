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

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

public class Handlers extends Data {
    public static class error{
        @ResponseStatus(code = HttpStatus.NOT_FOUND)
        public static final class NotFound extends Exception {
            public NotFound() {}
        }

        @ResponseStatus(code = HttpStatus.BAD_REQUEST)
        public static final class BadRequest extends Exception {
            public BadRequest() {}
        }

        @ResponseStatus(code = HttpStatus.INTERNAL_SERVER_ERROR)
        public static final class ServerError extends Exception {
            public ServerError() {}
        }
        @ResponseStatus(code = HttpStatus.PRECONDITION_REQUIRED, reason = "BISQ terms and conditions have not been accepted. hint: go to 'Preferences")
        public static final class TACerror extends Exception {
            public TACerror() {}
        }
    }

    public static void checkErrors() throws Exception {
        if(!preferences.isTacAccepted()) throw new error.TACerror();
        if(user.getPaymentAccounts() == null) throw new error.ServerError();

        /*TODO identify and add all conditions which should prevent Bisq node from operating */
    }
}
