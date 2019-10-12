/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.haekendes.arcdpsautoupdater;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

/**
 *
 * @author Robin Christ
 */
public class ArcDpsAutoUpdater {

    private static final String D3D9 = "d3d9.dll";
    private static final String WEBSITE = "https://www.deltaconnected.com/arcdps/x64/";

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equals("t")) {
            System.out.println("Check for ArcDps updates...");
            try {
                Thread.sleep(5000); //wait 5 seconds for system to fully boot
            } catch (InterruptedException ex) {
                Logger.getLogger(ArcDpsAutoUpdater.class.getName()).log(Level.SEVERE, null, ex);
            }

            checkForUpdates();
        } else {
            choice();
        }
    }

    private static void choice() {
        System.out.println("Press 1, 2, 3 or 4 to execute one of the steps below:");
        System.out.println("[1] Add/Remove the updater to system startup.");
        System.out.println("[2] Check for ArcDps Updates.");
        System.out.println("[3] Download ArcDps");
        System.out.println("[4] Delete ArcDps");

        Scanner scanner = new Scanner(System.in);
        String s = scanner.next().toLowerCase();
        if (s.equals("1")) {
            startupDialogue();
        }
        if (s.equals("2")) {
            checkForUpdates();
            enterClose();
        }
        if (s.equals("3")) {
            downloadArcDps();
            enterClose();
        }
        if (s.equals("4")) {
            deleteArcDps();
            enterClose();
        }
    }

    private static void deleteArcDps() {
        try {
            Files.deleteIfExists(Paths.get(D3D9));
            Files.deleteIfExists(Paths.get("lastUpdate.txt"));

            if (!Files.exists(Paths.get(D3D9)) && !Files.exists(Paths.get("lastUpdate.txt"))) {
                System.out.println("ArcDps deleted successfully");
            } else {
                System.out.println("Couldn't delete all ArcDps files.");
                System.out.println("Please close all open/used files like text editors, also close GW2, then try again");
            }
        } catch (IOException ex) {
            Logger.getLogger(ArcDpsAutoUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void startupDialogue() {
        int check = RegistryWriter.checkForSetup();

        switch (check) {
            case 1:
                askToRemove();
                break;
            case 2:
                System.out.println("ERROR: Startup path is wrong");
                System.out.println("Correcting path...");
                RegistryWriter.removeFromStartup();
                RegistryWriter.addToStartup();
                System.out.println("Path is now correct");
                askToRemove();
                break;
            case 3:
                System.out.println("Do you want to set this programm to automatically check for ArcDps updates\n"
                        + "upon system startup? Y/N: ");
                Scanner scanner = new Scanner(System.in);
                if (scanner.next().toLowerCase().equals("y")) {
                    RegistryWriter.addToStartup();
                }

                break;
            default:
                break;
        }

        if (!Files.exists(Paths.get(D3D9))) {
            System.out.println("ArcDps is not installed right now");
        }

        enterClose();
    }

    private static void askToRemove() {
        System.out.println("This program is set to check for ArcDps updates on system startup.");
        System.out.println("Do you want to remove it from startup? Y/N: ");

        Scanner scanner = new Scanner(System.in);
        if (scanner.next().toLowerCase().equals("y")) {
            RegistryWriter.removeFromStartup();
        }

    }

    private static void checkForUpdates() {
        LocalDate latestUpdate = getLastModifiedDate();

        if (Files.exists(Paths.get(D3D9))) {
            LocalDate localDate = getLocalModifiedDate(Paths.get(D3D9));

            if (latestUpdate.compareTo(localDate) >= 0) {
                System.out.println("ArcDps update is required");

                downloadArcDps();

                enterClose();
            } else if (latestUpdate.compareTo(localDate) < 0) {
                System.out.println("ArcDPS should be up to date. "
                        + "\nIf that's not the case, please download the d3d9.dll manually at "
                        + "\n" + WEBSITE + "\n");
            }
        } else if (Files.exists(Paths.get("lastUpdate.txt"))) {
            LocalDate localDate = getLastUpdateTxt();

            if (latestUpdate.compareTo(localDate) >= 0) {
                System.out.println("ArcDps update is available");
                downloadArcDps();

                enterClose();
            } else if (latestUpdate.compareTo(localDate) < 0) {
                System.out.println("You probably have removed the d3d9.dll as it is not working anymore after the latest GW2 patch.");
                System.out.println("If you have automatic updates enabled, the new ArcDps version will be downloaded when available.");
            }
        } else {
            System.out.println("ArcDps is not installed atm, also can't find latest update time");
            System.out.println("Do you want to download ArcDps now? Y/N: ");

            Scanner scanner = new Scanner(System.in);
            if (scanner.next().toLowerCase().equals("y")) {
                downloadArcDps();
            }

        }
    }

    private static LocalDate getLastUpdateTxt() {
        LocalDate date = null;

        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader("lastUpdate.txt"));
            String dS = br.readLine();
            DateTimeFormatter d = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            date = LocalDate.parse(dS);
            br.close();

            System.out.println("Last update was at:                       " + date.toString());
        } catch (IOException ex) {
            Logger.getLogger(ArcDpsAutoUpdater.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ex) {
                Logger.getLogger(ArcDpsAutoUpdater.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

        return date;
    }

    private static LocalDate getLocalModifiedDate(Path localPath) {
        try {

            long millis = Files.getLastModifiedTime(localPath).toMillis();
            LocalDate date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();

            System.out.println("Local d3d9.dll has last been modified at: " + date.toString());

            return date;
        } catch (IOException ex) {
            Logger.getLogger(ArcDpsAutoUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    /**
     * Can return null.
     *
     * @return
     */
    private static LocalDate getLastModifiedDate() {
        try {
            Document doc = Jsoup.connect(WEBSITE).get();

            List<String> list = doc.getAllElements().eachText();
            for (String s : list) {
                int start = s.indexOf(D3D9);
                int end = s.indexOf('K', start) + 1;
                String sub = s.substring(start, end);
                String trimmed = sub.replace(D3D9, "").trim().substring(0, 10);

                LocalDate date = LocalDate.parse(trimmed);

                System.out.println("Date of the latest d3d9.dll release:      " + date.toString());

                return date;
            }
        } catch (IOException ex) {
            Logger.getLogger(ArcDpsAutoUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }

    private static void downloadArcDps() {
        try {
            System.out.println("Starting download procedure:");
            System.out.println("Trying to access " + WEBSITE + D3D9);

            URL url = new URL(WEBSITE + D3D9);
            HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.setRequestMethod("HEAD");
            long fileLength = httpConnection.getContentLengthLong();

            System.out.println("Target file size: " + fileLength / 1024 + "kb");
            System.out.println("Attempting download...");

            BufferedInputStream in = new BufferedInputStream(url.openStream());
            Path path = Paths.get(D3D9);
            Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);

            if (Files.exists(path) && Files.size(path) == fileLength) {
                System.out.println("Download complete");

                long millis = Files.getLastModifiedTime(Paths.get(D3D9)).toMillis();
                LocalDate date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate();

                PrintWriter out = new PrintWriter("lastUpdate.txt");
                out.print(date);
                out.close();
            } else {
                System.err.println("Download failed\n");
            }
        } catch (MalformedURLException e) {
            System.err.print("Forming URL failed");
        } catch (IOException e) {
            System.err.println("Opening connection failed");
        }
    }

    public static void enterClose() {
        System.out.println("Press Enter to close...");
        try {
            System.in.read();
            System.exit(0);
        } catch (IOException ex) {
            Logger.getLogger(ArcDpsAutoUpdater.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
