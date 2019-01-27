/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.velux.bridge.slip;

import java.util.Random;

import org.openhab.binding.velux.bridge.common.RunScene;
import org.openhab.binding.velux.bridge.slip.util.KLF200Response;
import org.openhab.binding.velux.bridge.slip.util.Packet;
import org.openhab.binding.velux.things.VeluxKLFAPI.Command;
import org.openhab.binding.velux.things.VeluxKLFAPI.CommandNumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Protocol specific bridge communication supported by the Velux bridge:
 * <B>Run Scene</B>
 * <P>
 * Common Message semantic: Communication with the bridge and (optionally) storing returned information within the class
 * itself.
 * <P>
 * As 3rd level class it defines informations how to send query and receive answer through the
 * {@link org.openhab.binding.velux.bridge.VeluxBridgeProvider VeluxBridgeProvider}
 * as described by the interface {@link org.openhab.binding.velux.bridge.json.JsonBridgeCommunicationProtocol
 * SlipBridgeCommunicationProtocol}.
 * <P>
 * Methods in addition to the mentioned interface:
 * <UL>
 * <LI>{@link #setSceneId} to define the scene to be executed.</LI>
 * </UL>
 *
 * @see RunScene
 * @see SlipBridgeCommunicationProtocol
 *
 * @author Guenther Schreiner - Initial contribution.
 * @since 1.13.0
 */
public class SCrunScene extends RunScene implements SlipBridgeCommunicationProtocol {
    private final Logger logger = LoggerFactory.getLogger(SCrunScene.class);

    private static final String DESCRIPTION = "Run Scene";
    private static final Command COMMAND = Command.GW_ACTIVATE_SCENE_REQ;

    /*
     * ===========================================================
     * Message Content Parameters
     */

    private int reqSessionID = 0;
    private int reqCommandOriginator = 8; // SAAC
    private int reqPriorityLevel = 5; // Comfort Level 2
    private int reqSceneID = -1; // SceneID as one unsigned byte number
    private int reqVelocity = 0; // The product group operates by its default velocity.

    /*
     * ===========================================================
     * Message Objects
     */

    private byte[] requestData;

    /*
     * ===========================================================
     * Result Objects
     */

    private boolean success = false;
    private boolean finished = false;

    /*
     * ===========================================================
     * Constructor Method
     */

    /**
     * Constructor.
     * <P>
     * Initializes the session id {@link #reqSessionID} with a random start value.
     */
    public SCrunScene() {
        logger.debug("SCrunScene(Constructor) called.");
        Random rand = new Random();
        reqSessionID = rand.nextInt(0x0fff);
        logger.debug("SCrunScene(): starting sessions with the random number {}.", reqSessionID);
    }

    /*
     * ===========================================================
     * Methods required for interface {@link BridgeCommunicationProtocol}.
     */

    @Override
    public String name() {
        return DESCRIPTION;
    }

    @Override
    public CommandNumber getRequestCommand() {
        success = false;
        finished = false;
        logger.trace("getRequestCommand() returns {} ({}).", COMMAND.name(), COMMAND.getCommand());
        return COMMAND.getCommand();
    }

    @Override
    public byte[] getRequestDataAsArrayOfBytes() {
        Packet request = new Packet(new byte[6]);
        reqSessionID = (reqSessionID + 1) & 0xffff;
        request.setTwoByteValue(0, reqSessionID);
        request.setOneByteValue(2, reqCommandOriginator);
        request.setOneByteValue(3, reqPriorityLevel);
        request.setOneByteValue(4, reqSceneID);
        request.setOneByteValue(5, reqVelocity);
        requestData = request.toByteArray();
        logger.debug("getRequestCommand() returns {} ({}) with  SceneID {}.", COMMAND.name(), COMMAND.getCommand(),
                reqSceneID);
        return requestData;
    }

    @Override
    public void setResponse(short responseCommand, byte[] thisResponseData) {
        KLF200Response.introLogging(logger, responseCommand, thisResponseData);
        success = false;
        finished = false;
        Packet responseData = new Packet(thisResponseData);
        switch (Command.get(responseCommand)) {
            case GW_ACTIVATE_SCENE_CFM:
                if (!KLF200Response.isLengthValid(logger, responseCommand, thisResponseData, 3)) {
                    finished = true;
                    break;
                }
                int cfmStatus = responseData.getOneByteValue(0);
                int cfmSessionID = responseData.getTwoByteValue(1);
                switch (cfmStatus) {
                    case 0:
                        logger.trace("setResponse(): returned status: OK - Request accepted.");
                        if (!KLF200Response.check4matchingSessionID(logger, cfmSessionID, reqSessionID)) {
                            finished = true;
                        }
                        break;
                    case 1:
                        finished = true;
                        logger.trace("setResponse(): returned status: Error – Invalid parameter.");
                        break;
                    case 2:
                        finished = true;
                        logger.trace("setResponse(): returned status: Error – Request rejected.");
                        break;
                    default:
                        finished = true;
                        logger.warn("setResponse({}): returned status={} (Reserved/unknown).",
                                Command.get(responseCommand).toString(), cfmStatus);
                        break;
                }
                break;

            case GW_COMMAND_RUN_STATUS_NTF:
                logger.warn("setResponse(): received GW_COMMAND_RUN_STATUS_NTF, continuing.");
                break;

            case GW_COMMAND_REMAINING_TIME_NTF:
                logger.warn("setResponse(): received GW_COMMAND_REMAINING_TIME_NTF, continuing.");
                break;

            case GW_SESSION_FINISHED_NTF:
                logger.warn("setResponse(): received GW_SESSION_FINISHED_NTF.");
                success = true;
                finished = true;
                break;

            default:
                KLF200Response.errorLogging(logger, responseCommand);
                finished = true;
        }
        KLF200Response.outroLogging(logger, success, finished);
    }

    @Override
    public boolean isCommunicationFinished() {
        return finished;
    }

    @Override
    public boolean isCommunicationSuccessful() {
        return success;
    }

    /*
     * ===========================================================
     * Methods in addition to the interface {@link BridgeCommunicationProtocol}
     * and the abstract class {@link RunScene}
     */

    @Override
    public SCrunScene setSceneId(int sceneId) {
        logger.trace("setProductId({}) called.", sceneId);
        this.reqSceneID = sceneId;
        return this;
    }

}
