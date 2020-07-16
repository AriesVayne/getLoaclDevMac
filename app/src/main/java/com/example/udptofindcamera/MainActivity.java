package com.example.udptofindcamera;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    TextView tv_main_result = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv_main_result = findViewById(R.id.tv_main_result);

        //首先向所处网段的所有设备发送一遍数据包
        sendDataToLoacl();
        //延迟3秒后读取本机arp缓存表内容
        readArpLoop();

//        tryToSendUdp();
//        IpScanner类暂时用不到
//        IpScanner ipScanner = new IpScanner();
//        ipScanner.setOnScanListener(new IpScanner.OnScanListener() {
//            @Override
//            public void scan(Map<String, String> resultMap) {
//                Log.e("Scanner Results:", resultMap.toString());
//            }
//        });
//        ipScanner.startScan();
    }

    private void sendDataToLoacl() {
        //局域网内存在的ip集合
        final List<String> ipList = new ArrayList<>();
        final Map<String, String> map = new HashMap<>();

        //获取本机所在的局域网地址
        String hostIP = getHostIP();
        int lastIndexOf = hostIP.lastIndexOf(".");
        final String substring = hostIP.substring(0, lastIndexOf + 1);
        //创建线程池
        //        final ExecutorService fixedThreadPool = Executors.newFixedThreadPool(20);
        new Thread(new Runnable() {
            @Override
            public void run() {
                DatagramPacket dp = new DatagramPacket(new byte[0], 0, 0);
                DatagramSocket socket;
                try {
                    socket = new DatagramSocket();
                    int position = 2;
                    while (position < 255) {
                        Log.e("Scanner ", "run: udp-" + substring + position);
                        dp.setAddress(InetAddress.getByName(substring + String.valueOf(position)));
                        socket.send(dp);
                        position++;
                        if (position == 125) {//分两段掉包，一次性发的话，达到236左右，会耗时3秒左右再往下发
                            socket.close();
                            socket = new DatagramSocket();
                        }
                    }
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * 获取本机 ip地址
     *
     * @return
     */
    private String getHostIP() {
        String hostIp = null;
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    if (ia instanceof Inet6Address) {
                        continue;// skip ipv6
                    }
                    String ip = ia.getHostAddress();
                    if (!"127.0.0.1".equals(ip)) {
                        hostIp = ia.getHostAddress();
                        break;
                    }
                }
            }
        } catch (SocketException e) {
            Log.i("kalshen", "SocketException");
            e.printStackTrace();
        }
        return hostIp;
    }

    /**
     * 每隔8秒读取一下本机arp缓存表内容
     */
    private void readArpLoop() {
        readArp();
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                readArpLoop();
            }
        }, 8000);
    }

    private void readArp() {
        try {
            BufferedReader br = new BufferedReader(
                    new FileReader("/proc/net/arp"));
            String line = "";
            String ip = "";
            String flag = "";
            String mac = "";
            tv_main_result.setText("");
            if (br.readLine() == null) {
                Log.e("scanner", "readArp: null");
            }
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.length() < 63) continue;
                if (line.toUpperCase(Locale.US).contains("IP")) continue;
                ip = line.substring(0, 17).trim();
                flag = line.substring(29, 32).trim();
                mac = line.substring(41, 63).trim();
                if (mac.contains("00:00:00:00:00:00")) continue;
                Log.e("scanner", "readArp: mac= " + mac + " ; ip= " + ip + " ;flag= " + flag);
                tv_main_result.append("\nip:" + ip + "\tmac:" + mac);
            }
            br.close();
        } catch (Exception ignored) {
        }
    }

    private void tryToSendUdp() {
        try {
            final DatagramSocket detectSocket = new DatagramSocket(32333);
            int portRecXiaoMi = 32761;
            int portRecHaiKang = 37020;
            int portSsdp = 1900;

            byte[] dataXiaoMi = new byte[]{
                    0x6e, 0x4c, (byte) 0x9d, (byte) 0x8c, 0x40, (byte) 0xd1, 0x40, (byte) 0xda, 0x2d, 0x2d, 0x68, 0x2d, 0x00,
                    (byte) 0xe7, (byte) 0xca, (byte) 0xda, 0x6e, 0x2e, (byte) 0x8d, (byte) 0x8c, 0x40, (byte) 0xd0, 0x40, (byte) 0xca,
                    0x2d, 0x6d, 0x28, 0x0c, 0x40, (byte) 0xe4, (byte) 0xca, (byte) 0xd8, 0x6e, 0x2e, (byte) 0x8d, (byte) 0x8c, 0x40,
                    (byte) 0xd0, 0x40, (byte) 0xca, 0x2d, 0x6d, 0x28, 0x0c, 0x40, (byte) 0xe4, (byte) 0xca, (byte) 0xd8, 0x6e, 0x2e,
                    (byte) 0x8d, (byte) 0x8c, 0x40, (byte) 0xd0, 0x40, (byte) 0xca, 0x2d, 0x6c, 0x28, 0x0c, 0x50, (byte) 0xe4,
                    (byte) 0xfa, (byte) 0x8a, 0x61, 0x72, 0x43, 0x68};

            final byte[] finalDataSend = dataXiaoMi;
            final InetAddress finalHostAddress = InetAddress.getByName("255.255.255.255");
            final int finalTarRecPort = portRecXiaoMi;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (true) {
                        try {
                            Thread.sleep(3000);
                            DatagramPacket out = new DatagramPacket(finalDataSend,
                                    finalDataSend.length, finalHostAddress, finalTarRecPort);
                            detectSocket.send(out);
                            System.out.println("\n发送" + bytesToHexString(finalDataSend));
                        } catch (IOException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        while (true) {
                            byte[] buf = new byte[1024];
                            DatagramPacket packet = new DatagramPacket(buf, buf.length);
                            detectSocket.receive(packet);
                            byte[] dataArray = packet.getData();
                            String rcvd = "Received from:" + packet.getSocketAddress()
                                    + "\nLength:" + dataArray.length
                                    + "\nHostName:" + packet.getAddress().getHostName()
                                    + "\nHostAddress:" + packet.getAddress().getHostAddress()
                                    + "\nData:" + bytesToHexString(dataArray);
                            NetworkInterface ne = NetworkInterface.getByInetAddress(InetAddress.getByName(packet.getAddress().getHostAddress()));
                            if (null != ne) {
                                byte[] mac = ne.getHardwareAddress();
                                System.out.println("mac is:" + bytesToHexString(mac));
                            } else {
                                System.out.println("mac 's ne is null");
                            }
                            System.out.println(rcvd);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();

        } catch (UnknownHostException | SocketException e) {
            e.printStackTrace();
        }
    }

    public static String bytesToHexString(byte[] src) {
        StringBuilder stringBuilder = new StringBuilder("");
        if (src == null || src.length <= 0) {
            return "";
        }
        for (byte aSrc : src) {
            int v = aSrc & 0xFF;
            String hv = Integer.toHexString(v);
            if (hv.length() < 2) {
                stringBuilder.append(0);
            }
            stringBuilder.append(hv.toUpperCase()).append("");
        }
        return stringBuilder.toString();
    }
}
