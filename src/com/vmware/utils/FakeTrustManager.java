/*
 * ******************************************************
 * Copyright VMware, Inc. 2014. All Rights Reserved.
 * ******************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.vmware.utils;


import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

/**
 * Authentication is handled by using a TrustManager and supplying a host
 * name verifier method. (The host name verifier is declared in the main function.)
 *
 * <b>Do not use this in production code!  It is only for samples.</b>
 */
public class FakeTrustManager implements javax.net.ssl.TrustManager, javax.net.ssl.X509TrustManager {

    /*
     * The following five functions create a minimal Trust Manager.
     */

    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
    }

    public boolean isServerTrusted(java.security.cert.X509Certificate[] certs) {
        return true;
    }

    public boolean isClientTrusted(java.security.cert.X509Certificate[] certs) {
        return true;
    }

    public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType)
        throws java.security.cert.CertificateException {
        return;
    }

    public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType)
        throws java.security.cert.CertificateException {
        return;
    }

    /**
     * Sets up the trust, and needs to be called from the main part of your program.
     * The sample code in the general package all use this function.
     *
     *  @throws Exception
     *             if an exception occurred
     */
    public static void setupTrust() throws Exception {

        // Declare a host name verifier that will automatically enable the connection. The host name verifier is invoked during the SSL handshake.
        HostnameVerifier hostnameVerifier = new HostnameVerifier() {
            public boolean verify(String urlHostName, SSLSession session) {
                return true;
            }
        };

        // Create the trust manager.
        javax.net.ssl.TrustManager[] trustAllCerts = new javax.net.ssl.TrustManager[1];
        javax.net.ssl.TrustManager trustManager = new FakeTrustManager();
        trustAllCerts[0] = trustManager;
        // Create the SSL context
        javax.net.ssl.SSLContext sc = javax.net.ssl.SSLContext.getInstance("SSL");
        // Create the session context
        javax.net.ssl.SSLSessionContext sslsc = sc.getServerSessionContext();
        // Initialize the contexts; the session context takes the trust manager.
        sslsc.setSessionTimeout(0);
        sc.init(null, trustAllCerts, null);
        // Use the default socket factory to create the socket for the secure connection
        javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

        // Set the default host name verifier to enable the connection.
        HttpsURLConnection.setDefaultHostnameVerifier(hostnameVerifier);
    }
}