package org.example.project.windowsNative;

import com.sun.jna.Library;
import com.sun.jna.Native;


public interface win32CredentialsManager extends Library {

    val INSTANCE = Native.load("Advapi32", Advapi32::class.java) as Advapi32

    fun CredWrite(credential: Credential, flags: Int): Boolean
    fun CredRead(targetName: String, credType: Int, flags: Int, credential: PointerByReference): Boolean
    fun CredDelete(targetName: String, credType: Int, flags: Int): Boolean
    fun CredFree(buffer: Pointer)

}
