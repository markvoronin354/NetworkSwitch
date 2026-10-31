package com.markvoronin.networkswitch;

interface IRootController {
    boolean compatibilityCheck(int subId);
    int getCurrentNetworkMode(int subId);
    boolean setNetworkMode(int subId, int networkMode);
    boolean reconnectNetwork();
}
