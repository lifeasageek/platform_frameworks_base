/*
 * Copyright (C) 2014 Byoungyoung Lee
 * Copyright (C) 2007 The Android Open Source Project
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

package com.android.internal.os;

import android.os.Process;
import android.util.Slog;
import android.os.SystemClock;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import libcore.io.IoUtils;
import libcore.io.Libcore;
import libcore.io.ErrnoException;
import dalvik.system.Zygote;

public class MorulaInit {
    static class InstanceInfo {
        FileDescriptor readPipeFd = null;
        FileDescriptor writePipeFd = null;
        int pid = -1;
        InstanceInfo(){};
    }

    static ArrayList<InstanceInfo> Instances = new ArrayList<InstanceInfo>();

    private final static int MAX_PREPARE_INSTANCE = 10;
    private final static int MIN_PREPARE_INSTANCE = 1;
    private final static String TAG = "Morula";

    private MorulaInit() {
    }

    // will be executed from parent.
    public static void sendSpecializeInfo(FileDescriptor childPipeFd,
                                          int targetSdkVersion, int uid,
                                          int gid, int[] gids, int debugFlags,
                                          int[][] rlimits, int mountExternal,
                                          String seInfo, String niceName) {
        FileDescriptor writePipeFd = getCurrentInstance().writePipeFd;

        // Send all specialize info via writePipeFd.
        try {
            DataOutputStream os = new DataOutputStream(new FileOutputStream(writePipeFd));

            os.writeInt(childPipeFd != null ? childPipeFd.getInt$() : 0);
            os.writeInt(targetSdkVersion);
            os.writeInt(uid);
            os.writeInt(gid);

            if (gids != null) {
                os.writeInt(gids.length);
                for (int i=0; i<gids.length; i++) {
                  os.writeInt(gids[i]);
                }
            } else {
                os.writeInt(0);
            }

            os.writeInt(debugFlags);
            os.writeInt(mountExternal);
            os.writeUTF(niceName);
            os.close();
            IoUtils.closeQuietly(writePipeFd);

            deleteInstance();
        } catch (IOException ex) {
            Slog.e(TAG, "Failed to write specialize info using pipe.", ex);
        }
    }

    // Will be executed from child.
    public static int recvSpecializeInfoAndApply(int fdNum) {
        int childPipeFdNum = 0;
        int uid = 0;
        int gid = 0;
        int gids_len = 0;
        int gids[] = null;
        int debugFlags = 0;
        // ArrayList<int[]> rlimits; // TODO
        int mountExternal = 0;
        String niceName = null;
        int targetSdkVersion = 0;
        String seInfo = null;

        try {
            FileDescriptor fd = ZygoteInit.createFileDescriptor(fdNum);
            DataInputStream is = new DataInputStream(new FileInputStream(fd));
            childPipeFdNum = is.readInt();

            targetSdkVersion = is.readInt();
            uid = is.readInt();
            gid = is.readInt();
            gids_len = is.readInt();
            gids = new int[gids_len];

            for (int i=0; i<gids_len; i++)
                gids[i] = is.readInt();

            debugFlags = is.readInt();
            mountExternal = is.readInt();
            niceName = is.readUTF();
            is.close();
            IoUtils.closeQuietly(fd);
        } catch (IOException ex) {
            Slog.e(TAG, "Could not read specialize info using fdNum", ex);
        }
        Zygote.noForkButSpecialize(uid, gid, gids, debugFlags, null,
                                   mountExternal, seInfo, niceName);
        return targetSdkVersion;
    }

    public static void main(String[] args) {
        int fdNum;
        int targetSdkVersion;

        // Mimic Zygote preloading.
        ZygoteInit.preload();

        // Parse the arguments.
        fdNum = Integer.parseInt(args[0], 10);
        targetSdkVersion = recvSpecializeInfoAndApply(fdNum);

        try {
            // Launch the application.
            String[] runtimeArgs = new String[args.length - 1];
            System.arraycopy(args, 1, runtimeArgs, 0, runtimeArgs.length);

            RuntimeInit.wrapperInit(targetSdkVersion, runtimeArgs);
        } catch (ZygoteInit.MethodAndArgsCaller caller) {
            caller.run();
        }
        // NEVER REACH HERE
    }

    public static void deleteInstance() {
        // TODO : assertion on the instance size.
        Instances.remove(0);
    }

    public static InstanceInfo getCurrentInstance() {
        // TODO : assertion on the instance size.
        return Instances.get(0);
    }

    public static void ensureMorulaInitInstance() {
        if (Instances.size() >= MIN_PREPARE_INSTANCE)
            return;
        createMorulaInitInstance();
    }

    public static int createMorulaInitInstance() {
        FileDescriptor readPipeFd = null;
        FileDescriptor writePipeFd = null;

        if (Instances.size() >= MAX_PREPARE_INSTANCE)
            return -1;

        try {
            FileDescriptor[] pipeFds = Libcore.os.pipe();
            writePipeFd = pipeFds[1];
            readPipeFd = pipeFds[0];
        } catch (ErrnoException ex) {
            Slog.e(TAG, "createMorulaInitInstance() : Exception creating pipe",
                   ex);
        }

        int pid = Zygote.fork();

        InstanceInfo instance = new InstanceInfo();
        instance.readPipeFd = readPipeFd;
        instance.writePipeFd = writePipeFd;
        instance.pid = pid;
        Instances.add(instance);

        // FIXME : close pipes.
        if (pid == 0) {
            // Child case.
            execApplication(readPipeFd);
            // Should not reach here.
        }
        // Parent case.
        return pid;
    }

    private static void execApplication(FileDescriptor pipeFd) {
        // This is emulating startViaZygote().
        StringBuilder command = new StringBuilder("exec");
        command.append(" /system/bin/app_process /system/bin --application");
        command.append(" com.android.internal.os.MorulaInit ");
        command.append(pipeFd != null ? pipeFd.getInt$() : 0);
        command.append(" android.app.ActivityThread");
        Zygote.execShell(command.toString());

        // SHOULD NOT REACH HERE.
        Slog.e(TAG, "execApplication() : SHOULD NOT REACH HERE");
    }
}
