/*
 * This is free software; you can redistribute it and/or modify it under
 * the terms of version 3 of the GNU General Public License as published
 * by the Free Software Foundation.
 *
 * Copyright 2017
 */
package TCPServer;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Scanner;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Jadnostavan TCP server program koji sluša na nekom portu. Prilikom svake
 * konekcije otvara se novi thread koji samo radi echo klijentovog unosa.
 *
 * Server prima i salje stringove.
 *
 * Port se može postaviti preko argumenata komandne linije prilikom pokrejanja
 * programa: npr: pokretanje servera na portu 1234:
 *
 * @author tin
 */
public class SimpleTCPServer {

    public static void main(String[] args) throws Exception {

        final int SOCKET_PORT;

        if (args.length > 0) {
            SOCKET_PORT = Integer.parseInt(args[0]);
        } else {
            SOCKET_PORT = 8896;
        }

        try {
            int i = 1;
            ServerSocket s = new ServerSocket(SOCKET_PORT);
            System.out.printf("Server started: %s, port %d, timeout %d\n",
                    InetAddress.getLocalHost(),
                    s.getLocalPort(),
                    s.getSoTimeout());

            while (true) {

//                Connection conn =  serverBase.ConnectToServerBase.getConnection(); 
                Socket incoming = s.accept();
                String name = String.format("konekcija %d", i);

                System.out.printf("Otvorena %s\n", name);

                Runnable r = new ThreadedEchoHandler(incoming, name);
                Thread t = new Thread(r);

                t.start();
                i++;
            }
        } catch (IOException e) {
            System.err.println("Socket greska: " + e);
        }

    }

}

/**
 * hendla Thread . klasa SimpleTCPServer radi objekt nad ovom klasom.
 *
 * @author tin
 */
class ThreadedEchoHandler implements Runnable {

    private Socket incoming;
    private String name;

    public ThreadedEchoHandler(Socket i, String connName) {
        incoming = i;
        name = connName;
    }

    @Override
    public void run() {

        try {
            try {
                Scanner in = new Scanner(incoming.getInputStream());
                PrintWriter out = new PrintWriter(incoming.getOutputStream(), true);

                out.printf("Spojeni ste na %s, port %d, %s\n",
                        InetAddress.getLocalHost(),
                        incoming.getLocalPort(),
                        name);

                // echo client input
                boolean done = false;
                String brojTelefonaPosiljatelja = "";
                while (!done && in.hasNextLine()) {

                    // line !!!
                    String line = in.nextLine();
                    System.out.printf("%s: %s\n", name, line);

                    if (line.contains(" ")) {

                        ////////---aktivacija kartice---///////
                        //////////////////////////////////ZA CITANJE PIN-a, PUK-a i TEL.BROJ-a 
                        /////////////////////////////////KAKO BI POSPREMILI KARTICU U BAZU I TIME JE KATIVIRALI
                        int index1 = line.indexOf(" ");
                        int index2 = line.lastIndexOf(" ");
                        int pin = Integer.parseInt(line.substring(0, index1));
                        int puk = Integer.parseInt(line.substring(index1 + 1, index2));
                        brojTelefonaPosiljatelja = line.substring(index2 + 1);

                        if (brojTelefonaPosiljatelja.length() >= 3) {
                            insertSimDoc(pin, puk, brojTelefonaPosiljatelja);
                            int fb = findSerijskiBroj(brojTelefonaPosiljatelja);
                            out.println("Uspjesno aktiviranje kartice");

                            out.printf("SerijskiBroj: %d\n", fb);
                            System.out.println("uspjesna aktivacija. uneseno u bazu [ " + fb + " ]");

                        } else {
                            out.println("neuspjesno aktiviranje kartice!");
                            System.out.println("neuspjesno aktiviranje kartice!");
                        }
                        ////////---end : aktivacija kartice---///////
                    } else if (line.length() == 4) {
                        //////---logiranje kartice----/////
                        int pin = Integer.parseInt(line);
                        int fb = findSerijskiBrojPostojeceKartice(pin);
                        out.println("Ulogirani ste :) ");
                        out.printf("SerijskiBroj: %d\n", fb);
                        ////---end logiranje kartice ---/////
                        ///---poslan tel broj -> vraca 
                    } else if (line.length() >= 9) {
                        String brojTelefonaPrimatelja = line.trim();
                        //tu ide medota za provjeru postojeceg tel broja
                        String brojJeValjan = checkIfExistTelefonskiBroj(brojTelefonaPrimatelja);
                        System.out.println(brojJeValjan);

                        out.println(brojJeValjan);

                    } else if (line.length() <= 3) {
                        String serijskiBroj = line;
//                        System.out.println("tu sam $$$$ " + serijskiBroj);
                        String brojTelefona = vratiBrojTelefona(Integer.parseInt(serijskiBroj));
                        out.println(brojTelefona);
                        out.println(brojTelefona);
                        out.println(brojTelefona);
                    } else {
                        //dio koda za unos podataka u tablicu poruke:
                        //ovdje imamo PRIMATELJA (broj telefona primatelja)
                        //potrebno : POSILJATELJ 
                        //potrebno : TEXT
                    }

                    if (line.trim().equals("kraj")) {
                        done = true;
                    }
                }
            } catch (Exception ex) {
                Logger.getLogger(ThreadedEchoHandler.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                incoming.close();
                System.out.printf("%s closed.\n", name);
            }
        } catch (IOException e) {
            System.err.printf("%s greska: %s ", name, e);
        }

    }

    /**
     * metoda koja aktivira sim karticu klijenta. Posprema u tablicu simKartica
     * vrijednosti parametara i pridjeljuje joj serijski broj
     *
     * @param pin
     * @param puk
     * @param brojTelefona
     * @return false uukoliko nije pospremio u bazu .
     * @throws Exception
     */
    public static boolean insertSimDoc(int pin, int puk, String brojTelefona) throws Exception {
        Connection conn = serverBase.ConnectToServerBase.getConnection();
        try {
            String sql = "Insert into simKartica  ( pin , puk , brojTelefona ) values(?,?,?)";
            PreparedStatement pstm = conn.prepareStatement(sql);

            pstm.setInt(1, pin);
            pstm.setInt(2, puk);
            pstm.setString(3, brojTelefona);

            if (pstm.executeUpdate() == 1) {
                return true;
            }

        } catch (SQLException sqle) {

            System.out.println(sqle.getMessage());
        }

        return false;
    }

    /**
     * metoda vraca serijski broj klijentu nakon uspjesne aktivacije kartice
     *
     * @param brojTelefona
     * @return serijski broj
     * @throws Exception
     */
    public static int findSerijskiBroj(String brojTelefona) throws Exception {
        Connection conn = serverBase.ConnectToServerBase.getConnection();
        int serijskiBroj = 0;
        try {

            String sql = "Select serijskiBroj from simKartica where brojTelefona = ? ";
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setString(1, brojTelefona);

            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                serijskiBroj = rs.getInt("serijskiBroj");

            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return serijskiBroj;
    }

    /**
     * metoda vraca serijski broj prilikom logiranja kartice prema PIN-u
     *
     * @param pin
     * @return
     * @throws Exception
     */
    public static int findSerijskiBrojPostojeceKartice(int pin) throws Exception {
        Connection conn = serverBase.ConnectToServerBase.getConnection();
        int serijskiBroj = 0;
        try {

            String sql = "Select serijskiBroj from simKartica where pin = ? ";
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setInt(1, pin);

            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                serijskiBroj = rs.getInt("serijskiBroj");

            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return serijskiBroj;
    }

    /**
     * provjerava da li broj telefona primatelja postoji u simKartici (da li je
     * taj broj uopce aktiviran kako bi mogao primiti poruku)
     *
     * @param telefonskiBroj
     * @return
     * @throws Exception
     */
    public static String checkIfExistTelefonskiBroj(String brojTelefona) throws Exception {

        String tel = "";
        Connection conn = serverBase.ConnectToServerBase.getConnection();

        String sql = "Select brojTelefona  from simKartica where brojTelefona =  ? ";
        PreparedStatement pstm = conn.prepareStatement(sql);
        pstm.setString(1, brojTelefona);

        ResultSet rs = pstm.executeQuery();
        while (rs.next()) {
            System.out.println("usao!!!");
            tel = rs.getString("brojTelefona");
        }

        System.out.println("izasao");
        return tel;
    }

    /**
     * uzima sa outputa serijski broj. vraca broj telefona na output.
     *
     * @param serijskiBroj
     * @return
     * @throws Exception
     */
    public static String vratiBrojTelefona(int serijskiBroj) throws Exception {
        Connection conn = serverBase.ConnectToServerBase.getConnection();
        String brojTelefona = "";
        try {

            String sql = "Select brojTelefona  from simKartica where serijskiBroj = ? ";
            PreparedStatement pstm = conn.prepareStatement(sql);
            pstm.setInt(1, serijskiBroj);

            ResultSet rs = pstm.executeQuery();
            if (rs.next()) {
                brojTelefona = rs.getString("brojTelefona");

            }

        } catch (SQLException ex) {
            System.out.println(ex.getMessage());
        }
        return brojTelefona;
    }
}