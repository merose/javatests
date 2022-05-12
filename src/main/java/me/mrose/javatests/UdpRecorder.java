package me.mrose.javatests;

import com.github.rvesse.airline.HelpOption;
import com.github.rvesse.airline.SingleCommand;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.File;
import com.github.rvesse.airline.annotations.restrictions.Port;
import com.github.rvesse.airline.annotations.restrictions.PortType;
import com.github.rvesse.airline.parser.errors.ParseException;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import javax.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Command(name = "udprecord", description = "Proxy UDP communication and record datagrams")
public class UdpRecorder {

    private static final Logger log = LoggerFactory.getLogger(UdpRecorder.class);

    @Inject
    private HelpOption<UdpRecorder> help;

    @Option(name = { "-i", "--in-port" }, description = "The input port to proxy")
    @Port(acceptablePorts = { PortType.ANY })
    private int inPort = -1;

    @Option(name = { "--host" }, description = "The host to forward to")
    private String host = "localhost";

    @Option(name = { "-o", "--out-port" }, description = "The output port to forward to")
    @Port(acceptablePorts = { PortType.ANY })
    private int outPort = -1;

    @Option(name = { "-f", "--file" }, description = "The file where to record datagrams")
    @File(readable = false, writable = true)
    private String file = null;

    public static void main(String[] args) {
        log.info("Starting main()");
        SingleCommand<UdpRecorder> parser = SingleCommand.singleCommand(UdpRecorder.class);
        try {
            UdpRecorder cmd = parser.parse(args);
            cmd.run();
        } catch (ParseException e) {
            System.out.println(e.toString());
            System.out.println("Use -h to show help information");
        } catch (SocketException e) {
            log.error("Cannot create socket", e);
            e.printStackTrace();
        }
    }

    private void run() throws SocketException {
        if (help.showHelpIfRequested()) {
            return;
        }

        log.info("Running with inPort={} outPort={} file={}", inPort, outPort, file);
        try (DatagramSocket in = new DatagramSocket(inPort);
                DatagramSocket out = new DatagramSocket();
                OutputStream f = new FileOutputStream(file)) {

            InetAddress addr = InetAddress.getByName(host);
            int packetCount = 0;
            for (;;) {
                byte[] buf = new byte[65536];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                in.receive(packet);
                f.write(packet.getData(), 0, packet.getLength());
                f.flush();
                DatagramPacket outPacket = new DatagramPacket(packet.getData(), packet.getLength(), addr, outPort);
                out.send(outPacket);
                ++packetCount;
                if (packetCount % 1000 == 0) {
                    log.info("Received {} packets", packetCount);
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("Cannot write to file " + file);
        } catch (IOException e) {
            System.out.println("Exception writing to file " + file);
            e.printStackTrace();
        }
    }

}
