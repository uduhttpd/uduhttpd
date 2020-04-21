package org.nanohttpd.junit.protocols.http;

import org.junit.Assert;
import org.junit.Test;
import org.nanohttpd.protocols.http.response.DefaultStatusCode;
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
        overrideValues.put(DefaultStatusCode.INTERNAL_ERROR, "500 Internal Server Error");
        overrideValues.put(DefaultStatusCode.SWITCH_PROTOCOL, "101 Switching Protocols");
        overrideValues.put(DefaultStatusCode.OK, "200 OK");
        overrideValues.put(DefaultStatusCode.MULTI_STATUS, "207 Multi-Status");
        overrideValues.put(DefaultStatusCode.REDIRECT, "301 Moved Permanently");
        overrideValues.put(DefaultStatusCode.REDIRECT_SEE_OTHER, "303 See Other");
        overrideValues.put(DefaultStatusCode.RANGE_NOT_SATISFIABLE, "416 Requested Range Not Satisfiable");
        overrideValues.put(DefaultStatusCode.UNSUPPORTED_HTTP_VERSION, "505 HTTP Version Not Supported");

        for (DefaultStatusCode status : DefaultStatusCode.values()) {
            if (overrideValues.containsKey(status)) {
                Assert.assertEquals(overrideValues.get(status), status.getHttpDescription());
            } else {
                Assert.assertEquals(getExpectedMessage(status), status.getHttpDescription());
            }
        }
    }

    private String getExpectedMessage(DefaultStatusCode status) {
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
        Map<StatusCode, Integer> map = new HashMap<>(DefaultStatusCode.values().length);

        map.put(DefaultStatusCode.SWITCH_PROTOCOL, 101);

        map.put(DefaultStatusCode.OK, 200);
        map.put(DefaultStatusCode.CREATED, 201);
        map.put(DefaultStatusCode.ACCEPTED, 202);
        map.put(DefaultStatusCode.NO_CONTENT, 204);
        map.put(DefaultStatusCode.PARTIAL_CONTENT, 206);
        map.put(DefaultStatusCode.MULTI_STATUS, 207);

        map.put(DefaultStatusCode.REDIRECT, 301);
        map.put(DefaultStatusCode.FOUND, 302);
        map.put(DefaultStatusCode.REDIRECT_SEE_OTHER, 303);
        map.put(DefaultStatusCode.NOT_MODIFIED, 304);
        map.put(DefaultStatusCode.TEMPORARY_REDIRECT, 307);

        map.put(DefaultStatusCode.BAD_REQUEST, 400);
        map.put(DefaultStatusCode.UNAUTHORIZED, 401);
        map.put(DefaultStatusCode.FORBIDDEN, 403);
        map.put(DefaultStatusCode.NOT_FOUND, 404);
        map.put(DefaultStatusCode.METHOD_NOT_ALLOWED, 405);
        map.put(DefaultStatusCode.NOT_ACCEPTABLE, 406);
        map.put(DefaultStatusCode.REQUEST_TIMEOUT, 408);
        map.put(DefaultStatusCode.CONFLICT, 409);
        map.put(DefaultStatusCode.GONE, 410);
        map.put(DefaultStatusCode.LENGTH_REQUIRED, 411);
        map.put(DefaultStatusCode.PRECONDITION_FAILED, 412);
        map.put(DefaultStatusCode.PAYLOAD_TOO_LARGE, 413);
        map.put(DefaultStatusCode.UNSUPPORTED_MEDIA_TYPE, 415);
        map.put(DefaultStatusCode.RANGE_NOT_SATISFIABLE, 416);
        map.put(DefaultStatusCode.EXPECTATION_FAILED, 417);
        map.put(DefaultStatusCode.TOO_MANY_REQUESTS, 429);
        map.put(DefaultStatusCode.INTERNAL_ERROR, 500);
        map.put(DefaultStatusCode.NOT_IMPLEMENTED, 501);
        map.put(DefaultStatusCode.SERVICE_UNAVAILABLE, 503);
        map.put(DefaultStatusCode.UNSUPPORTED_HTTP_VERSION, 505);

        Set<Map.Entry<StatusCode, Integer>> entrySet = map.entrySet();
        for (Map.Entry<StatusCode, Integer> entry : entrySet)
            Assert.assertEquals(entry.getKey().getStatusCode(), (int) entry.getValue());
    }
}
