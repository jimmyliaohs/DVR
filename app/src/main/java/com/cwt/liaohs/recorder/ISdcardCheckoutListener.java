package com.cwt.liaohs.recorder;

/**
 * Created by liaohs on 2018/5/2.
 */

public interface ISdcardCheckoutListener {
    void sdcardNoMounted();
    void sdcardStorageEnough();
    void sdcardStorageNotEnough();
}
