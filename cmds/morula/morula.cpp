/*
 * Copyright (C) 2014 Byoungyoung Lee
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "appproc"

#include <binder/IPCThreadState.h>
#include <utils/Log.h>

#include <cutils/sockets.h>
#include <stdio.h>
#include <unistd.h>

using namespace android;

int main(void)
{
    char buffer[256];
    int sockfd;
    int numBytes;

    sockfd = socket_local_client("zygote",
                                 ANDROID_SOCKET_NAMESPACE_RESERVED,
                                 SOCK_STREAM);
    if (sockfd < 0) {
        printf("ERROR : cannot connect to the serer\n");
        return 1;
    }

    sprintf(buffer, "%d\n--prepare-instance\n", 1);
    numBytes = write(sockfd, buffer, strlen(buffer));
    close(sockfd);
}
