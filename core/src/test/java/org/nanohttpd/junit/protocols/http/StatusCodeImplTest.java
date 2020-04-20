package org.nanohttpd.junit.protocols.http;

import org.junit.Assert;
import org.junit.Test;
import org.nanohttpd.protocols.http.response.FixedStatusCode;
import org.nanohttpd.protocols.http.response.StatusCode;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/*
 * #%L
 * NanoHttpd-Core
 * %%
 * Copyright (C) 2012 - 2015 nanohttpd
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the nanohttpd nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

public class StatusCodeImplTest {

    @Test
    public void testMessages() {
        // These are values where the name of the enum does not match the status
        // code description.
        // By default you should not need to add any new values to this map if
        // you
        // make the name of the enum name match the status code description.
        Map<StatusCode, String> overrideValues = new HashMap<>();
        overrideValues.put(FixedStatusCode.INTERNAL_ERROR, "500 Internal Server Error");
        overrideValues.put(FixedStatusCode.SWITCH_PROTOCOL, "101 Switching Protocols");
        overrideValues.put(FixedStatusCode.OK, "200 OK");
        overrideValues.put(FixedStatusCode.MULTI_STATUS, "207 Multi-Status");
        overrideValues.put(FixedStatusCode.REDIRECT, "301 Moved Permanently");
        overrideValues.put(FixedStatusCode.REDIRECT_SEE_OTHER, "303 See Other");
        overrideValues.put(FixedStatusCode.RANGE_NOT_SATISFIABLE, "416 Requested Range Not Satisfiable");
        overrideValues.put(FixedStatusCode.UNSUPPORTED_HTTP_VERSION, "505 HTTP Version Not Supported");

        for (FixedStatusCode status : FixedStatusCode.values()) {
            if (overrideValues.containsKey(status)) {
                Assert.assertEquals(overrideValues.get(status), status.getHttpDescription());
            } else {
                Assert.assertEquals(getExpectedMessage(status), status.getHttpDescription());
            }
        }
    }

    private String getExpectedMessage(FixedStatusCode status) {
        String name = status.name().toLowerCase();
        String[] words = name.split("_");
        StringBuilder builder = new StringBuilder();
        builder.append(status.getStatusCode());
        builder.append(' ');

        for (String word : words) {
            builder.append(Character.toUpperCase(word.charAt(0)));
            builder.append(word.substring(1));
            builder.append(' ');
        }

        return builder.toString().trim();
    }

    @Test
    public void testLookup() throws Exception {
        Map<StatusCode, Integer> map = new HashMap<>(FixedStatusCode.values().length);

        map.put(FixedStatusCode.SWITCH_PROTOCOL, 101);

        map.put(FixedStatusCode.OK, 200);
        map.put(FixedStatusCode.CREATED, 201);
        map.put(FixedStatusCode.ACCEPTED, 202);
        map.put(FixedStatusCode.NO_CONTENT, 204);
        map.put(FixedStatusCode.PARTIAL_CONTENT, 206);
        map.put(FixedStatusCode.MULTI_STATUS, 207);

        map.put(FixedStatusCode.REDIRECT, 301);
        map.put(FixedStatusCode.FOUND, 302);
        map.put(FixedStatusCode.REDIRECT_SEE_OTHER, 303);
        map.put(FixedStatusCode.NOT_MODIFIED, 304);
        map.put(FixedStatusCode.TEMPORARY_REDIRECT, 307);

        map.put(FixedStatusCode.BAD_REQUEST, 400);
        map.put(FixedStatusCode.UNAUTHORIZED, 401);
        map.put(FixedStatusCode.FORBIDDEN, 403);
        map.put(FixedStatusCode.NOT_FOUND, 404);
        map.put(FixedStatusCode.METHOD_NOT_ALLOWED, 405);
        map.put(FixedStatusCode.NOT_ACCEPTABLE, 406);
        map.put(FixedStatusCode.REQUEST_TIMEOUT, 408);
        map.put(FixedStatusCode.CONFLICT, 409);
        map.put(FixedStatusCode.GONE, 410);
        map.put(FixedStatusCode.LENGTH_REQUIRED, 411);
        map.put(FixedStatusCode.PRECONDITION_FAILED, 412);
        map.put(FixedStatusCode.PAYLOAD_TOO_LARGE, 413);
        map.put(FixedStatusCode.UNSUPPORTED_MEDIA_TYPE, 415);
        map.put(FixedStatusCode.RANGE_NOT_SATISFIABLE, 416);
        map.put(FixedStatusCode.EXPECTATION_FAILED, 417);
        map.put(FixedStatusCode.TOO_MANY_REQUESTS, 429);
        map.put(FixedStatusCode.INTERNAL_ERROR, 500);
        map.put(FixedStatusCode.NOT_IMPLEMENTED, 501);
        map.put(FixedStatusCode.SERVICE_UNAVAILABLE, 503);
        map.put(FixedStatusCode.UNSUPPORTED_HTTP_VERSION, 505);

        Set<Map.Entry<StatusCode, Integer>> entrySet = map.entrySet();
        for (Map.Entry<StatusCode, Integer> entry : entrySet)
            Assert.assertEquals(entry.getKey().getStatusCode(), (int) entry.getValue());
    }
}
