/*
 * Copyright (c) 2018 WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.ballerinalang.stdlib.socket.tcp;

import org.ballerinalang.jvm.api.BErrorCreator;
import org.ballerinalang.jvm.api.BStringUtils;
import org.ballerinalang.jvm.api.BValueCreator;
import org.ballerinalang.jvm.api.values.BError;
import org.ballerinalang.jvm.api.values.BObject;
import org.ballerinalang.stdlib.socket.SocketConstants;

import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.ballerinalang.stdlib.socket.SocketConstants.CLIENT;
import static org.ballerinalang.stdlib.socket.SocketConstants.ErrorType.GenericError;
import static org.ballerinalang.stdlib.socket.SocketConstants.ID;
import static org.ballerinalang.stdlib.socket.SocketConstants.LOCAL_ADDRESS;
import static org.ballerinalang.stdlib.socket.SocketConstants.LOCAL_PORT;
import static org.ballerinalang.stdlib.socket.SocketConstants.REMOTE_ADDRESS;
import static org.ballerinalang.stdlib.socket.SocketConstants.REMOTE_PORT;
import static org.ballerinalang.stdlib.socket.SocketConstants.SOCKET_KEY;
import static org.ballerinalang.stdlib.socket.SocketConstants.SOCKET_PACKAGE_ID;
import static org.ballerinalang.stdlib.socket.SocketConstants.SOCKET_SERVICE;

/**
 * Represents the util functions of Socket operations.
 *
 * @since 0.985.0
 */
public class SocketUtils {

    private SocketUtils() {
    }

    /**
     * Create Generic socket error with given error message.
     *
     * @param errMsg the error message
     * @return BError instance which contains the error details
     */
    public static BError createSocketError(String errMsg) {
        return BErrorCreator.createDistinctError(GenericError.errorType(), SOCKET_PACKAGE_ID,
                                                 BStringUtils.fromString(errMsg));
    }

    /**
     * Create socket error with given error type and message.
     *
     * @param type   the error type which cause for this error
     * @param errMsg the error message
     * @return BError instance which contains the error details
     */
    public static BError createSocketError(SocketConstants.ErrorType type, String errMsg) {
        return BErrorCreator.createDistinctError(type.errorType(), SOCKET_PACKAGE_ID, BStringUtils.fromString(errMsg));
    }

    /**
     * Create a `Caller` object that associated with the given SocketChannel.
     *
     * @param socketService {@link SocketService} instance that contains SocketChannel and resource map
     * @return 'Caller' object
     */
    static BObject createClient(SocketService socketService) {
        Object[] args = new Object[] { null };
        final BObject caller = BValueCreator.createObjectValue(SOCKET_PACKAGE_ID, CLIENT, args);
        caller.addNativeData(SOCKET_SERVICE, socketService);
        SocketChannel client = null;
        // An error can be thrown during the onAccept function. So there is a possibility of client not
        // available at that time. Hence the below null check.
        if (socketService.getSocketChannel() != null) {
            client = (SocketChannel) socketService.getSocketChannel();
        }
        if (client != null) {
            caller.addNativeData(SOCKET_KEY, client);
            Socket socket = client.socket();
            caller.set(BStringUtils.fromString(REMOTE_PORT), socket.getPort());
            caller.set(BStringUtils.fromString(LOCAL_PORT), socket.getLocalPort());
            caller.set(BStringUtils.fromString(REMOTE_ADDRESS), socket.getInetAddress().getHostAddress());
            caller.set(BStringUtils.fromString(LOCAL_ADDRESS), socket.getLocalAddress().getHostAddress());
            caller.set(BStringUtils.fromString(ID), client.hashCode());
        }
        return caller;
    }

    /**
     * This will return a byte array that only contains the data from ByteBuffer.
     * This will not copy any unused byte from ByteBuffer.
     *
     * @param content {@link ByteBuffer} with content
     * @return a byte array
     */
    public static byte[] getByteArrayFromByteBuffer(ByteBuffer content) {
        int contentLength = content.position();
        byte[] bytesArray = new byte[contentLength];
        content.flip();
        content.get(bytesArray, 0, contentLength);
        return bytesArray;
    }

    /**
     * This will try to shutdown executor service gracefully.
     *
     * @param executorService {@link ExecutorService} that need shutdown
     */
    public static void shutdownExecutorGracefully(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(1, TimeUnit.MINUTES)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executorService.shutdownNow();
        }
    }

    /**
     * This will shutdown executor immediately.
     *
     * @param executorService {@link ExecutorService} that need shutdown
     */
    public static void shutdownExecutorImmediately(ExecutorService executorService) {
        executorService.shutdownNow();
    }
}
