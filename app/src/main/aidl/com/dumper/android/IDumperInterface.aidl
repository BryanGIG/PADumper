// IDumperInterface.aidl
package com.dumper.android;

// Declare any non-default types here with import statements
import com.dumper.android.dumper.process.ProcessData;
import com.dumper.android.dumper.DumperConfig;

interface IDumperInterface {
    List<ProcessData> getListProcess();
    void dump(in DumperConfig config, in ParcelFileDescriptor fileDescriptor);
}