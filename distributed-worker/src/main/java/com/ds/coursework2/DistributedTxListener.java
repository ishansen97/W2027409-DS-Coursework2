package com.ds.coursework2;

public interface DistributedTxListener {
    void onGlobalCommit();

    void onGlobalAbort();
}
