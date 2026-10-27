package com.markvoronin.networkswitch;

interface IShizukuController {
    boolean compatibilityCheck(int subId);
    int getCurrentNetworkMode(int subId);
    boolean setNetworkMode(int subId, int networkMode);
    void destroy();
}
