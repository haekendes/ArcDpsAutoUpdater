package com.haekendes.arcdpsautoupdater;

import com.sun.jna.platform.win32.Advapi32Util;
import com.sun.jna.platform.win32.WinReg;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *
 * @author Robin Christ
 */
public class RegistryWriter {

    private static Path CURRENT_PATH = Paths.get(System.getProperty("user.dir") + "/ArcDpsAutoUpdater.exe t");
    private static String NAME = "Software\\Microsoft\\Windows\\CurrentVersion\\Run";
    private static String VALUE_NAME = "ArcDpsAutoUpdater";

    public static void addToStartup() {
        Advapi32Util.registrySetStringValue(WinReg.HKEY_CURRENT_USER, NAME, VALUE_NAME, CURRENT_PATH.toString());

        if(Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, NAME, VALUE_NAME)) {
            System.out.println("\nRegistry startup value points to the following path:");
            System.out.println(Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, NAME, VALUE_NAME));
            System.out.println("Successfully added to startup");
        } else {
            System.err.println("Failed to add startup value to registry");
        }
    }

    public static void removeFromStartup() {
        Advapi32Util.registryDeleteValue(WinReg.HKEY_CURRENT_USER, NAME, VALUE_NAME);
        if(!Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, NAME, VALUE_NAME)) {
            System.out.println("\nRegistry startup value removed successfully");
        } else {
            System.err.println("Removal of registry startup value failed!");
        }
    }

    /**
     * Case 1: Reg Value exists and points to the right path.
     * Case 2: Reg Value exists, but path is incorrect.
     * Case 3: Reg Value doesn't exist.
     * @return
     */
    public static int checkForSetup() {
        if(Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, NAME, VALUE_NAME)) {
            Path value = Paths.get(Advapi32Util.registryGetStringValue(WinReg.HKEY_CURRENT_USER, NAME, VALUE_NAME));
            if(value.equals(CURRENT_PATH)) {
                return 1;
            } else {
                return 2;
            }
        } else {
            return 3;
        }
    }
}
