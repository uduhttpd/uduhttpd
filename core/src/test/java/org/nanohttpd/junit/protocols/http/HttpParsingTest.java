/*
 * Copyright (C) 2012 - 2016 nanohttpd (HttpParsingTest.java)
 * Copyright (C) 2020 uduhttpd
 *
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
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package org.nanohttpd.junit.protocols.http;

import org.junit.Test;
import org.nanohttpd.protocols.http.NanoHTTPD;

import static junit.framework.Assert.assertEquals;

public class HttpParsingTest extends HttpServerTest {

    @Test
    public void testMultibyteCharacterSupport() throws Exception {
        String expected = "Chinese \u738b Letters";
        String input = "Chinese+%e7%8e%8b+Letters";
        assertEquals(expected, NanoHTTPD.decodePercent(input));
    }

    @Test
    public void testNormalCharacters() throws Exception {
        for (int i = 0x20; i < 0x80; i++) {
            String hex = Integer.toHexString(i);
            String input = "%" + hex;
            char expected = (char) i;
            assertEquals("" + expected, NanoHTTPD.decodePercent(input));
        }
    }

    @Test
    public void testPlusInQueryParams() throws Exception {
        assertEquals("foo bar", NanoHTTPD.decodePercent("foo+bar"));
    }
}
