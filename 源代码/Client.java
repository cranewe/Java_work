import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.io.BufferedReader;
import java.time.format.DateTimeFormatter;

public class Client extends JFrame implements ActionListener, WindowListener {
    public static final String _specialMark = "@XdY#";
    public static final String _endMark = _specialMark + "endChatting";
    public static final String _duplicationName = _specialMark + "duplicationName";
    public static final String _kickout = _specialMark + "kickout";
    public static final int _basicTimeGap = 25;

    boolean flagContinue = false;
    Sender sender = null;
    Receiver receiver = null;
    Socket socket = null;
    PrintWriter clientToServer = null;
    BufferedReader serverToClient = null;
    String messageToSend = null;

    static DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    JTextField fieldIP = new JTextField();
    JTextField fieldPort = new JTextField();
    JTextField fieldNickname = new JTextField();
    JButton buttonLogin = new JButton("进入聊天室");
    JButton buttonLogout = new JButton("退出聊天室");
    JTextArea areaContent = new JTextArea();
    JTextArea areaMessage = new JTextArea(5, 20);
    JButton buttonSend = new JButton("发送");
    public Client() {
        initGUI();
        initEvents();
    }

    private void initGUI() {
        setTitle("全民聊天室——计科221张宸玮制作");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); 
        setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new FlowLayout());
        topPanel.add(new JLabel("IP: "));
        topPanel.add(fieldIP);
        fieldIP.setColumns(20);

        topPanel.add(new JLabel("端口: "));
        topPanel.add(fieldPort);
        fieldPort.setColumns(20);

        topPanel.add(new JLabel("昵称: "));
        topPanel.add(fieldNickname);
        fieldNickname.setColumns(20);

        topPanel.add(buttonLogin);
        topPanel.add(buttonLogout);
        add(topPanel, BorderLayout.NORTH);

        areaContent.setEditable(false);
        areaContent.setRows(30);
        JScrollPane scrollPane = new JScrollPane(areaContent);
        scrollPane.setPreferredSize(new Dimension(500, 600));
        add(scrollPane, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.add(areaMessage, BorderLayout.CENTER);
        bottomPanel.add(buttonSend, BorderLayout.EAST);
        add(bottomPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
        buttonLogout.setEnabled(false);
        buttonSend.setEnabled(false);

        setVisible(true);
    }

    private void initEvents() {
        buttonLogin.addActionListener(this);
        buttonLogout.addActionListener(this);
        buttonSend.addActionListener(this);
        addWindowListener(this);
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowIconified(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
    }

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        flagContinue = false;
        try {
            clientToServer.close();
            serverToClient.close();
            socket.close();
            messageToSend = _endMark;
            Thread.sleep(_basicTimeGap * 10);
        } catch (IOException ioe) {           
        } catch (InterruptedException ie) {
        }
   }

    @Override
    public void actionPerformed(ActionEvent ae) {
        Object source = ae.getSource();
        if (source == buttonLogin) {
            String ip = fieldIP.getText().trim();
            int port = Integer.parseInt(fieldPort.getText().trim());
            String nickname = fieldNickname.getText().trim();
            if(nickname.length()==0 || ip.length()==0) 
            {
                JOptionPane.showMessageDialog(this, "您必须登录后方可进入聊天室！");
            }
            else{
                login(ip, port, nickname);
            }
        } else if (source == buttonLogout) {
            String nickname = fieldNickname.getText().trim();
            logout(nickname);
        } else if (source == buttonSend) {
            messageToSend = areaMessage.getText().trim();
            if(!messageToSend.isEmpty())
            {
                areaMessage.setText(null);
                clientToServer.println(messageToSend);
            }
            else{
                JOptionPane.showMessageDialog(this,"警告！您不能发送空白内容");
        }
        }
    }

    private void login(String ip, int port, String nickname) {
        try {
            socket = new Socket(ip, port);
            clientToServer = new PrintWriter(socket.getOutputStream(), true);
            serverToClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            sender = new Sender(this);
            receiver = new Receiver(this);
            sender.start();
            receiver.start();
            clientToServer.println(nickname);
            flagContinue = true;
            buttonLogin.setEnabled(false);
            buttonLogout.setEnabled(true);
            buttonSend.setEnabled(true);
            JOptionPane.showMessageDialog(this, "已经与聊天室服务器建立连接!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void logout(String nickname) {
            flagContinue = false;
            clientToServer.println(_endMark);
            clientToServer.flush(); 
            messageToSend=null;
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            buttonLogin.setEnabled(true);
            buttonLogout.setEnabled(false);
            buttonSend.setEnabled(false);
            areaContent.append(nickname+"已退出聊天室\n");
            Log.append(nickname+"已退出聊天室\n");
    }


    public class Sender extends Thread {
        Client client = null;

        public Sender(Client client) {
            this.client = client;
        }

        @Override
        public void run() {
                while (client.flagContinue) {
                    if (client.messageToSend != null) {
                        client.clientToServer.println(client.messageToSend);
                        client.clientToServer.flush();
                        client.messageToSend = null;
                    }
            } 
        }
    }

    public class Receiver extends Thread {
        Client client = null;

        public Receiver(Client client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                String receivedMessage;
                while ((receivedMessage = client.serverToClient.readLine()) != null) {
                  client.areaContent.append(receivedMessage+"\n");
                }
            } catch (IOException e) {
                System.err.println("Receiver 线程异常: " + e.getMessage());
            }
        }
    }

    public static void main(String[] args) {
        new Client();
    }
}
