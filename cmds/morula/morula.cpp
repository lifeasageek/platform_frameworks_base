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
