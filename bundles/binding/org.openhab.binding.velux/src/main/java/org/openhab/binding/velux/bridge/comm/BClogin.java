/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velux.bridge.comm;

import org.openhab.binding.velux.bridge.VeluxBridge;

/**
 * Specific bridge communication message supported by the Velux bridge.
 * <P>
 * Message semantic: Communication to authenticate itself, resulting in a return of current bridge state.
 * <P>
 *
 * It defines informations how to send query and receive answer through the
 * {@link org.openhab.binding.velux.bridge.VeluxBridgeProvider VeluxBridgeProvider}
 * as described by the {@link org.openhab.binding.velux.bridge.comm.BridgeCommunicationProtocol
 * BridgeCommunicationProtocol}.
 *
 * @author Guenther Schreiner - Initial contribution.
 * @since 1.13.0
 */
@Deprecated
public class BClogin implements BridgeCommunicationProtocol<BClogin.Response> {

    public static String url = "/api/v1/auth";

    private static String password;

    public BClogin(String pw) {
        BClogin.password = pw;
    }

    /*
     * Message Objects
     */

    static class ParamsLogin {
        @SuppressWarnings("unused")
        private String password;

        private ParamsLogin(String password) {
            this.password = password;
        }
    }

    /**
     * Bridge I/O Request message used by {@link VeluxBridge} for serializing:
     * <P>
     * Resulting JSON:
     * 
     * <pre>
     * {"action":"login","params":{"password":"PASSWORD"}}
     * </pre>
     */
    public static class Request {

        @SuppressWarnings("unused")
        private String action;
        @SuppressWarnings("unused")
        private ParamsLogin params;

        public Request() {
            this.action = "login";
            this.params = new ParamsLogin(BClogin.password);
        }
    }

    /**
     * Bridge I/O Response message used by {@link VeluxBridge} for deserializing with including component access methods
     * <P>
     * <B>Expected JSON (sample):</B>
     *
     * <pre>
     * '{"token": "PHPnfLda71xfGlxoYEOTGQ==", "result": true, "deviceStatus": "IDLE", "data": {}, "errors": [] }'
     * </pre>
     */
    public static class Response {
        private String token;
        private boolean result;
        private String deviceStatus;
        @SuppressWarnings("unused")
        private Object data;
        private String[] errors;

        public String getToken() {
            return token;
        }

        public boolean getResult() {
            return result;
        }

        public String getDeviceStatus() {
            return deviceStatus;
        }

        public String[] getErrors() {
            return errors;
        }
    }

    /*
     * ===========================================================
     * Methods required for interface {@link BridgeCommunicationProtocol}.
     */

    @Override
    public String name() {
        return "authenticate / login";
    }

    @Override
    public String getURL() {
        return url;
    }

    @Override
    public boolean isCommunicationSuccessful(Response response) {
        return response.getResult();
    }

    @Override
    public Class<Response> getClassOfResponse() {
        return Response.class;
    }

    @Override
    public Object getObjectOfRequest() {
        return new Request();
    }

    @Override
    public String getAuthToken(Response response) {
        return response.getToken();
    }

    @Override
    public String getDeviceStatus(Response response) {
        return response.getDeviceStatus();
    }

    @Override
    public String[] getErrors(Response response) {
        return response.getErrors();
    }

}
