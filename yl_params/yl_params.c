/*
 * Copyright (C) 2014, The CyanogenMod Project
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

//#define LOG_NDEBUG 0

#define LOG_TAG "yl_params"

#include <cutils/log.h>
#include <fcntl.h>
#include <sys/types.h>
#include <stdbool.h>
#include <stdlib.h>
#include <unistd.h>

#include "yl_params.h"

#define YL_PARAMS_PATH "/dev/yl_params1"

#define YL_PARAM_BLK_SZ 512

#define INIT_MAX_RETRY 5
#define INIT_RETRY_INTERVAL_SEC 1

enum yl_params_index {
    YL_DEVICE = 0,
    YL_CONFIGURATION,
    YL_PRODUCTLINE,
    YL_DYNAMIC,
    YL_GUARD,
    YL_CMDLINE,
    YL_TOUCHSCREEN0,
    YL_TOUCHSCREEN1,
    YL_TOUCHSCREEN2,
    YL_TOUCHSCREEN3,
    YL_RESERVE0,
    YL_RESERVE1,
    YL_PROJECT0,
    YL_PROJECT1,
    YL_PROJECT2,
    YL_PROJECT3,
    YL_FETCH_PASSWD,
    YL_FCT_DIAG,
    YL_RCP,
    YL_RETURNZERO,
    YL_VIRTOS_PASSWD,
    YL_PARAMS_COUNT,
};

static char * yl_params_map[YL_PARAMS_COUNT] = {
    [YL_DEVICE] = "DEVICE",
    [YL_CONFIGURATION] = "CONFIGURATION",
    [YL_PRODUCTLINE] = "PRODUCTLINE",
    [YL_DYNAMIC] = "DYNAMIC",
    [YL_GUARD] = "GUARD",
    [YL_CMDLINE] = "CMDLINE",
    [YL_TOUCHSCREEN0] = "TOUCHSCREEN0",
    [YL_TOUCHSCREEN1] = "TOUCHSCREEN1",
    [YL_TOUCHSCREEN2] = "TOUCHSCREEN2",
    [YL_TOUCHSCREEN3] = "TOUCHSCREEN3",
    [YL_RESERVE0] = "RESERVE0",
    [YL_RESERVE1] = "RESERVE1",
    [YL_PROJECT0] = "PROJECT0",
    [YL_PROJECT1] = "PROJECT1",
    [YL_PROJECT2] = "PROJECT2",
    [YL_PROJECT3] = "PROJECT3",
    [YL_FETCH_PASSWD] = "FETCH_PASSWD",
    [YL_FCT_DIAG] = "FCT_DIAG",
    [YL_RCP] = "RCP",
    [YL_RETURNZERO] = "RETURNZERO",
    [YL_VIRTOS_PASSWD] = "VIRTOS_PASSWD",

};

struct MainDevInfo {
    uint8_t name[8];
    uint8_t Vendor[16];
    uint8_t model[16];
};

struct ConfigurationInfo {
    uint8_t SyncByte[16];
    uint8_t ProductName[16];
    uint8_t HardwareVersionMajor[6];
    uint8_t HardwareVersionAux[6];
    uint8_t HardwareRF_NV[6];
    struct MainDevInfo DevInfo[11];
    uint8_t pad[22];
};

struct DeviceInfo {
    uint8_t SyncByte[16];
    uint8_t ParamVer[2];
    uint8_t Date[6];
    uint8_t CommunicationModel1[16];
    uint8_t CommunicationModel2[16];
    uint8_t ImageSensorModel[16];
    uint8_t SafeboxKey[128];
    uint8_t pad1[128];
    uint8_t Sim1Capacity[2];
    uint8_t Sim2Capacity[2];
    uint8_t Sim3Capacity[2];
    uint8_t NET_CARRIER;
    uint8_t SimSlots;
    uint8_t pad[176];
};

struct ProductlineInfo {
    uint8_t SyncByte[16];
    uint8_t SN[16];
    uint8_t IMEI1[32];
    uint8_t IMEI2[32];
    uint8_t ModuleCalStatus1;
    uint8_t ModuleCalStatus2;
    uint8_t ModuleRFTestStatus1;
    uint8_t ModuleRFTestStatus2;
    uint8_t ModuleCouplingTestStatus1;
    uint8_t ModuleCouplingTestStatus2;
    uint8_t DMtag;
    uint8_t CameraCal;
    uint8_t RPtag;
    uint8_t BatteryTest;
    uint8_t ModuleSoftVersion1[48];
    uint8_t ModuleSoftVersion2[48];
    uint8_t ModuleAudioVersion1[48];
    uint8_t ModuleAudioVersion2[48];
    uint8_t FuseBurnStatus;
    uint8_t MiscStatus[5];
    uint8_t LightProxInfo[8];
    uint8_t AccInfo[8];
    uint8_t PressInfo[8];
    uint8_t SensorReserved1[8];
    uint8_t SensorReserved2[8];
    uint8_t SensorReserved3[8];
    uint8_t DSDS_IMEI[32];
    uint8_t WIFI_MAC[6];
    uint8_t BT_MAC[6];
    uint8_t pad[116];
};

struct yl_params_t {
    struct DeviceInfo device_info;
    struct ConfigurationInfo config_info;
    struct ProductlineInfo product_info;
    bool initialized;
};

static struct yl_params_t yl_params;

static int
yl_wait_ready(void)
{
    uint8_t buf[YL_PARAM_BLK_SZ];
    int i, fd, rc;

    fd = open(YL_PARAMS_PATH, O_RDONLY);
    if (fd < 0) {
        rc = -errno;
        ALOGE("%s:%d: Failed to open %s: %d\n",
                __func__, __LINE__, YL_PARAMS_PATH, rc);
        goto wait_ret;
    }

    memset(buf, 0, YL_PARAM_BLK_SZ);
    memcpy(buf, yl_params_map[YL_DEVICE], strlen(yl_params_map[YL_DEVICE]));
    for (i = 0; i < INIT_MAX_RETRY + 1; i++) {
        /* Attempt a read as a proxy for determining whether MMC is available */
        rc = read(fd, buf, YL_PARAM_BLK_SZ);
        if (rc > 0) {
            ALOGI("%s:%d: yl_params block device ready\n", __func__, __LINE__);
            rc = 0;
            break;
        } else {
            rc = -errno;
            ALOGW("%s:%d: Failed to read: %d, retrying\n", __func__, __LINE__, rc);
            if (i < INIT_MAX_RETRY) {
                sleep(INIT_RETRY_INTERVAL_SEC);
            }
        }
    }

    close(fd);
wait_ret:
    return rc;
}

int
yl_get_param(int param, void *buf, size_t len)
{
    if (!yl_params.initialized) {
        return -EFAULT;
    }

    if (buf == NULL) {
        return -EINVAL;
    }

    switch (param) {
        case YL_PARAM_WLAN_MAC:
            if (len < 6) {
                return -ENOSPC;
            }
            memcpy(buf, yl_params.product_info.WIFI_MAC, 6);
            return 0;
        case YL_PARAM_BT_MAC:
            if (len < 6) {
                return -ENOSPC;
            }
            memcpy(buf, yl_params.product_info.BT_MAC, 6);
            return 0;
        case YL_PARAM_IMEI0:
            if (len < 32) {
                return -ENOSPC;
            }
            memcpy(buf, yl_params.product_info.IMEI1, 32);
            return 0;
        case YL_PARAM_IMEI1:
            if (len < 32) {
                return -ENOSPC;
            }
            memcpy(buf, yl_params.product_info.IMEI2, 32);
            return 0;
    }

    ALOGI("%s: Invalid parameter requested: %d\n", __func__, param);
    return -ENOTTY;
}

int
yl_params_init(void)
{
    uint8_t buf[YL_PARAM_BLK_SZ];
    int rc;
    int fd;

    if (yl_params.initialized) {
        return 0;
    }

    rc = yl_wait_ready();
    if (rc) {
        ALOGE("%s:%d: yl_params block device failed to become ready: %d\n",
                __func__, __LINE__, rc);
        return rc;
    }

    fd = open(YL_PARAMS_PATH, O_RDONLY);
    if (fd < 0) {
        rc = -errno;
        ALOGE("%s:%d: Failed to open %s: %d\n",
                __func__, __LINE__, YL_PARAMS_PATH, rc);
        return rc;
    }

    /* Read device block */
    memset(&yl_params, 0, sizeof(struct yl_params_t));
    memset(buf, 0, YL_PARAM_BLK_SZ);
    memcpy(buf, yl_params_map[YL_DEVICE], strlen(yl_params_map[YL_DEVICE]));
    rc = read(fd, buf, YL_PARAM_BLK_SZ);
    if (rc < 0) {
        rc = -errno;
        ALOGE("%s:%d: Failed to read device block: %d\n",
                __func__, __LINE__, rc);
        goto init_err;
    }
    memcpy(&yl_params.device_info, buf, YL_PARAM_BLK_SZ);

    /* Read config block */
    memset(buf, 0, YL_PARAM_BLK_SZ);
    memcpy(buf, yl_params_map[YL_CONFIGURATION],
            strlen(yl_params_map[YL_CONFIGURATION]));
    rc = read(fd, buf, YL_PARAM_BLK_SZ);
    if (rc < 0) {
        rc = -errno;
        ALOGE("%s:%d: Failed to read config block: %d\n",
                __func__, __LINE__, rc);
        goto init_err;
    }
    memcpy(&yl_params.config_info, buf, YL_PARAM_BLK_SZ);

    /* Read product block */
    memset(buf, 0, YL_PARAM_BLK_SZ);
    memcpy(buf, yl_params_map[YL_PRODUCTLINE],
            strlen(yl_params_map[YL_PRODUCTLINE]));
    rc = read(fd, buf, YL_PARAM_BLK_SZ);
    if (rc < 0) {
        rc = -errno;
        ALOGE("%s:%d: Failed to read product block: %d\n",
                __func__, __LINE__, rc);
        goto init_err;
    }

    memcpy(&yl_params.product_info, buf, YL_PARAM_BLK_SZ);

    yl_params.initialized = true;
    rc = 0;

init_err:
    close(fd);

    return rc;
}
